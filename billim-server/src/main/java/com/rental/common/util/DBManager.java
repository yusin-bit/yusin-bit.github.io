package main.java.com.rental.common.util;

import java.io.FileInputStream;

/**
 * JDBC를 위한 로드, 연결, 닫기
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBManager {
	private static Properties proFile = new Properties();

	static {
		try {

			proFile.load(new FileInputStream("resources/dbmanager.properties"));

		} catch (Exception e) {
			// e.printStackTrace();
		}
		// 배포 환경에서는 설정 파일 대신 환경변수(DB_URL, DB_USER, DB_PASSWORD)를 사용한다.
		overrideFromEnv("url", "DB_URL");
		overrideFromEnv("userName", "DB_USER");
		overrideFromEnv("userPass", "DB_PASSWORD");
		try {
			Class.forName(proFile.getProperty("driverName", "com.mysql.cj.jdbc.Driver"));
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	private static void overrideFromEnv(String key, String envName) {
		String value = System.getenv(envName);
		if (value != null && !value.isBlank()) proFile.setProperty(key, value);
	}

	
	public static Properties getProFile() {
		return proFile;
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(
				proFile.getProperty("url"),
				proFile.getProperty("userName"),
				proFile.getProperty("userPass"));
	}
	

	public static void close(Connection con, Statement st, ResultSet rs) {
		try {
			if(rs != null) rs.close();
			close(con, st);

		}
		catch(Exception e) {
			// e.printStackTrace();
		}
	}
	
	public static void close(Connection con, Statement st) {
		try {
			if(st != null) st.close();
			if(con != null) con.close();
		}
		catch(Exception e) {
			// e.printStackTrace();
		}
	}

}
