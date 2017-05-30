/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2012. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.lp.alm.adapter.services;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

/**
 * Helper class providing methods to 
 * - find a path of actions from a given start state to a given target state.
 * - find a path of actions from a given start state to a given specific action. 
 * 
 * Both approaches are implemented using two different strategies:
 * - Depth First Recursive Descent search as quickest most reliable strategy. 
 *   The disadvantage of this strategy is that the path could have a maximal length, dependent on the order of
 *   actions being found.
 * - Breadth First Recursive Descent search that looks at all potential paths from the start state. 
 *   An experimental option tries to pick the shortest distance path.     
 *   
 * @author rschoon
 *
 */
public class WorkflowPathFinder {
	IWorkflowInfo workflowInfo = null;
	IProgressMonitor monitor = null;

	/**
	 * @param workflowInfo
	 * @param monitor
	 */
	public WorkflowPathFinder(IWorkflowInfo workflowInfo,
			IProgressMonitor monitor) {
		super();
		this.workflowInfo = workflowInfo;
		this.monitor = monitor;
	}

	/**
	 * Depth First Recursive Descent search for a path from a given starting state to a
	 * given end state. Dependent on the work flow configuration, this could find a path with intermediate steps.
	 * 
	 * @param currentState
	 * @param targetStateID
	 * @return
	 */
	public Path findPathToState_DF(Identifier<IState> currentState,
			String targetStateID) {
		return findPathToState_DF(currentState, targetStateID, null, null);
	}

	/**
	 * @param currentState
	 * @param targetStateID
	 * @param visitTracker
	 * @param level
	 * @return
	 */
	private Path findPathToState_DF(Identifier<IState> currentState,
			String targetStateID, VisitTracker visitTracker, String level) {
		if (null == level) {
			level = "  ";
		}
		
		System.out.println(level + "Find: "
				+ currentState.getStringIdentifier() + " -> " + targetStateID);
		if (visitTracker == null) {
			visitTracker = new VisitTracker();
		}
		Identifier<IWorkflowAction> availableActions[] = workflowInfo
				.getActionIds(currentState);
		Path path = new Path();
		for (int i = 0; i < availableActions.length; i++) {
			Identifier<IWorkflowAction> action = availableActions[i];
			if (visitTracker.isVisited(currentState.getStringIdentifier()
					+ action.getStringIdentifier())) {
				return path;
			}
			visitTracker.visit(currentState.getStringIdentifier()
					+ action.getStringIdentifier());
			if (workflowInfo.getActionResultState(action).getStringIdentifier()
					.equals(targetStateID)) {
				path.addFront(currentState, action);
				System.out.println(level + "found: " + path.toString());
				return path;
			} else {
				Path found = findPathToState_DF(
						workflowInfo.getActionResultState(action),
						targetStateID, visitTracker, level + "  ");
				if (!found.isEmpty()) {
					found.addFront(currentState, action);
					System.out.println(level + "foundr: " + path.toString());
					return found;
				}
			}
		}
		return path;
	}

	/**
	 * Breadth First Recursive Descent search: Recursive descent search for a path from a given starting 
	 * state to a given end state. Tries to find a minimal path, if option shortestDistance is true.
	 * 
	 * @param currentState
	 * @param targetStateID
	 * @param shortestDistance
	 * @return
	 */
	public Path findPathToState_BF(Identifier<IState> currentState,
			String targetStateID, boolean shortestDistance) {
		return findPathToState_BF(currentState, targetStateID, shortestDistance, null, null);
	}

	private Path findPathToState_BF(Identifier<IState> currentState,
			String targetStateID, boolean shortestDistance, VisitTracker visitTracker, String level) {
		Path currentPath = new Path();
		if (level == null) {
			level = "  ";
		}
		System.out.println(level + "FindPath: "
				+ currentState.getStringIdentifier());
		Identifier<IWorkflowAction> availableActions[] = workflowInfo
				.getActionIds(currentState);
		for (int i = 0; i < availableActions.length; i++) {
			Identifier<IWorkflowAction> action = availableActions[i];
			if (visitTracker == null) {
				visitTracker = new VisitTracker();
			}
			if (visitTracker.isVisited(currentState.getStringIdentifier()
					+ action.getStringIdentifier())) {
				return new Path();
			}
			visitTracker.visit(currentState.getStringIdentifier()
					+ action.getStringIdentifier());
			System.out.println(level + "  Test "
					+ currentState.getStringIdentifier() + " ; "
					+ action.getStringIdentifier());
			if (workflowInfo.getActionResultState(action).getStringIdentifier()
					.equals(targetStateID)) {
				// This is shortest distance from this state, always return
				Path directPath = new Path();
				directPath.addFront(currentState, action);
				System.out.println(level + "  found direct:"
						+ directPath.toString());
				return directPath;
			} else {
				Path found = findPathToState_BF(
						workflowInfo.getActionResultState(action),
						targetStateID, shortestDistance,
						visitTracker, level
								+ "   ("
								+ workflowInfo.getActionResultState(action)
										.getStringIdentifier() + ") ");
				System.out.println(level + "  current:"
						+ currentPath.toString());
				if (!found.isEmpty()) {
					// found a path.
					found.addFront(currentState, action);
					System.out.println(level + "  Possible path:"
							+ found.toString());
					if (shortestDistance) {
						System.out.println(level + "  TestShortest");
						// try all options and keep the shortest
						if (currentPath.isEmpty()) {
							System.out.println(level + "    UseFound...");
							// found a first path, keep it and try the next
							// action.
							currentPath = found;
							continue;
						}
						if (found.pathLenght() < currentPath.pathLenght()) {
							System.out.println(level + "    FoundShorter");
							// We found a shorter path, keep that one.
							currentPath = found;
							continue;
						} else {
							System.out.println(level + "    longer");
						}
					} else {
						System.out.println(level + "    NotShortestDistance");
						return found;
					}
				} else {
					System.out.println(level + "empty...");
				}
			}
		}
		System.out.println(level + "picked:" + currentPath.toString());
		return currentPath;
	}

	/**
	 * Breadth First Recursive Descent search: Finds a path from a state to a target state identified by 
	 * the target action, if option shortestDistance is true.
	 * 
	 * @param currentState
	 * @param resolveActionId
	 * @param workflowInfo
	 * @param shortestDistance
	 * @param visitTracker
	 * @param level
	 * @return
	 */
	public Path findPathToAction_BF(Identifier<IState> currentState,
			Identifier<IWorkflowAction> targetActionId, boolean shortestDistance) {
		return findPathToAction_BF(currentState, targetActionId, shortestDistance,
				null, null);
	}

	/**
	 * @param currentState
	 * @param targetActionId
	 * @param workflowInfo
	 * @param shortestDistance
	 * @param visitTracker
	 * @param level
	 * @return
	 */
	private Path findPathToAction_BF(Identifier<IState> currentState,
			Identifier<IWorkflowAction> targetActionId,
			boolean shortestDistance, VisitTracker visitTracker, String level) {
		Path currentPath = new Path();
		if (level == null) {
			level = "  ";
		}
		if (visitTracker == null) {
			visitTracker = new VisitTracker();
		}
		System.out.println(level + "FindPath: "
				+ currentState.getStringIdentifier());
		Identifier<IWorkflowAction> availableActions[] = workflowInfo
				.getActionIds(currentState);
		for (int i = 0; i < availableActions.length; i++) {
			Identifier<IWorkflowAction> action = availableActions[i];
			if (visitTracker.isVisited(currentState.getStringIdentifier()
					+ action.getStringIdentifier())) {
				return new Path();
			}
			visitTracker.visit(currentState.getStringIdentifier()
					+ action.getStringIdentifier());
			System.out.println(level + "  Test "
					+ currentState.getStringIdentifier() + " ; "
					+ action.getStringIdentifier());
			if (action.getStringIdentifier().equals(
					targetActionId.getStringIdentifier())) {
				// This is shortest distance from this state, always return
				Path directPath = new Path();
				directPath.addFront(currentState, action);
				System.out.println(level + "  found direct:"
						+ directPath.toString());
				return directPath;
			} else {
				Path found = findPathToAction_BF(
						workflowInfo.getActionResultState(action),
						targetActionId,
						shortestDistance,
						visitTracker,
						level
								+ "   ("
								+ workflowInfo.getActionResultState(action)
										.getStringIdentifier() + ") ");
				System.out.println(level + "  current:"
						+ currentPath.toString());
				if (!found.isEmpty()) {
					// found a path.
					found.addFront(currentState, action);
					System.out.println(level + "  Possible path:"
							+ found.toString());
					if (shortestDistance) {
						System.out.println(level + "  TestShortest");
						// try all options and keep the shortest
						if (currentPath.isEmpty()) {
							System.out.println(level + "    UseFound...");
							// found a first path, keep it and try the next
							// action.
							currentPath = found;
							continue;
						}
						if (found.pathLenght() < currentPath.pathLenght()) {
							System.out.println(level + "    FoundShorter");
							// We found a shorter path, keep that one.
							currentPath = found;
							continue;
						} else {
							System.out.println(level + "    longer");
						}
					} else {
						System.out.println(level + "    NotShortestDistance");
						return found;
					}
				} else {
					System.out.println(level + "empty...");
				}
			}
		}
		System.out.println(level + "picked:" + currentPath.toString());
		return currentPath;
	}

	/**
	 * Depth First Recursive Descent search, finds a path from a state to a target state identified
	 * by the target action. Dependent on the work flow configuration, this could find a 
	 * path with intermediate steps.
	 * 
	 * @param currentState
	 * @param targetActionId
	 * @param workflowInfo
	 * @param visitTracker
	 * @return
	 * 
	 */
	public Path findPathToAction_DF(Identifier<IState> currentState,
			Identifier<IWorkflowAction> targetActionId) {
		return findPathToAction_DF(currentState, targetActionId, null);

	}

	/**
	 * @param currentState
	 * @param targetActionId
	 * @param workflowInfo
	 * @param visitTracker
	 * @return
	 * 
	 */
	private Path findPathToAction_DF(Identifier<IState> currentState,
			Identifier<IWorkflowAction> targetActionId,
			VisitTracker visitTracker) {
		if (visitTracker == null) {
			visitTracker = new VisitTracker();
		}
		Identifier<IWorkflowAction> availableActions[] = workflowInfo
				.getActionIds(currentState);
		Path path = new Path();
		for (int i = 0; i < availableActions.length; i++) {
			Identifier<IWorkflowAction> action = availableActions[i];
			if (visitTracker.isVisited(action.getStringIdentifier())) {
				return path;
			}
			visitTracker.visit(action.getStringIdentifier());
			if (action.getStringIdentifier().equals(
					targetActionId.getStringIdentifier())) {
				path.addFront(currentState, action);
				return path;
			} else {
				Path found = findPathToAction_DF(
						workflowInfo.getActionResultState(action),
						targetActionId, visitTracker);
				if (!found.isEmpty()) {
					found.addFront(currentState, action);
					return found;
				}
			}
		}
		return path;
	}
}
