package com.lp.alm.adapter.client.pojo.jazz;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IItem;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.IQueryableAttributeFactory;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.expression.Term.Operator;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;

public class QueryWorkItemsByModifiedDate {
	final static Logger logger = LoggerFactory.getLogger(QueryWorkItemsByModifiedDate.class);

　
	private static class LoginHandler implements ILoginHandler, ILoginInfo {

		private String fUserId;
		private String fPassword;

		private LoginHandler(String userId, String password) {
			fUserId = userId;
			fPassword = password;
		}

		public String getUserId() {
			return fUserId;
		}

		public String getPassword() {
			return fPassword;
		}

		public ILoginInfo challenge(ITeamRepository repository) {
			return this;
		}
	}

	public static void main(String[] args) throws URISyntaxException {

		List<ChangeRequest> result;
		TeamPlatform.startup();
		try {
			result = getModifiedWorkItemList();
		} finally {
			TeamPlatform.shutdown();
		}

	}

　
	public static List<ChangeRequest> getModifiedWorkItemList() throws URISyntaxException {

		
		List<ChangeRequest> changeRequestList = new ArrayList();
		try{
			
		TeamPlatform.startup();

		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];

		IProgressMonitor monitor = new NullProgressMonitor();
		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(monitor);

		IProcessClientService processClient = (IProcessClientService) teamRepository
				.getClientLibrary(IProcessClientService.class);
		
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);

		
		IAuditableClient auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
		URI uri = URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea = (IProjectArea) processClient.findProcessArea(uri, null, null);
		IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
		String arg1 = "modified";

		IQueryableAttribute recAttr1 = factory.findAttribute(projectArea, IItem.MODIFIED_PROPERTY, auditableClient, null ); 

		Date date = new Date();
		date.setDate(date.getDate());
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		String reportDate = df.format(date);
		System.out.println("Date: "+reportDate);

		int timerFrequency = Integer.parseInt(OSLCConstants.RTC_POLLING_FREQUENCY);

		long minsAgo = timerFrequency * 60 * 1000;
		long tenAgo = System.currentTimeMillis() - minsAgo;
		Timestamp after = new Timestamp(tenAgo);
		String afterDate = df.format(after);

		System.out.println("after: "+afterDate);

		AttributeExpression recExpr1 = new AttributeExpression(recAttr1, AttributeOperation.EQUALS, after ); 

		Term term= new Term(Operator.AND); 
		term.add(recExpr1); 

		IQueryClient queryClient = (IQueryClient) teamRepository.getClientLibrary(IQueryClient.class); 
		IQueryResult<IResolvedResult<IWorkItem>> result = queryClient.getResolvedExpressionResults(projectArea, (Expression)term, IWorkItem.FULL_PROFILE); 
		logger.info("RTC Query Result : Total number of modified items found in the last "+timerFrequency+" mins are "+result.getResultSize(monitor).getTotal()+"\n");                 

		while(result.hasNext(null)){ 
			
		    IResolvedResult<IWorkItem> resolvedWorkItem = result.next(null); 
		    IWorkItem workItem = resolvedWorkItem.getItem();
		    Date date1 = resolvedWorkItem.getItem().modified(); 
		    System.out.println("Id: "+workItem.getId());
			String modifiedDate = df.format(date1);
		    
			logger.debug(" ten min ago time is "+tenAgo);
			logger.debug("workitem time is "+resolvedWorkItem.getItem().modified().getTime());
			logger.debug("Modified date :"+modifiedDate+"\n"); 

		    if (resolvedWorkItem.getItem().modified().getTime() < tenAgo)
		    	continue;
		    
		    ChangeRequest cr = new ChangeRequest();
		    
			IAttribute customString = workItemClient.findAttribute(projectArea, OSLCConstants.RTC_CUSTOM_FIELD, monitor);
			if (workItem.hasCustomAttribute(customString)) {
				cr.setExternal_link((String) workItem.getValue(customString));
			}
			IWorkflowInfo workflowInfo = workItemClient.findWorkflowInfo(workItem, monitor);
			String currentStatus = workflowInfo.getStateName(workItem.getState2());
			
			cr.setStatus(currentStatus);
		    changeRequestList.add(cr);
		    System.out.println("Found one workitem \n"); 
		}   
		
		
		teamRepository.logout();
		
		TeamPlatform.shutdown();
		}catch(TeamRepositoryException tr){
			
		}

		return changeRequestList;
	}
	
	
}
