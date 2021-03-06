/**
 * Copyright (C) 2009 BonitaSoft S.A.
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
package org.bonitasoft.connectors.microsoft.sharepoint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.util.BonitaException;

import junit.framework.TestCase;

/**
 * @author Jordi Anguela
 */
public class CancelCheckoutTest extends TestCase {
	
	protected static final Logger LOG = Logger.getLogger(CancelCheckoutTest.class.getName());

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
	    if (LOG.isLoggable(Level.WARNING)) {
	    	LOG.warning("======== Starting test: " + this.getClass().getName() + "." + this.getName() + "() ==========");
	    }
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (LOG.isLoggable(Level.WARNING)) {
	    	LOG.warning("======== Ending test: " + this.getName() + " ==========");
	    }
	    super.tearDown();
	}

	public void testValidParameters() throws BonitaException {
		SharepointCancelCheckout connector = getValidConnector();
		try {
			List<ConnectorError> errors =  connector.validateValues();
			assertTrue( errors.size() == 0 );
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	public void testEmptyUsername() throws BonitaException {
		SharepointCancelCheckout connector = getValidConnector();
		connector.setUsername("");
		try {
			List<ConnectorError> errors =  connector.validateValues();
			assertTrue( errors.size() == 1 );
			ConnectorError error = errors.get(0);
			assertTrue("username".equals(error.getField()));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	public void testEmptyPassword() throws BonitaException {
		SharepointCancelCheckout connector = getValidConnector();
		connector.setPassword("");
		try {
			List<ConnectorError> errors =  connector.validateValues();
			assertTrue( errors.size() == 1 );
			ConnectorError error = errors.get(0);
			assertTrue("password".equals(error.getField()));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	public void testEmptyFileUrl() throws BonitaException {
		SharepointCancelCheckout connector = getValidConnector();
		connector.setFileUrl("");
		try {
			List<ConnectorError> errors =  connector.validateValues();
			assertTrue( errors.size() == 1 );
			ConnectorError error = errors.get(0);
			assertTrue("fileUrl".equals(error.getField()));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	private SharepointCancelCheckout getValidConnector() {
		SharepointCancelCheckout connector = new SharepointCancelCheckout();
		
		String username = "username";
		String password = "password";
		String fileUrl  = "https://yourhost.sharepoint.emea.microsoftonline.com/Folder/File";
		
		connector.setUsername(username);
		connector.setPassword(password);
		connector.setFileUrl(fileUrl);
		return connector;
	}
}
