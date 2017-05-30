/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2008. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.lp.alm.adapter.client.pojo.jazz;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;

/**
 * Example code, see
 * https://jazz.net/wiki/bin/view/Main/ProgrammaticWorkItemCreation.
 */
public class ModifyWorkItem {

	private static class LoginHandler implements ILoginHandler, ILoginInfo {

		private String fUserId;
		private String fPassword;

		private LoginHandler(String userId, String password) {
			fUserId = userId;
			fPassword = password;
		}

		public String getUserId() {
			return fUserId;
		}

		public String getPassword() {
			return fPassword;
		}

		public ILoginInfo challenge(ITeamRepository repository) {
			return this;
		}
	}
/*
	public static void main(String[] args) {

		boolean result;
		TeamPlatform.startup();
		try {
			result = run(args);
		} catch (TeamRepositoryException x) {
			x.printStackTrace();
			result = false;
		} finally {
			TeamPlatform.shutdown();
		}

		if (!result)
			System.exit(1);

	}*/
	
	
/*	public static void main(String[] args) {

		boolean result;
		TeamPlatform.startup();
		try {
			result = run(args);
		} catch (TeamRepositoryException x) {
			x.printStackTrace();
			result = false;
		} finally {
			TeamPlatform.shutdown();
		}

		if (!result)
			System.exit(1);

	}*/

	private static int run(String idString, String priorityValue,
			String acceptanceCriteria, String summary, Timestamp dueDate, String description) throws TeamRepositoryException {


		System.out
		.println("Usage: ModifyWorkItem [repositoryURI] [userId] [password] [projectArea] [workItemID] [summary]");
//		String repositoryURI = args[0];
//		String userId = args[1];
//		String password = args[2];
//		String projectAreaName = args[3];
		
		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];
		

		ITeamRepository teamRepository = TeamPlatform
				.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(null);

		IProcessClientService processClient = (IProcessClientService) teamRepository
				.getClientLibrary(IProcessClientService.class);
		IAuditableClient auditableClient = (IAuditableClient) teamRepository
				.getClientLibrary(IAuditableClient.class);
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository
				.getClientLibrary(IWorkItemClient.class);

		URI uri = URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea = (IProjectArea) processClient
				.findProcessArea(uri, null, null);
		if (projectArea == null) {
			System.out.println("Project area not found.");
			return -1;
		}

		int id = new Integer(idString).intValue();

		IWorkItem workItem = workItemClient.findWorkItemById(id,
				IWorkItem.SMALL_PROFILE, null);
		IWorkItemWorkingCopyManager wcm = workItemClient
				.getWorkItemWorkingCopyManager();

		wcm.connect(workItem, IWorkItem.FULL_PROFILE, null);

		try {
			WorkItemWorkingCopy wc = wcm.getWorkingCopy(workItem);

			wc.getWorkItem().setHTMLSummary(
					XMLString.createFromPlainText(summary));
			IWorkItem wi = wc.getWorkItem();
			
			wi.setHTMLDescription(XMLString.createFromPlainText(description));
			if(dueDate!=null){
				wi.setDueDate(dueDate);
			}

			IAttribute priority = workItemClient.findAttribute(projectArea, "internalPriority", null);

			if (null != priority && wi.hasAttribute(priority)) {
				wi.setValue(priority,
						getLiteralEqualsString(priorityValue, workItemClient, priority));
				Object prio = wi.getValue(priority);
				if (prio instanceof Identifier<?>) {
					Identifier<?> identifier = (Identifier<?>) prio;
					System.out.print("\tIdentifier: " + identifier.getStringIdentifier());
				}
			}
			IAttribute attr = workItemClient.findAttribute(wi.getProjectArea(),
					"com.ibm.team.apt.attribute.acceptance", null);
			if (attr != null) {
				if (wi.hasAttribute(attr))
					wi.setValue(attr, acceptanceCriteria);
			}

			IDetailedStatus s = wc.save(null);
			if (!s.isOK()) {
				throw new TeamRepositoryException("Error saving work item",
						s.getException());
			}
		} finally {
			wcm.disconnect(workItem);
		}

		System.out.println("Modified work item: " + workItem.getId() + ".");

		teamRepository.logout();

		return workItem.getId();
	}
	private static Identifier<? extends ILiteral> getLiteralEqualsString(String name, IWorkItemCommon workItemCommon,
			IAttributeHandle ia) throws TeamRepositoryException {
		Identifier<? extends ILiteral> literalID = null;
		IEnumeration<? extends ILiteral> enumeration = workItemCommon.resolveEnumeration(ia, null);

		List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
		for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();
			if (iLiteral.getName().equals(name)) {
				literalID = iLiteral.getIdentifier2();
				break;
			}
		}
		return literalID;
	}
	public static int executePush(String idString, String priorityValue, String jiraIssueId,
			String acceptanceCriteria, String summary, Timestamp dueDate, String description) {

		int result =-1;
		TeamPlatform.startup();
		try {
			// String typeIdentifier = "task";
			// result = run(args);
			return result = run(idString, priorityValue, acceptanceCriteria, summary,dueDate,description);
		} catch (TeamRepositoryException x) {
			x.printStackTrace();
			result = -1;
		} finally {
			TeamPlatform.shutdown();
		}
		return result;

	}
}
