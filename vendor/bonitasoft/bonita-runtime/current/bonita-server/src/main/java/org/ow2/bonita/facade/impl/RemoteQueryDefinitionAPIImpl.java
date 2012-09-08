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
package org.ow2.bonita.facade.impl;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.def.element.AttachmentDefinition;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ParticipantDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.exception.ActivityDefNotFoundException;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DataFieldNotFoundException;
import org.ow2.bonita.facade.exception.ParticipantNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.internal.RemoteQueryDefinitionAPI;
import org.ow2.bonita.facade.paging.ProcessDefinitionCriterion;
import org.ow2.bonita.facade.runtime.InitialAttachment;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ParticipantDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.PasswordOwner;
import org.ow2.bonita.identity.auth.RESTUserOwner;
import org.ow2.bonita.identity.auth.UserOwner;
import org.ow2.bonita.light.LightProcessDefinition;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class RemoteQueryDefinitionAPIImpl implements RemoteQueryDefinitionAPI{

  private static final long serialVersionUID = 1944131311703286099L;

  protected Map<String, QueryDefinitionAPI> apis = new HashMap<String, QueryDefinitionAPI>();

  protected QueryDefinitionAPI getAPI(final Map<String, String> options) {
  	final String queryList = options.get(APIAccessor.QUERYLIST_OPTION);
  	final String user = options.get(APIAccessor.USER_OPTION);
  	final String domain = options.get(APIAccessor.DOMAIN_OPTION);
  	UserOwner.setUser(user);
  	DomainOwner.setDomain(domain);
    
    final String restUser = options.get(APIAccessor.REST_USER_OPTION);
    if (restUser != null) {
      RESTUserOwner.setUser(restUser);
      String restPswd = options.get(APIAccessor.PASSWORD_HASH_OPTION);
      PasswordOwner.setPassword(restPswd);
    }
  	
    if (!apis.containsKey(queryList)) {
      apis.put(queryList, new StandardAPIAccessorImpl().getQueryDefinitionAPI(queryList));
    }
    return apis.get(queryList);
  }

  public int getNumberOfProcesses(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfProcesses();
  }

  public ProcessDefinition getProcess(final String processId, final String version, final Map<String, String> options)
      throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcess(processId, version);
  }

  public ProcessDefinition getLastProcess(final String processId, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getLastProcess(processId);
  }

  public LightProcessDefinition getLastLightProcess(final String processId, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getLastLightProcess(processId);
  }
  
  public List<LightProcessDefinition> getLightProcesses(int fromIndex, int pageSize, final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightProcesses(fromIndex, pageSize);
  }

  public List<LightProcessDefinition> getLightProcesses(int fromIndex,
      int pageSize, ProcessDefinitionCriterion pagingCriterion,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getLightProcesses(fromIndex, pageSize, pagingCriterion);
  }

  public BusinessArchive getBusinessArchive(ProcessDefinitionUUID processDefinitionUUID, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getBusinessArchive(processDefinitionUUID);
  }

  public ProcessDefinition getProcess(final ProcessDefinitionUUID processDefinitionUUID,  final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcess(processDefinitionUUID);
  }

  public List<ProcessDefinition> getProcesses(int fromIndex, int pageSize, final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getProcesses(fromIndex, pageSize);
  }
  
  public Set<ActivityDefinition> getProcessActivities(final ProcessDefinitionUUID
      processDefinitionUUID, final Map<String, String> options) throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessActivities(processDefinitionUUID);
  }

  public ActivityDefinition getProcessActivity(
      final ProcessDefinitionUUID processDefinitionUUID, final String activityId, 
      final Map<String, String> options) throws ProcessNotFoundException, ActivityNotFoundException, RemoteException {
    return getAPI(options).getProcessActivity(processDefinitionUUID, activityId);
  }

  public ActivityDefinitionUUID getProcessActivityId(
      final ProcessDefinitionUUID processDefinitionUUID, final String activityName, 
      final Map<String, String> options) throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessActivityId(processDefinitionUUID, activityName);
  }

  public ParticipantDefinition getProcessParticipant(
      final ProcessDefinitionUUID processDefinitionUUID, final String participantId, 
      final Map<String, String> options) throws ProcessNotFoundException, ParticipantNotFoundException, RemoteException {
    return getAPI(options).getProcessParticipant(processDefinitionUUID, participantId);
  }

  public ParticipantDefinitionUUID getProcessParticipantId(
      final ProcessDefinitionUUID processDefinitionUUID, final String participantName, 
      final Map<String, String> options) throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessParticipantId(processDefinitionUUID, participantName);
  }

  public Set<ParticipantDefinition> getProcessParticipants(final ProcessDefinitionUUID processDefinitionUUID, 
      final Map<String, String> options) throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessParticipants(processDefinitionUUID);
  }

  public Set<ProcessDefinition> getProcesses(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcesses();
  }

  public Set<ProcessDefinition> getProcesses(final String processId, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcesses(processId);
  }

  public Set<ProcessDefinition> getProcesses(final ProcessDefinition.ProcessState processState, 
      final Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcesses(processState);
  }

  public Set<ProcessDefinition> getProcesses(final String processId, final ProcessDefinition.ProcessState processState, 
      final Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcesses(processId, processState);
  }

  public DataFieldDefinition getProcessDataField(final ProcessDefinitionUUID processDefinitionUUID, 
      final String dataFieldId, final Map<String, String> options)
    throws ProcessNotFoundException, DataFieldNotFoundException, RemoteException {
    return getAPI(options).getProcessDataField(processDefinitionUUID, dataFieldId);
  }

  public Set<DataFieldDefinition> getProcessDataFields(final ProcessDefinitionUUID processDefinitionUUID, final Map<String, String> options)
    throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessDataFields(processDefinitionUUID);
  }

  public Set<DataFieldDefinition> getActivityDataFields(final ActivityDefinitionUUID activityDefinitionUUID, 
      final Map<String, String> options) throws ActivityDefNotFoundException, RemoteException {
    return getAPI(options).getActivityDataFields(activityDefinitionUUID);
  }

  public DataFieldDefinition getActivityDataField(
    final ActivityDefinitionUUID activityDefinitionUUID, final String dataFieldId, final Map<String, String> options)
    throws ActivityDefNotFoundException, DataFieldNotFoundException, RemoteException {
    return getAPI(options).getActivityDataField(activityDefinitionUUID, dataFieldId);
  }

  public String getProcessMetaData(ProcessDefinitionUUID uuid, String key,
      final Map<String, String> options) throws RemoteException, ProcessNotFoundException {
    return getAPI(options).getProcessMetaData(uuid, key);
  }

	public InitialAttachment getProcessAttachment(ProcessDefinitionUUID processUUID, String attachmentName, final Map<String, String> options)
			throws ProcessNotFoundException, RemoteException {
		return getAPI(options).getProcessAttachment(processUUID, attachmentName);
	}

	public Set<InitialAttachment> getProcessAttachments(ProcessDefinitionUUID processUUID, final Map<String, String> options)
			throws ProcessNotFoundException, RemoteException {
		return getAPI(options).getProcessAttachments(processUUID);
	}

	public AttachmentDefinition getAttachmentDefinition(ProcessDefinitionUUID processUUID, String attachmentName, final Map<String, String> options)
	throws ProcessNotFoundException, RemoteException {
	  return getAPI(options).getAttachmentDefinition(processUUID, attachmentName);
	}

	public Set<AttachmentDefinition> getAttachmentDefinitions(ProcessDefinitionUUID processUUID, final Map<String, String> options)
	throws ProcessNotFoundException, RemoteException {
	  return getAPI(options).getAttachmentDefinitions(processUUID);
	}

	public Set<LightProcessDefinition> getLightProcesses(ProcessState processState, final Map<String, String> options) throws RemoteException {
	  return getAPI(options).getLightProcesses(processState);
	}

	public Set<LightProcessDefinition> getLightProcesses(final Map<String, String> options) throws RemoteException {
	  return getAPI(options).getLightProcesses();
	}

	public LightProcessDefinition getLightProcess(ProcessDefinitionUUID processDefinitionUUID, final Map<String, String> options)
	throws RemoteException, ProcessNotFoundException {
	  return getAPI(options).getLightProcess(processDefinitionUUID);
	}

  public byte[] getResource(ProcessDefinitionUUID definitionUUID, String resourcePath, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getResource(definitionUUID, resourcePath);
  }


  public Set<LightProcessDefinition> getLightProcesses(Set<ProcessDefinitionUUID> processUUIDs, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getLightProcesses(processUUIDs);
  }

  public List<LightProcessDefinition> getLightProcesses(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion, Map<String, String> options)
      throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getLightProcesses(processUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessDefinition> getAllLightProcessesExcept(Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize, final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getAllLightProcessesExcept(processUUIDs,fromIndex, pageSize);
  }

  public List<LightProcessDefinition> getAllLightProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getAllLightProcessesExcept(processUUIDs,fromIndex, pageSize, pagingCriterion);
  }

  public Set<ProcessDefinitionUUID> getProcessUUIDs(String category, final Map<String, String> options)
  throws RemoteException {
	 return getAPI(options).getProcessUUIDs(category);
  }

  public Set<ActivityDefinitionUUID> getProcessTaskUUIDs(ProcessDefinitionUUID processsUUID, Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessTaskUUIDs(processsUUID);
  }

  public Date getMigrationDate(final ProcessDefinitionUUID processUUID,
      final Map<String, String> options) throws ProcessNotFoundException,
      RemoteException {
    return getAPI(options).getMigrationDate(processUUID);
  }

}
