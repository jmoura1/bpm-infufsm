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
package org.ow2.bonita.facade.rest.wrapper;

import java.io.IOException;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.xml.XStreamUtil;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 * @param <T>
 */
public class RESTCommand<T> implements Command<T> {
	private static final long serialVersionUID = 8801886980381585171L;
	
	private byte [] serializedCommand;
	
	public RESTCommand(Command<T> command) throws IOException, ClassNotFoundException{
		this.serializedCommand = Misc.serialize(command);
	}

	public T execute(Environment environment) throws Exception {	  
	  return getCommand().execute(environment);
  }
	
	@SuppressWarnings("unchecked")
	public Command<T> getCommand() throws IOException, ClassNotFoundException{
		return (Command<T>) Misc.deserialize(this.serializedCommand);
	}
	
	@Override
	public String toString() {
		XStream xstream = XStreamUtil.getDefaultXstream();
	  return xstream.toXML(this);
	}
	
}
