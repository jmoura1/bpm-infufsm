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
package org.ow2.bonita.facade.rest.stringconverter;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.StringConverter;
import org.ow2.bonita.util.xml.XStreamUtil;

import com.thoughtworks.xstream.XStream;

/**
 * @author Elias Ricken de Medeiros
 *
 */
@Provider
public class GenericObjectStringConverter implements StringConverter<Object> {

	public Object fromString(String str) {
		Object objectInstance = null;
		if (str.startsWith("<")) {			
			try {
				XStream xstream = XStreamUtil.getDefaultXstream();
				objectInstance = xstream.fromXML(str);
			} catch (Exception e) {			
		    objectInstance = getNotSerializedObject(str);	
			}
    } else {
    	objectInstance = getNotSerializedObject(str);
    }
		return objectInstance;
  }

	public String toString(Object value) {
		XStream xstream = XStreamUtil.getDefaultXstream();
		return xstream.toXML(value);
  }
	
	private Object getNotSerializedObject(String str) {		
		if(str.equalsIgnoreCase("true")){
			return true;
		}
		if (str.equalsIgnoreCase("false")){
			return false;
		}
	  return str;
  }

}
