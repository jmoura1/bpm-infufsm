/**
 * Copyright (C) 2009-2011 BonitaSoft S.A.
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
package org.bonitasoft.connectors.sugar;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;
import javax.xml.rpc.ServiceException;

import org.bonitasoft.connectors.sugar.common.SugarCrmSoapClient;
import org.bonitasoft.connectors.sugar.common.soap.v2.Link_name_to_fields_array;
import org.ow2.bonita.connector.core.ConnectorError;

/**
 * 
 * @author Jordi Anguela, Matthieu Chaffotte, Yanyan Liu
 * 
 */
public class GetSugarEntries extends SugarCrmSoapClient {

    // SugarConfiguration
    private String sugarSoapPort = "";
    private String applicationName = "";
    private String user = "";
    private String password = "";

    // Input Parameters
    private String module = "";
    private String query = "";
    private String orderBy = "";
    private String[] fieldsToRetrieve = new String[] {};
    private String linkName = "";
    private String[] linkFields = new String[] {};

    // Output Parameters
    private List<List<Object>> response;

    private Link_name_to_fields_array[] links = null;

    @Override
    protected List<ConnectorError> validateValues() {
        final List<ConnectorError> errors = new ArrayList<ConnectorError>();

        testEmptyVar(sugarSoapPort, "sugarSoapPort", errors);
        testEmptyVar(applicationName, "applicationName", errors);
        testEmptyVar(user, "user", errors);
        testEmptyVar(module, "module", errors);

        try {
            this.initSugarCrmSoapClient(sugarSoapPort, user, password, applicationName);
        } catch (MalformedURLException e) {
            errors.add(new ConnectorError("sugarSoapPort", new MalformedURLException("URL not valid! " + e.getMessage())));
        } catch (LoginException e) {
            errors.add(new ConnectorError("user", new LoginException("Wrong user or password! " + e.getMessage())));
        } catch (RemoteException e) {
            errors.add(new ConnectorError("sugarSoapPort", new RemoteException("Error accessing Sugar services! " + e.getMessage())));
        } catch (ServiceException e) {
            errors.add(new ConnectorError("sugarSoapPort", new ServiceException("Error accessing Sugar services! " + e.getMessage())));
        } catch (Exception e) {
            errors.add(new ConnectorError("sugarSoapPort", new Exception("Exception occurred! " + e.getMessage())));
        }

        return errors;
    }

    @Override
    protected void executeConnector() throws Exception {

        if (links == null && linkName.length() > 0 && linkFields.length > 0) {
            final Link_name_to_fields_array newLink = new Link_name_to_fields_array(linkName, linkFields);
            links = new Link_name_to_fields_array[] { newLink };
        }

        response = this.getEntryList(module, query, orderBy, fieldsToRetrieve, links);

    }

    /**
     * validation
     * 
     * @param var
     * @param varName
     * @param errors
     */
    private void testEmptyVar(String var, final String varName, final List<ConnectorError> errors) {

        if (var != null) {
            var = var.trim();
            if (var.length() > 0) {
                return;
            }
        }
        errors.add(new ConnectorError(varName, new IllegalArgumentException("Cannot be empty!")));
    }

    /**
     * set the sugarSoapPort
     * 
     * @param sugarSoapPort the sugar SOAP port
     */
    public void setSugarSoapPort(final String sugarSoapPort) {
        this.sugarSoapPort = sugarSoapPort;
    }

    /**
     * set the applicationName
     * 
     * @param applicationName the application name
     */
    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * set the user
     * 
     * @param user the user name
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * set the password
     * 
     * @param password the password
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * set the module
     * 
     * @param module the sugar module
     */
    public void setModule(final String module) {
        this.module = module;
    }

    /**
     * set the query
     * 
     * @param query the query condition
     */
    public void setQuery(final String query) {
        this.query = query;
    }

    /**
     * set the orderBy
     * 
     * @param orderBy the order by
     */
    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * set the fieldsToRetrieve
     * 
     * @param fieldsToRetrieve String[] the fields to retrieve
     */
    public void setFieldsToRetrieve(final String[] fieldsToRetrieve) {
        this.fieldsToRetrieve = fieldsToRetrieve;
    }

    /**
     * set fieldsToRetrieve
     * 
     * @param fieldsToRetrieve List<List<Object>> set the fields to retrieve
     */
    public void setFieldsToRetrieve(final List<List<Object>> fieldsToRetrieve) {
        this.fieldsToRetrieve = convertToStringArray(fieldsToRetrieve);
    }

    /**
     * set the links
     * 
     * @param links the links
     */
    public void setLinks(Link_name_to_fields_array[] links) {
        this.links = links;
    }

    /**
     * set the linkName
     * 
     * @param linkName the link name
     */
    public void setLinkName(final String linkName) {
        if (linkName == null) {
            this.linkName = "";
        } else {
            this.linkName = linkName.toLowerCase();
        }
    }

    /**
     * set the linkFields
     * 
     * @param linkFields the link fields
     */
    public void setLinkFields(final String[] linkFields) {
        this.linkFields = linkFields;
    }

    /**
     * set the linkFields
     * 
     * @param linkFields the link fields
     */
    public void setLinkFields(final List<List<Object>> linkFields) {
        this.linkFields = convertToStringArray(linkFields);
    }

    /**
     * @return <List<Object>>
     */
    public List<List<Object>> getResponse() {
        return response;
    }

    /**
     * convert to String Array
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

}
