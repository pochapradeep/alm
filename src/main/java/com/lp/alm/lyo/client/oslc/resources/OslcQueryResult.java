/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation.
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
 *     Sean Kennedy     - initial API and implementation
 *     Steve Pitschke   - added getMembers() method
 *     Samuel Padgett   - support setting member property
 *     Samuel Padgett   - preserve member property on call to getNext()
 *******************************************************************************/
package com.lp.alm.lyo.client.oslc.resources;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.provider.jena.JenaModelHelper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.lp.alm.lyo.client.oslc.OSLCConstants;


/**
 * The results of an OSLC query. If the query was paged, subsequent pages can be retrieved using the Iterator interface.
 *
 * This class is not currently thread safe.
 */
public class OslcQueryResult implements Iterator<OslcQueryResult> {
	/**
	 * The default member property to look for in OSLC query results
	 * (rdfs:member). Can be changed using {@link #setMemberProperty(Property)}.
	 */
	public final static Property DEFAULT_MEMBER_PROPERTY = RDFS.member;

	/**
	 * If system property {@value} is set to true, find any member in the
	 */
	public final static String SELECT_ANY_MEMBER = "org.eclipse.lyo.client.oslc.query.selectAnyMember";

	/**
	 * Treat any resource in the members resource as a query result (except rdf:type).
	 *
	 * @see OslcQueryResult#SELECT_ANY_MEMBER
	 */
	private final class AnyMemberSelector extends SimpleSelector {
	    private AnyMemberSelector(Resource subject) {
		    super(subject, null, (RDFNode) null);
	    }

	    public boolean selects(Statement s) {
	    	String fqPredicateName = s.getPredicate().getNameSpace() + s.getPredicate().getLocalName();
	    	if (OSLCConstants.RDF_TYPE_PROP.equals(fqPredicateName)) {
	    		return false;
	    	}

	    	return s.getObject().isResource();
	    }
    }

	private final OslcQuery query;

	private final ClientResponse response;

	private final int pageNumber;

	private Property memberProperty = DEFAULT_MEMBER_PROPERTY;

	private Model rdfModel;

	private Resource infoResource, membersResource;

	private String nextPageUrl = "";

	private boolean rdfInitialized = false;

	public OslcQueryResult(OslcQuery query, ClientResponse response) {
		this.query = query;
		this.response = response;

		this.pageNumber = 1;


	}

	private OslcQueryResult(OslcQueryResult prev) {
		this.query = new OslcQuery(prev);
		this.response = this.query.getResponse();
		this.membersResource = prev.membersResource;
		this.memberProperty = prev.memberProperty;

		this.pageNumber = prev.pageNumber + 1;

	}

	private synchronized void initializeRdf() {
		if (!rdfInitialized) {
			rdfInitialized = true;
			rdfModel = ModelFactory.createDefaultModel();
			rdfModel.read(response.getEntity(InputStream.class), query.getCapabilityUrl());

			//Find a resource with rdf:type of oslc:ResourceInfo
			Property rdfType = rdfModel.createProperty(OslcConstants.RDF_NAMESPACE, "type");
			Property responseInfo = rdfModel.createProperty(OslcConstants.OSLC_CORE_NAMESPACE, "ResponseInfo");
			ResIterator iter = rdfModel.listResourcesWithProperty(rdfType, responseInfo);

			//There should only be one - take the first
			infoResource = null;
			while (iter.hasNext()) {
				infoResource = iter.next();
				break;
			}
			membersResource = rdfModel.getResource(query.getCapabilityUrl());
		}
	}

	String getNextPageUrl() {
		initializeRdf();
		if ((nextPageUrl == null || nextPageUrl.isEmpty()) && infoResource != null) {
			Property predicate = rdfModel.getProperty(OslcConstants.OSLC_CORE_NAMESPACE, "nextPage");
			Selector select = new SimpleSelector(infoResource, predicate, (RDFNode) null);
			StmtIterator iter = rdfModel.listStatements(select);
			if (iter.hasNext()) {
				Statement nextPage = iter.next();
				nextPageUrl = nextPage.getResource().getURI();
			} else {
				nextPageUrl = "";
			}
		}
		return nextPageUrl;
	}

	/**
	 * @return whether there is another page of results after this
	 */
	public boolean hasNext() {
		return (!"".equals(getNextPageUrl()));
	}

	/**
	 * @return the next page of results
	 * @throws NoSuchElementException if there is no next page
	 */
	public OslcQueryResult next() {
		return new OslcQueryResult(this);
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public OslcQuery getQuery() {
		return query;
	}

	/**
	 * Returns the member property to find query result resources.
	 *
	 * @return the member property URI
	 * @see #setMemberProperty(String)
	 */
	public String getMemberProperty() {
		return this.memberProperty.getURI();
	}

	/**
	 * Sets the predicate to use to find query result resources. If unset,
	 * defaults to {@code http://www.w3.org/2000/01/rdf-schema#member}.
	 *
	 * @param memberPredicate
	 *            the RDF predicate for member resources from the provider's
	 *            query shape
	 * @see <a href="http://open-services.net/bin/view/Main/OSLCCoreSpecRDFXMLExamples?sortcol=table;up=#Specifying_the_shape_of_a_query">Specifying the sahpe of a query</a>
	 */
	public void setMemberProperty(String memberPredicate) {
		this.memberProperty = ModelFactory.createDefaultModel().createProperty(memberPredicate);
	}

	/**
	 * Get the raw Wink client response to a query.
	 *
	 * NOTE:  Using this method and consuming the response will make other methods
	 * which examine the response unavailable (Examples:  getMemberUrls(), next() and hasNext()).
	 * When this method is invoked, the consumer is responsible for OSLC page processing
	 *
	 * @return
	 */
	public ClientResponse getRawResponse() {
		return response;
	}

	private Selector getMemberSelector() {
		if ("true".equalsIgnoreCase(System.getProperty(SELECT_ANY_MEMBER))) {
			return new AnyMemberSelector(membersResource);
		}

		return new SimpleSelector(membersResource, memberProperty, (RDFNode) null);
	}

	/**
	 * Return the subject URLs of the query response.  The URLs are the location of all artifacts
	 * which satisfy the query conditions.
	 *
	 * NOTE:  Using this method consumes the query response and makes other methods
	 * which examine the response unavailable (Example: getRawResponse().
	 * @return
	 */
	public String[] getMembersUrls() {
		initializeRdf();
		ArrayList<String> membersUrls = new ArrayList<String>();
        Selector select = getMemberSelector();
		StmtIterator iter = rdfModel.listStatements(select);
		while (iter.hasNext()) {
			Statement member = iter.next();
			membersUrls.add(member.getResource().getURI());
		}
		return membersUrls.toArray(new String[membersUrls.size()]);
	}

	/**
	 * Return the enumeration of queried results from this page
	 *
	 * @param T
	 * @param clazz
	 *
	 * @return member statements from current page.
	 */
    public <T> Iterable<T> getMembers(final Class<T> clazz) {
        initializeRdf();

        Selector select = getMemberSelector();
        final StmtIterator iter = rdfModel.listStatements(select);
        Iterable<T> result = new Iterable<T>() {
                public Iterator<T>
                iterator() {
                    return new Iterator<T>() {
                            public boolean hasNext() {
                                return iter.hasNext();
                            }

                            @SuppressWarnings("unchecked")
                            public T next() {
                                Statement member = iter.next();

                                try {
                                    return (T)JenaModelHelper.fromJenaResource((Resource)member.getObject(), clazz);
                                } catch (IllegalArgumentException e) {
                                   throw new IllegalStateException(e.getMessage());
                                } catch (SecurityException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (DatatypeConfigurationException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (IllegalAccessException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (InstantiationException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (InvocationTargetException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (OslcCoreApplicationException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (URISyntaxException e) {
                                    throw new IllegalStateException(e.getMessage());
                                } catch (NoSuchMethodException e) {
                                    throw new IllegalStateException(e.getMessage());
                                }
                            }

                            public void remove() {
                                iter.remove();
                            }
                        };
                }
            };

        return result;
    }
}
