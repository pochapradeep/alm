/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2012. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.lp.alm.adapter.services;

import java.util.HashMap;

/**
 * @author rschoon
 * 
 * To be used in work flow (graph) algorithms to mark which states or transitions or whatever elements 
 * identified by a string have been already processed. 
 *
 */
public class VisitTracker {
		private HashMap<String, Boolean> visit = null;
	
		public VisitTracker() {
			visit = new HashMap<String, Boolean>();
		}
	
		/**
		 * Marks an object as visited. Pass a string identifier for the object that is visited.
		 *  
		 * @param identifier
		 */
		public void visit(String identifier) {
			visit.put(identifier, new Boolean(true));
		}
	
		/**
		 * Checks if an object has been visited. Pass a string identifier for the object.
		 * Return true if the object was already visited, false otherwise.
		 * 
		 * @param identifier
		 * @return
		 */
		public boolean isVisited(String identifier) {
			Boolean visited = visit.get(identifier);
			if (visited != null) {
				return visited.booleanValue();
			}
			return false;
		}
}
