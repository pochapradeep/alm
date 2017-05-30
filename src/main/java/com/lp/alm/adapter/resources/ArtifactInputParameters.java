package com.lp.alm.adapter.resources;

import java.net.URISyntaxException;
import java.util.List;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.utils.AdapterUtils;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class ArtifactInputParameters {

	/**
	 * Converts a Jira Issue to RTC Change Request defined by OSLC spec
	 * 
	 * @param issue
	 *            JIRA issue
	 * @param selectedProperties
	 *            OSLC properties
	 * @param project
	 *            Project
	 * @return ChangeRequest
	 * @throws URISyntaxException
	 * @throws Exception
	 */
	public static ChangeRequest toIssueParameters(Issue issue, List<String> selectedProperties)
			throws URISyntaxException {

		ChangeRequest changeRequest = new ChangeRequest();

		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(issue,
				OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		if (remoteIssueLink != null) {
			changeRequest.setExistingURI(remoteIssueLink);
		}

		// assignee
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_ASIGNEE)) {
			// jiraIssueInputParams.setAssigneeId(issue.getAssignee());
		}

		// title
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.DCTERMS_TITLE)) {
			changeRequest.setTitle(issue.getSummary());
		}

		// description
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_DESCRIPTION)) {
			changeRequest.setDescription(issue.getDescription());
		}

		// priority
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_PRIORITY)) {
			BasicPriority priority = issue.getPriority();
			
			String mappedPriority = AdapterUtils.convertToRespectiveInterfaceValue("priority", priority.getName(), OSLCConstants.JIRA_OFFSET);
			changeRequest.setPriority(mappedPriority);
			// TODO determine method to set the priority.
		}

		// status
		// NOTE: changing to different status is not enough to actually change
		// issue status.
		// Status must be changed via proper action (e.g. 'Start progress' from
		// 'Open' state
		// to 'In progress' state). So here we just set the status to
		// corresponding field
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_STATUS)) {

		}

		// Get issue type id from change request
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_ISSUE_TYPE)) {
			IssueType issueType = issue.getIssueType();
			if (issueType != null && issueType.getName() != null) {
				IssueType issueTypeObject = issue.getIssueType();
				long issueTypeId = issueTypeObject.getId();
				String type = ChangeRequest.STORY;
				if (issueTypeId == OSLCConstants.JIRA_EPIC_ID) {
					type = ChangeRequest.EPIC;
				} else if (issueTypeId == OSLCConstants.JIRA_STORY_ID) {
					type = ChangeRequest.STORY;
				} else if (issueTypeId == OSLCConstants.JIRA_TASK_ID) {
					type = ChangeRequest.TASK;
				} else if (issueTypeId == OSLCConstants.JIRA_DEFECT_ID) {
					type = ChangeRequest.DEFECT;
				}
				changeRequest.setWorkItemType(type);
			}
		}

		// duedate
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.DCTERMS_DUEDATE)) {
			changeRequest.setDueDate(issue.getDueDate());
		}

		// resolution
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_RESOLUTION)) {
			Resolution res = issue.getResolution();
			if (res != null && res.getName() != null) {
				// TODO
			}
		}

		// custom fields
		if (AdapterUtils.allowUpdate(selectedProperties, OSLCConstants.JIRA_TYPE_CUSTOM_FIELD)) {
			List<String> custFields = AdapterUtils.getSelectedFields(OSLCConstants.CUST_FIELDS);
			if (AdapterUtils.allowUpdate(custFields, OSLCConstants.ACCEPTANCE_CRITERIA)) {
				String acceptCriteria = JiraArtifactBuilder.getValueFromCustomField(issue, OSLCConstants.STRING_CUSTOM_FIELD_ACCEPTANCE_CRITERIA);
				changeRequest.setAcceptanceCriteria(acceptCriteria);
			}


		}

		changeRequest.setExternal_link(issue.getKey());

		return changeRequest;
	}
}
