package org.openimaj.text.nlp.namedentity;

import java.util.List;
import java.util.Map;

/**
 * TODO: Laurence, huh?
 * 
 * @author Laurence Willmore (lgw1e10@ecs.soton.ac.uk)
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * 
 */
public class YagoPropertyMapper {

	/**
	 * TODO: Laurence, huh?
	 * 
	 * @author Laurence Willmore (lgw1e10@ecs.soton.ac.uk)
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 * 
	 */
	public enum PropertyType {
		/**
		 * 
		 */
		FACT_LITERAL, /**
		 * 
		 */
		FACT_RESOURCE, /**
		 * 
		 */
		TRIPLE_LITERAL, /**
		 * 
		 */
		TRIPLE_RESOURCE,
	}

	/**
	 * 
	 */
	public YagoPropertyMapper() {

	}

	/**
	 * @param URIs
	 * @param properties
	 * @return dunno
	 */
	public Map<String, Map<String, String>> mapProperties(List<String> URIs, Map<String, PropertyType> properties) {
		// final Map<String, Map<String, String>> result = new HashMap<String,
		// Map<String, String>>();
		// for (final String uri : URIs) {
		//
		// }
		return null;
	}

}
