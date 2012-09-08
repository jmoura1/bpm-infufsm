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
package org.ow2.bonita.runtime.event;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.Command;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class InstantiateProcessCommand implements Command<ProcessInstanceUUID> {

  private static final long serialVersionUID = 4095198099076719809L;
  private ProcessDefinitionUUID processUUID;
  private ActivityDefinitionUUID activityUUID;

  public InstantiateProcessCommand(ProcessDefinitionUUID processUUID,
      ActivityDefinitionUUID activityUUID) {
    super();
    this.processUUID = processUUID;
    this.activityUUID = activityUUID;
  }

  public ProcessInstanceUUID execute(Environment environment) throws Exception {
    final StandardAPIAccessorImpl accessor = new StandardAPIAccessorImpl();
    final RuntimeAPI runtimeAPI = accessor.getRuntimeAPI();
    return runtimeAPI.instantiateProcess(processUUID, activityUUID);
  }

}
