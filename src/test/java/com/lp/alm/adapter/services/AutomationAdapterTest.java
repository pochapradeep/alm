package com.lp.alm.adapter.services;

import static com.atlassian.jira.rest.client.api.domain.EntityHelper.findEntityByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.joda.time.DateTime;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptionsBuilder;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.WorkItemOperation;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;
import com.lp.alm.adapter.rest.client.api.JiraClientFactory;
import com.lp.alm.adapter.utils.AdapterUtils;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.oslc.client.jazz.JazzArtifactBuilder;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class AutomationAdapterTest {

	// @Test
	public void testIntegratedTestCase()
			throws URISyntaxException, IOException, ServletException, InterruptedException {

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();

		final Iterable<CimProject> metadataProjects = issueClient
				.getCreateIssueMetadata(new GetCreateIssueMetadataOptionsBuilder()
						.withProjectKeys(OSLCConstants.PROJECT[OSLCConstants.JIRA_OFFSET])
						.withExpandedIssueTypesFields().build())
				.claim();

		// select project and issue
		assertEquals(1, Iterables.size(metadataProjects));
		final CimProject project = metadataProjects.iterator().next();
		final CimIssueType issueType = findEntityByName(project.getIssueTypes(), "Story");

		// grab the first priority
		final Iterable<Object> allowedValuesForPriority = issueType.getField(IssueFieldId.PRIORITY_FIELD)
				.getAllowedValues();
		assertNotNull(allowedValuesForPriority);
		assertTrue(allowedValuesForPriority.iterator().hasNext());
		final BasicPriority priority = (BasicPriority) allowedValuesForPriority.iterator().next();

		// build issue input
		final String summary = "My new integrated story !";
		final String description = "Story created through integrated test case";

		final IssueInputBuilder issueInputBuilder = new IssueInputBuilder(project, issueType, summary)
				.setDescription(description).setPriority(priority);
		// create
		final BasicIssue basicCreatedIssue = issueClient.createIssue(issueInputBuilder.build()).claim();
		assertNotNull(basicCreatedIssue.getKey());

		// getLatestIssues
		List<Issue> latestCreatedIssues = JiraArtifactBuilder.getLatestCreatedIssues();

		// get issue and check if everything was set as we expected
		final Issue createdIssue = latestCreatedIssues.get(0);
		assertNotNull(createdIssue);

		assertEquals(basicCreatedIssue.getKey(), createdIssue.getKey());
		assertEquals(project.getKey(), createdIssue.getProject().getKey());
		assertEquals(issueType.getId(), createdIssue.getIssueType().getId());
		assertEquals(summary, createdIssue.getSummary());
		assertEquals(description, createdIssue.getDescription());
		assertTrue(!latestCreatedIssues.isEmpty());

		AutomationAdapter.pushToRemoteJazzSystem(createdIssue);

		TimeUnit.SECONDS.sleep(10);

		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(createdIssue,
				OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		assertNotNull(remoteIssueLink);
		List<ChangeRequest> latestIssues = JazzArtifactBuilder.getLatestIssues();
		assertTrue(!latestIssues.isEmpty());
		assertTrue(!JazzArtifactBuilder.isQualifiedForPush(latestIssues.get(0)));

	}

	public void testBothCreateAndModifyRTCWorkItem()
			throws URISyntaxException, IOException, ServletException, InterruptedException {

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();

		final Iterable<CimProject> metadataProjects = issueClient
				.getCreateIssueMetadata(new GetCreateIssueMetadataOptionsBuilder()
						.withProjectKeys(OSLCConstants.PROJECT[OSLCConstants.JIRA_OFFSET])
						.withExpandedIssueTypesFields().build())
				.claim();

		// select project and issue
		assertEquals(1, Iterables.size(metadataProjects));
		final CimProject project = metadataProjects.iterator().next();
		final CimIssueType issueType = findEntityByName(project.getIssueTypes(), "New Feature");

		// grab the first priority
		final Iterable<Object> allowedValuesForPriority = issueType.getField(IssueFieldId.PRIORITY_FIELD)
				.getAllowedValues();
		assertNotNull(allowedValuesForPriority);
		assertTrue(allowedValuesForPriority.iterator().hasNext());
		final BasicPriority priority = (BasicPriority) allowedValuesForPriority.iterator().next();

		// build issue input
		final String summary = "My new integrated story !";
		final String description = "Story created through integrated test case";

		final IssueInputBuilder issueInputBuilder = new IssueInputBuilder(project, issueType, summary)
				.setDescription(description).setPriority(priority);
		// create
		final BasicIssue basicCreatedIssue = issueClient.createIssue(issueInputBuilder.build()).claim();
		assertNotNull(basicCreatedIssue.getKey());

		// getLatestIssues
		List<Issue> latestCreatedIssues = JiraArtifactBuilder.getLatestCreatedIssues();

		// get issue and check if everything was set as we expected
		final Issue createdIssue = latestCreatedIssues.get(0);
		assertNotNull(createdIssue);

		assertEquals(basicCreatedIssue.getKey(), createdIssue.getKey());
		assertEquals(project.getKey(), createdIssue.getProject().getKey());
		assertEquals(issueType.getId(), createdIssue.getIssueType().getId());
		assertEquals(summary, createdIssue.getSummary());
		assertEquals(description, createdIssue.getDescription());
		assertTrue(!latestCreatedIssues.isEmpty());

		AutomationAdapter.pushToRemoteJazzSystem(createdIssue);

		TimeUnit.SECONDS.sleep(10);

		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(createdIssue,
				OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		assertNotNull(remoteIssueLink);
		List<ChangeRequest> latestIssues = JazzArtifactBuilder.getLatestIssues();
		assertTrue(!latestIssues.isEmpty());
		assertTrue(!JazzArtifactBuilder.isQualifiedForPush(latestIssues.get(0)));

	}

	@Test
	public void testCompleteCycle() throws URISyntaxException, IOException, ServletException, TeamRepositoryException {

		Issue createJiraIssue = createJiraIssueAndAssert();

		int rtcWorkItemId = AutomationAdapter.pushToRemoteJazzSystemV2(createJiraIssue);
		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		final Issue justCreatedIssue = issueClient.getIssue(createJiraIssue.getKey()).claim();// LOYAL-37

		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(justCreatedIssue,
				OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		System.out.println("remote issue link is " + remoteIssueLink);
		assertEquals(rtcWorkItemId, Integer.parseInt(remoteIssueLink));

		List<ChangeRequest> latestIssues = JazzArtifactBuilder.getLatestIssues();
		assertEquals(1, latestIssues.size());
		assertTrue(JazzArtifactBuilder.isQualifiedForPush(latestIssues.get(0)) == false);
		String status = AdapterUtils.convertToJiraEq(latestIssues.get(0).getStatus());
		System.out.println("Status of item in Jira should be "+status);
		assertTrue(JiraArtifactBuilder.isAttributeInSync(status, latestIssues.get(0).getExternal_link()));

		changeStateOfRTCWorkItem(Integer.toString(rtcWorkItemId));
		List<ChangeRequest> latestIssuesAfterTransition = JazzArtifactBuilder.getLatestIssues();

		String statusAfterTransistion = AdapterUtils.convertToJiraEq(latestIssuesAfterTransition.get(0).getStatus());
		assertTrue(JiraArtifactBuilder.isAttributeInSync(statusAfterTransistion,
				latestIssuesAfterTransition.get(0).getExternal_link()) == false);
		assertTrue(JazzArtifactBuilder.isQualifiedForPush(latestIssuesAfterTransition.get(0)));
		assertEquals(latestIssuesAfterTransition.size(), 1);

		JiraArtifactBuilder.pushToRemoteJiraSystem(latestIssuesAfterTransition.get(0).getExternal_link(),
				statusAfterTransistion);

		// get issue and check if everything was set as we expected
		List<Issue> latestCreatedIssues = JiraArtifactBuilder.getLatestCreatedIssues();
		List<Issue> latestUpdatedIssues = JiraArtifactBuilder.getLatestModifiedIssues();

		assertTrue(latestCreatedIssues.isEmpty());
		assertTrue(latestUpdatedIssues.isEmpty());

		final Issue modifiedIssue = issueClient.getIssue(latestIssuesAfterTransition.get(0).getExternal_link()).claim();// LOYAL-37
		System.out.println("status after transistion is " + modifiedIssue.getStatus().getName());
		assertEquals(statusAfterTransistion, modifiedIssue.getStatus().getName());

	}

	private Issue createJiraIssueAndAssert() throws URISyntaxException {
		final String summary = "My new issue!";
		final String description = "Some description";

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();

		final Iterable<CimProject> metadataProjects = issueClient.getCreateIssueMetadata(
				new GetCreateIssueMetadataOptionsBuilder().withProjectKeys("PM").withExpandedIssueTypesFields().build())
				.claim();

		// select project and issue
		assertEquals(1, Iterables.size(metadataProjects));
		final CimProject project = metadataProjects.iterator().next();
		final CimIssueType issueType = findEntityByName(project.getIssueTypes(), "New Feature");

		// grab the first priority
		final Iterable<Object> allowedValuesForPriority = issueType.getField(IssueFieldId.PRIORITY_FIELD)
				.getAllowedValues();
		assertNotNull(allowedValuesForPriority);
		assertTrue(allowedValuesForPriority.iterator().hasNext());
		final BasicPriority priority = (BasicPriority) allowedValuesForPriority.iterator().next();

		// build issue input

		final BasicUser assignee = new BasicUser(getUserUri("admin"), "admin", "admin");
		final List<String> affectedVersionsNames = Collections.emptyList();
		final DateTime dueDate = new DateTime(new Date().getTime());
		// final ArrayList<String> fixVersionsNames = Lists.newArrayList("1.1");

		// prepare IssueInput
		// final String multiUserCustomFieldId = "customfield_10031";
		final ImmutableList<BasicUser> multiUserCustomFieldValues = ImmutableList.of(assignee, assignee);
		final IssueInputBuilder issueInputBuilder = new IssueInputBuilder(project, issueType, summary)
				.setDescription(description).setAssignee(assignee).setAffectedVersionsNames(affectedVersionsNames);
		// .setFixVersionsNames(fixVersionsNames).setDueDate(dueDate).setPriority(priority);
		// .setFieldValue(multiUserCustomFieldId, multiUserCustomFieldValues);

		// final IssueInputBuilder issueInputBuilder = new
		// IssueInputBuilder(project, issueType, summary)
		// .setDescription(description).setPriority(priority);

		// create
		final BasicIssue basicCreatedIssue = issueClient.createIssue(issueInputBuilder.build()).claim();
		assertNotNull(basicCreatedIssue.getKey());

		final Issue justCreatedIssue = issueClient.getIssue(basicCreatedIssue.getKey()).claim();// LOYAL-37

		List<Issue> latestCreatedIssues1 = JiraArtifactBuilder.getLatestCreatedIssues();

		// get issue and check if everything was set as we expected
		assertTrue(latestCreatedIssues1.isEmpty());

		final Iterable<Transition> transitions = issueClient.getTransitions(justCreatedIssue.getTransitionsUri())
				.claim();

		final Transition startProgressTransition = getTransitionByName(transitions, "APPROVED AND FUNDED");
		if (startProgressTransition == null) {
		}
		issueClient
				.transition(justCreatedIssue.getTransitionsUri(), new TransitionInput(startProgressTransition.getId()))
				.claim();

		final Transition startProgressTransition1 = getTransitionByName(transitions, "TO BE DEVELOPED");
		if (startProgressTransition1 == null) {
		}
		issueClient
				.transition(justCreatedIssue.getTransitionsUri(), new TransitionInput(startProgressTransition1.getId()))
				.claim();

		// getLatestIssues
		List<Issue> latestCreatedIssues = JiraArtifactBuilder.getLatestCreatedIssues();

		// get issue and check if everything was set as we expected
		final Issue createdIssue = latestCreatedIssues.get(0);
		assertNotNull(createdIssue);

		assertEquals(basicCreatedIssue.getKey(), createdIssue.getKey());
		assertEquals(project.getKey(), createdIssue.getProject().getKey());
		assertEquals(issueType.getId(), createdIssue.getIssueType().getId());
		assertEquals(summary, createdIssue.getSummary());
		assertEquals(description, createdIssue.getDescription());
		assertTrue(!latestCreatedIssues.isEmpty());

		return createdIssue;
	}

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

	public static URI getUserUri(String username) {
		return UriBuilder.fromUri("http://34.209.145.222:8082").path("/rest/api/" + 2 + "/user")
				.queryParam("username", username).build();
	}

	private static Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
		for (Transition transition : transitions) {
			if (transition.getName().equals(transitionName)) {
				return transition;
			}
		}
		return null;
	}

	/**
	 * Resolve using the resolve transition
	 * 
	 * @param workItem
	 * @param monitor
	 * @throws TeamRepositoryException
	 */
	private static boolean changeStateOfRTCWorkItem(String idString) throws TeamRepositoryException {

		/*
		 * if (args.length != 5) { System.out.println(
		 * "Usage: ModifyWorkItem <repositoryURI> <userId> <password> <projectArea> <workItemID>"
		 * ); return false; }
		 */

		if (!TeamPlatform.isStarted()) {
			System.out.println("Starting");
			TeamPlatform.startup();
		}

		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];

		// String repositoryURI = args[0];
		// String userId = args[1];
		// String password = args[2];
		// String projectAreaName = args[3];

		IProgressMonitor monitor = new NullProgressMonitor();
		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(null);

		IProcessClientService processClient = (IProcessClientService) teamRepository
				.getClientLibrary(IProcessClientService.class);
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);

		URI uri = URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea = (IProjectArea) processClient.findProcessArea(uri, null, monitor);
		if (projectArea == null) {
			System.out.println("Project area not found.");
			return false;
		}

		int id = new Integer(idString).intValue();
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, monitor);
		IWorkflowInfo workflowInfo = workItemClient.findWorkflowInfo(workItem, monitor);
		if (workflowInfo != null) {
			// check if already closed
			if (workflowInfo.getStateGroup(workItem.getState2()) != IWorkflowInfo.CLOSED_STATES) {
				Identifier<IWorkflowAction> resolveActionId = workflowInfo.getResolveActionId();
				if (resolveActionId != null) {
					WorkflowPathFinder tManager = new WorkflowPathFinder(workflowInfo, monitor);
					Path path = tManager.findPathToAction_BF(workItem.getState2(), resolveActionId, true);
					WorkItemSetWorkflowActionModification workflowOperation = new WorkItemSetWorkflowActionModification(
							null);
					ArrayList<PathElement> transitions = path.getElements();
					for (Iterator<PathElement> iterator = transitions.iterator(); iterator.hasNext();) {
						PathElement pathElement = (PathElement) iterator.next();
						workflowOperation.setfWorkFlowAtion(pathElement.getActionID());
						workflowOperation.run(workItem, monitor);
						System.out.println("Workflow Action for work item " + workItem.getId() + " : "
								+ pathElement.getActionID());
					}
				}
			}
		}
		System.out.println("Modified work item " + workItem.getId() + ".");
		teamRepository.logout();
		TeamPlatform.shutdown();

		return true;
	}

	private static class WorkItemSetWorkflowActionModification extends WorkItemOperation {

		private String fWorkFlowAtion;

		public WorkItemSetWorkflowActionModification(String workFlowAtion) {
			super("Modifying Work Item State", IWorkItem.FULL_PROFILE);
			fWorkFlowAtion = workFlowAtion;
		}

		@Override
		protected void execute(WorkItemWorkingCopy workingCopy, IProgressMonitor monitor)
				throws TeamRepositoryException {
			workingCopy.setWorkflowAction(fWorkFlowAtion);
		}

		public void setfWorkFlowAtion(String fWorkFlowAtion) {
			this.fWorkFlowAtion = fWorkFlowAtion;
		}
	}
}
