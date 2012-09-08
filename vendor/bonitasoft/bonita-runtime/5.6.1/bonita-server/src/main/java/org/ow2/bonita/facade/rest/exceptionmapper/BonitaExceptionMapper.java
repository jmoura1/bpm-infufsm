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
package org.ow2.bonita.facade.rest.exceptionmapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ApplicationException;
import org.ow2.bonita.util.BonitaConstants;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
@Provider
public class BonitaExceptionMapper implements ExceptionMapper<Exception> {

	public Response toResponse(Exception exception) {
	  Throwable result = null;
    if (exception instanceof ApplicationException) {
      result = exception.getCause();
    } else {
      result = exception;
    }
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    .header(BonitaConstants.REST_SERVER_EXCEPTION, result.getClass().getName())
    .header("Content-Type", "text/xml;charset=UTF-8")
    .entity(result)
    .build();
	}

}
