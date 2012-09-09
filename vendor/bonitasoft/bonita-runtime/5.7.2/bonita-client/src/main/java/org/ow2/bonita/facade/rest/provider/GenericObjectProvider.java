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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

import org.ow2.bonita.facade.CommandAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.rest.wrapper.RESTObject;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.xml.XStreamUtil;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Elias Ricken de Medeiros, Matthieu Chaffotte
 * 
 */
@Provider
@Consumes("*/*")
@Produces("*/*")
public class GenericObjectProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

	private static Logger LOG = Logger.getLogger(GenericObjectProvider.class.getName());

	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
	  if (mediaType.getType().equalsIgnoreCase("application/octet-stream")){
      return false;
    }
		return isReadableOrWriteable(genericType);
	}

	private boolean isReadableOrWriteable(Type genericType) {
		boolean isPrimitive = false;
		boolean isReadableorWriteable = true;
		if (genericType != null) {
  		if (genericType instanceof Class<?>) {
  			isPrimitive = ((Class<?>) genericType).isPrimitive();
  		}
  
  		boolean mustHaveUserProvider = !isPrimitive
  				&& !genericType.equals(String.class)
  				&& !genericType.equals(Boolean.class)
  				&& !genericType.equals(BusinessArchive.class);
  
  		isReadableorWriteable = genericType.equals(Object.class)
  				|| genericType.equals(CommandAPI.class) || mustHaveUserProvider;
		}

		return isReadableorWriteable;
	}

	public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
	    MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
	throws IOException, WebApplicationException {
	  Object ret = null;
	  String content = getContent(entityStream);
	  if (content.startsWith("<")) {
	    XStream xstream = XStreamUtil.getDefaultXstream();
	    try {
	      ret = xstream.fromXML(content);
	    } catch (Exception e) {
	    	if (LOG.isLoggable(Level.SEVERE)) {
          LOG.severe("Error while decoding " + content + ": " + Misc.getStackTraceFrom(e));
        }
	      ret = content;
	    }
	  } else {
	  	if (LOG.isLoggable(Level.WARNING)) {
        LOG.warning("The string does not begin with \"<\". It will be considered as a simple String:" + content);
      }
	    ret = content;
	  }
	  try {
	    // verify if returned object was wrapped in a RESTObject
	    if (ret instanceof RESTObject) {
	      RESTObject restObject = (RESTObject) ret;
	      ret = restObject.getObject();
	    }
	  } catch (ClassNotFoundException e) {
	    if (LOG.isLoggable(Level.SEVERE)) {
	      LOG.severe(e.getStackTrace().toString());
	    }
	  }
	  return ret;
	}

	private String getContent(final InputStream entityStream) throws IOException {
	  BufferedReader reader = null;
	  StringBuilder builder = new StringBuilder();
	  try {
	    reader = new BufferedReader(new InputStreamReader(entityStream, "UTF-8"));
	    String line = reader.readLine();
	    while (line != null) {
	      builder.append(line).append("\n");
	      line = reader.readLine();
	    }
	  } catch (Exception e) {
	  	if (LOG.isLoggable(Level.SEVERE)) {
	      LOG.severe("Error while reading the InputStream: " + Misc.getStackTraceFrom(e));
	    }
	  } finally {
	    if (reader != null) {
	      reader.close();
	    }
	  }
	  String result = builder.toString();
	  return result.trim();
	}

	public long getSize(Object t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
	  if (mediaType.getType().equalsIgnoreCase("application/octet-stream")){
      return false;
    }
		return isReadableOrWriteable(genericType);
	}

	public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
	throws IOException, WebApplicationException {
		XStream xstream = XStreamUtil.getDefaultXstream();
		Writer writer = null;
		try {
		  writer = new OutputStreamWriter(entityStream, "UTF-8");
	    xstream.toXML(t, writer);
		} catch (Exception e){
			if (LOG.isLoggable(Level.SEVERE)) {
	      LOG.severe("Error while writing in the OutputStream: " + Misc.getStackTraceFrom(e));
	    }
		} finally {
		  if (writer != null) {
		    writer.close();
		  }
		}
	}

}
