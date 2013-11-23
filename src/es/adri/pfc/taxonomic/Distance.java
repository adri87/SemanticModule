package es.adri.pfc.taxonomic;

import es.adri.pfc.connections.ConnectionLMF;

/**
 * Realiza el calculo de la coincidencia de conceptos dentro de las taxonomia.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class Distance {
	private int levelDeepest=5;
	private ConnectionLMF conlmf;
	
	public Distance(ConnectionLMF conlmf){
		this.conlmf=conlmf;
	}
	
	/**
	 * Calcula la distancia semantica entre dos conceptos
	 * 
	 * @return un double con la distancia semantica entre los dos conceptos.
	 */
	public double distanceBetweenConcepts(String concept, String otherConcept) {
		String conc = modifyConcept(concept);
		String othConc = modifyConcept(otherConcept);
		String parentCommon = conlmf.getParentCommon(conc, othConc);
		double levelConcept = conlmf.giveDepth(conc);
		double levelOtherConcept = conlmf.giveDepth(othConc);
		double levelParentCommon = conlmf.giveDepth(parentCommon);
		return distParent(levelConcept, levelParentCommon) + distParent(levelOtherConcept, levelParentCommon);
	}
	
	/**
	 * Calcula la distancia de un concepto al padre comun mas cercano.
	 * 
	 * @param levelConcept .- profunidad de un concepto.
	 * @param levelParent .- profundidad del padre comun y mas cercano a los dos conceptos a comparar.
	 * @return distancia central al padre.
	 */
	public double distParent (double levelConcept, double levelParent) {
		return milestoneLineal(levelParent) - milestoneLineal(levelConcept);
	}
	
	/**
	 * Calculo lineal para la similitud semantica.
	 * 
	 * @param levelNode .- profundidad de nodo.
	 * @return variable lineal
	 */
	public double milestoneLineal(double levelNode) {
		return 1 - (levelNode/levelDeepest);
	}
	
	/**
	 * Modifica un string dado.
	 * 
	 * @param s .- String a modificar.
	 * @return s.- String modificado.
	 */
	private String modifyConcept (String s){
		s = s.replace("http://localhost:8080/LMF-2.6.0/resource/skills#", "http://kmm.lboro.ac.uk/ecos/1.0#");
		s = s.replace(" ", "_");
		return s;
	}
}
