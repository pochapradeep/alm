package com.lp.alm.adapter.client.pojo.jazz;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.internal.TeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.model.query.BaseContributorQueryModel.ContributorQueryModel;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.repository.common.query.IItemQueryPage;
import com.ibm.team.repository.common.query.ast.IPredicate;
import com.ibm.team.repository.common.service.IQueryService;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.IQueryableAttributeFactory;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.expression.Term.Operator;
import com.ibm.team.workitem.common.internal.expression.QueryableAttribute;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.properties.PropertiesCache;

/**
 * Uses the ContributorQueryModel to search for a user by the user name and not
 * the ID.
 * 
 * 
 * Example code, see
 * https://jazz.net/wiki/bin/view/Main/ProgrammaticWorkItemCreation.
 */
public class QueryUserByName {

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

	public static void main(String[] args) {

		boolean result;
		TeamPlatform.startup();
		try {
			result = run(args);
		} catch (TeamRepositoryException x) {
			x.printStackTrace();
			result = false;
		} finally {
			TeamPlatform.shutdown();
		}

		if (!result)
			System.exit(1);
	}

	private static boolean run(String[] args) throws TeamRepositoryException {

		// if (args.length != 4) {
		System.out.println("Usage: QueryWorkItems [repositoryURI] [userId] [password] [NameOfUserToSearch]");
		// return false;
		// }

		String repositoryURI = PropertiesCache.getInstance().getProperty("jazz_cm_root_services");
		String userId = PropertiesCache.getInstance().getProperty("jazz_admin_username");
		String password = PropertiesCache.getInstance().getProperty("jazz_admin_password");
		String projectAreaName = OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET];

		IProgressMonitor monitor = new NullProgressMonitor();
		// final String repositoryURI = args[0];
		// final String userId = args[1];
		// final String password = args[2];
		final String findUserByName = "admin";
		ITeamRepository teamRepository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryURI);
		teamRepository.registerLoginHandler(new LoginHandler(userId, password));
		teamRepository.login(monitor);

		IProcessClientService processClient = (IProcessClientService) teamRepository
				.getClientLibrary(IProcessClientService.class);
		IAuditableClient auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
		URI uri = URI.create(projectAreaName.replaceAll(" ", "%20"));
		IProjectArea projectArea = (IProjectArea) processClient.findProcessArea(uri, null, null);
		IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
		String arg1 = "modified";
		IQueryableAttribute queryableAttribute = factory.findAttribute(projectArea, /*IWorkItem.MODIFIED_PROPERTY*/arg1,
				auditableClient, monitor);
		
		long oneMin = 5 * 60 * 1000;
		long tenAgo = System.currentTimeMillis() - oneMin;

		Calendar instance = Calendar.getInstance();
		instance.set(2017, 4, 10, 0, 0);
		
		Timestamp after = new Timestamp(tenAgo);
		System.out.println("time stamp is "+after.toString());
		AttributeExpression queryExpression = new AttributeExpression(queryableAttribute,
				AttributeOperation.AFTER, after);
		IQueryableAttribute projectAreaAttribute = factory.findAttribute(projectArea, IWorkItem.PROJECT_AREA_PROPERTY,
				auditableClient, monitor);
		AttributeExpression projectAreaExpression = new AttributeExpression(projectAreaAttribute,
				AttributeOperation.EQUALS, projectArea);

		Term term = new Term(Operator.AND);
		term.add(queryExpression);
		term.add(projectAreaExpression);

		IQueryClient queryClient = (IQueryClient) teamRepository.getClientLibrary(IQueryClient.class);

		IQueryResult<IResolvedResult<IWorkItem>> result = queryClient.getResolvedExpressionResults(projectArea,
				(Expression) term, IWorkItem.FULL_PROFILE);

		System.out.println("This is total number: " + result.getResultSize(monitor).getTotal() + "\n");

		if (result.hasNext(null)) {
			IResolvedResult<IWorkItem> resolved = result.next(null);
			Date date1 = resolved.getItem().modified();
			System.out.println("Modified is :" + date1.toString() + "\n");
			System.out.println("Modified by:" + resolved.getItem().getModifiedBy() + "\n");
			System.out.println("State is:" + resolved.getItem().getFullState().toString() + "\n");
			System.out.println("Name=" + resolved.getItem().getHTMLSummary());
		}
		// }

		/*
		 * // Run this ItemQuery. Note, there are also other types of queries
		 * qs.queryData(dataQuery, parameters, pageSize) final IItemQueryPage
		 * page = qs.queryItems(filtered, new Object[] { findUserByName }, 1
		 * IQueryService.DATA_QUERY_MAX_PAGE_SIZE ); // Get the item handles if
		 * any final List<?> handles = page.getItemHandles();
		 * System.out.println("Hits handles.size()"); if (!handles.isEmpty()) {
		 * System.out.println("Found."); // Resolve and print the information to
		 * the contributor object. final IContributorHandle handle =
		 * (IContributorHandle) handles.get(0); IContributor foundContributor =
		 * (IContributor) teamRepository.itemManager().fetchCompleteItem(handle,
		 * IItemManager.DEFAULT, monitor); System.out.println("UUID "+
		 * foundContributor.getItemId()); System.out.println("ID "
		 * +foundContributor.getUserId()); System.out.println("Name "
		 * +foundContributor.getName()); System.out.println("E-Mail "
		 * +foundContributor.getEmailAddress()); System.out.println("Archived "
		 * +foundContributor.isArchived()); }
		 */

		teamRepository.logout();
		return true;
	}
}