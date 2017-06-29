package gov.alaska.dggs.photodb.api;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;


public class NationalDigitalCatalogXMLServlet extends HttpServlet
{
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }

	@SuppressWarnings("unchecked")
	public void doPostGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		SqlSession sess = PhotoDBFactory.openSession();
		try {
			List<Map> rows = sess.selectList(
				"gov.alaska.dggs.photodb.Image.getNDC"
			);

			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = factory.createXMLStreamWriter(
				response.getOutputStream(), "utf-8"
			);
			writer.writeStartDocument("utf-8", "1.0");
			writer.writeStartElement("samples");
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute(
				"http://www.w3.org/2001/XMLSchema-instance",
				"noNamespaceSchemaLocation",
				"http://data.usgs.gov/nggdpp/NGGDPPMetadataSample_v4.xsd"
			);
			for(Map row : rows){
				writer.writeStartElement("sample");

				writer.writeStartElement("collectionID");
				writer.writeCharacters(context.getInitParameter("collectionid"));
				writer.writeEndElement();

				writer.writeStartElement("title");
				writer.writeCharacters(row.get("summary").toString());
				writer.writeCharacters(" ID #");
				writer.writeCharacters(row.get("image_id").toString());
				writer.writeEndElement();

				writer.writeStartElement("datasetReferenceDate");
				writer.writeCharacters(row.get("modified").toString());
				writer.writeEndElement();

				writer.writeStartElement("abstract");
				writer.writeCharacters(row.get("description").toString());
				if(row.containsKey("credit")){
					writer.writeCharacters(" Taken by ");
					writer.writeCharacters(row.get("credit").toString());
					writer.writeCharacters(".");
				}
				writer.writeEndElement();

				writer.writeStartElement("supplementalInformation");
				writer.writeCharacters(
					"Image courtesy of Alaska Division of Geological and Geophysical " +
					"Surveys (ADGGS). Please cite the photographer and ADGGS when " +
					"using this image. Access to the original photo may or may not " +
					"be available. Please contact us during business hours to " +
					"determine whether we can facilitate your request."
				);
				writer.writeEndElement();

				writer.writeStartElement("alternateGeometry");
				writer.writeCharacters("The coordinates are represented in World Geodetic System 1984 (WGS84)");
				writer.writeEndElement();

				writer.writeStartElement("dates");
				writer.writeStartElement("date");
				writer.writeCharacters(row.get("taken").toString());
				writer.writeEndElement();
				writer.writeEndElement();

				writer.writeStartElement("datatype");
				writer.writeCharacters("Photograph");
				writer.writeEndElement();

				writer.writeStartElement("coordinates");
				writer.writeCharacters(row.get("longitude").toString());
				writer.writeCharacters(",");
				writer.writeCharacters(row.get("latitude").toString());
				writer.writeEndElement();

				writer.writeStartElement("onlineResource");
				writer.writeStartElement("resourceURL");
				writer.writeCharacters(context.getInitParameter("rooturl"));
				writer.writeCharacters("image/");
				writer.writeCharacters(row.get("image_id").toString());
				writer.writeEndElement();
				writer.writeEndElement();

				writer.writeStartElement("browseGraphic");
				writer.writeStartElement("resourceURL");
				writer.writeCharacters(context.getInitParameter("rooturl"));
				writer.writeCharacters("thumbnail/");
				writer.writeCharacters(row.get("image_id").toString());
				writer.writeEndElement();
				writer.writeEndElement();

				writer.writeEndElement();
			}
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.close();

		} catch(Exception ex){
			throw new ServletException(ex);
		} finally {
			sess.close();	
		}
	}
}
