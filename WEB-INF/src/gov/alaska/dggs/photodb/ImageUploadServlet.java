package gov.alaska.dggs.photodb;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

import mjson.Json;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.lang.GeoLocation;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.ByteBufferBackedInputStream;


public class ImageUploadServlet extends HttpServlet
{
	private static final int FILE_SIZE_LIMIT = 52428800;
  private static final int TARGET_WIDTH = 256;

	private String SQL =
		"INSERT INTO image (" +
			"image, thumbnail, filename, taken, " +
			"credit, description, metadata, geog " +
		") VALUES (" +
			"?, ?, ?, ?, " +
			"?, ?, " + 
			"(regexp_replace(?::TEXT, '\\\\u0000', '', 'g'))::JSONB, " +
			"ST_Transform(ST_SetSRID(ST_MakePoint(?,?), ?), 4326)::GEOGRAPHY" +
		")";

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		List<Integer> ids = new ArrayList<Integer>();
		List<String> errors = new ArrayList<String>();

		boolean usejson = false;

		try (SqlSession sess = PhotoDBFactory.openSession()) {
			Connection conn = sess.getConnection();
			if(ServletFileUpload.isMultipartContent(request)){

				ServletFileUpload upload = new ServletFileUpload();
				FileItemIterator items = upload.getItemIterator(request);
				try (
					PreparedStatement ps = conn.prepareStatement(
						SQL, PreparedStatement.RETURN_GENERATED_KEYS
					)
				){
					while(items.hasNext()){
						ByteBuffer buffer = ByteBuffer.allocateDirect(FILE_SIZE_LIMIT);
						FileItemStream item = items.next();

						try (InputStream istream = item.openStream()){
							byte[] bf = new byte[4096];
							buffer.clear();
							for(int i = 0; (i = istream.read(bf, 0, 4096)) > 0; buffer.put(bf, 0, i));
							buffer.flip();
						} catch(BufferOverflowException ex){
							errors.add(
								(item.isFormField() ? item.getFieldName() : item.getName()) +
								", too big (over " + String.valueOf(FILE_SIZE_LIMIT) +
								" bytes)"
							);
							continue;
						} catch(Exception ex){
							errors.add(
								(item.isFormField() ? item.getFieldName() : item.getName()) +
								", unknown read error"
							);
							continue;
						} 

						// Handle form fields differently
						if(item.isFormField()){
							if("format".equals(item.getFieldName()) && buffer.remaining() < 25){
								byte[] bf = new byte[buffer.remaining()];
								buffer.get(bf, 0, bf.length);

								String fmt = new String(bf, "utf-8");
								if("json".equals(fmt)) usejson = true;
							}
							continue;
						}

						ps.clearParameters();
						
						// Initialize the parameters that'll go into the database
						Map metadata = new HashMap();
						String filename = null;
						Date file_date = null;
						Double lon = null, lat = null;
						Integer srid = null;
						byte[] thumbnail = null;
						String credit = null;
						String description = null;

						// Deal with the filename
						try {
							filename = item.getName();
							String mime = context.getMimeType(filename.toLowerCase());

							// Check mime types, don't accept non-images
							if(mime == null || !mime.startsWith("image/")){
								errors.add(filename + ", is not a supported image (MIME)");
								continue;
							}
						} catch(Exception exe){
							// If fetching the filename or the mime type
							// generates an error, just skip over the file.
							errors.add("Invalid filename");
							continue;
						}

						ByteBufferBackedInputStream bbis = new ByteBufferBackedInputStream(buffer);

						// Deal with the metadata
						try {
							Metadata mdreader = ImageMetadataReader.readMetadata(bbis);

							// Scan over all the directories, building
							// an object for JSON encoding to load into the database
							for(Directory directory : mdreader.getDirectories()){
								String dn = directory.getName();
								if(dn == null) continue;
								if(dn.contains("Thumbnail")) continue;

								Map hm = new HashMap();
								for(Tag tag : directory.getTags()){
									String tn = tag.getTagName();
									String td = tag.getDescription();

									if(tn == null || td == null) continue;
									if(tn.startsWith("Unknown tag")) continue;

									td = td.trim();
									if(td.length() == 0) continue;
									hm.put(tn, td);
								}
								if(!hm.isEmpty()) metadata.put(directory.getName(), hm);
							}

							// Try to pull the date and the description from the top
							// EXIF directory
							ExifIFD0Directory ifd = mdreader.getDirectory(ExifIFD0Directory.class);
							if(ifd != null){
								file_date = ifd.getDate(ExifIFD0Directory.TAG_DATETIME);
								description = ifd.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
								if(description != null){
									description = description.trim();
									if(description.length() == 0) description = null;
								}
							}

							//  Try an alternate tag for the date/time if we don't have it
							if(file_date == null){
								ExifSubIFDDirectory subif = mdreader.getDirectory(ExifSubIFDDirectory.class);
								if(subif != null){
									file_date = subif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
								}
							}

							// More alternates, try the IPTC directory
							// for both the date, credit and description
							IptcDirectory iptc = mdreader.getDirectory(IptcDirectory.class);
							if(iptc != null){
								// Fallback: Read date/time from IPTC
								if(file_date == null){
									file_date = iptc.getDate(IptcDirectory.TAG_DATE_CREATED);
								}

								credit = iptc.getString(IptcDirectory.TAG_CREDIT);
								if(credit != null){
									credit = credit.trim();
									if(credit.length() == 0) credit = null;
								}

								// Fallback: Read description from IPTC
								if(description == null){
									description = iptc.getString(IptcDirectory.TAG_CAPTION);
									if(description != null){
										description = description.trim();
										if(description.length() == 0) description = null;
									}
								}
							}

							// Try reading the GPS directory for the spatial data
							GpsDirectory gps = mdreader.getDirectory(GpsDirectory.class);
							if(gps != null){
								GeoLocation gl = gps.getGeoLocation();
								if(gl != null){
									lon = gl.getLongitude();
									lat = gl.getLatitude();

									metadata.put("geolocation", gl.toDMSString());
								}

								String datum = gps.getString(GpsDirectory.TAG_MAP_DATUM);
								if(datum != null && datum.contains("NAD 27")) srid = 4267;
								else if(datum != null && datum.contains("NAD 83")) srid = 4269;
								// Default to WGS 84
								else srid = 4326;

								if(datum != null && datum.trim().length() > 0){
									metadata.put("datum", datum.trim());
								}
							}
						} catch(Exception exe){
							// Just ignore it if the metadata reading fails
						}

						buffer.rewind();

						// Try generating a thumbnail, if needed
						try {
							BufferedImage source = ImageIO.read(bbis);

							if(source.getWidth() > TARGET_WIDTH){
								BufferedImage dest = Scalr.resize(
									source, Scalr.Method.AUTOMATIC,
									Scalr.Mode.FIT_TO_WIDTH, TARGET_WIDTH,
									Scalr.OP_ANTIALIAS
								);

								ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
								ImageIO.write(dest, "jpeg", bos);
								thumbnail = bos.toByteArray();
								bos.close();
							}
						} catch(Exception exe){
							// If the image scaling throws an exception, it's
							// probably because it's an invalid image, so just ignore
							// it.
							errors.add(filename + ", is not a supported image (ImageIO)");
							continue;
						}

						buffer.rewind();

						// Finally, run the insert
						try {
							int c = 1;
							ps.setBinaryStream(c++, bbis);

							if(thumbnail != null) ps.setBytes(c++, thumbnail);
							else ps.setNull(c++, Types.BINARY);

							ps.setString(c++, filename);

							if(file_date != null){
								ps.setDate(c++, new java.sql.Date(file_date.getTime()));
							} else ps.setNull(c++, Types.DATE);

							if(credit != null) ps.setString(c++, credit);
							else ps.setNull(c++, Types.VARCHAR);

							if(description != null) ps.setString(c++, description);
							else ps.setNull(c++, Types.VARCHAR);

							ps.setString(c++, Json.make(metadata).toString());

							if(lat != null && lon != null){
								ps.setBigDecimal(c++, new BigDecimal(lon));
								ps.setBigDecimal(c++, new BigDecimal(lat));
								ps.setInt(c++, srid);
							} else {
								ps.setNull(c++, Types.NUMERIC);
								ps.setNull(c++, Types.NUMERIC);
								ps.setNull(c++, Types.INTEGER);
							}
							ps.execute();
							conn.commit();

							try (ResultSet rs = ps.getGeneratedKeys()){
								if(rs.next()) ids.add(rs.getInt(1));
							}
						} catch(Exception exe){
							// File fails
							if(exe.getMessage().matches("(?is).*duplicate key.*")){
								errors.add(filename + ", already in database");
							} else {
								errors.add(filename + ", could not be added");
								exe.printStackTrace();
							}
							continue;
						}
					}
				}
			}

			if(usejson){
				Json json = Json.object();

				if(!errors.isEmpty()) json.set("errors", errors);
				if(!ids.isEmpty()) json.set("ids", ids);

				response.setContentType("application/json");
				response.getOutputStream().print(json.toString());
			} else {
				if(errors.isEmpty()){
					StringBuilder b = new StringBuilder();
					for(Integer id : ids){
						if(b.length() > 0) b.append(",");
						b.append(id);
					}
					response.sendRedirect("edit/" + b.toString());
				} else {
					response.setContentType("text/plain");
					StringBuilder b = new StringBuilder(
						"Errors occured during upload:"
					);
					for(String err : errors){
						b.append("\n\t");
						b.append(err);
					}
					response.getOutputStream().print(b.toString());
				}
			}
		} catch(Exception ex){
			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().print(ex.getMessage());
		}
	}
}
