/**
 * Copyright (C) 2006  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
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
package org.ow2.bonita.facade.exception;

import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.BonitaException;
import org.ow2.bonita.util.ExceptionManager;

/**
 * Thrown by deleteProcessrocess method of the ManagementAPI if processes within the package have
 * still running instances.
 */
public class UndeletableProcessException extends BonitaException {

  /**
   * 
   */
  private static final long serialVersionUID = -8094983559958618304L;
  private ProcessDefinitionUUID processDefinitionUUID;
  private ProcessInstanceUUID processInstanceUUID;

  public UndeletableProcessException(String id,
      ProcessDefinitionUUID processDefinitionUUID,
      ProcessInstanceUUID processInstanceUUID) {
	  super(ExceptionManager.getInstance().getIdMessage(id)
	  			+ ExceptionManager.getInstance().getMessage("UPE1", processDefinitionUUID, processInstanceUUID));
    this.processDefinitionUUID = processDefinitionUUID;
    this.processInstanceUUID = processInstanceUUID;
  }

  public ProcessDefinitionUUID getProcessDefinitionUUID() {
    return processDefinitionUUID;
  }


  public ProcessInstanceUUID getProcessInstanceUUID() {
    return processInstanceUUID;
  }


}


