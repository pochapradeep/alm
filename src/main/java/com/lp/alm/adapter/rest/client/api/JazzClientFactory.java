package com.lp.alm.adapter.rest.client.api;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lp.alm.adapter.constants.OSLCConstants;
import com.lp.alm.lyo.client.exception.JazzAuthErrorException;
import com.lp.alm.lyo.client.exception.JazzAuthFailedException;
import com.lp.alm.lyo.client.exception.ResourceNotFoundException;
import com.lp.alm.lyo.client.exception.RootServicesException;
import com.lp.alm.lyo.client.oslc.jazz.JazzFormAuthClient;
import com.lp.alm.lyo.client.oslc.jazz.JazzRootServicesHelper;
import com.lp.alm.oslc.client.jazz.JazzArtifactBuilder;

import net.oauth.OAuthException;

public class JazzClientFactory {

	final static Logger logger = LoggerFactory.getLogger(JazzArtifactBuilder.class);
	public static JazzRootServicesHelper JAZZ_HELPER = null;
	private static JazzFormAuthClient JAZZ_CLIENT = null;
	private static String QUERY_CAPABILITY = null;
	private static String SERVICE_PROVIDER_URL = null;

	static {
		try {
			JAZZ_HELPER = new JazzRootServicesHelper(OSLCConstants.JAZZ_CM_ROOT_SERVICES,
					"http://open-services.net/ns/cm#");
			jazzRestClientLogin();
			SERVICE_PROVIDER_URL = buildServiceProviderUrl();
			// STEP 6: Get the Query Capabilities URL so that we can run some
			// OSLC queries
			buildQueryCapability();

		} catch (RootServicesException e) {
			logger.error("Root Service Exception while init Jazz client ");
			e.printStackTrace();
		} catch (JazzAuthFailedException e) {
			logger.error("Jazz Auth Failed Exception  while init Jazz client ");
			e.printStackTrace();
		} catch (JazzAuthErrorException e) {
			logger.error("Jazz Auth Failed Exception  while init Jazz client ");
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			logger.error("Client Protocol Exception  while init Jazz client ");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("IO Exception while init Jazz client ");
			e.printStackTrace();
		}

	}

	private static void buildQueryCapability()
			/*throws IOException, OAuthException, URISyntaxException, ResourceNotFoundException*/ {
		try {
			QUERY_CAPABILITY = JAZZ_CLIENT.lookupQueryCapability(SERVICE_PROVIDER_URL, OSLCConstants.OSLC_CM_V2,
					"http://open-services.net/ns/cm#ChangeRequest");
		} catch (ResourceNotFoundException | IOException | OAuthException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getQueryCapability(){
		return QUERY_CAPABILITY;
	}

	private static String buildServiceProviderUrl()
			/*throws IOException, OAuthException, URISyntaxException, ResourceNotFoundException*/ {
		String serviceProviderUrl = null;
		try {
			serviceProviderUrl = JAZZ_CLIENT.lookupServiceProviderUrl(JAZZ_HELPER.getCatalogUrl(),
					OSLCConstants.PROJECT[OSLCConstants.RTC_OFFSET]);
		} catch (ResourceNotFoundException | IOException | OAuthException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serviceProviderUrl;
	}

	private static void jazzRestClientLogin()
			throws JazzAuthFailedException, JazzAuthErrorException, ClientProtocolException, IOException {
		JAZZ_CLIENT = getRootServiceHelper().initFormClient(OSLCConstants.JAZZ_ADMIN_USERNAME, OSLCConstants.JAZZ_ADMIN_PASSWORD);
		JAZZ_CLIENT.login();
	}
	
	public static String getServiceProviderUrl(){
		return SERVICE_PROVIDER_URL;
	}

	public static JazzFormAuthClient jazzRestClient()
			/*throws JazzAuthFailedException, JazzAuthErrorException, ClientProtocolException, IOException*/ {
		return JAZZ_CLIENT;
	}

	public static JazzRootServicesHelper getRootServiceHelper() {
		return JAZZ_HELPER;

	}
}
