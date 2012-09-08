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
package org.ow2.bonita.facade.rest.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.util.Misc;

/**
 * @author Elias Ricken de Medeiros
 *
 */
@Provider
@Consumes("application/octet-stream")
@Produces("application/octet-stream")
public class OctectStreamProvider  implements MessageBodyReader<BusinessArchive>,
MessageBodyWriter<BusinessArchive>{

  private static Logger LOG = Logger.getLogger(OctectStreamProvider.class.getName());
  static final int BUFF_SIZE = 100000;
  static final byte[] buffer = new byte[BUFF_SIZE];
  
  private static void readFromInputStream (InputStream in, OutputStream out) throws IOException{
    while (true) {
      synchronized (buffer) {
        int amountRead = in.read(buffer);
        if (amountRead == -1) {
          break;
        }
        out.write(buffer, 0, amountRead);
      }
    }
  }
  
  public boolean isReadable(Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return isReadableOrWritable(genericType);
  }

  private boolean isReadableOrWritable(Type genericType) {
    return genericType.equals(BusinessArchive.class);
  }

  public BusinessArchive readFrom(Class<BusinessArchive> type,
      Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
      throws IOException, WebApplicationException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Serializable serializable = null;
    try {
      readFromInputStream(entityStream, out);
      serializable = Misc.deserialize(out.toByteArray());
    } catch (ClassNotFoundException e) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.severe("Error while reading the InputStream: " + Misc.getStackTraceFrom(e));
      }
    } catch (IOException e) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.severe("Error while reading the InputStream: " + Misc.getStackTraceFrom(e));
      }
    } finally {
      if (entityStream != null) {
        entityStream.close();
      }
      if (out != null) {
        out.close();
      }
    }
    
    BusinessArchive businessArchive = null;
    if (serializable != null && serializable instanceof BusinessArchive) {
      businessArchive = (BusinessArchive) serializable;
    }
    return businessArchive;
  }

  public long getSize(BusinessArchive t, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  public boolean isWriteable(Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return isReadableOrWritable(genericType);
  }

  public void writeTo(BusinessArchive t, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
      throws IOException, WebApplicationException {
    try {
      entityStream.write(Misc.serialize(t));
    } catch (ClassNotFoundException e) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.severe("Error while serizing the object: " + Misc.getStackTraceFrom(e));
      }
      if (entityStream != null) {
        entityStream.close();
      }
    } catch (IOException e) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.severe("Error while serizing the object: " + Misc.getStackTraceFrom(e));
      } 
      if (entityStream != null) {
        entityStream.close();
      }
    } 
  }

}
