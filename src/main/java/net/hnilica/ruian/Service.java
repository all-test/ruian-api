package net.hnilica.ruian;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.hnilica.ruian.model.Location;

public class Service implements Closeable{
	Connection conn;
	Map<String,PreparedStatement> psCache=new HashMap<>();

	public Service(Connection conn) {
		super();
		this.conn=conn;
	}
	
	
	public List<Location> searchLocation(String name) throws SQLException{
		final int limit = 10;
		//hledani presneho nazvu
		final String sql1 = "select obec_kod, sobec_nazev, locality_desc from vh_ruian_sobec where sobec_nazev_upper=upper(?) or sobec_nazev_stripped=upper(?) order by sobec_nazev_stripped,locality_level";
		//hledani podobneho (omezen limitem)
		final String sql2 = "select obec_kod, sobec_nazev, locality_desc from vh_ruian_sobec where (sobec_nazev_upper!=upper(?) and sobec_nazev_upper like upper(?)||'%') or (sobec_nazev_stripped!=upper(?) and sobec_nazev_stripped like upper(?)||'%') order by sobec_nazev_stripped,locality_level";

		ResultSet rs;

		ArrayList<Location> locations = new ArrayList<Location>(limit);

		PreparedStatement ps1=psCache.get(sql1);
		if(ps1==null){
			ps1 = conn.prepareStatement(sql1);
			psCache.put(sql1, ps1);
		}
		//hledani presneho nazvu
		ps1.setString(1, name);
		ps1.setString(2, name);
		rs = ps1.executeQuery();
		while (rs.next()) {
			Location place = new Location();
			place.obecKod = rs.getBigDecimal("obec_kod");
			place.sobecNazev = rs.getString("sobec_nazev");
			place.sobecDesc = rs.getString("locality_desc");
			place.sobecNazevDesc = place.sobecNazev;
			if(place.sobecDesc!=null && !place.sobecDesc.isEmpty())place.sobecNazevDesc=place.sobecNazevDesc+" ("+place.sobecDesc+")";
			locations.add(place);
		}
		rs.close();
		//ps1.close();


		PreparedStatement ps2=psCache.get(sql2);
		if(ps2==null){
			ps2 = conn.prepareStatement(sql2);
			psCache.put(sql2, ps2);
		}
		//hledani podobneho (omezen limitem)
		ps2.setString(1, name);
		ps2.setString(2, name);
		ps2.setString(3, name);
		ps2.setString(4, name);
		rs = ps2.executeQuery();
		for (int i = 0; rs.next() && i < limit; i++) {
			Location place = new Location();
			place.obecKod = rs.getBigDecimal("obec_kod");
			place.sobecNazev = rs.getString("sobec_nazev");
			place.sobecDesc = rs.getString("locality_desc");
			place.sobecNazevDesc = place.sobecNazev;
			if(place.sobecDesc!=null && !place.sobecDesc.isEmpty())place.sobecNazevDesc=place.sobecNazevDesc+" ("+place.sobecDesc+")";
			locations.add(place);
		}
		rs.close();
		//ps2.close();
		
		return locations;
	}

	
	
	public List<Location> getLocation(String name, String postCode) throws SQLException{
		final int limit = 10;
		//hledani presneho nazvu
		final String sql1 = "select l.obec_kod, l.sobec_nazev, l.locality_desc from vh_ruian_psc p join vh_ruian_sobec l on p.obec_kod=l.obec_kod where p.psc=? and (l.sobec_nazev_upper=upper(?) or l.sobec_nazev_stripped=upper(?)) order by l.locality_level";
		
		//procisteni psc
		if(postCode!=null){
			postCode=postCode.replaceAll("[^0-9]", "");
		}
		
		
		ResultSet rs;

		ArrayList<Location> locations = new ArrayList<Location>(limit);

		PreparedStatement ps1=psCache.get(sql1);
		if(ps1==null){
			ps1 = conn.prepareStatement(sql1);
			psCache.put(sql1, ps1);
		}
		//hledani presneho nazvu
		ps1.setString(1, postCode);
		ps1.setString(2, name);
		ps1.setString(3, name);
		rs = ps1.executeQuery();
		while (rs.next()) {
			Location place = new Location();
			place.obecKod = rs.getBigDecimal("obec_kod");
			place.sobecNazev = rs.getString("sobec_nazev");
			place.sobecDesc = rs.getString("locality_desc");
			place.sobecNazevDesc = place.sobecNazev;
			if(place.sobecDesc!=null && !place.sobecDesc.isEmpty())place.sobecNazevDesc=place.sobecNazevDesc+" ("+place.sobecDesc+")";
			locations.add(place);
		}
		rs.close();


		
		return locations;
	}


	@Override
	public void close() throws IOException {
		//uzavre dotazy
		for(PreparedStatement ps:psCache.values()){
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		//uzavre spojeni na db
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

}
