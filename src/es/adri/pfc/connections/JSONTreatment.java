package es.adri.pfc.connections;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.data.Profile;

/**
 * Clase que realiza distintos tipos de tratamientos sobre JSONs. 
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class JSONTreatment {
	private Logger log = LoggerFactory.getLogger(JSONTreatment.class);

	/**
	 * Este metodo devuelve el perfil de usuario para ser tratado.
	 * 
	 * @param jo .- Petición con el perfil que cumple el usuario que usa el servicio
	 * @return prof .- Objeto de tipo Profile que contiene el perfil de usuario (sus habilidades y competencias).
	 */
	public Profile getProfile(JSONObject jo){
		ArrayList<String> skills = new ArrayList<String>();
		ArrayList<String> competencelevel = new ArrayList<String>();		
		ArrayList<String> provinces = new ArrayList<String>();
		ArrayList<String> contracts = new ArrayList<String>();
		String id = null;
		try {
			JSONObject form = jo.getJSONArray("episteme.search.new_search").getJSONObject(0);
			id = form.getString("name");
			JSONArray semantic = form.getJSONArray("result").getJSONObject(0).getJSONArray("semantic");
			for (int i = 0; i < semantic.length(); i++) {
				skills.add(semantic.getJSONObject(i).getString("skill"));
				competencelevel.add(semantic.getJSONObject(i).getString("level"));
			}
			JSONArray province = form.getJSONArray("result").getJSONObject(0).getJSONArray("solr").getJSONObject(1).getJSONArray("values");
			for (int j = 0; j < province.length(); j++) {
				provinces.add(province.getString(j));
			}
			JSONArray contract = form.getJSONArray("result").getJSONObject(0).getJSONArray("solr").getJSONObject(2).getJSONArray("values");
			for (int j = 0; j < contract.length(); j++) {
				contracts.add(contract.getString(j));
			}
		} catch (JSONException e) {
			log.error("Error al generar profile");
			e.printStackTrace();
		}
		Profile prof = new Profile(skills, competencelevel, provinces, contracts, id);
		return prof;
	}
}
