package com.lp.alm.oslc.client.jira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.util.concurrent.Promise;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.rest.client.api.JiraClientFactory;

public class JiraArtifactBuilder implements ArtifactBuilder {

	final static Logger logger = LoggerFactory.getLogger(JiraArtifactBuilder.class);

	public static Response addOslcLinkToJiraV2(final String URItoAdd,
			Issue issue) throws IOException, ServletException {
		try {
			logger.info("RTC item id is being updated in JIRA ");
			IssueField fieldByName = issue.getFieldByName(OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
			final IssueInputBuilder issueInputBuilder = new IssueInputBuilder().setFieldValue(fieldByName.getId(),
					URItoAdd);
			JiraClientFactory.getJiraRestClient().getIssueClient().updateIssue(issue.getKey(), issueInputBuilder.build()).claim();
			logger.info("success..... RTC item id is updated in JIRA ");

			return Response.ok().build();

		} catch (Exception e) {
			logger.error(JiraArtifactBuilder.class + "." + " addOslcLinkToJiraV2  Exception: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Unexpected error. Please contact your administrator").build();
		}
	}

	public static String getValueFromCustomField(Issue issue, String customerField) throws URISyntaxException {
		return (issue.getFieldByName(customerField) != null) ? (String) issue.getFieldByName(customerField).getValue()
				: null;
	}

	public static List<Issue> getLatestCreatedIssues() throws URISyntaxException {
		List<Issue> issueList = new ArrayList();

		Promise<SearchResult> searchJqlPromise = JiraClientFactory.getJiraRestClient().getSearchClient()
				.searchJql(OSLCConstants.JQL_CREATED_ISSUES_QUERY);
		for (Issue issue : searchJqlPromise.claim().getIssues()) {
			logger.debug("Issue Summary " + issue.getSummary());
			String remoteIssueLink = getValueFromCustomField(issue, OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
			if (remoteIssueLink == null) {
				issueList.add(issue);
			}
		}
		return issueList;
	}

	public static boolean isQualifiedForPush(Issue issue) {

		try {
			String customField = getValueFromCustomField(issue, OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
			if (customField == null) {
				return true;
			} else {
				return false;
			}

		} catch (URISyntaxException e) {
			logger.error("error while checking if the issues is a new or a modified one ");
			return false;
		}

	}

	public static boolean isAttributeInSync(String status, String issueKey) {
		final IssueRestClient issueClient = JiraClientFactory.getJiraRestClient().getIssueClient();
		final Issue syncIssue = issueClient.getIssue(issueKey).claim();
		Status jiraIssueStatus = syncIssue.getStatus();
		if (jiraIssueStatus.getName().equals(status)) {
			return true;
		}
		return false;

	}


	public static void pushToRemoteJiraSystem(String issueKey, String status) {

		final IssueRestClient issueClient = JiraClientFactory.getJiraRestClient().getIssueClient();
		final Issue syncIssue = issueClient.getIssue(issueKey).claim();
		// now let's start progress on this issue
		final Iterable<Transition> transitions = issueClient.getTransitions(syncIssue.getTransitionsUri()).claim();
		logger.debug("transistions are " + transitions);
		logger.debug("Jira eq status for transition is " + status);

		final Transition startProgressTransition = getTransitionByName(transitions, status);
		if (startProgressTransition == null) {
			logger.info("No transistion found for status " + status);
			return;
		}
		logger.info(" udpating the status in JIRA for issue"+ syncIssue.getKey()+" to staus " + startProgressTransition);
		issueClient.transition(syncIssue.getTransitionsUri(), new TransitionInput(startProgressTransition.getId()))
				.claim();

	}

	private static Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
		for (Transition transition : transitions) {
			if (transition.getName().equals(transitionName)) {
				return transition;
			}
		}
		return null;
	}

	public static List<Issue> getLatestModifiedIssues() throws URISyntaxException {
		List<Issue> issueList = new ArrayList();

		Promise<SearchResult> searchJqlPromise = JiraClientFactory.getJiraRestClient().getSearchClient()
				.searchJql(OSLCConstants.JQL_MODIFIED_ISSUES_QUERY);

		for (Issue issue : searchJqlPromise.claim().getIssues()) {
			logger.debug("Issue Summary " + issue.getSummary());

			String remoteIssueLink = getValueFromCustomField(issue, OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
			if (remoteIssueLink != null) {
				issueList.add(issue);
			}
		}
		return issueList;
	}

}
