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
package org.ow2.bonita.facade.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.ow2.bonita.facade.BAMAPI;
import org.ow2.bonita.facade.QueryAPIAccessor;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.internal.RESTRemoteQueryRuntimeAPI;
import org.ow2.bonita.facade.internal.RemoteBAMAPI;
import org.ow2.bonita.facade.internal.RemoteQueryDefinitionAPI;
import org.ow2.bonita.facade.rest.interceptor.BonitaClientErrorInterceptor;
import org.ow2.bonita.facade.rest.interceptor.RESTClientExecutionInterceptor;
import org.ow2.bonita.facade.rest.provider.GenericObjectProvider;
import org.ow2.bonita.facade.rest.provider.OctectStreamProvider;
import org.ow2.bonita.facade.rest.stringconverter.ActivityInstanceImplStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.ActivityInstanceStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.AttachementInstanceStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.BusinessArchiveStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.DateStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.GenericObjectStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.HashMapStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.MapStringConverter;
import org.ow2.bonita.facade.rest.stringconverter.RuleStringConverter;
import org.ow2.bonita.util.AccessorProxyUtil;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class RESTQueryAPIAccessorImpl implements QueryAPIAccessor {
	
  private static Logger LOG = Logger.getLogger(RESTQueryAPIAccessorImpl.class.getName());
	private static ResteasyProviderFactory providerFactory;
	
	public RESTQueryAPIAccessorImpl(){
		initializeProviderFactory();
	}
	
	private void initializeProviderFactory() {
		if (providerFactory == null) {
	    providerFactory = ResteasyProviderFactory.getInstance();
	    RegisterBuiltin.register(providerFactory);
	    
	    //String converters	    
	    providerFactory.addStringConverter(BusinessArchiveStringConverter.class);	    
	    providerFactory.addStringConverter(HashMapStringConverter.class);
	    providerFactory.addStringConverter(MapStringConverter.class);
	    providerFactory.addStringConverter(AttachementInstanceStringConverter.class);
	    providerFactory.addStringConverter(ActivityInstanceStringConverter.class);
	    providerFactory.addStringConverter(ActivityInstanceImplStringConverter.class);
	    providerFactory.addStringConverter(GenericObjectStringConverter.class);
	    providerFactory.addStringConverter(RuleStringConverter.class);
	    providerFactory.addStringConverter(DateStringConverter.class);
	    
	    //providers	    
	    providerFactory.registerProvider(GenericObjectProvider.class);
	    providerFactory.registerProvider(OctectStreamProvider.class);
	    
	    //interceptors
	    providerFactory.registerProvider(RESTClientExecutionInterceptor.class);
	    
	    //client errorInterceptor
      providerFactory.addClientErrorInterceptor(new BonitaClientErrorInterceptor());
    }
	}
	
	/**
	 * Get a proxy implementing the REST API
	 * @param <T> REST interface
	 * @param clazz
	 * @return
	 */
	protected <T> T getRESTAccess(final Class<T> clazz) {
	  String restServerAddress = System.getProperty(BonitaConstants.REST_SERVER_ADDRESS_PROPERTY);
	  if (restServerAddress == null && LOG.isLoggable(Level.SEVERE)) {
	    LOG.severe("The property " + BonitaConstants.REST_SERVER_ADDRESS_PROPERTY + " is null!");
	  } else if (restServerAddress != null && LOG.isLoggable(Level.FINE)) {
	    LOG.fine(BonitaConstants.REST_SERVER_ADDRESS_PROPERTY + ": " + restServerAddress);
	  }
	  return ProxyFactory.create(clazz, restServerAddress);
  }

	
	public BAMAPI getBAMAPI() {
		return getBAMAPI(AccessorUtil.QUERYLIST_DEFAULT_KEY);
	}
		

	public BAMAPI getBAMAPI(String queryList) {
		RemoteBAMAPI remoteBAMAPI = getRESTAccess(RemoteBAMAPI.class);		
		return AccessorProxyUtil.getRemoteClientAPI(BAMAPI.class,
  		  remoteBAMAPI, queryList);
	}

	public QueryDefinitionAPI getQueryDefinitionAPI() {
		return getQueryDefinitionAPI(AccessorUtil.QUERYLIST_DEFAULT_KEY);
	}

	public QueryDefinitionAPI getQueryDefinitionAPI(String queryList) {
		RemoteQueryDefinitionAPI remoteQueryRuntimeAPI = getRESTAccess(RemoteQueryDefinitionAPI.class);		
		return AccessorProxyUtil.getRemoteClientAPI(QueryDefinitionAPI.class,
  		  remoteQueryRuntimeAPI, queryList);
	}

	public QueryRuntimeAPI getQueryRuntimeAPI() {
		return getQueryRuntimeAPI(AccessorUtil.QUERYLIST_DEFAULT_KEY);
	}

	public QueryRuntimeAPI getQueryRuntimeAPI(String queryList) {
		RESTRemoteQueryRuntimeAPI remoteQueryRuntimeAPI = getRESTAccess(RESTRemoteQueryRuntimeAPI.class);		
		return AccessorProxyUtil.getRemoteClientAPI(QueryRuntimeAPI.class,
  		  remoteQueryRuntimeAPI, queryList);
	}

}
