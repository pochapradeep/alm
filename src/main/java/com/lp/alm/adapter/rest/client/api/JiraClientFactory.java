package com.lp.alm.adapter.rest.client.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class JiraClientFactory {
	final static Logger logger = LoggerFactory.getLogger(JiraClientFactory.class);
	private static JiraRestClient client;
	
	static {
		try {
			client = buildJiraRestClient();
			logger.info("Jira client successfully created !");
		} catch (URISyntaxException e) {
			logger.error("**** Error while created JIRA Client ***** ");
			e.printStackTrace();
		}

	}

	private static JiraRestClient buildJiraRestClient() throws URISyntaxException {
		System.out.println(String.format("Logging in to %s with username '%s' and password '%s'",
				OSLCConstants.JIRA_URL, OSLCConstants.JIRA_ADMIN_USERNAME, OSLCConstants.JIRA_ADMIN_PASSWORD));
		JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		URI uri = new URI(OSLCConstants.JIRA_URL);
		JiraRestClient client = factory.createWithBasicHttpAuthentication(uri, OSLCConstants.JIRA_ADMIN_USERNAME,
				OSLCConstants.JIRA_ADMIN_PASSWORD);
		return client;
	}

	
	

	public static JiraRestClient getJiraRestClient() {
		return client;
	}
}
