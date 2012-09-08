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
 **/package org.ow2.bonita.rest.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.ow2.bonita.facade.impl.RemoteBAMAPIImpl;
import org.ow2.bonita.facade.impl.RemoteQueryDefinitionAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteCommandAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteIdentityAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteManagementAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteQueryRuntimeAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteRepairAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteRuntimeAPIImpl;
import org.ow2.bonita.facade.rest.RESTRemoteWebAPIImpl;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class BonitaRESTServerApplication extends Application {
	HashSet<Object> singletons = new HashSet<Object>();
	
	public BonitaRESTServerApplication (){
		//Add APIs
		singletons.add(new RemoteBAMAPIImpl());
		singletons.add(new RESTRemoteCommandAPIImpl());
		singletons.add(new RESTRemoteIdentityAPIImpl());
		singletons.add(new RESTRemoteManagementAPIImpl());
		singletons.add(new RemoteQueryDefinitionAPIImpl());
		singletons.add(new RESTRemoteQueryRuntimeAPIImpl());
		singletons.add(new RESTRemoteRepairAPIImpl());
		singletons.add(new RESTRemoteRuntimeAPIImpl());
		singletons.add(new RESTRemoteWebAPIImpl());
	}

	public Set<Object> getSingletons(){
		return this.singletons;
	}
	
	public Set<Class<?>> getClasses(){
		HashSet<Class<?>> set = new HashSet<Class<?>>();
		return set;
	}
}
