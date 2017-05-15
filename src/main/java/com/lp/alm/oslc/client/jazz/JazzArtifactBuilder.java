package com.lp.alm.oslc.client.jazz;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.oslc4j.core.model.AllowedValues;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.Property;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.adapter.resources.ArtifactInputParameters;
import com.lp.alm.adapter.rest.client.api.JazzClientFactory;
import com.lp.alm.adapter.utils.AdapterUtils;
import com.lp.alm.lyo.client.exception.ResourceNotFoundException;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;
import com.lp.alm.lyo.client.oslc.resources.OslcQuery;
import com.lp.alm.lyo.client.oslc.resources.OslcQueryParameters;
import com.lp.alm.lyo.client.oslc.resources.OslcQueryResult;
import com.lp.alm.oslc.HTTP;
import com.lp.alm.oslc.client.jira.ArtifactBuilder;
import com.lp.alm.oslc.client.jira.JiraArtifactBuilder;

public class JazzArtifactBuilder implements ArtifactBuilder {

	private static final String NEW = "New";
	private static final String APPLICATION_RDF_XML = "application/rdf+xml";
	private static final String ESCAPE_SEQ = "\"";
	private static final String DCTERMS_MODIFIED = "dcterms:modified>\"";
	private static final String CLOSE_BRACES = "}";
	private static final String OPEN_BRACES = "{-";
	final static Logger logger = LoggerFactory.getLogger(JazzArtifactBuilder.class);


	 // TODO JIRA Issue to be moved out of RTC classes, only the property
		// values needs to be passed.
	public static ClientResponse createOrUpdateIssueInRTC(Issue issue) throws URISyntaxException {

		logger.info("issue is being pushed to Jazz ");
		
		List<String> selectedFields = AdapterUtils.getSelectedFields();
		ChangeRequest cr = ArtifactInputParameters.toIssueParameters(issue, selectedFields);


/*		String remoteIssueLink = JiraArtifactBuilder.getValueFromCustomField(issue,
				OSLCConstants.STRING_CUSTOM_FIELD_EXT_LINKS);
		if (remoteIssueLink != null) {
			cr.setExistingURI(remoteIssueLink);
		}

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

		cr.setWorkItemType(type);
		cr.setTitle(issue.getSummary());
		cr.setDescription(issue.getDescription());
		cr.setExternal_link(issue.getKey());

		issue.getPriority();*/

		ClientResponse writeToExternalSystem = HTTP.writeToExternalSystem(cr,
				OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET]);
		return writeToExternalSystem;

	}

   /**
    * method to get the latest issues from RTC ,
    * all issues updated recently would be collected and returned.
    * @return list of updated artifacts.
    */
	public static List<ChangeRequest> getLatestIssues() {

		List<ChangeRequest> crList = new ArrayList();

		OslcQueryParameters queryParams2 = new OslcQueryParameters();
		String timerFrequency = OSLCConstants.RTC_POLLING_FREQUENCY;
		String pollingFrequency = OPEN_BRACES + timerFrequency + CLOSE_BRACES;
		queryParams2.setWhere(DCTERMS_MODIFIED + pollingFrequency + ESCAPE_SEQ);

		logger.debug("format time is " + pollingFrequency);
		logger.debug("queryParams is " + queryParams2);

		OslcQuery query2 = new OslcQuery(JazzClientFactory.jazzRestClient(), JazzClientFactory.getQueryCapability(),
				queryParams2);

		OslcQueryResult result2 = query2.submit();
		for (String resultsUrl : result2.getMembersUrls()) {
			System.out.println(resultsUrl);

			ClientResponse response = null;
			try {
				// Get a single artifact by its URL
				response = JazzClientFactory.jazzRestClient().getResource(resultsUrl, APPLICATION_RDF_XML);
				if (response != null) {
					// De-serialize it as a Java object
					ChangeRequest cr = response.getEntity(ChangeRequest.class);
					crList.add(cr);
					System.out.println(" cr " + cr.getTitle());
				}
			} catch (Exception e) {
				logger.error("Unable to process artfiact at url: " + resultsUrl, e);
			}
		}
		return crList;
	}

	public static boolean isQualifiedForPush(ChangeRequest cr) {
		if (cr.getExternal_link() == null) {
			return false;
		}
		if (cr.getStatus() == NEW) {
			return false;
		}

		return true;
	}

	public static URI getResourceURI(ChangeRequest createJazzData, String rtcNamespace, String rtcOSLCName,
			String lookupValue) throws Exception {

		CreationFactory taskCreation = JazzClientFactory.jazzRestClient().lookupCreationFactoryResource(
				JazzClientFactory.getServiceProviderUrl(), OSLCConstants.OSLC_CM_V2,
				createJazzData.getRdfTypes()[0].toString(),
				OSLCConstants.OSLC_CM_V2 + createJazzData.getWorkItemType());

		// Determine what to use for the Filed Against attribute by requesting
		// the resource shape for the creation factory.
		String shapeUrl = taskCreation.getResourceShapes()[0].toString();
		ClientResponse shapeResponse = JazzClientFactory.jazzRestClient().getResource(shapeUrl);
		ResourceShape shape = shapeResponse.getEntity(ResourceShape.class);

		// Look at the allowed values for Filed Against. This is generally a
		// required field for defects.
		// String rtcNamespace =
		// com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE;

		// String rtcFiledAgainst =
		// com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST;

		Property filedAgainstProperty = shape.getProperty(new URI(rtcNamespace + rtcOSLCName));
		if (filedAgainstProperty != null) {
			URI allowedValuesRef = filedAgainstProperty.getAllowedValuesRef();
			ClientResponse allowedValuesResponse = JazzClientFactory.jazzRestClient()
					.getResource(allowedValuesRef.toString());
			AllowedValues allowedValues = allowedValuesResponse.getEntity(AllowedValues.class);
			Object[] values = allowedValues.getValues().toArray();
			URI resourceURL = (URI) values[0];
			for (Object resultsUrl : values) {

				ClientResponse response1 = JazzClientFactory.jazzRestClient().getResource(resultsUrl.toString(),
						"application/rdf+xml");

				InputStream is = response1.getEntity(InputStream.class);
				Model rdfModel = ModelFactory.createDefaultModel();
				rdfModel.read(is, resultsUrl.toString());

				boolean rootServicesProperty = getRootServicesProperty(rdfModel, "http://purl.org/dc/terms/", "title",
						lookupValue);
				if (rootServicesProperty) {
					resourceURL = (URI) resultsUrl;
					return resourceURL;
				}
			}
		}
		return null;
	}

	private static boolean getRootServicesProperty(Model rdfModel, String namespace, String predicate, String value)
			throws ResourceNotFoundException {
		String returnVal = null;

		com.hp.hpl.jena.rdf.model.Property prop = rdfModel.createProperty(namespace, predicate);
		Statement stmt = rdfModel.getProperty((Resource) null, prop);
		if (stmt != null && stmt.getObject() != null)
			returnVal = stmt.getObject().toString();
		if (returnVal == null) {
			return false;
		} else {
			if (returnVal.contains(value)) {
				return true;
			}
		}
		return false;
	}

}
