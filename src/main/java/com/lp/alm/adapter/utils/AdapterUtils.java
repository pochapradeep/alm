package com.lp.alm.adapter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;
import com.lp.alm.oslc.client.jazz.JazzArtifactBuilder;

public class AdapterUtils {
	final static Logger logger = LoggerFactory.getLogger(AdapterUtils.class);

	public static String convertToJiraEq(String status) {
		
		logger.info("Finding Jira status for status "+status+" in RTC");
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
	
	
	public static List<String> getSelectedFields(String fieldType){
		String[] property = PropertiesCache.getInstance().getProperty(fieldType).split(PropertiesCache.DELIMTER);
		List<String> selectedFields = Arrays.asList(property);
		return selectedFields;
		
	}

}
