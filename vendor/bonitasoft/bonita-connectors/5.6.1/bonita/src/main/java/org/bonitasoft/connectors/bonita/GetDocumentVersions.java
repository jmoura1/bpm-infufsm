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
public class GetDocumentVersions extends ProcessConnector {

    private String documentUUID;
    private List<Document> documentList = new ArrayList<Document>();
    private static final Log logger = LogFactory.getLog(GetDocumentVersions.class.getClass());

    /*
     * (non-Javadoc)
     * 
     * @see org.ow2.bonita.connector.core.Connector#executeConnector()
     */
    @Override
    protected void executeConnector() throws Exception {
        final QueryRuntimeAPI queryTimeAPI = getApiAccessor().getQueryRuntimeAPI();
        try {
            documentList = queryTimeAPI.getDocumentVersions(new DocumentUUID(documentUUID));// .getDocument(new
        } catch (final DocumentNotFoundException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("document not found with UUID " + documentUUID);
            }
            throw e;
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
        final QueryRuntimeAPI queryTimeAPI = getApiAccessor().getQueryRuntimeAPI();
        ConnectorError error = null;
        if (documentUUID.length() != 32) {
            error = new ConnectorError("documentUUID Error", new Exception("length of documentUUID should be 32"));
            errors.add(error);
        }
        try {
            queryTimeAPI.getDocument(new DocumentUUID(documentUUID));
        } catch (final DocumentNotFoundException e) {
            error = new ConnectorError("document not found with UUID " + documentUUID, e);
            errors.add(error);
        }
        return errors;
    }

    /**
     * set document UUIDs
     * 
     * @param documentUUIDs
     */
    public void setDocumentUUID(final String documentUUID) {
        this.documentUUID = documentUUID;
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
