package net.hnilica.ruian;

//import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.h2.jdbcx.JdbcConnectionPool;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Main {
	private static final int PORT = 8080;
	public static DB mapdb;
	public static JdbcConnectionPool h2cp;

	public static void main(String[] args) throws SQLException, URISyntaxException, MalformedURLException {
		String dbUrl = Main.class.getClassLoader().getResource("db/ruiandb.mv.db").toString();
		String dbStr;
		if (dbUrl.startsWith("jar:file")) {
			dbStr = "jdbc:h2:zip:" + dbUrl.replaceAll("^jar\\:file\\:", "").replaceAll("\\.mv\\.db$", "")
					+ ";MODE=Oracle";//smaze protokol a priponu
		} else if (dbUrl.startsWith("file")) {
			dbStr = "jdbc:h2:" + dbUrl.replaceAll("\\.mv\\.db$", "") + ";MODE=Oracle";//smaze priponu 
		} else {
			System.out.println("databaze nelze otevrit " + dbUrl);
			return;
		}

		//
		System.out.println("oteviram db " + dbStr);

		h2cp = JdbcConnectionPool.create(dbStr, "sa", "");
		//h2cp = JdbcConnectionPool.create("jdbc:h2:zip:./db/ruiandb.zip!/ruiandb;MODE=Oracle", "sa", "");
		//h2cp = JdbcConnectionPool.create("jdbc:h2:file:/.../test4;MODE=Oracle", "sa", "");

		//db test
		Connection conn = h2cp.getConnection();
		conn.createStatement().executeQuery("select 1 from vh_ruian_sobec where 1=2");
		conn.close();

		mapdb = DBMaker.newTempFileDB().transactionDisable().closeOnJvmShutdown().make();

		port(PORT);

		staticFiles.location("/public");

		//nastavi znakovou sadu hlavne pro staticke soubory
		//before("/*","text/html",(req, res) -> {
		//	res.type("text/html; charset=utf-8");
		//});
		staticFiles.header("Content-Type", "text/html; charset=utf-8");

		//pro testovani
		get("/ping", (req, res) -> {
			return "pong";
		});

		sobec();

	}

	public static String getExpire(long sec) {
		Date expdate = new Date();
		expdate.setTime(expdate.getTime() + (sec * 1000));
		DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(expdate);
	}

	public static void sobec() {
		final BTreeMap<String, String> cacheSObec = mapdb.createTreeMap("sobec").make();
		final int limit = 10;
		//hledani presneho nazvu
		final String sql1 = "select obec_kod, sobec_nazev, sobec_desc from vh_ruian_sobec where sobec_nazev_upper=upper(?) or sobec_nazev_stripped=upper(?) order by sobec_nazev_stripped,sobec_desc";
		//hledani podobneho (omezen limitem)
		final String sql2 = "select obec_kod, sobec_nazev, sobec_desc from vh_ruian_sobec where (sobec_nazev_upper!=upper(?) and sobec_nazev_upper like upper(?)||'%') or (sobec_nazev_stripped!=upper(?) and sobec_nazev_stripped like upper(?)||'%') order by sobec_nazev_stripped,sobec_desc";

		get("/api/sobec", (req, res) -> {
			String q = req.queryString();
			res.type("application/json; charset=UTF-8");
			res.header("X-Frame-Options", "SAMEORIGIN");
			res.header("X-XSS-Protection", "1; mode=block");
			res.header("Access-Control-Allow-Origin", "*");
			res.header("Cache-Control", "max-age=31536000"); // HTTP 1.1.
			res.header("Expires", getExpire(31536000)); // Proxies.
			if (cacheSObec.containsKey(q)) {
				return cacheSObec.get(q);
			}
			try {
				String sobec = req.queryParams("sobec");
				if (sobec == null || sobec.isEmpty()) {
					//nic do vyhledavani nevsoupilo, tak nic nevratim
					cacheSObec.put(q, "");
					return "[]";
				}
				String pretty = req.queryParams("pretty");

				Connection conn = h2cp.getConnection();
				ResultSet rs;

				ArrayList<Place> places = new ArrayList<Place>(limit);

				//hledani presneho nazvu
				PreparedStatement ps1 = conn.prepareStatement(sql1);
				//hledani presneho nazvu
				ps1.setString(1, sobec);
				ps1.setString(2, sobec);
				rs = ps1.executeQuery();
				while (rs.next()) {
					Place place = new Place();
					place.obecKod = rs.getBigDecimal("obec_kod");
					place.sobecNazev = rs.getString("sobec_nazev");
					place.sobecDesc = rs.getString("sobec_desc");
					place.sobecNazevDesc = place.sobecNazev;
					if(place.sobecDesc!=null && !place.sobecDesc.isEmpty())place.sobecNazevDesc=place.sobecNazevDesc+" ("+place.sobecDesc+")";
					places.add(place);
				}
				rs.close();
				ps1.close();

				//hledani podobneho (omezen limitem)
				PreparedStatement ps2 = conn.prepareStatement(sql2);
				//hledani podobneho (omezen limitem)
				ps2.setString(1, sobec);
				ps2.setString(2, sobec);
				ps2.setString(3, sobec);
				ps2.setString(4, sobec);
				rs = ps2.executeQuery();
				for (int i = 0; rs.next() && i < limit; i++) {
					Place place = new Place();
					place.obecKod = rs.getBigDecimal("obec_kod");
					place.sobecNazev = rs.getString("sobec_nazev");
					place.sobecDesc = rs.getString("sobec_desc");
					place.sobecNazevDesc = place.sobecNazev;
					if(place.sobecDesc!=null && !place.sobecDesc.isEmpty())place.sobecNazevDesc=place.sobecNazevDesc+" ("+place.sobecDesc+")";
					places.add(place);
				}
				rs.close();
				ps2.close();

				conn.close();

				Gson gson;
				if ("true".equalsIgnoreCase(pretty)) {
					gson = new GsonBuilder().setPrettyPrinting().create();
				} else {
					gson = new Gson();
				}

				String data = gson.toJson(places);
				cacheSObec.put(q, data);
				return data;
			} catch (Exception e) {
				System.out.println("err " + q + " " + e.getMessage());
				e.printStackTrace();
				return "[]";
			}
		});
	}
	

	public static class Place implements Serializable {
		@SerializedName("obec_kod")
		public BigDecimal obecKod;
		@SerializedName("sobec_nazev")
		public String sobecNazev;
		@SerializedName("sobec_desc")
		public String sobecDesc;
		@SerializedName("sobec_nazev_desc")
		public String sobecNazevDesc;
	}

}
