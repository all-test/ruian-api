package net.hnilica.ruian;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.SparkBase.setPort;
import static spark.SparkBase.staticFileLocation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.h2.jdbcx.JdbcConnectionPool;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;


public class Server {
    private static final int PORT = 8080;
    public static DB mapdb;
    public static JdbcConnectionPool h2cp;
    
	public static void main(String[] args) throws SQLException, URISyntaxException, MalformedURLException {
		String dbUrl=Server.class.getClassLoader().getResource("db/ruiandb.mv.db").toString();
		String dbStr;
		if(dbUrl.startsWith("jar:file")){
			dbStr="jdbc:h2:zip:"+dbUrl.replaceAll("^jar\\:file\\:", "").replaceAll("\\.mv\\.db$", "")+";MODE=Oracle";//smaze protokol a priponu
		}
		else if(dbUrl.startsWith("file")){
			dbStr="jdbc:h2:"+dbUrl.replaceAll("\\.mv\\.db$", "")+";MODE=Oracle";//smaze priponu 
		}else{
			System.out.println("databaze nelze otevrit "+dbUrl);
			return;
		}
			
		//
		System.out.println("oteviram db "+dbStr);
		

    	h2cp = JdbcConnectionPool.create(dbStr, "sa", "");
    	//h2cp = JdbcConnectionPool.create("jdbc:h2:zip:./db/ruiandb.zip!/ruiandb;MODE=Oracle", "sa", "");
    	//h2cp = JdbcConnectionPool.create("jdbc:h2:file:/.../test4;MODE=Oracle", "sa", "");
    	
    	//db test
		Connection conn = h2cp.getConnection();
		conn.createStatement().executeQuery("select 1 from vh_ruian_sobce where 1=2");
    	conn.close();

    	mapdb = DBMaker
    	        //.memoryDB()
    			.tempFileDB()
    	        .transactionDisable()
    	        .closeOnJvmShutdown()
    	        .make();
    	

        setPort(PORT);
        staticFileLocation("/public");
        
        //nastavi znakovou sadu hlavne pro staticke soubory
        before("/*","text/html",(req, res) -> {
        	res.type("text/html; charset=utf-8");
        });
        
        
        //pro testovani
		get("/ping", (req, res) -> {
			return "pong";
		});
		
		sobec();
    	
	}
	
	

    public static void sobec(){
        final BTreeMap<String,String> cacheSObec = mapdb.treeMap("sobec");
        final int limit=10;
        //hledani presneho nazvu
        final String sql1 = "select obec_kod, cobce_kod, sobec_nazev, formatted_address from vh_ruian_sobce where sobec_nazev_upper=upper(?) or sobec_nazev_stripped=upper(?) order by sobec_nazev_stripped,formatted_address";
        //hledani podobneho (omezen limitem)
        final String sql2 = "select obec_kod, cobce_kod, sobec_nazev, formatted_address from vh_ruian_sobce where (sobec_nazev_upper!=upper(?) and sobec_nazev_upper like upper(?)||'%') or (sobec_nazev_stripped!=upper(?) and sobec_nazev_stripped like upper(?)||'%') order by sobec_nazev_stripped,formatted_address";

		get("/ruian/sobec", (req, res) -> {
			String q=req.queryString();
			if (cacheSObec.containsKey(q)){
				res.type("application/json; charset=UTF-8");
				res.header("X-Frame-Options", "SAMEORIGIN");
				res.header("X-XSS-Protection", "1; mode=block");
				res.header("Access-Control-Allow-Origin", "*");
				return cacheSObec.get(q);
			}
			try {
				res.type("application/json; charset=UTF-8");
				res.header("X-Frame-Options", "SAMEORIGIN");
				res.header("X-XSS-Protection", "1; mode=block");
				res.header("Access-Control-Allow-Origin", "*");
				String sobec=req.queryParams("sobec");
				if(sobec==null || sobec.isEmpty()){
					//nic do vyhledavani nevsoupilo, tak nic nevratim
					cacheSObec.put(q, "");
					return "[]";
				}
				String pretty=req.queryParams("pretty");
		        
				Connection conn = h2cp.getConnection();
                ResultSet rs;

                ArrayList<Place> places=new ArrayList<Place>(limit);
                
                //hledani presneho nazvu
                PreparedStatement ps1 = conn.prepareStatement(sql1);
                //hledani presneho nazvu
                ps1.setString(1, sobec);
                ps1.setString(2, sobec);
                rs = ps1.executeQuery();
                while(rs.next()){
                	Place place=new Place();
                	place.obecKod=rs.getBigDecimal("obec_kod");
                	place.cobceKod=rs.getBigDecimal("cobce_kod");
                	place.sobecNazev=rs.getString("sobec_nazev");
                	place.formattedAddress=rs.getString("formatted_address");
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
                for(int i=0;rs.next()&&i<limit;i++){
                	Place place=new Place();
                	place.obecKod=rs.getBigDecimal("obec_kod");
                	place.cobceKod=rs.getBigDecimal("cobce_kod");
                	place.sobecNazev=rs.getString("sobec_nazev");
                	place.formattedAddress=rs.getString("formatted_address");
                	places.add(place);
                }
                rs.close();
                ps2.close();
                
                
                conn.close();
                
                Gson gson;
                if("true".equalsIgnoreCase(pretty)){
                	gson = new GsonBuilder().setPrettyPrinting().create();
                }else{
                	gson = new Gson();
                }
                
                
                
                String data=gson.toJson(places);
				cacheSObec.put(q, data);
				return data;
			} catch (Exception e) {
				System.out.println("err "+q+" "+e.getMessage());
				e.printStackTrace();
				return "[]";
			}
		});
    }    

    
    public static class Place implements Serializable{ 
    	@SerializedName("obec_kod")
        public BigDecimal obecKod;
    	@SerializedName("cobce_kod")
        public BigDecimal cobceKod;
    	@SerializedName("sobec_nazev")
        public String sobecNazev;
    	@SerializedName("formatted_address")
        public String formattedAddress;
    } 

}
