package es.adri.pfc.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.config.Configuration;
import es.adri.pfc.connections.ConnectionLMF;
import es.adri.pfc.connections.JSONTreatment;
import es.adri.pfc.data.Profile;
import es.adri.pfc.taxonomic.QuerySemantic;

/**
 * Servlet del sistema recomendador.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class SemMatcher extends HttpServlet{
	private Logger log;
	private String baseUrl;
	private String urlServerLmf;
	private String urlFileConfiguration;
	private Configuration conf;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Inicializacion del servlet.
	 */
    public void init() throws ServletException {
    	log =  LoggerFactory.getLogger(SemMatcher.class);
    	baseUrl = getServletContext().getRealPath("/");
		urlFileConfiguration = getServletContext().getRealPath("/config/configuration.properties");
		conf = new Configuration(urlFileConfiguration);
		urlServerLmf = conf.getProperty("serverLmf");
		log.info("Se ha inicializado correctamente el servlet");
    	super.init();
    	log.info("se ha ejecutado el init");
    }
       

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			doProcess(request, response); 
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			doProcess(request, response);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Metodo al que se redirigen tanto los post como los get realizados al servlet. Se encarga de recoger la peticion realizada 
	 * por el usuario, producir la comunicacion con los motores de recomendacion y devolver la respuesta al cliente.
	 *  
	 * @param request .- Peticion realizada
	 * @param response .- Respuesta del servlet. Sera la recomendacion semantica o social que se ha solicitado.
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONException
	 */
	private void doProcess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, JSONException {
				
		// Extraccion de los parametros que contiene la "query"
		JSONTreatment jt = new JSONTreatment();
		Profile prof = jt.getProfile(new JSONObject(request.getParameter("q")));
		
		// Ejecuntando algoritmo de similitud semantica
		ConnectionLMF conlmf = new ConnectionLMF(baseUrl, urlServerLmf);
		QuerySemantic qs = new QuerySemantic(baseUrl, urlServerLmf, conlmf, prof);		
		JSONArray req = qs.getSimilaritySemantic();
		
		// Devolviendo la salida
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin","*");
		PrintWriter pw = new PrintWriter(response.getOutputStream());
		pw.println(req);
		pw.close();
	}
}
