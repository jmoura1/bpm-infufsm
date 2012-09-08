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
package org.bonitasoft.connectors.bonita;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.ProcessConnector;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.uuid.DocumentUUID;

/**
 * @author Yanyan Liu
 * 
 */
public class GetDocuments extends ProcessConnector {

    private String[] documentUUIDs;
    private List<Document> documentList = new ArrayList<Document>();
    private static final Log logger = LogFactory.getLog(GetDocuments.class.getClass());

    /*
     * (non-Javadoc)
     * 
     * @see org.ow2.bonita.connector.core.Connector#executeConnector()
     */
    @Override
    protected void executeConnector() throws Exception {
        final QueryRuntimeAPI queryTimeAPI = getApiAccessor().getQueryRuntimeAPI();
        Document document = null;
        for (String documentUUID : documentUUIDs) {
            try {
                document = queryTimeAPI.getDocument(new DocumentUUID(documentUUID));
            } catch (final DocumentNotFoundException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("document not found with UUID " + documentUUID);
                }
                throw e;
            }
            if (document != null)
                documentList.add(document);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ow2.bonita.connector.core.Connector#validateValues()
     */
    @Override
    protected List<ConnectorError> validateValues() {
        final List<ConnectorError> errors = new ArrayList<ConnectorError>();
        ConnectorError error = null;
        for (final String documentUUID : documentUUIDs) {
            if (documentUUID.length() != 32) {
                error = new ConnectorError("documentUUID Error", new Exception("length of documentUUID should be 32"));
                errors.add(error);
            }
        }
        return errors;
    }

    /**
     * set document UUIDs
     * 
     * @param documentUUIDs
     */
    public void setDocumentUUIDs(final String[] documentUUIDs) {
        this.documentUUIDs = documentUUIDs;
    }

    /**
     * set document UUIDs
     * 
     * @param documentUUIDs
     */
    public void setDocumentUUIDs(final List<List<Object>> documentUUIDs) {
        this.documentUUIDs = convertToStringArray(documentUUIDs);
    }

    /**
     * convert List to String Array
     * 
     * @param arrayList
     * @return
     */
    private String[] convertToStringArray(final List<List<Object>> arrayList) {
        String[] stringArray = null;
        if (arrayList != null && arrayList.size() > 0) {
            stringArray = new String[arrayList.size()];
            int i = 0;
            String fieldToRetrieve;
            for (List<Object> list : arrayList) {
                fieldToRetrieve = (String) list.get(0);
                stringArray[i] = fieldToRetrieve;
                i++;
            }
        }
        return stringArray;
    }

    /**
     * get document list
     * 
     * @return List<Document>
     */
    public List<Document> getDocumentList() {
        return documentList;
    }
}
