package com.lp.alm.adapter.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.lp.alm.adapter.constants.OSLCConstants;

public class AdapterUtilsTest {
	
//	@Test
	public void testConvertToRespectiveInterfaceValueForStatus(){
		String propertyKey = "status";
		
		
		int offset = OSLCConstants.JIRA_OFFSET;
		String lookupValue = "In Progress";
		String convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("In Progress",convertToRespectiveInterfaceValue);
		
		offset = OSLCConstants.RTC_OFFSET;
		lookupValue = "In Progress";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("In Progress",convertToRespectiveInterfaceValue);
	
		offset = OSLCConstants.RTC_OFFSET;
		lookupValue = "Done";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("Done",convertToRespectiveInterfaceValue);
		
		
		offset = OSLCConstants.RTC_OFFSET;
		lookupValue = "Deferred";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("Done",convertToRespectiveInterfaceValue);
	}
	
	
	@Test
	public void testConvertToRespectiveInterfaceValueForPriority(){
		String propertyKey = "priority";
		
		
		int offset = OSLCConstants.JIRA_OFFSET;
		String lookupValue = "High";
		String convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("High",convertToRespectiveInterfaceValue);
		
		offset = OSLCConstants.JIRA_OFFSET;
		lookupValue = "Highest";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("High",convertToRespectiveInterfaceValue);
	
		offset = OSLCConstants.JIRA_OFFSET;
		lookupValue = "Medium";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("Medium",convertToRespectiveInterfaceValue);
		
		
		offset = OSLCConstants.JIRA_OFFSET;
		lookupValue = "Low";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("Low",convertToRespectiveInterfaceValue);
		
		
		offset = OSLCConstants.JIRA_OFFSET;
		lookupValue = "Lowest";
		convertToRespectiveInterfaceValue = AdapterUtils.convertToRespectiveInterfaceValue(propertyKey, lookupValue, offset);
		assertEquals("Low",convertToRespectiveInterfaceValue);
	}

}
