package com.lp.alm.oslc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.http.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.oslc4j.core.model.AllowedValues;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Property;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.eclipse.lyo.oslc4j.provider.jena.AbstractOslcRdfXmlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lp.alm.adapter.rest.client.api.JazzClientFactory;
import com.lp.alm.lyo.client.exception.ResourceNotFoundException;
import com.lp.alm.lyo.client.oslc.OSLCConstants;
import com.lp.alm.lyo.client.oslc.jazz.JazzFormAuthClient;
import com.lp.alm.lyo.client.oslc.jazz.JazzRootServicesHelper;
import com.lp.alm.lyo.client.oslc.resources.ChangeRequest;

import net.oauth.OAuthException;

/**
 * A simple HTTP client
 */
public class HTTP {

	private static final Logger logger = LoggerFactory.getLogger(HTTP.class);

	public static ClientResponse writeToExternalSystem(ChangeRequest createJazzData, String projectArea/*,
			JazzFormAuthClient client, JazzRootServicesHelper helper*/) {

		// RTC sometimes will declare a property's type, but leave the value
		// empty, which causes errors when parsed by OSLC4J. Set a system
		// property to tell OSLC4J to skip these invalid values.
		System.setProperty(AbstractOslcRdfXmlProvider.OSLC4J_STRICT_DATATYPES, "false");

		try {

			// if (client.login() == HttpStatus.SC_OK) {
			System.out.println(" Form Auth client login successful with the supplied user/password");

//			String catalogUrl = helper.getCatalogUrl();
			// Find the OSLC Service Provider for the project area
			// we want to work with
//			String serviceProviderUrl = JazzClientFactory.jazzRestClient().lookupServiceProviderUrl(catalogUrl, projectArea);
			ClientResponse creationResponse;
			if (createJazzData.getExistingURI() == null) {
				creationResponse = createRemoteObject(createJazzData, JazzClientFactory.jazzRestClient(), JazzClientFactory.getServiceProviderUrl());
			} else {
				creationResponse = updateRemoteObject(createJazzData, JazzClientFactory.jazzRestClient());
			}
			return creationResponse;
			// }
		} catch (Exception e) {
			// logger.debug("login failed....");
			logger.debug(e.getMessage(), e);
		}
		return null;

	}

	private static ClientResponse updateRemoteObject(ChangeRequest createJazzData, JazzFormAuthClient client)
			throws IOException, OAuthException, URISyntaxException {
		ClientResponse creationResponse;
		ChangeRequest updatedCR = client.getResource(createJazzData.getExistingURI()).getEntity(ChangeRequest.class);
		String updateUrl = updatedCR.getAbout().toString(); // +
															// com.lp.alm.adapter.constants.OSLCConstants.OSLC_PROPERTIES_DCTERMS_TITLE;
		//
		// Update the change request at the service provider
		creationResponse = client.updateResource(updateUrl, createJazzData, OslcMediaType.APPLICATION_RDF_XML,
				OslcMediaType.APPLICATION_RDF_XML);

		if (creationResponse.getStatusCode() != HttpStatus.SC_OK) {
			System.err.println("ERROR: Could not create the task (status " + creationResponse.getStatusCode() + ")\n");
			System.err.println(creationResponse.getEntity(String.class));
			return null;
		}

		creationResponse.consumeContent();
		return creationResponse;
	}

	private static ClientResponse createRemoteObject(ChangeRequest createJazzData, JazzFormAuthClient client,
			String serviceProviderUrl)
			throws IOException, OAuthException, URISyntaxException, ResourceNotFoundException {
		CreationFactory taskCreation = client.lookupCreationFactoryResource(serviceProviderUrl,
				OSLCConstants.OSLC_CM_V2, createJazzData.getRdfTypes()[0].toString(),
				OSLCConstants.OSLC_CM_V2 + createJazzData.getWorkItemType());
		String factoryUrl = taskCreation.getCreation().toString();

		// Determine what to use for the Filed Against attribute by requesting
		// the resource shape for the creation factory.
		String shapeUrl = taskCreation.getResourceShapes()[0].toString();
		ClientResponse shapeResponse = client.getResource(shapeUrl);
		ResourceShape shape = shapeResponse.getEntity(ResourceShape.class);

		// Look at the allowed values for Filed Against. This is generally a
		// required field for defects.
		Property filedAgainstProperty = shape
				.getProperty(new URI(com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE
						+ com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST));
		if (filedAgainstProperty != null) {
			URI allowedValuesRef = filedAgainstProperty.getAllowedValuesRef();
			ClientResponse allowedValuesResponse = client.getResource(allowedValuesRef.toString());
			AllowedValues allowedValues = allowedValuesResponse.getEntity(AllowedValues.class);
			Object[] values = allowedValues.getValues().toArray();

			createJazzData.getExtendedProperties()
					.put(new QName(com.lp.alm.adapter.constants.OSLCConstants.RTC_NAMESPACE,
							com.lp.alm.adapter.constants.OSLCConstants.RTC_FILED_AGAINST), (URI) values[1]);
		}

		ClientResponse creationResponse = null;

		// Create the change request
		creationResponse = client.createResource(factoryUrl, createJazzData, OslcMediaType.APPLICATION_RDF_XML,
				OslcMediaType.APPLICATION_RDF_XML);

		if (creationResponse.getStatusCode() != HttpStatus.SC_CREATED) {
			System.err.println("ERROR: Could not create the task (status " + creationResponse.getStatusCode() + ")\n");
			System.err.println(creationResponse.getEntity(String.class));
			return null;
		}
		creationResponse.consumeContent();

		return creationResponse;
	}


}
