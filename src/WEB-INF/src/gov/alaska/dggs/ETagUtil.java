package gov.alaska.dggs;

import java.security.MessageDigest;

// ETag utility class
public class ETagUtil {
	private static final char[] HEX_CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'a', 'b', 'c', 'd', 'e', 'f'
	};


	public static String tag(byte[] input)
	{
		StringBuilder sb = new StringBuilder(16);
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] sum = md.digest(input);

			for(byte b : sum){
				sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
				sb.append(HEX_CHARS[b & 0x0F]);
			}
		} catch(Exception ex){ }
		return sb.length() > 0 ? sb.toString() : null;
	}
}
