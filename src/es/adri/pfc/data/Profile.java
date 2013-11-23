package es.adri.pfc.data;

import java.util.ArrayList;

/**
 * Clase que se encarga de almacenar el perfil que rellena el cliente en el servicio.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class Profile {
	private ArrayList<String> skills;
	private ArrayList<String> competencelevel;		
	private ArrayList<String> provinces;	
	private ArrayList<String> contracts;		
	private String id;
	
	/**
	 * Constructor de la clase Profile.
	 */
	public Profile(ArrayList<String> skills, ArrayList<String> competencelevel, ArrayList<String> provinces, ArrayList<String> contracts, String id){
		this.skills=skills;
		this.competencelevel=competencelevel;
		this.provinces=provinces;
		this.contracts=contracts;
		this.id=id;
	}
	
	// Metodos getter y setter de los atributos de la clase Profile.
	
	public void setSkills(ArrayList<String> skills){
		this.skills=skills;
	}
	
	public ArrayList<String> getSkills(){
		return this.skills;
	}
	
	public void setCompetence(ArrayList<String> competence){
		this.competencelevel=competence;
	}
	
	public ArrayList<String> getCompetence(){
		return this.competencelevel;
	}
	
	public void setProvinces(ArrayList<String> provinces){
		this.provinces=provinces;
	}
	
	public ArrayList<String> getProvinces(){
		return this.provinces;
	}
	
	public void setContracts(ArrayList<String> contracts){
		this.contracts=contracts;
	}
	
	public ArrayList<String> getContracts(){
		return this.contracts;
	}
	
	public void setId(String id){
		this.id=id;
	}
	
	public String getId(){
		return this.id;
	}

}
