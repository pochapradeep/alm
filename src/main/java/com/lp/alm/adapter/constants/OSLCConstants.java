package com.lp.alm.adapter.constants;

import com.lp.alm.adapter.properties.PropertiesCache;

public interface OSLCConstants {

	public final String JIRA_ADMIN_PASSWORD=PropertiesCache.getInstance().getProperty("jira_admin_password");
	public final String JIRA_ADMIN_USERNAME = PropertiesCache.getInstance().getProperty("jira_admin_username");
	public final String JIRA_URL = PropertiesCache.getInstance().getProperty("jira_url");
	public final String STRING_CUSTOM_FIELD_EXT_LINKS = PropertiesCache.getInstance().getProperty("string_custom_field_ext_links");
	public final String JIRA_ADMIN_USER = PropertiesCache.getInstance().getProperty("jira_admin_user");
	
	
	public final String JIRA_PROJECT = PropertiesCache.getInstance().getPropertyFor("project")[0];
	public final String FROM_STATUS = PropertiesCache.getInstance().getProperty("from_status");
	public final String TO_STATUS = PropertiesCache.getInstance().getProperty("to_status");
	public final String LAPSE_TIME = PropertiesCache.getInstance().getProperty("lapse_time");

	
	public final String JQL_MODIFIED_ISSUES_QUERY = "project = "+JIRA_PROJECT+" AND updated > '-1m' AND created < '-1m' and not status changed during (-"+LAPSE_TIME+",now())";
//	public final String JQL_CREATED_ISSUES_QUERY = "project = "+JIRA_PROJECT+" AND created >'-1m'";
	public final String JQL_CREATED_ISSUES_QUERY = "project="+JIRA_PROJECT+" and status changed FROM '"+FROM_STATUS+"' TO '"+TO_STATUS+"' DURING (-"+LAPSE_TIME+",now())";

	public final String OSLC_PROPERTIES_DCTERMS_TITLE = "?oslc.properties=dcterms:title";
	public final String RTC_FILED_AGAINST = "filedAgainst";
	public final String RTC_NAMESPACE = "http://jazz.net/xmlns/prod/jazz/rtc/cm/1.0/";
	public static String OSLC_CM_V2 = "http://open-services.net/ns/cm#";

	
	public final String JIRA_AUTH_HEADER = PropertiesCache.getInstance().getProperty("jira_auth_header");
	public final String JAZZ_CM_ROOT_SERVICES = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
	public final String JAZZ_ADMIN_USERNAME = PropertiesCache.getInstance().getProperty("jazz_admin_username");
	public final String JAZZ_ADMIN_PASSWORD = PropertiesCache.getInstance().getProperty("jazz_admin_password");
	
//	public final String JAZZ_PROJECT_AREA = PropertiesCache.getInstance().getProperty("jira_auth_header");
	
	
	
	public final int JIRA_DEFECT_ID = 10004;
	public final int JIRA_TASK_ID = 10002;
	public final int JIRA_EPIC_ID = 10000;
	public final int JIRA_STORY_ID = 10001;
	public int RTC_OFFSET = 1;
	public int JIRA_OFFSET = 0;
	public String[] PROJECT = PropertiesCache.getInstance().getPropertyFor("project");
	public String RTC_POLLING_FREQUENCY = PropertiesCache.getInstance().getProperty("rtc_polling_frequency");
	
	
	public final String JIRA_TYPE_ASIGNEE = null;
	public final String JIRA_TYPE_REPORTER = null;
	public final String DCTERMS_TITLE = "Summary";
	public final String JIRA_TYPE_DESCRIPTION = "Description";
	public final String JIRA_TYPE_PRIORITY = "Priority";
	public final String JIRA_TYPE_STATUS = "Status";
	public final String JIRA_TYPE_ISSUE_TYPE = "IssueType";
	public final String DCTERMS_DUEDATE = "Due Date";
	public final String JIRA_TYPE_RESOLUTION = "Resolution";
	public final String JIRA_TYPE_CUSTOM_FIELD = "Custom";
	String SYNC_FIELDS = "sync_fields";
	String CUST_FIELDS = "custom_fields";

	public final String STRING_CUSTOM_FIELD_ACCEPTANCE_CRITERIA = "Acceptance Criteria";
	public final String ACCEPTANCE_CRITERIA = "Acceptance Criteria";

}
