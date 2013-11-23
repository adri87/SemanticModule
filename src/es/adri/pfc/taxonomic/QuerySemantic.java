package es.adri.pfc.taxonomic;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.connections.ConnectionLMF;
import es.adri.pfc.data.Profile;

/**
 * Se encarga de llevar a cabo las consultas semanticas. De igual forma calcula y devuelve la respuesta semantica al 
 * servlet del sistema recomendador con cada una de las ofertas disponibles y su peso semantico respecto al perfil
 * introducido en el servicio.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class QuerySemantic {
	private Logger log;
	private JSONArray offers;
	private String baseUrl;
	private String urlServerLmf;
	private ConnectionLMF conlmf;
	private ArrayList<String> nameFilOffers;
	private Profile prof;
	private Distance dist;
	
	/**
	 * Constructor de la clase QuerySemantic.
	 * 
	 * @param baseUrl .- Raíz de donde se encuentra desplegado el recomendador.
	 * @param urlServerLmf .- URL en la que se encuentra desplegado LMF.
	 * @param prof .- Perfil con los parametros de la busqueda.
	 */
	public QuerySemantic(String baseUrl, String urlServerLmf, ConnectionLMF conlmf, Profile prof){
		this.baseUrl=baseUrl;
		this.urlServerLmf=urlServerLmf;
		this.conlmf=conlmf;
		this.dist = new Distance(conlmf);
		this.prof=prof;
		this.nameFilOffers = getNameFilOffers();
		offers = new JSONArray();
		getOffers();
		log = LoggerFactory.getLogger(QuerySemantic.class);
	}
	
	/**
	 * Devuelve un JSONArray que contiene las ofertas de empleo y sus correspondientes habilidades demandadas.
	 * 
	 * @return offers .- JSONArray con la información de las ofertas de empleo.
	 */
	public JSONArray getOffers(){
		ArrayList<String> nameFilOffer = this.nameFilOffers;
		for (int i = 0; i < nameFilOffer.size(); i++) {
			try {
				String url = nameFilOffer.get(i);
				String querySkillOffers = conlmf.readSparql(baseUrl + "/resources/skillsOffers.sparql");
				JSONArray results = conlmf.getResponseQuerySparql(querySkillOffers, true, url);
				JSONArray skills = new JSONArray();
				for (int j = 0; j < results.length(); j++) {
					String nameSkill = results.getJSONObject(j).getJSONObject("n").getString("value");
					String levelSkill = results.getJSONObject(j).getJSONObject("l").getString("value");
					JSONObject jo = new JSONObject();
					jo.put("name", nameSkill);
					jo.put("level", levelSkill);
					skills.put(jo);
				}
				JSONObject job = new JSONObject();
				job.put("url", url);
				job.put("skills", skills);
				this.offers.put(job);
			} catch (JSONException e) {
				log.error("Error al acceder al JSON");
				e.printStackTrace();
			}			
		}
		return this.offers;
	}
	
	/**
	 * Devuelve el conjunto de ofertas disponibles en el servicio.
	 * 
	 * @return nameOffers .- Array con los URI de las distintas ofertas de empleo disponibles.
	 */
	private ArrayList<String> getNameOffers(){
		ArrayList<String> nameOffers = new ArrayList<>();
		String queryURLOffers = conlmf.readSparql(baseUrl + "/resources/nameOffers.sparql");
		try {
			JSONArray results = conlmf.getResponseQuerySparql(queryURLOffers, false, null);
			for (int i = 0; i < results.length(); i++) {
				String urlOffer = results.getJSONObject(i).getJSONObject("s").getString("value");
				nameOffers.add(urlOffer);
			}
		} catch (JSONException e) {
			log.error("Error al acceder al JSON");
			e.printStackTrace();
		}
		return nameOffers;
	}
	
	/**
	 * Devuelve el conjunto de ofertas disponibles en el servicio, filtradas en funcion de la busqueda 
	 * del usuario.
	 * 
	 * @return nameOffers .- Array con los URI de las distintas ofertas de empleo filtradas.
	 */
	public ArrayList<String> getNameFilOffers(){
		ArrayList<String> replace = new ArrayList<>(), keys = new ArrayList<>(), nameFilOffers = new ArrayList<>();
		String provinces = "", contracts = "";
		String queryURLOffers = conlmf.readSparql(baseUrl + "/resources/nameFilOffers.sparql");
		// Se observa si hay filtros de provincias
		if (this.prof.getProvinces() != null) {
			ArrayList<String> prov = this.prof.getProvinces();
			keys.add("Provinces");
			for (int i = 0; i < prov.size(); i++) {
				if (i==0) provinces="FILTER (?locality = \""+prov.get(i)+"\"";
				else provinces += "|| ?locality = \""+prov.get(i)+"\"";
				if (i+1 == prov.size()) provinces += ")";
			}
			replace.add(provinces);
		} else {
			keys.add("Provinces");
			replace.add(provinces);
		}
		// Se observa si hay filtros de contratos
		if (this.prof.getContracts() != null) {
			ArrayList<String> cont = this.prof.getContracts();
			keys.add("Contract");
			for (int i = 0; i < cont.size(); i++) {
				if (i==0) contracts="FILTER (?contract = \""+cont.get(i)+"\"";
				else contracts += "|| ?contract = \""+cont.get(i)+"\"";
				if (i+1 == cont.size()) contracts += ")";
			}
			replace.add(contracts);
		} else {
			keys.add("Contract");
			replace.add(contracts);
		}
		//
		try {
			JSONArray results = conlmf.getResponseQueryFilSparql(queryURLOffers, true, replace, keys);
			for (int i = 0; i < results.length(); i++) {
				String urlOffer = results.getJSONObject(i).getJSONObject("s").getString("value");
				nameFilOffers.add(urlOffer);
			}
		} catch (JSONException e) {
			log.error("Error al acceder al JSON");
			e.printStackTrace();
		}
		return nameFilOffers;
	}
	
	/**
	 * Calcula y devuelve un objeto con la similitud semantica entre las distintas ofertas de empleo y el perfil del candidato que 
	 * esta buscando mediante el uso del servicio.
	 * 
	 * @param prof .- Perfil que contiene las caracteristicas y preferencias del usuario.
	 * @return JSONArray con el resultado semantico.
	 */
	public JSONArray getSimilaritySemantic() {
		ArrayList<String> nameOffers = getNameOffers();
		JSONArray responseSemantic = new JSONArray();
		ArrayList<String> skills = this.prof.getSkills();
		ArrayList<String> experience = this.prof.getCompetence();
		HashMap<String, Double> distances = new HashMap<>();
		try {
			for (int i = 0; i < offers.length(); i++) {
				ArrayList<Double> simSemSkills = new ArrayList<Double>();
				for (int j = 0; j < skills.size(); j++) {
					double disAux = 100, levelSim = 0;
					String skill = urlServerLmf+"resource/skills#"+skills.get(j);
					String level = urlServerLmf+"resource/levels#"+experience.get(j);
					JSONArray skillsToCompare = offers.getJSONObject(i).getJSONArray("skills");
					for (int k = 0; k < skillsToCompare.length(); k++) {
						// Nombre de la habilidad
						String skillToCompare = skillsToCompare.getJSONObject(k).getString("name");
						double disTemp;
						if (distances.get(skill+"-"+skillToCompare)!=null) {
							disTemp = distances.get(skill+"-"+skillToCompare);
						} else {
							disTemp = dist.distanceBetweenConcepts(skill, skillToCompare);
							distances.put(skill+"-"+skillToCompare, disTemp);
							distances.put(skillToCompare+"-"+skill, disTemp);
						}			
						if (disTemp < disAux) 
							disAux = disTemp;
						// Experiencia
						String levelToCompare = skillsToCompare.getJSONObject(k).getString("level");
						levelSim = getLevelSim(level, levelToCompare);
					}
					double sim = 0.9*(1.0 - (disAux/2)) + 0.1*levelSim;
					sim = Math.rint(sim*100)/100;
					simSemSkills.add(sim/skills.size());
				}
				double sim = 0.0;
				for (int l = 0; l < simSemSkills.size(); l++) {
					sim += simSemSkills.get(l);
				}
				String url = offers.getJSONObject(i).getString("url");
				JSONObject response = new JSONObject();
				response.put("url", url);
				response.put("sim", sim);
				responseSemantic.put(response);
			}
			for (int i = 0; i < nameOffers.size(); i++) {
				if (!this.nameFilOffers.contains(nameOffers.get(i))) {
					JSONObject response = new JSONObject();
					response.put("url", nameOffers.get(i));
					response.put("sim", 0.0);
					responseSemantic.put(response);
				}
			}
		} catch (JSONException e) {
			log.error("Error al acceder al JSON");
			e.printStackTrace();
		}	
		return responseSemantic;
	}
	
	/**
	 * Calcula la similitud entre los niveles de experiencia.
	 * 
	 * @param levelAcq .- Experiencia adquirida.
	 * @param levelNec .- Experiencia necesaria.
	 * @return Coeficiente de similitud entre niveles de experiencia.
	 */
	private double getLevelSim (String levelAcq, String levelNec) {
		if (levelAcq.equals(levelNec))
			return 1.0;
		if (levelAcq.equals("http://localhost:8080/LMF-2.6.0/resource/levels#expert"))
			return 1.0;
		if (levelAcq.equals("http://localhost:8080/LMF-2.6.0/resource/levels#intermediate")){
			if (levelNec.equals("http://localhost:8080/LMF-2.6.0/resource/levels#expert")) 
				return 0.5;
			else
				return 1.0;
		}
		if (levelAcq.equals("http://localhost:8080/LMF-2.6.0/resource/levels#beginner")) {
			if (levelNec.equals("http://localhost:8080/LMF-2.6.0/resource/levels#intermediate"))
				return 0.66;
			else if (levelNec.equals("http://localhost:8080/LMF-2.6.0/resource/levels#expert"))
				return 0.33;
		}
		return 0.0;
	}
}
