package com.concordance.services.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQLUtil {

	public static Map<String, String> bases;
	
	private SQLUtil() {
		try {
			Class.forName("org.sqlite.JDBC");

			bases = new HashMap<>();
			bases.put("RVR1960", "D:\\Desarrollo\\databases\\rvr1960.sqlite");
			bases.put("NTV", "D:\\Desarrollo\\databases\\ntv.sqlite");
			bases.put("NVI", "D:\\Desarrollo\\databases\\nvi.sqlite");
			bases.put("TLA", "D:\\Desarrollo\\databases\\tla.sqlite");
			bases.put("DHH", "D:\\Desarrollo\\databases\\DHH.sqlite");
			bases.put("NBLA", "D:\\Desarrollo\\databases\\nbla.sqlite");
			bases.put("Patristica", "D:\\Desarrollo\\databases\\patristica.db");
			bases.put("Autores", "D:\\Desarrollo\\databases\\Autores.db");
			bases.put("Notas", "D:\\Desarrollo\\databases\\patristica.db");
			bases.put("Interlineal", "D:\\Desarrollo\\databases\\interlineal.sqlite");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static class LazyHolder {
		private static final SQLUtil INSTANCE = new SQLUtil();
	}
	
	public static SQLUtil getInstance() {
		return LazyHolder.INSTANCE;
	}

	public Connection connection(String db) throws SQLException {
		
		return DriverManager.getConnection("jdbc:sqlite:" + bases.get(db));
	}
}
