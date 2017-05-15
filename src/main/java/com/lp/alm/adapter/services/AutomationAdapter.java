package com.lp.alm.adapter.services;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.lp.alm.adapter.utils.AdapterUtils;
import com.lp.alm.lyo.client.exception.RootServicesException;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.oslc.client.jazz.JazzArtifactBuilder;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class AutomationAdapter {
	public final static Logger logger = LoggerFactory.getLogger(AutomationAdapter.class);

	public static void main(String[] args) throws Exception {
		// pollAndFireService();
		while (true) {
			// performJiraToRTCSync();
			performRTCToJiraSync();
		}
	}

	public static void pollAndFireService()
			throws URISyntaxException, IOException, ServletException, RootServicesException {

		synchronized (AutomationAdapter.class) {
			logger.info("Sync process started......");
			performJiraToRTCSync();
			performRTCToJiraSync();
			logger.info("Sync process completed......");

		}
	}

	public static void performRTCToJiraSync() throws URISyntaxException {
		List<ChangeRequest> latestIssues = JazzArtifactBuilder.getLatestIssues();
		if (latestIssues.isEmpty()) {
			return;
		}
		if (!anyIssueStatusNotInSync(latestIssues)) {
			return;
		}

		for (ChangeRequest cr : latestIssues) {
			logger.debug("Issue Status " + cr.getStatus());
			if (JazzArtifactBuilder.isQualifiedForPush(cr)) {
				String status = AdapterUtils.convertToJiraEq(cr.getStatus());
				if (JiraArtifactBuilder.isAttributeInSync(status, cr.getExternal_link())) {
					continue;
				} else {
					JiraArtifactBuilder.pushToRemoteJiraSystem(cr.getExternal_link(), status);

				}
			}
		}

	}

	private static boolean anyIssueStatusNotInSync(List<ChangeRequest> latestIssues) {
		for (ChangeRequest cr : latestIssues) {
			logger.debug("Issue Status " + cr.getStatus());
			if (JazzArtifactBuilder.isQualifiedForPush(cr)) {
				String status = AdapterUtils.convertToJiraEq(cr.getStatus());
				if (JiraArtifactBuilder.isAttributeInSync(status, cr.getExternal_link())) {
					continue;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public static void performJiraToRTCSync()
			throws URISyntaxException, IOException, ServletException, RootServicesException {
		logger.debug("Polling service started ......");

		List<Issue> latestCreatedIssues = JiraArtifactBuilder.getLatestCreatedIssues();
		List<Issue> latestUpdatedIssues = JiraArtifactBuilder.getLatestModifiedIssues();

		if (latestCreatedIssues.isEmpty() && latestUpdatedIssues.isEmpty()) {
			logger.info("No latest created or udpated issues found ");
			return;
		}

		pushIssues(latestCreatedIssues);

		pushIssues(latestUpdatedIssues);

		logger.debug("Polling service complete. Now existing. ");
	}

	public static void pushIssues(List<Issue> latestIssues)
			throws IOException, ServletException, URISyntaxException {
		for (Issue issue : latestIssues) {
			logger.debug("Issue Summary " + issue.getSummary());
			pushToRemoteJazzSystem(issue);
			logger.debug("A New artifact has been pushed: "+issue.getKey());
		}
	}

	public static void pushToRemoteJazzSystem(Issue issue)
			throws IOException, ServletException, URISyntaxException {

		ClientResponse clientResponse = JazzArtifactBuilder.createOrUpdateIssueInRTC(issue);
		if (clientResponse == null || clientResponse.getStatusCode() != HttpStatus.SC_CREATED
				&& clientResponse.getStatusCode() != HttpStatus.SC_OK) {
			System.err.println("ERROR: Could not create the task \n");
		} else {
			String changeRequestLocation = clientResponse.getHeaders().getFirst(HttpHeaders.LOCATION);
			JiraArtifactBuilder.addOslcLinkToJiraV2(changeRequestLocation,
					issue);

		}
	}

}