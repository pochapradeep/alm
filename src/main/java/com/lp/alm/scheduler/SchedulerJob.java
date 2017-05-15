package com.lp.alm.scheduler;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lp.alm.adapter.services.AutomationAdapter;
import com.lp.alm.lyo.client.exception.RootServicesException;

public class SchedulerJob implements Job {
	  final static Logger logger = LoggerFactory.getLogger(SchedulerJob.class);

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		
		logger.info("Polling for new items.....");
		try {
			AutomationAdapter.pollAndFireService();
			logger.info("completed the last job.");
			
		} catch (URISyntaxException e) {
			logger.error("URI Syntax Exception while fetching new issues ...."+e.getMessage());;
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("IOException while fetching new issues ...."+e.getMessage());;
			e.printStackTrace();
		} catch (ServletException e) {
			logger.error("ServletException while fetching new issues ...."+e.getMessage());;
			e.printStackTrace();
		} catch (RootServicesException e) {
			logger.error("RootServicesException while fetching new issues ...."+e.getMessage());;
			e.printStackTrace();
		}

	}

}
