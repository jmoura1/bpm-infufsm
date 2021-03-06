/**
 * Copyright (C) 2010  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.ow2.bonita.facade.rest.httpurlconnection;

import java.net.HttpURLConnection;

import org.ow2.bonita.facade.identity.User;
import org.ow2.bonita.facade.rest.httpurlconnection.api.RESTHttpURLConnectionIdentityAPI;
import org.ow2.bonita.util.xml.XStreamUtil;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class RESTHttpURLConnectionAuthenticationTest extends RESTHttpURLConnectionAPITestCase {
	
	public void testUnauthorizedUser() throws Exception{
		setDefaultAuthenticator();		
		setCurrentUserAndPassword("unauthorizeduser", "unauthorized");
		HttpURLConnection getUserByUUIDResponse = RESTHttpURLConnectionIdentityAPI.getUserByUUID("unauthorizeduser", getUserForTheOptionsMap());
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getUserByUUIDResponse.getResponseCode());
		getUserByUUIDResponse.disconnect();
	}
	
	public void testAuthorizedUser() throws Exception{
		setDefaultAuthenticator();		
		setCurrentUserAndPassword(REST_USER, REST_PSWD);
		HttpURLConnection addUserConnection = RESTHttpURLConnectionIdentityAPI.addUser("authorizeduser", "authorized", getUserForTheOptionsMap());
		assertEquals(HttpURLConnection.HTTP_OK, addUserConnection.getResponseCode());
		
		String xmlUser = HttpURLConnectionUtil.getResponseContent(addUserConnection);
		addUserConnection.disconnect();
		
		XStream xstream = XStreamUtil.getDefaultXstream();		
		User expectedUser = (User)xstream.fromXML(xmlUser);
		
		setCurrentUserAndPassword(REST_USER, REST_PSWD);
		HttpURLConnection getUserByUUIDConnection = RESTHttpURLConnectionIdentityAPI.getUserByUUID(expectedUser.getUUID().toString(), getUserForTheOptionsMap());
		assertEquals(HttpURLConnection.HTTP_OK, getUserByUUIDConnection.getResponseCode());
		String xmlRetrievedUser = HttpURLConnectionUtil.getResponseContent(getUserByUUIDConnection);
		getUserByUUIDConnection.disconnect();
		
		User retrievedUser = (User) xstream.fromXML(xmlRetrievedUser);
		assertEquals(expectedUser.getUUID(), retrievedUser.getUUID());
				
		setCurrentUserAndPassword(REST_USER, REST_PSWD);
		HttpURLConnection removeUserByUUIDConnection = RESTHttpURLConnectionIdentityAPI.removeUserByUUID(expectedUser.getUUID().toString(), getUserForTheOptionsMap());
		assertEquals(HttpURLConnection.HTTP_NO_CONTENT, removeUserByUUIDConnection.getResponseCode());
		removeUserByUUIDConnection.disconnect();
	}
}
