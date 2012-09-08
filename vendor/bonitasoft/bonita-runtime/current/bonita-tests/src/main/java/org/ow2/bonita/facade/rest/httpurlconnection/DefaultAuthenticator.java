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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class DefaultAuthenticator extends Authenticator {
	
	private static ThreadLocal<String> username = new ThreadLocal<String>();
	private static ThreadLocal<String> password = new ThreadLocal<String>();
	
	public static void setThreadUserName (String user){
		username.set(user);
	}
	
	public static void setThreadPassword (String pwd){
		password.set(pwd);
	}
	
	/* (non-Javadoc)
	 * @see java.net.Authenticator#getPasswordAuthentication()
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {		
		return new PasswordAuthentication(username.get(), password.get().toCharArray());
	}
}
