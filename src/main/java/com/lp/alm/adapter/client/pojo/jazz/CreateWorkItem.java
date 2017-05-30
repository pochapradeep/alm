package com.lp.alm.adapter.client.pojo.jazz;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.WorkItemOperation;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;

public class CreateWorkItem {

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

	private static class WorkItemInitialization extends WorkItemOperation {

		private String fSummary;
		private ICategoryHandle fCategory;
		private ITeamRepository fTeamRepository;
		private IProjectArea fProjectArea;
		private String fPriorityEnumerationString;
		private String jiraIssueId;
		private String acceptanceCriteriaValue;
		private Timestamp dueDate;
		private String fDescription;

		public WorkItemInitialization(ITeamRepository teamRepository, IProjectArea projectArea, String summary,
				ICategoryHandle category, String priorityString, String jiraIssueId, String acceptanceCriteriaValue,
				Timestamp dueDate, String description) {
			super("Initializing Work Item");
			fSummary = summary;
			fCategory = category;
			fPriorityEnumerationString = priorityString;
			fTeamRepository = teamRepository;
			fProjectArea = projectArea;
			this.jiraIssueId = jiraIssueId;
			this.acceptanceCriteriaValue = acceptanceCriteriaValue;
			this.dueDate = dueDate;
			this.fDescription = description;

		}

		@Override
		protected void execute(WorkItemWorkingCopy workingCopy, IProgressMonitor monitor)
				throws TeamRepositoryException {
			IWorkItem workItem = workingCopy.getWorkItem();
			workItem.setHTMLSummary(XMLString.createFromPlainText(fSummary));
			workItem.setCategory(fCategory);
			workItem.setHTMLDescription(XMLString.createFromPlainText(fDescription));
			if(dueDate!=null){
				workItem.setDueDate(dueDate);
			}

			IWorkItemClient workItemClient = (IWorkItemClient) fTeamRepository.getClientLibrary(IWorkItemClient.class);
			IAttribute priority = workItemClient.findAttribute(fProjectArea, "internalPriority", null);

			if (null != priority && workItem.hasAttribute(priority)) {
				workItem.setValue(priority,
						getLiteralEqualsString(fPriorityEnumerationString, workItemClient, priority));
				Object prio = workItem.getValue(priority);
				if (prio instanceof Identifier<?>) {
					Identifier<?> identifier = (Identifier<?>) prio;
					System.out.print("\tIdentifier: " + identifier.getStringIdentifier());
				}
			}

			IAttribute attr = workItemClient.findAttribute(workingCopy.getWorkItem().getProjectArea(),
					"com.ibm.team.apt.attribute.acceptance", monitor);
			if (attr != null) {
				if (workItem.hasAttribute(attr))
					workItem.setValue(attr, acceptanceCriteriaValue);
			}

			IAttribute customString = workItemClient.findAttribute(fProjectArea, "external_link", monitor);

			if (workItem.hasCustomAttribute(customString))
				workItem.setValue(customString, jiraIssueId);

		}
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

	/*
	 * public static void main(String[] args) {
	 * 
	 * boolean result; TeamPlatform.startup(); try { result = run(args); } catch
	 * (TeamRepositoryException x) { x.printStackTrace(); result = false; }
	 * finally { TeamPlatform.shutdown(); }
	 * 
	 * }
	 */

	public static int executePush(String typeIdentifier, String priorityValue, String jiraIssueId,
			String acceptanceCriteria, String summary, Timestamp dueDate, String description) {

		int result =-1;
		TeamPlatform.startup();
		try {
			// String typeIdentifier = "task";
			// result = run(args);
			return result = run(typeIdentifier, priorityValue, jiraIssueId, acceptanceCriteria, summary,dueDate,description);
		} catch (TeamRepositoryException x) {
			x.printStackTrace();
			result = -1;
		} finally {
			TeamPlatform.shutdown();
		}
		return result;

	}
	
	/**
	 * "Usage: CreateWorkItem <repositoryURI> <userId> <password> <projectArea> <workItemType> <summary> <dueDate> <description>"
	 */
	private static int run(String typeIdentifier, String priorityValue, String jiraIssueId,
			String acceptanceCriteria, String summary, Timestamp dueDate, String description) throws TeamRepositoryException {

		
		System.out.println(
				"Usage: CreateWorkItem <workItemType> <priorityValue> <jiraIssueId> <acceptanceCriteria> <summary> <summary> <category>");

		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];
		// String typeIdentifier = "com.ibm.team.apt.workItemType.story";

		String categoryName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];

		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(null);

		
		IProcessClientService processClient = (IProcessClientService) teamRepository
				.getClientLibrary(IProcessClientService.class);
		IAuditableClient auditableClient= (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class); 
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);

		URI uri = URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea = (IProjectArea) processClient.findProcessArea(uri, null, null);
		if (projectArea == null) {
			System.out.println("Project area not found.");
			return -1;
		}
		IWorkItemType workItemType = workItemClient.findWorkItemType(projectArea, typeIdentifier, null);
		if (workItemType == null) {
			System.out.println("Work item type not found.");
			return -1;
		}

		List path = Arrays.asList(categoryName.split("/"));
		ICategoryHandle category = workItemClient.findCategoryByNamePath(projectArea, path, null);
		if (category == null) {
			System.out.println("Category not found.");
			return -1;
		}

		WorkItemInitialization operation = new WorkItemInitialization(teamRepository, projectArea, summary, category,
				priorityValue, jiraIssueId, acceptanceCriteria,dueDate,description);
		IWorkItemHandle handle = operation.run(workItemType, null);
		IWorkItem workItem = auditableClient.resolveAuditable(handle, IWorkItem.FULL_PROFILE, null);
		System.out.println("Created work item " + workItem.getId() + ".");
		System.out.println("Created work item Item Id" + workItem.getItemId() + ".");

		teamRepository.logout();

		return workItem.getId();
	}
}