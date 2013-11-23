package es.adri.pfc.connections;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Se encarga de llevar a cabo las consultas a LMF y realizar tratamientos.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class ConnectionLMF {
	private Logger log;
	private String baseUrl;
	private String urlServerLmf;
	
	/**
	 * Constructor de la clase ConnectionLmf
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param urlSeverLmf .- URL de despliegue de LMF.
	 */
	public ConnectionLMF(String baseUrl, String urlSeverLmf){
		this.baseUrl=baseUrl;
		this.urlServerLmf=urlSeverLmf;
		log = LoggerFactory.getLogger(ConnectionLMF.class);
	}
	
	/**
	 * Busca el padre comun mas cercano de dos conceptos dentro del arbol de la taxonomia.
	 * 
	 * @param concept .- Primer concepto.
	 * @param otherConcept .- Segundo Concepto
	 * @return Padre comun mas cercano.
	 */
	public String getParentCommon (String concept, String otherConcept){
		String parentConcept = "" , parentOtherConcept = "";
		if (concept.equalsIgnoreCase(otherConcept))
			return concept;
		if (giveDepth(concept) == giveDepth(otherConcept)) {
			parentConcept = getParent(concept);
			parentOtherConcept = getParent(otherConcept);
		} else if (giveDepth(concept) > giveDepth(otherConcept)) {
			parentConcept = getParent(concept);
			parentOtherConcept = otherConcept;
		} else if (giveDepth(concept) < giveDepth(otherConcept)) {
			parentConcept = concept;
			parentOtherConcept = getParent(otherConcept);
		}
		if (parentConcept.equals(parentOtherConcept))
			return parentConcept;
		else
			return getParentCommon(parentConcept, parentOtherConcept);
	}
	
	/**
	 * Devuelve el padre de un concepto dentro de la taxonomia.
	 * 
	 * @param concept .- Concepto del que queremos hallar el padre.
	 * @return parent .- Concepto padre dentro del arbol.
	 */
	/**
	 * @param concept
	 * @return
	 */
	private String getParent(String concept) {
		concept = concept.replace("http://localhost:8080/LMF-2.6.0/resource/skills/", "http://kmm.lboro.ac.uk/ecos/1.0#");
		if (concept.equals("http://kmm.lboro.ac.uk/ecos/1.0#IT_Cats"))
			return concept;
		String parent = null;
		try {
			String queryParentConcept = readSparql(baseUrl + "/resources/parentConcept.sparql");
			JSONArray results = getResponseQuerySparql(queryParentConcept, true, concept);
			parent = results.getJSONObject(0).getJSONObject("p").getString("value");
		} catch (JSONException e) {
			log.error("Error al acceder al JSON");
			e.printStackTrace();
		}
		return parent;
	}
	
	/**
	 * Devuelve la profundidad de un concepto dentro del arbol de la taxonomia.
	 * 
	 * @param concept .- Concepto al que se le calcula la profundidad.
	 * @return depth .- Profundidad del concepto denttro de la taxonomia.
	 */
	public double giveDepth(String concept) {
		double depth = 0;
		String root = concept;
		String parent = "";
		while (!root.equals("http://kmm.lboro.ac.uk/ecos/1.0#IT_Cats")){
			parent = getParent(root);
			root = parent;
			depth++;
		}
		return depth;
	}

	/**
	 * Este metodo se encarga de leer y devolver el contenido del fichero que se le solicita.
	 * 
	 * @param fileName .- Nombre del fichero a leer.
	 * @return Un String con el contenido del fichero.
	 */
	public String readSparql(String fileName){
		BufferedReader br = null;
		String content;
		try {
			br = new BufferedReader(new FileReader(fileName));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        while (line != null) {
	        	sb.append(line);
		        sb.append('\n');
		        line = br.readLine();
	        }
	        content = sb.toString();
		} catch (FileNotFoundException e) {
			log.error("Archivo no encontrado");
			throw new RuntimeException("File not found");
		} catch (IOException e) {
			log.error("Erron en ejecucion");
			throw new RuntimeException("IO Error occured");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return content;
	}
	
	/**
	 * Hace una consulta al servidor donde se encuentran los datos para obtener la respuesta deseada.
	 * 
	 * @param query .- consulta a realizar al servidor.
	 * @return JSONObject con la información solicitada.
	 */
	public JSONObject getJson(String query){
		URL url;
	    HttpURLConnection conn;
	    BufferedReader rd;
	    JSONObject json = new JSONObject();
	    String line;
	    String result = "";
	    try {
	    	url = new URL(query);
	        conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        while ((line = rd.readLine()) != null) {
	        	result += line;
	        }
	        rd.close();
	        json = new JSONObject(result);
	    } catch (Exception e) {
	    	log.info("Error al obtener JSON");
	    	e.printStackTrace();
		}
		return json;
	}
	
	/**
	 * Obtiene el JSON de respuesta resultado de consultar a LMF una determinada peticion. También se 
	 * encarga de codificar la consulta.
	 * 
	 * @param pet .- Peticion que se desea realizar sobre el servidor.
	 * @param conlmf .- Objeto Connection LMF.
	 * @param mustReplace .- booleano que indica si hay un parametro de la peticion a sustituir.
	 * @param replace .- String que debe ser incluido dentro de la peticion, en caso de sustitucion.
	 * @return JSONArray con la respuesta del servidor
	 */
	public JSONArray getResponseQuerySparql(String pet, boolean mustReplace, String replace){
		String query = pet;
		JSONArray response = new JSONArray();
		if (mustReplace)
			query = pet.replace("Resource", replace);
		try {
			query = URLEncoder.encode(query, "UTF-8");
			query = urlServerLmf + "sparql/select?query="+query+"&output=json";
			response = getJson(query).getJSONObject("results").getJSONArray("bindings");
		} catch (UnsupportedEncodingException e) {
			log.error("Error de codificacion no soportada");
			e.printStackTrace();
		} catch (JSONException e) {
			log.error("Error al obtener JSON");
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	 * Obtiene el JSON de respuesta resultado de consultar a LMF una determinada peticion. También se 
	 * encarga de codificar la consulta.
	 * 
	 * @param pet .- Peticion que se desea realizar sobre el servidor.
	 * @param conlmf .- Objeto Connection LMF.
	 * @param mustReplace .- booleano que indica si hay un parametro de la peticion a sustituir.
	 * @param replace .- String que debe ser incluido dentro de la peticion, en caso de sustitucion.
	 * @return JSONArray con la respuesta del servidor
	 */
	public JSONArray getResponseQueryFilSparql(String pet, boolean mustReplace, ArrayList<String> replace, ArrayList<String> keys){
		String query = pet;
		JSONArray response = new JSONArray();
		if (mustReplace) {
			for (int i = 0; i < replace.size(); i++) {
				query = query.replace(keys.get(i), replace.get(i));
			}
		}
		try {
			query = URLEncoder.encode(query, "UTF-8");
			query = urlServerLmf + "sparql/select?query="+query+"&output=json";
			response = getJson(query).getJSONObject("results").getJSONArray("bindings");
		} catch (UnsupportedEncodingException e) {
			log.error("Error de codificacion no soportada");
			e.printStackTrace();
		} catch (JSONException e) {
			log.error("Error al obtener JSON");
			e.printStackTrace();
		}
		return response;
	}

}
