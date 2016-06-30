package net.hnilica.ruian.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

public class Location implements Serializable {
	@SerializedName("obec_kod")
	public BigDecimal obecKod;
	@SerializedName("sobec_nazev")
	public String sobecNazev;
	@SerializedName("sobec_desc")
	public String sobecDesc;
	@SerializedName("sobec_nazev_desc")
	public String sobecNazevDesc;
}
