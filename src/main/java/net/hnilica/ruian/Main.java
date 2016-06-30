package net.hnilica.ruian;

//import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.staticFiles;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.h2.jdbcx.JdbcConnectionPool;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.hnilica.ruian.model.Location;

public class Main {
	public static DB mapdb;
	public static JdbcConnectionPool h2cp;
	static Service service;

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
		System.out.println("db: " + dbStr);

		h2cp = JdbcConnectionPool.create(dbStr, "sa", "");
		//h2cp = JdbcConnectionPool.create("jdbc:h2:zip:./db/ruiandb.zip!/ruiandb;MODE=Oracle", "sa", "");
		//h2cp = JdbcConnectionPool.create("jdbc:h2:file:/.../test4;MODE=Oracle", "sa", "");

		//db test
		Connection conn = h2cp.getConnection();
		conn.createStatement().executeQuery("select 1 from vh_ruian_sobec where 1=2");
		conn.close();
		
		service=new Service(h2cp.getConnection());

		mapdb = DBMaker.newTempFileDB().transactionDisable().closeOnJvmShutdown().make();

		//port(PORT);

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
		//final BTreeMap<String, String> cacheSObec = mapdb.createTreeMap("sobec").make();
		

		get("/api/sobec", (req, res) -> {
			String q = req.queryString();
			res.type("application/json; charset=UTF-8");
			res.header("X-Frame-Options", "SAMEORIGIN");
			res.header("X-XSS-Protection", "1; mode=block");
			res.header("Access-Control-Allow-Origin", "*");
			//res.header("Cache-Control", "max-age=31536000"); // HTTP 1.1.
			//res.header("Expires", getExpire(31536000)); // Proxies.
			//if (cacheSObec.containsKey(q)) {
			//	return cacheSObec.get(q);
			//}
			try {
				String sobec = req.queryParams("sobec");
				if (sobec == null || sobec.isEmpty()) {
					//nic do vyhledavani nevsoupilo, tak nic nevratim
					//cacheSObec.put(q, "");
					return "[]";
				}
				String pretty = req.queryParams("pretty");

				List<Location> locations=service.searchLocation(sobec);

				Gson gson;
				if ("true".equalsIgnoreCase(pretty)) {
					gson = new GsonBuilder().setPrettyPrinting().create();
				} else {
					gson = new Gson();
				}

				String data = gson.toJson(locations);
				//cacheSObec.put(q, data);
				return data;
			} catch (Exception e) {
				System.out.println("err " + q + " " + e.getMessage());
				e.printStackTrace();
				return "[]";
			}
		});
		
		
		get("/api/gobec", (req, res) -> {
			String q = req.queryString();
			res.type("application/json; charset=UTF-8");
			res.header("X-Frame-Options", "SAMEORIGIN");
			res.header("X-XSS-Protection", "1; mode=block");
			res.header("Access-Control-Allow-Origin", "*");
			//res.header("Cache-Control", "max-age=31536000"); // HTTP 1.1.
			//res.header("Expires", getExpire(31536000)); // Proxies.
			//if (cacheSObec.containsKey(q)) {
			//	return cacheSObec.get(q);
			//}
			try {
				String postCode = req.queryParams("post_code");
				String name = req.queryParams("location");
				if (postCode == null || postCode.isEmpty() || name == null || name.isEmpty()) {
					//nic do vyhledavani nevsoupilo, tak nic nevratim
					//cacheSObec.put(q, "");
					return "[]";
				}
				String pretty = req.queryParams("pretty");

				List<Location> locations=service.getLocation(name,postCode);

				Gson gson;
				if ("true".equalsIgnoreCase(pretty)) {
					gson = new GsonBuilder().setPrettyPrinting().create();
				} else {
					gson = new Gson();
				}

				String data = gson.toJson(locations);
				//cacheSObec.put(q, data);
				return data;
			} catch (Exception e) {
				System.out.println("err " + q + " " + e.getMessage());
				e.printStackTrace();
				return "[]";
			}
		});		
	}
	


}
