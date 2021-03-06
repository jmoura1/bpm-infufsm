/**
 * Copyright (C) 2009  BonitaSoft S.A.
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
package org.ow2.bonita.facade.runtime.command;

import java.util.Collection;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.CommandAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.util.Command;

/**
 * 
 * @author Nicolas Chabanoles
 *
 */
public class WebDeleteProcessesCommand implements Command<Void> {

  private static final long serialVersionUID = -9006642921795672291L;
  private Collection<ProcessDefinitionUUID> processUUIDs;
  
  public WebDeleteProcessesCommand(Collection<ProcessDefinitionUUID> processUUIDs) {
    super();
    this.processUUIDs = processUUIDs;
  }

  public Void execute(Environment environment) throws Exception {
    final APIAccessor accessor = new StandardAPIAccessorImpl();
    final CommandAPI api = accessor.getCommandAPI();
    for (ProcessDefinitionUUID processUUID : processUUIDs) {
      api.execute(new WebDeleteProcessCommand(processUUID));
    }
    return null;
  }
  
}
