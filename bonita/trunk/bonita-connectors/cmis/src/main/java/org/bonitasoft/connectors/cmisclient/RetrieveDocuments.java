/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.cmisclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bonitasoft.connectors.cmisclient.common.AbstractCmisConnector;
import org.ow2.bonita.connector.core.ConnectorError;

/**
 * @author Yanyan Liu
 * 
 */
public class RetrieveDocuments extends AbstractCmisConnector {

    private Map<String, String> conditions;
    private List<Document> cmisDocuments = new ArrayList<Document>();
    private static final Log logger = LogFactory.getLog(RetrieveDocuments.class.getClass());

    /*
     * (non-Javadoc)
     * 
     * @see org.ow2.bonita.connector.core.Connector#executeConnector()
     */
    @Override
    protected void executeConnector() throws Exception {
        cmisDocuments = retrieveDocumentsByConditions(username, password, url, binding_type, repositoryName, conditions);
    }

    /**
     * generate the whole query sentence.
     * 
     * @param conditions
     * @return
     */
    private String prepareQuery(final Map<String, String> conditions) {

        final String query = "SELECT * from cmis:document";
        final StringBuilder stringBuilder = new StringBuilder(query);
        if (conditions != null && !conditions.isEmpty()) {
            stringBuilder.append(" where");
            int i = 0;
            for (Entry<String, String> entry : conditions.entrySet()) {
                if (i != 0) {
                    stringBuilder.append(" and");
                }
                stringBuilder.append(" cmis:" + entry.getKey() + " = '" + entry.getValue() + "'");
                i++;
            }
        }
        return stringBuilder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ow2.bonita.connector.core.Connector#validateValues()
     */
    @Override
    protected List<ConnectorError> validateValues() {
        return null;
    }

    /**
     * retrieve CMIS documents by query.
     * 
     * @param username
     * @param password
     * @param url
     * @param binding
     * @param repositoryName
     * @param conditions
     * @return List<Document>
     */
    protected List<Document> retrieveDocumentsByConditions(final String username, final String password, final String url, final String binding, final String repositoryName, final Map<String, String> conditions) {
        final Session s = createSessionByName(username, password, url, binding, repositoryName);
        String cmisSql = prepareQuery(conditions);
        if (logger.isInfoEnabled())
            logger.info("cmisSql = " + cmisSql);
        ItemIterable<QueryResult> items = s.query(cmisSql, true).getPage(100); // limitation: 100
        Document cmisDocument = null;
        for (QueryResult item : items) {
            PropertyData<?> propertyData = item.getPropertyByQueryName("cmis:objectId");
            if (logger.isInfoEnabled()) {
                // print information
                List<PropertyData<?>> properties = item.getProperties();
                for (PropertyData<?> property : properties) {
                    logger.info(property.getQueryName() + " => " + property.getValues().toString());
                }
            }
            cmisDocument = (Document) s.getObject(s.createObjectId(propertyData.getFirstValue().toString()));

            cmisDocuments.add(cmisDocument);
        }

        return cmisDocuments;
    }

    /**
     * set conditions
     */
    public void setConditions(final Map<String, String> conditions) {
        this.conditions = conditions;
    }

    /**
     * get CMIS documents
     * @return List<Document>
     */
    public List<Document> getCmisDocuments() {
        return cmisDocuments;
    }

    /**
     * set the parameters
     * 
     * @param parameters the parameters
     */
    public void setConditions(final List<List<Object>> conditions) {
        setConditions(bonitaListToMap(conditions, String.class, String.class));
    }

}
