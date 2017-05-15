package com.lp.alm.adapter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;

public class AdapterUtils {

	public static String convertToJiraEq(String status) {

		String[] transition = PropertiesCache.getInstance().getProperty("status").split(PropertiesCache.DELIMTER);
		for (String stage : transition) {
			String[] lookup = PropertiesCache.getInstance().getProperty(stage).split(PropertiesCache.DELIMTER);
			if (status.equals(lookup[OSLCConstants.RTC_OFFSET])) {
				return lookup[OSLCConstants.JIRA_OFFSET];
			}
		}
		return "";
	}

	public static String convertToRespectiveInterfaceValue(String propertyKey, String lookupValue, int offset) {

		int correspondingOffset = (offset == OSLCConstants.JIRA_OFFSET) ? OSLCConstants.RTC_OFFSET
				: OSLCConstants.JIRA_OFFSET;

		String[] transition = PropertiesCache.getInstance().getProperty(propertyKey).split(PropertiesCache.DELIMTER);
		for (String stage : transition) {
			String[] lookup = PropertiesCache.getInstance().getProperty(stage).split(PropertiesCache.DELIMTER);
			if (lookupValue.equals(lookup[offset])) {
				return lookup[correspondingOffset];
			}
		}
		return "";
	}

	public static boolean allowUpdate(List<String> selectedProperties, String availableProperty) {
		return ((selectedProperties == null || selectedProperties.contains(availableProperty)));
	}
	
	
	public static List<String> getSelectedFields(){
		String[] property = PropertiesCache.getInstance().getProperty(OSLCConstants.SYNC_FIELDS).split(PropertiesCache.DELIMTER);
		List<String> selectedFields = Arrays.asList(property);
		return selectedFields;
		
	}

}
