/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2012. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.lp.alm.adapter.services;

import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;

/**
 * Represents a transition in a path, stores a state and a transition/action. 
 * For example store the source state and the transition.
 * 
 * @author rschoon
 *
 */
public class PathElement {
	private Identifier<IState> state;
	private Identifier<IWorkflowAction> action;

	public PathElement(Identifier<IState> state,
			Identifier<IWorkflowAction> action) {
		this.state = state;
		this.action = action;
	}

	public String getStateID() {
		
		return getState().getStringIdentifier();
	}

	public Identifier<IWorkflowAction> getAction() {
		return action;
	}

	public String getActionID() {
		return action.getStringIdentifier();
	}

	public Identifier<IState> getState() {
		return state;
	}
}

