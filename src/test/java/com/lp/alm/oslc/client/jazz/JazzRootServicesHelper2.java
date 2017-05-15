/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *
 *  Contributors:
 *
 *     Michael Fiedler     - initial API and implementation
 *     Samuel Padgett      - add getter for RDF model so clients can read other services
 *     Samuel Padgett      - add request consumer key and OAuth approval module URLs
 *     Samuel Padgett      - handle trailing '/' in baseUrl
 *******************************************************************************/
package com.lp.alm.oslc.client.jazz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.wink.client.ClientResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.lp.alm.lyo.client.exception.ResourceNotFoundException;
import com.lp.alm.lyo.client.exception.RootServicesException;
import com.lp.alm.lyo.client.oslc.OSLCConstants;
import com.lp.alm.lyo.client.oslc.OslcClient;
import com.lp.alm.lyo.client.oslc.OslcOAuthClient;
import com.lp.alm.lyo.client.oslc.jazz.JazzFormAuthClient;
import com.lp.alm.lyo.client.oslc.jazz.JazzRootServicesConstants;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;

/**
 * Helper class to assist in retrieval of attributes from the IBM Rational
 * Jazz rootservices document
 *
 * This class is not currently thread safe.
 */
public class JazzRootServicesHelper2 {
	private String baseUrl;
	private String rootServicesUrl;
	private String catalogDomain;
	private String catalogNamespace;
	private String catalogProperty;
	private String catalogUrl;
	private Model rdfModel;

	//OAuth URLs
	String authorizationRealm;
	String requestTokenUrl;
	String authorizationTokenUrl;
	String accessTokenUrl;
	String requestConsumerKeyUrl;
	String consumerApprovalUrl;

	public static final String JFS_NAMESPACE = "http://jazz.net/xmlns/prod/jazz/jfs/1.0/";
	public static final String JD_NAMESPACE = "http://jazz.net/xmlns/prod/jazz/discovery/1.0/";

	private static final Logger logger = LoggerFactory.getLogger(JazzRootServicesHelper2.class.getName());

	/**
	 * Initialize Jazz rootservices-related URLs such as the catalog location and OAuth URLs
	 *
	 * rootservices is unprotected and access does not require authentication
	 *
	 * @param url - base URL of the Jazz server, no including /rootservices.  Example:  https://example.com:9443/ccm
	 * @param catalogDomain - Namespace of the OSLC domain to find the catalog for.  Example:  OSLCConstants.OSLC_CM
	 * @throws RootServicesException
	 */
	public JazzRootServicesHelper2 (String url, String catalogDomain) throws RootServicesException {
		this.baseUrl = url;
		this.rootServicesUrl = UriBuilder.fromUri(this.baseUrl).path("rootservices").build().toString();
		logger.debug(String.format("Fetching rootservices document at URL <%s>", this.rootServicesUrl));
		this.catalogDomain = catalogDomain;
		logger.debug(String.format("Using catalog domain <%s>", this.catalogDomain));

		if (this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_CM) ||
		    this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_CM_V2)) {

			this.catalogNamespace = OSLCConstants.OSLC_CM;
			this.catalogProperty  = JazzRootServicesConstants.CM_ROOTSERVICES_CATALOG_PROP;

		} else if (this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_QM) ||
			       this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_QM_V2)) {

			this.catalogNamespace = OSLCConstants.OSLC_QM;
			this.catalogProperty =  JazzRootServicesConstants.QM_ROOTSERVICES_CATALOG_PROP;

		} else if (this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_RM) ||
			       this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_RM_V2)) {

			this.catalogNamespace = OSLCConstants.OSLC_RM;
			this.catalogProperty =  JazzRootServicesConstants.RM_ROOTSERVICES_CATALOG_PROP;

		} else if (this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_AM_V2)) {

			this.catalogNamespace = OSLCConstants.OSLC_AM_V2;
			this.catalogProperty =  JazzRootServicesConstants.AM_ROOTSERVICES_CATALOG_PROP;

		}
		else if (this.catalogDomain.equalsIgnoreCase(OSLCConstants.OSLC_AUTO)) {

			this.catalogNamespace = OSLCConstants.OSLC_AUTO;
			this.catalogProperty =  JazzRootServicesConstants.AUTO_ROOTSERVICES_CATALOG_PROP;

		}
		else {
			logger.error("Jazz rootservices only supports CM, RM, QM, and Automation catalogs");
		}

		processRootServices();
	}
	
	public static void main(String args[]){
		
			try {
				new JazzRootServicesHelper2("https://34.209.145.222:9443/ccm/rootservices","http://open-services.net/ns/cm#");
			} catch (RootServicesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	/**
	 * Get the OSLC Catalog URL
	 *
	 * @return the catalog URL
	 */
	public String getCatalogUrl()
	{
		return catalogUrl;
	}

	public OslcOAuthClient initOAuthClient(String consumerKey, String secret) {
		return new OslcOAuthClient (
								requestTokenUrl,
								authorizationTokenUrl,
								accessTokenUrl,
								consumerKey,
								secret,
								authorizationRealm );
	}

	public JazzFormAuthClient initFormClient(String userid, String password)
	{
		return new JazzFormAuthClient(baseUrl, userid, password);

	}

	/**
	 * Creates a form auth client for authenticating with the Jazz server.
	 *
	 * @param userid
	 *            the Jazz user ID
	 * @param password
	 *            the Jazz user password or form-based authentication
	 * @param authUrl
	 *            - the base URL to use for authentication. This is normally the
	 *            application base URL for RQM and RTC and is the JTS
	 *            application URL for fronting applications like RRC and DM.
	 *
	 * @return the form auth client
	 */
	public JazzFormAuthClient initFormClient(String userid, String password, String authUrl)
	{
		return new JazzFormAuthClient(baseUrl, authUrl, userid, password);

	}
	
	  private static DefaultHttpClient wrapClient(DefaultHttpClient base) {
		    String currentMethod = "wrapClient";
		    try {
		      SSLContext ctx = SSLContext.getInstance("TLS");
		      X509TrustManager tm = new X509TrustManager() {

		        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		        }

		        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		        }

		        public X509Certificate[] getAcceptedIssuers() {
		          return (X509Certificate[]) null;
		        }
		      };
		      ctx.init(null, new TrustManager[] { tm }, null);
		      SSLSocketFactory ssf = new SSLSocketFactory(ctx);
		      ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		      ClientConnectionManager ccm = base.getConnectionManager();
		      SchemeRegistry sr = ccm.getSchemeRegistry();
		      sr.register(new Scheme("https", ssf, 443));
		      return new DefaultHttpClient(ccm, base.getParams());
		    } 
		    catch (Exception ex) {
//		      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
		      return null;
		    }
		  }
	  
	  public static HttpResponse get(String url, String accept, String user, String password, Map<String, String> headers) 
		      throws ClientProtocolException, IOException {
		    return get(url, accept, null, user, password, null, null, headers);
		  }
		  

	  public static HttpResponse get(String url, String accept, String content, 
		      String user, String password, OAuthAccessor accessor, String verifier, Map<String, String> headers) 
		          throws ClientProtocolException, IOException {
		    String currentMethod = "get";
		    
		    DefaultHttpClient client = new DefaultHttpClient();
		    client = wrapClient(client);
		    
		    HttpGet httpget = new HttpGet(url);
		    

		    
		    httpget.addHeader("OSLC-Core-Version", "2.0");
		    if (accept != null)
		      httpget.addHeader("Accept", accept);
		    if (content != null)
		      httpget.addHeader("Content-Type", content);
		    
		    if (accessor != null) {
//			httpget.addHeader(
//		          "Authorization", getOAuthAuthorizationHeader(url, "GET", accessor, verifier, phase2));
		    }
		    else if (user != null && password != null) {
		      StringBuilder builder = new StringBuilder();
		      httpget.addHeader("Authorization", "Basic " + 
		          javax.xml.bind.DatatypeConverter.printBase64Binary(
		              builder.append(user).append(':').append(password).toString().getBytes()));
		    }
		    
		    if(headers != null){
		      Set<Entry<String, String>> entrySet = headers.entrySet();
		      for (Entry<String, String> entry : entrySet) {
		        httpget.addHeader(entry.getKey(), entry.getValue());
		      }
		    }
		    
		    try {
//		      String logMessage =LogUtils.createLogMessage(url, null, httpget.getAllHeaders(),"GET");
//		      logger.debug(logMessage);
		      HttpResponse response = client.execute(httpget);
//		      logResponse(response, url, "GET");
		      return response;
		      
		    } 
		    catch (ClientProtocolException e) {
//		      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
		      throw e;
		    } 
		    catch (IOException e) {
//		      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
		      throw e;
		    }
		  }
	  public static enum OAuthPhases {
		    /**
		     * OAuth dance phase 1 - getting request token
		     */
		    OAUTH_PHASE_1,
		    /**
		     * OAuth dance phase 2 - user authorization and getting access token
		     */
		    OAUTH_PHASE_2,
		    /**
		     * OAuth authorization (using access token)
		     */
		    OAUTH_PHASE_3
		  };
	  private static String getOAuthAuthorizationHeader(
		      String url, String method, OAuthAccessor accessor, String verifier, OAuthPhases phase) {
		    String currentMethod = "getOAuthAuthorization";
		    
		    String oAuthHeader = null;
		    
		    List<OAuth.Parameter> params = new ArrayList<OAuth.Parameter>();
		    switch (phase) {
		      case OAUTH_PHASE_1:
		        params.add(new OAuth.Parameter("oauth_callback", accessor.consumer.callbackURL));
		        break;
		      case OAUTH_PHASE_2:
		        params.add(new OAuth.Parameter("oauth_token", accessor.requestToken));
		        if (verifier != null) {
		          params.add(new OAuth.Parameter("oauth_verifier", verifier));
		        }
		        break;
		      case OAUTH_PHASE_3:
		        params.add(new OAuth.Parameter("oauth_token", accessor.accessToken));
		        break;
		    }
		    
		    OAuthMessage request;
		    try {
		      request = accessor.newRequestMessage(method, url, params);
		      oAuthHeader = request.getAuthorizationHeader("JIRA");
		    }
		    catch (OAuthException e) {
//		      logger.error(CURREN?T_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
		    } 
		    catch (IOException e) {
//		      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
		    } 
		    catch (URISyntaxException e) {
//		      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
		    }
		    
		    return oAuthHeader;
		  }
	  
	private void processRootServices() throws RootServicesException
	{
		try {
			OslcClient rootServicesClient = new OslcClient();
		      HttpResponse resp =get(rootServicesUrl, "application/rdf+xml", null, null, null);

			ClientResponse response = rootServicesClient.getResource(rootServicesUrl,OSLCConstants.CT_RDF);
			
			
			InputStream is = response.getEntity(InputStream.class);
			rdfModel = ModelFactory.createDefaultModel();
			rdfModel.read(is,rootServicesUrl);

			//get the catalog URL
			this.catalogUrl = getRootServicesProperty(rdfModel, this.catalogNamespace, this.catalogProperty);

			//get the OAuth URLs
			this.requestTokenUrl = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_REQUEST_TOKEN_URL);
			this.authorizationTokenUrl = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_USER_AUTH_URL);
			this.accessTokenUrl = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_ACCESS_TOKEN_URL);
			try { // Following field is optional, try to get it, if not found ignore exception because it will use the default
				this.authorizationRealm = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_REALM_NAME);
			} catch (ResourceNotFoundException e) {
				logger.debug(String.format("OAuth authorization realm not found in rootservices <%s>", rootServicesUrl));
			}

			try {
				this.requestConsumerKeyUrl = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_REQUEST_CONSUMER_KEY_URL);
			} catch (ResourceNotFoundException e) {
				logger.debug(String.format("OAuth request consumer key URL not found in rootservices <%s>", rootServicesUrl));
			}

			try {
				this.consumerApprovalUrl = getRootServicesProperty(rdfModel, JFS_NAMESPACE, JazzRootServicesConstants.OAUTH_APPROVAL_MODULE_URL);
			} catch (ResourceNotFoundException e) {
				logger.debug(String.format("OAuth approval module URL not found in rootservices <%s>", rootServicesUrl));
			}
		} catch (Exception e) {
			throw new RootServicesException(this.baseUrl, e);
		}


	}

	private String getRootServicesProperty(Model rdfModel, String namespace, String predicate) throws ResourceNotFoundException {
		String returnVal = null;

		Property prop = rdfModel.createProperty(namespace, predicate);
		Statement stmt = rdfModel.getProperty((Resource) null, prop);
		if (stmt != null && stmt.getObject() != null)
			returnVal = stmt.getObject().toString();
		if (returnVal == null)
		{
			throw new ResourceNotFoundException(baseUrl, namespace + predicate);
		}
		return returnVal;
	}

	/**
	 * Returns the underlying RDF model for the rootservices document. This
	 * allows clients to read other service URLs not directly supported by this
	 * class.
	 *
	 * @return the RDF model
	 */
	public Model getRdfModel() {
		return rdfModel;
	}

	/**
	 * Gets the URL for requesting an OAuth consumer key.
	 *
	 * @return the request consumer key URL
	 * @see <a href="https://jazz.net/wiki/bin/view/Main/RootServicesSpecAddendum2">RootServicesSpecAddendum2</a>
	 */
	public String getRequestConsumerKeyUrl() {
		return requestConsumerKeyUrl;
	}

	/**
	 * Gets the URL for approving an OAuth consumer
	 *
	 * @return the approval URL
	 * @see <a href="https://jazz.net/wiki/bin/view/Main/RootServicesSpecAddendum2">RootServicesSpecAddendum2</a>
	 */
	public String getConsumerApprovalUrl() {
		return consumerApprovalUrl;
	}
}
