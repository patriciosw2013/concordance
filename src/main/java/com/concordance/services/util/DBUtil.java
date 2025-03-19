package com.concordance.services.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {

    private static SQLUtil db = SQLUtil.getInstance();
    private static String base = "base";

    public static List<String> baseList(boolean conc, boolean intl, boolean bible) throws SQLException {
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.name from bases c where c.conc = ? or c.intl = ? or c.bible = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, conc ? 1 : -1);
                st.setInt(2, intl ? 1 : -1);
                st.setInt(3, bible ? 1 : -1);
				result = st.executeQuery();
				while(result.next()) {
					res.add(result.getString(1));
				}
			}
		}
		return res;
	}

    public static int max(String table, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select max(id) from " + table;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return (int)result.getLong(1);
				} else return 0;
			}
		}
	}

	public static int type(String baseExt) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select type from bases where name = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setString(1, baseExt);
				result = st.executeQuery();
				if(result.next()) {
					return result.getInt(1);
				} else return 0;
			}
		}
	}
}
