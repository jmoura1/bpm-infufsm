/**
 * Copyright (C) 2011  BonitaSoft S.A.
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
package org.ow2.bonita.facade.rest.interceptor;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.BaseClientResponse;
import org.jboss.resteasy.client.core.ClientErrorInterceptor;
import org.ow2.bonita.util.BonitaRuntimeException;

/**
 * @author Elias Ricken de Medeiros
 *
 */
public class BonitaClientErrorInterceptor implements ClientErrorInterceptor {

  public void handle(ClientResponse<?> response) throws RuntimeException {
    try
    {
      BaseClientResponse<?> r = (BaseClientResponse<?>) response;
      InputStream stream = r.getStreamFactory().getInputStream();
      stream.reset();
      
      Object entity = response.getEntity(Object.class);
      if (entity instanceof RuntimeException) {
        RuntimeException exception = (RuntimeException) entity;
        throw exception;  
      }
      if (entity instanceof Exception) {
        Exception exception = (Exception) entity;
        throw new RESTBonitaRuntimeExceptionWrapper(exception);
      }
    
      if(Status.FORBIDDEN.equals(response.getResponseStatus().getStatusCode()))
      {
        throw new BonitaRuntimeException("Forbidden returned");
      }
    
    }
    catch (IOException e)
    {
      new BonitaRuntimeException("Error while reading client response", e);
    }
    // RESTEasy will throw the original ClientResponseFailure
  }

}
