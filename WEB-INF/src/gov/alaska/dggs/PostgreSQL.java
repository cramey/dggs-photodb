package gov.alaska.dggs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


// Postgresql Utility Functions
public class PostgreSQL {
	public static boolean isObject(Connection conn, String name, String type)
	{
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT COUNT(*) FROM pg_class " +
			"WHERE relname = ? AND relkind = ?"
		)) {
			ps.setString(1, name);
			ps.setString(2, type);
			try (ResultSet rs = ps.executeQuery()) {
				if(rs.next() && rs.getInt(1) > 0) return true;
			}
		} catch(Exception ex){
			// Ignore execution errors
		}
		return false;
	}


	public static boolean isExtension(Connection conn, String extension)
	{
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT COUNT(*) FROM pg_extension " +
			"WHERE extname = ?"
		)) {
			ps.setString(1, extension);
			try (ResultSet rs = ps.executeQuery()) {
				if(rs.next() && rs.getInt(1) > 0) return true;
			}
		} catch(Exception ex){
			// Ignore execution errors
		}
		return false;
	}


	public static boolean isFunction(Connection conn, String func)
	{
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT COUNT(*) FROM pg_proc " +
			"WHERE proname = ?"
		)) {
			ps.setString(1, func);
			try (ResultSet rs = ps.executeQuery()) {
				if(rs.next() && rs.getInt(1) > 0) return true;
			}
		} catch(Exception ex){
			// Ignore execution errors
		}
		return false;
	}


	public static boolean isIndex(Connection conn, String idx)
	{
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT COUNT(*) FROM pg_indexes " +
			"WHERE indexname = ?"
		)) {
			ps.setString(1, idx);
			try (ResultSet rs = ps.executeQuery()) {
				if(rs.next() && rs.getInt(1) > 0) return true;
			}
		} catch(Exception ex){
			// Ignore execution errors
		}
		return false;
	}


	public static boolean isTrigger(Connection conn, String trigger)
	{
		try (PreparedStatement ps = conn.prepareStatement(
			"SELECT COUNT(*) FROM pg_trigger " +
			"WHERE tgname = ?"
		)) {
			ps.setString(1, trigger);
			try (ResultSet rs = ps.executeQuery()) {
				if(rs.next() && rs.getInt(1) > 0) return true;
			}
		} catch(Exception ex){
			// Ignore execution errors
		}
		return false;
	}
}
