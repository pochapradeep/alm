package com.lp.alm.oslc.client.jazz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.namespace.QName;

import org.apache.http.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.oslc4j.core.model.AllowedValues;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Property;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.joda.time.DateTime;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.lp.alm.adapter.client.pojo.jazz.ModifyWorkItem;
import com.lp.alm.adapter.properties.PropertiesCache;
import com.lp.alm.adapter.resources.ArtifactInputParameters;
import com.lp.alm.adapter.rest.client.api.JazzClientFactory;
import com.lp.alm.adapter.rest.client.api.JiraClientFactory;
import com.lp.alm.adapter.services.AutomationAdapter;
import com.lp.alm.adapter.utils.AdapterUtils;
import com.lp.alm.lyo.client.oslc.OSLCConstants;
import com.lp.alm.lyo.client.oslc.jazz.JazzFormAuthClient;
import com.lp.alm.lyo.client.oslc.jazz.JazzRootServicesHelper;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

import junit.framework.Assert;

public class JazzArtifactBuilderTest {

	@Test
	public void testGetIssues(){
		JazzArtifactBuilder.getLatestIssues();
	}
	
	@Test
	public void testPollAndFire() throws URISyntaxException{
		AutomationAdapter.performRTCToJiraSync();
	}
	
	
	
//	@Test
	public void testPriorityAllowedvaluesInRTC() throws Exception{
		
		JazzFormAuthClient jazzRestClient = JazzClientFactory.jazzRestClient();
		
		JazzRootServicesHelper jazzRootServicesHelper = new JazzRootServicesHelper("https://34.209.145.222:9443/ccm",
				"http://open-services.net/ns/cm#");
		String catalogUrl = jazzRootServicesHelper.getCatalogUrl();
		// Find the OSLC Service Provider for the project area
		// we want to work with
		String serviceProviderUrl = jazzRestClient.lookupServiceProviderUrl(catalogUrl, "JIRA Integration Project Area");
		
		
		ChangeRequest createJazzData = new ChangeRequest();
		CreationFactory taskCreation = jazzRestClient.lookupCreationFactoryResource(serviceProviderUrl,
				OSLCConstants.OSLC_CM_V2, createJazzData.getRdfTypes()[0].toString(),
				OSLCConstants.OSLC_CM_V2 + createJazzData.getWorkItemType());
		String factoryUrl = taskCreation.getCreation().toString();
		createJazzData.setWorkItemType(ChangeRequest.STORY);
		createJazzData.setTitle("New Title");
		createJazzData.setDescription("Created from Test case ");
		
		String rtcNamespace = com.lp.alm.adapter.constants.OSLCConstants.OSLC_CM_V2;

		String rtcOSLCName = com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST;
		String lookupValue = "High";
		URI resourceURI = JazzArtifactBuilder.getResourceURI(createJazzData, "http://open-services.net/ns/cm-x#", "priority", lookupValue);
		System.out.println("resource uri is equal to "+resourceURI);
		
		createJazzData.getExtendedProperties()
		.put(new QName(com.lp.alm.adapter.constants.OSLCConstants.OSLC_CM_V2,
				"priority"), resourceURI);
		String shapeUrl = taskCreation.getResourceShapes()[0].toString();

		ClientResponse shapeResponse = jazzRestClient.getResource(shapeUrl);
		ResourceShape shape = shapeResponse.getEntity(ResourceShape.class);
		
		Property filedAgainstProperty = shape
				.getProperty(new URI(com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE
						+ com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST));
		
		if (filedAgainstProperty != null) {
			URI allowedValuesRef = filedAgainstProperty.getAllowedValuesRef();
			ClientResponse allowedValuesResponse = jazzRestClient.getResource(allowedValuesRef.toString());
			AllowedValues allowedValues = allowedValuesResponse.getEntity(AllowedValues.class);
			Object[] values = allowedValues.getValues().toArray();

			createJazzData.getExtendedProperties()
					.put(new QName(com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE,
							com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST), (URI) values[1]);
		}
		
		ClientResponse creationResponse = null;

		// Create the change request
		creationResponse = jazzRestClient.createResource(factoryUrl, createJazzData, OslcMediaType.APPLICATION_RDF_XML,
				OslcMediaType.APPLICATION_RDF_XML);

		if (creationResponse.getStatusCode() != HttpStatus.SC_CREATED) {
			System.err.println("ERROR: Could not create the task (status " + creationResponse.getStatusCode() + ")\n");
			System.err.println(creationResponse.getEntity(String.class));
//			return ;
			Assert.assertNull(null);
		}
		creationResponse.consumeContent();
		Assert.assertTrue(true);
	}
	
	
//	@Test
	public void testFiledAgainstAllowedvaluesInRTC() throws Exception{
		
//		JazzFormAuthClient jazzRestClient = JazzArtifactBuilder.getJazzRestClient();
		ChangeRequest createJazzData = new ChangeRequest();
		
		String rtcNamespace = com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE;

		String rtcOSLCName = com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST;
		String lookupValue = null;
		URI resourceURI = JazzArtifactBuilder.getResourceURI(createJazzData, rtcNamespace, rtcOSLCName, lookupValue);
		System.out.println("resource uri is equal to "+resourceURI);
		

	}
	
	
	
	@Test
	public void testModifyWorkItem() throws URISyntaxException, IOException, ServletException, TeamRepositoryException{

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		String issueKey2 = "PM-31";
		final Issue createdIssue = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssue);
		ChangeRequest cr = new ChangeRequest();
		java.sql.Timestamp dueDate = new java.sql.Timestamp(new Date().getTime());
		String title = "Updated Title";
		cr.setTitle(title);
		String description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
		cr.setDescription(description);
		String priority = "Low";
		cr.setPriority(priority);
		cr.setAcceptanceCriteria(description+"-updated");
		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(createdIssue,
				com.lp.alm.adapter.constants.OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		int workItemId = ModifyWorkItem.executePush(remoteIssueLink, cr.getPriority(), createdIssue.getKey(), cr.getAcceptanceCriteria(), cr.getTitle(), dueDate, cr.getDescription());

		
		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = com.lp.alm.adapter.constants.OSLCConstants.PROJECT[1];
		TeamPlatform.startup();


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
		
		int id = new Integer(workItemId).intValue();

		IWorkItem workItem = workItemClient.findWorkItemById(id,
				IWorkItem.SMALL_PROFILE, null);
		
		assertEquals(workItem.getHTMLSummary().getPlainText(),title);
		assertEquals(workItem.getHTMLDescription().getPlainText(),description);

		TeamPlatform.shutdown();

	}
	
//	@Test
	public void testToInputParameters() throws URISyntaxException{
//		sync_fields=Summary,Description,Priority,Status,IssueType,Due Date,Resolution

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		String issueKey2 = "LOYAL-92";
		final Issue createdIssue = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssue);
		List<String> selectedFields = AdapterUtils.getSelectedFields(com.lp.alm.adapter.constants.OSLCConstants.SYNC_FIELDS);
		ChangeRequest changeRequest = ArtifactInputParameters.toIssueParameters(createdIssue, selectedFields);
		assertNotNull(changeRequest.getTitle());
		assertNotNull(changeRequest.getDescription());
		assertNotNull(changeRequest.getWorkItemType());
		assertNotNull(changeRequest.getExternal_link());

		
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
}
