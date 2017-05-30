/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2012. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.lp.alm.adapter.services;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;

/**
 * Used to store sequences of states and actions to transition form one state to another using an 
 * action/transition.
 * 
 * @author rschoon
 *
 */
public class Path {
	private ArrayList<PathElement> path = null;

	public Path() {
		path = new ArrayList<PathElement>();
	}

	/**
	 * Returns true if the path has no elements, false otherwise
	 * @return
	 */
	public boolean isEmpty() {
		return path.isEmpty();
	}

	/**
	 * @return the elements of the path
	 */
	public ArrayList<PathElement> getElements() {
		return path;
	}
	
	/**
	 * @return the length of the path.
	 */
	public int pathLenght(){
		return path.size();
	}

	/**
	 * Appends a transition at the end of the path
	 * 
	 * @param state
	 * @param action
	 */
	public void append(Identifier<IState> state,
			Identifier<IWorkflowAction> action) {
		path.add(new PathElement(state, action));
	}

	/**
	 * Adds a transition at the beginning of the path.
	 * 
	 * @param state
	 * @param action
	 */
	public void addFront(Identifier<IState> state,
			Identifier<IWorkflowAction> action) {
		path.add(0, new PathElement(state, action));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * 
	 * Get a string representing the transitions
	 * 
	 */
	public String toString(){
		String pathString = "Length: " + pathLenght();
		for (Iterator<PathElement> iterator = path.iterator(); iterator.hasNext();) {
			PathElement transition = (PathElement) iterator.next();
			pathString+= "( "+transition.getStateID()+" ; "+transition.getActionID()+" ) ";
		}
		return pathString;
		
	}
}