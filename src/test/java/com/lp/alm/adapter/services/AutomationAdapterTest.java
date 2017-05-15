package com.lp.alm.adapter.services;

import static com.atlassian.jira.rest.client.api.domain.EntityHelper.findEntityByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.junit.Test;

import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptionsBuilder;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.google.common.collect.Iterables;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.rest.client.api.JiraClientFactory;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.oslc.client.jazz.JazzArtifactBuilder;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class AutomationAdapterTest {
	
	
	@Test
	public void testIntegratedTestCase() throws URISyntaxException, IOException, ServletException, InterruptedException {

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();

		final Iterable<CimProject> metadataProjects = issueClient
				.getCreateIssueMetadata(new GetCreateIssueMetadataOptionsBuilder().withProjectKeys(OSLCConstants.PROJECT[OSLCConstants.JIRA_OFFSET])
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
//		 create
		 final BasicIssue basicCreatedIssue =
		 issueClient.createIssue(issueInputBuilder.build()).claim();
		 assertNotNull(basicCreatedIssue.getKey());
		 
//		 getLatestIssues
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

}
