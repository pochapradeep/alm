package com.lp.alm.oslc.client.jira;

import static com.atlassian.jira.rest.client.api.domain.EntityHelper.findEntityByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptionsBuilder;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.lp.alm.adapter.rest.client.api.JiraClientFactory;

public class JiraArtifactBuilderTest {

	@Test
	public void testJiraGetCreatedIssues() throws URISyntaxException {

		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();

		// collect CreateIssueMetadata for project with key TST
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();

		final Iterable<CimProject> metadataProjects = issueClient
				.getCreateIssueMetadata(new GetCreateIssueMetadataOptionsBuilder().withProjectKeys("LOYAL")
						.withExpandedIssueTypesFields().build())
				.claim();

		// select project and issue
		assertEquals(1, Iterables.size(metadataProjects));
		final CimProject project = metadataProjects.iterator().next();
		final CimIssueType issueType = findEntityByName(project.getIssueTypes(), "Task");

		// grab the first priority
		final Iterable<Object> allowedValuesForPriority = issueType.getField(IssueFieldId.PRIORITY_FIELD)
				.getAllowedValues();
		assertNotNull(allowedValuesForPriority);
		assertTrue(allowedValuesForPriority.iterator().hasNext());
		final BasicPriority priority = (BasicPriority) allowedValuesForPriority.iterator().next();

		// build issue input
		final String summary = "My new issue!";
		final String description = "Some description";

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

	}
	
	@Test
	public void testJiraModifiedIssues() throws URISyntaxException{
		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		String issueKey2 = "OBP-5";
		final Issue createdIssue = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssue);
		
		List<Issue> latestModifiedIssues = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssues.isEmpty());
		
		final Comment comment = Comment.createWithRoleLevel("Simple test comment restricted for role Administrators.",
				"Administrators");
		final String issueKey = issueKey2;
		final Comment addedComment = testAddCommentToIssueImpl(issueKey, comment,jiraRestClient);

		
		List<Issue> latestModifiedIssuesAfterUpdate = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssuesAfterUpdate.isEmpty());
		
		// issue with external_link field
		final Issue createdIssueWithExternalLink = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssueWithExternalLink);
		
		List<Issue> latestModifiedIssuesWithExternalLink = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssuesWithExternalLink.isEmpty());
		
		final Comment addedComment1 = testAddCommentToIssueImpl(issueKey2, comment,jiraRestClient);

		List<Issue> latestModifiedIssuesAfterUpdateWithExtLink = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(!latestModifiedIssuesAfterUpdateWithExtLink.isEmpty());
		

	}
	


	private Comment testAddCommentToIssueImpl(final String issueKey, final Comment comment,JiraRestClient jiraRestClient) {
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		final Issue issue = issueClient.getIssue(issueKey).claim();
		final List<Comment> initialComments = Lists.newArrayList(issue.getComments());

		issueClient.addComment(issue.getCommentsUri(), comment).claim();

		final Issue issueWithComments = issueClient.getIssue(issueKey).claim();
		final List<Comment> newComments = Lists.newArrayList(issueWithComments.getComments());
		newComments.removeAll(initialComments);
		assertEquals(1, Iterables.size(newComments));
		Comment addedComment = newComments.get(0);
		assertEquals(comment.getBody(), addedComment.getBody());
		assertEquals(comment.getVisibility(), addedComment.getVisibility());
		return addedComment;
	}
	
	@Test
	public void testJiraIssueProperties() throws URISyntaxException{
		JiraRestClient jiraRestClient = JiraClientFactory.getJiraRestClient();
		final IssueRestClient issueClient = jiraRestClient.getIssueClient();
		String issueKey2 = "OBP-5";
		final Issue createdIssue = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssue);
		
		System.out.print("issue id is "+createdIssue.getId());

		
		
//		Issue Type
//		Due Date
//		Description
//		Priority
//		Project Name
//		Requesting Country
//		Requestor Name
//		Business Unit
//		BP ID
//		Clarity ID
//		Approved
//		Nature of change
//		Importance
		
	
		
		List<Issue> latestModifiedIssues = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssues.isEmpty());
		
		
		final Comment comment = Comment.createWithRoleLevel("Simple test comment restricted for role Administrators.",
				"Administrators");
		final String issueKey = issueKey2;
		final Comment addedComment = testAddCommentToIssueImpl(issueKey, comment,jiraRestClient);

		
		List<Issue> latestModifiedIssuesAfterUpdate = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssuesAfterUpdate.isEmpty());
		
		// issue with external_link field
		final Issue createdIssueWithExternalLink = issueClient.getIssue(issueKey2).claim();//LOYAL-37
		assertNotNull(createdIssueWithExternalLink);
		
		List<Issue> latestModifiedIssuesWithExternalLink = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(latestModifiedIssuesWithExternalLink.isEmpty());
		
		final Comment addedComment1 = testAddCommentToIssueImpl(issueKey2, comment,jiraRestClient);

		
		List<Issue> latestModifiedIssuesAfterUpdateWithExtLink = JiraArtifactBuilder.getLatestModifiedIssues(/*jiraRestClient*/);
		assertTrue(!latestModifiedIssuesAfterUpdateWithExtLink.isEmpty());
		

	}
	
	
}
