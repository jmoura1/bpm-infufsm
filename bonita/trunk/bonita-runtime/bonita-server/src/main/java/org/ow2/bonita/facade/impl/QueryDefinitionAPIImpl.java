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
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.element.AttachmentDefinition;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.element.impl.BusinessArchiveImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ParticipantDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.def.majorElement.impl.ActivityDefinitionImpl;
import org.ow2.bonita.facade.def.majorElement.impl.DataFieldDefinitionImpl;
import org.ow2.bonita.facade.def.majorElement.impl.ParticipantDefinitionImpl;
import org.ow2.bonita.facade.def.majorElement.impl.ProcessDefinitionImpl;
import org.ow2.bonita.facade.exception.ActivityDefNotFoundException;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DataFieldNotFoundException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.ParticipantNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.paging.ProcessDefinitionCriterion;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.runtime.InitialAttachment;
import org.ow2.bonita.facade.runtime.impl.InitialAttachmentImpl;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ParticipantDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.light.impl.LightProcessDefinitionImpl;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.LargeDataRepository;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

public class QueryDefinitionAPIImpl implements QueryDefinitionAPI {

  private String queryList;

  protected QueryDefinitionAPIImpl(final String queryList) {
    this.queryList = queryList;
  }

  private String getQueryList() {
    return this.queryList;
  }

  private Set<ProcessDefinition> getProcessCopy(final Set<ProcessDefinition> src) {
    final Set<ProcessDefinition> result = new HashSet<ProcessDefinition>();
    if (src != null) {
      for (final ProcessDefinition p : src) {
        result.add(new ProcessDefinitionImpl(p));
      }
    }
    return result;
  }

  private Set<ActivityDefinition> getActivityCopy(final Set<ActivityDefinition> src) {
    final Set<ActivityDefinition> result = new HashSet<ActivityDefinition>();
    if (src != null) {
      for (final ActivityDefinition p : src) {
        result.add(new ActivityDefinitionImpl(p));
      }
    }
    return result;
  }

  private Set<ParticipantDefinition> getParticipantCopy(final Set<ParticipantDefinition> src) {
    final Set<ParticipantDefinition> result = new HashSet<ParticipantDefinition>();
    if (src != null) {
      for (final ParticipantDefinition p : src) {
        result.add(new ParticipantDefinitionImpl(p));
      }
    }
    return result;
  }

  public List<ProcessDefinition> getProcesses(int fromIndex, int pageSize)  {
    final List<ProcessDefinition> result = new ArrayList<ProcessDefinition>();
    if (pageSize <= 0) {
      return result;
    }

    List<InternalProcessDefinition> processes = getIndexedProcesses(fromIndex, pageSize, ProcessDefinitionCriterion.DEFAULT);
    for (final ProcessDefinition record : processes) {
      result.add(new ProcessDefinitionImpl(record));
    }
    return result;
  }

  public List<LightProcessDefinition> getLightProcesses(int fromIndex, int pageSize)  {
    final List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
    if (pageSize <= 0) {
      return result;
    }

    List<InternalProcessDefinition> processes = getIndexedProcesses(fromIndex, pageSize, ProcessDefinitionCriterion.DEFAULT);
    for (final ProcessDefinition record : processes) {
      result.add(new LightProcessDefinitionImpl(record));
    }
    return result;
  }

  public List<LightProcessDefinition> getLightProcesses(int fromIndex,
      int pageSize, ProcessDefinitionCriterion pagingCriterion) {
    final List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
    if (pageSize <= 0) {
      return result;
    }

    List<InternalProcessDefinition> processes = getIndexedProcesses(fromIndex, pageSize, pagingCriterion);
    for (final ProcessDefinition record : processes) {
      result.add(new LightProcessDefinitionImpl(record));
    }
    return result;
  }

  private List<InternalProcessDefinition> getIndexedProcesses(int fromIndex, int pageSize, ProcessDefinitionCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if(!visibleProcessUUIDs.isEmpty()){
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(visibleProcessUUIDs, fromIndex, pageSize, pagingCriterion);
        }
      }
    } else {      
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(fromIndex ,pageSize, pagingCriterion);
    }
    return processes;
  }

  public int getNumberOfProcesses()  {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (!access) {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfProcesses();
    } else {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        return visibleProcessUUIDs.size();
      } else {
        return 0;
      }
    }
  }

  public BusinessArchive getBusinessArchive(ProcessDefinitionUUID processDefinitionUUID) throws ProcessNotFoundException {
    Misc.checkArgsNotNull(processDefinitionUUID);
    final LargeDataRepository ldr = EnvTool.getLargeDataRepository();
    final Map<String, byte[]> resources = ldr.getData(byte[].class, Misc.getBusinessArchiveCategories(processDefinitionUUID));
    final BusinessArchive businessArchive = new BusinessArchiveImpl(processDefinitionUUID, resources);
    return businessArchive;
  }

  private Set<DataFieldDefinition> getDataFieldCopy(final Set<DataFieldDefinition> src) {
    final Set<DataFieldDefinition> result = new HashSet<DataFieldDefinition>();
    if (src != null) {
      for (final DataFieldDefinition p : src) {
        result.add(new DataFieldDefinitionImpl(p));
      }
    }
    return result;
  }

  public ProcessDefinition getLastProcess(final String processId) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processId);
    boolean access = EnvTool.isRestrictedApplicationAcces();
    ProcessDefinition last = null;
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName == null) {
        throw new ProcessNotFoundException("bai_QDAPII_2", processId);
      }
      Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);

      if (visibleProcessUUIDs.isEmpty()) {
        throw new ProcessNotFoundException("bai_QDAPII_2", processId);
      }

      Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();

      for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
        final ProcessDefinition definition = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
        if (processId.equals(definition.getName())) {
          definitionUUIDs.add(processUUID);
        }
      }
      last = EnvTool.getAllQueriers(getQueryList()).getLastDeployedProcess(definitionUUIDs, ProcessState.ENABLED);

    } else {
      last = EnvTool.getAllQueriers(getQueryList()).getLastDeployedProcess(processId, ProcessState.ENABLED);
    }
    if (last == null) {
      throw new ProcessNotFoundException("bai_QDAPII_2", processId);
    }		
    return new ProcessDefinitionImpl(last);
  }

  public LightProcessDefinition getLastLightProcess(final String processId) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processId);
    boolean access = EnvTool.isRestrictedApplicationAcces();
    ProcessDefinition last = null;
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName==null) {
        throw new ProcessNotFoundException("bai_QDAPII_2", processId);
      }
      Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);

      if (visibleProcessUUIDs.isEmpty()) {
        throw new ProcessNotFoundException("bai_QDAPII_2", processId);
      }

      Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();

      for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
        final ProcessDefinition definition = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
        if (processId.equals(definition.getName())) {
          definitionUUIDs.add(processUUID);
        }
      }
      last = EnvTool.getAllQueriers(getQueryList()).getLastDeployedProcess(definitionUUIDs, ProcessState.ENABLED);

    } else {
      last = EnvTool.getAllQueriers(getQueryList()).getLastDeployedProcess(processId, ProcessState.ENABLED);
    }
    if (last == null) {
      throw new ProcessNotFoundException("bai_QDAPII_2", processId);
    }   
    return new LightProcessDefinitionImpl(last);
  }

  private InternalProcessDefinition getInternalProcess(final ProcessDefinitionUUID processUUID) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID);
    final InternalProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_5", processUUID);
    }
    return process;
  }

  public ProcessDefinition getProcess(final ProcessDefinitionUUID processUUID) throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    return new ProcessDefinitionImpl(process);
  }

  public LightProcessDefinition getLightProcess(final ProcessDefinitionUUID processUUID) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID);
    final ProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_5", processUUID);
    }
    return new LightProcessDefinitionImpl(process);
  }

  public ProcessDefinition getProcess(final String processId, final String processVersion) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processId, processVersion);

    final ProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processId, processVersion);
    if (process == null) {
      throw new ProcessNotFoundException(processId, processVersion);
    }
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName==null) {
        throw new ProcessNotFoundException(processId, processVersion);
      }
      Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
      if (visibleProcessUUIDs == null || !visibleProcessUUIDs.contains(process.getUUID())) {
        throw new ProcessNotFoundException(processId, processVersion);
      }
    }
    return new ProcessDefinitionImpl(process);
  }

  public Set<ActivityDefinition> getProcessActivities(final ProcessDefinitionUUID processUUID) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID);
    final ProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_7", processUUID);
    }
    final Set<ActivityDefinition> activities = process.getActivities();
    return getActivityCopy(activities);
  }

  public ActivityDefinition getProcessActivity(final ProcessDefinitionUUID processUUID, 
      final String activityId)
  throws ProcessNotFoundException, ActivityNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID, activityId);
    final ProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_8", processUUID);
    }
    final Set<ActivityDefinition> activities = process.getActivities();
    if (activities != null) {
      for (final ActivityDefinition activityDefinition : activities) {
        if (activityDefinition.getName().equals(activityId)) {
          return new ActivityDefinitionImpl(activityDefinition);
        }
      }
    }
    throw new ActivityNotFoundException("bai_QDAPII_9", processUUID, activityId);
  }

  public ParticipantDefinition getProcessParticipant(final ProcessDefinitionUUID processUUID, final String participantId)
  throws ProcessNotFoundException, ParticipantNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID, participantId);
    final Set<ParticipantDefinition> participants = getProcessParticipants(processUUID);
    if (participants != null) {
      for (final ParticipantDefinition participant : participants) {
        if (participant.getName().equals(participantId)) {
          return new ParticipantDefinitionImpl(participant);
        }
      }
    }
    throw new ParticipantNotFoundException("bai_QDAPII_10", participantId, processUUID);
  }

  public Set<ParticipantDefinition> getProcessParticipants(final ProcessDefinitionUUID processUUID) throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    final Set<ParticipantDefinition> participants = process.getParticipants();
    return getParticipantCopy(participants);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<ProcessDefinition> getProcesses() {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if(visibleProcessUUIDs!=null && !visibleProcessUUIDs.isEmpty()){
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses();
    }
    return getProcessCopy((Set) processes);
  }

  public Set<LightProcessDefinition> getLightProcesses()  {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses();
    }

    Set<LightProcessDefinition> result = new HashSet<LightProcessDefinition>();
    for (final ProcessDefinition p : processes) {
      result.add(new LightProcessDefinitionImpl(p));
    }
    return result;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<ProcessDefinition> getProcesses(final String processId) {
    FacadeUtil.checkArgsNotNull(processId);
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();
          for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
            final ProcessDefinition definition = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
            if (processId.equals(definition.getName())) {
              definitionUUIDs.add(processUUID);
            }
          }
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(definitionUUIDs);
        }
      }      
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processId);
    }
    return getProcessCopy((Set) processes);
  }

  public ParticipantDefinitionUUID getProcessParticipantId(final ProcessDefinitionUUID processUUID, final String participantName)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID, participantName);
    final Set<ParticipantDefinition> participants = getProcessParticipants(processUUID);
    if (participants != null) {
      for (final ParticipantDefinition participant : participants) {
        if (participantName.equals(participant.getName())) {
          return participant.getUUID();
        }
      }
    }
    return null;
  }

  public ActivityDefinitionUUID getProcessActivityId(final ProcessDefinitionUUID processUUID, 
      final String activityName)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID, activityName);
    final Set<ActivityDefinition> activities = getProcessActivities(processUUID);
    if (activities != null) {
      for (final ActivityDefinition activity : activities) {
        if (activityName.equals(activity.getName())) {
          return activity.getUUID();
        }
      }
    }
    return null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<ProcessDefinition> getProcesses(final String processId, final ProcessDefinition.ProcessState processState) {
    FacadeUtil.checkArgsNotNull(processId, processState);
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();
          for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
            final ProcessDefinition definition = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
            if (processId.equals(definition.getName())) {
              definitionUUIDs.add(processUUID);
            }
          }
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(definitionUUIDs, processState);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processId, processState);
    }
    return getProcessCopy((Set) processes);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<ProcessDefinition> getProcesses(final ProcessDefinition.ProcessState processState) {
    FacadeUtil.checkArgsNotNull(processState);

    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();
          for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
            definitionUUIDs.add(processUUID);
          }
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(definitionUUIDs, processState);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processState);
    }

    return getProcessCopy((Set) processes);
  }

  public Set<LightProcessDefinition> getLightProcesses(ProcessState processState)  {
    FacadeUtil.checkArgsNotNull(processState);
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();
          for (ProcessDefinitionUUID processUUID : visibleProcessUUIDs) {
            definitionUUIDs.add(processUUID);
          }
          processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(definitionUUIDs, processState);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processState);
    }

    final Set<LightProcessDefinition> result = new HashSet<LightProcessDefinition>();
    for (final ProcessDefinition p : processes) {
      result.add(new LightProcessDefinitionImpl(p));
    }
    return result;
  }

  public DataFieldDefinition getProcessDataField(final ProcessDefinitionUUID processDefinitionUUID, final String dataFieldId)
  throws ProcessNotFoundException, DataFieldNotFoundException {
    FacadeUtil.checkArgsNotNull(processDefinitionUUID, dataFieldId);
    final Set<DataFieldDefinition> datafields = getProcessDataFields(processDefinitionUUID);
    if (datafields != null) {
      for (final DataFieldDefinition datefield : datafields) {
        if (datefield.getName().equals(dataFieldId)) {
          return new DataFieldDefinitionImpl(datefield);
        }
      }
    }
    throw new DataFieldNotFoundException("bai_QDAPII_14", dataFieldId, processDefinitionUUID);
  }

  public Set<DataFieldDefinition> getProcessDataFields(final ProcessDefinitionUUID processDefinitionUUID)
  throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processDefinitionUUID);
    final Set<DataFieldDefinition> datafields = process.getDataFields();
    return getDataFieldCopy(datafields);
  }

  public Set<DataFieldDefinition> getActivityDataFields(final ActivityDefinitionUUID activityDefinitionUUID) throws ActivityDefNotFoundException {
    FacadeUtil.checkArgsNotNull(activityDefinitionUUID);
    final ActivityDefinition activity = EnvTool.getAllQueriers(getQueryList()).getActivity(activityDefinitionUUID);
    if (activity == null) {
      throw new ActivityDefNotFoundException("bai_QDAPII_16", activityDefinitionUUID);
    }
    final Set<DataFieldDefinition> datafields = activity.getDataFields();
    return getDataFieldCopy(datafields);
  }

  public DataFieldDefinition getActivityDataField(final ActivityDefinitionUUID activityDefinitionUUID, final String dataFieldId)
  throws ActivityDefNotFoundException, DataFieldNotFoundException {
    FacadeUtil.checkArgsNotNull(activityDefinitionUUID);
    final ActivityDefinition activity = EnvTool.getAllQueriers(getQueryList()).getActivity(activityDefinitionUUID);
    if (activity == null) {
      throw new ActivityDefNotFoundException("bai_QDAPII_17", activityDefinitionUUID);
    }
    final Set<DataFieldDefinition> datafields = activity.getDataFields();
    if (datafields != null) {
      for (final DataFieldDefinition datefield : datafields) {
        if (datefield.getName().equals(dataFieldId)) {
          return new DataFieldDefinitionImpl(datefield);
        }
      }
    }
    throw new DataFieldNotFoundException("bai_QDAPII_18", dataFieldId, activityDefinitionUUID);
  }

  public String getProcessMetaData(ProcessDefinitionUUID uuid, String key)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(uuid, key);
    ProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(uuid);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_19", uuid);
    }
    return process.getAMetaData(key);
  }

  private InitialAttachment getProcessAttachment(DocumentationManager manager, ProcessDefinitionUUID processUUID, AttachmentDefinition attachment) {
    final SearchResult result = DocumentService.getDocuments(manager, processUUID, attachment.getName());
    final List<Document> documents = result.getDocuments();
    byte[] content = null;
    if (!documents.isEmpty()) {
      try {
        content = manager.getContent(documents.get(0));
      } catch (DocumentNotFoundException e) {
        throw new BonitaRuntimeException(e);
      }
    }
    return new InitialAttachmentImpl(attachment, content);
  }
  
  public InitialAttachment getProcessAttachment(ProcessDefinitionUUID processUUID, String attachmentName)
  throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    final AttachmentDefinition attachment = process.getAttachment(attachmentName);
    if (attachment == null) {
      return null;
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    return getProcessAttachment(manager, processUUID, attachment);
  }

  public Set<InitialAttachment> getProcessAttachments(ProcessDefinitionUUID processUUID)
  throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    final Map<String, AttachmentDefinition> processAttachments = process.getAttachments();
    final Set<InitialAttachment> attachments = new HashSet<InitialAttachment>();
    if (!processAttachments.isEmpty()) {
      final DocumentationManager manager = EnvTool.getDocumentationManager();
      for (AttachmentDefinition attachment : processAttachments.values()) {
        final InitialAttachment attach = getProcessAttachment(manager, processUUID, attachment);
        attachments.add(attach);
      }
    }
    return attachments;
  }

  public AttachmentDefinition getAttachmentDefinition(ProcessDefinitionUUID processUUID, String attachmentName)
  throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    return process.getAttachment(attachmentName);
  }

  public Set<AttachmentDefinition> getAttachmentDefinitions(ProcessDefinitionUUID processUUID)
  throws ProcessNotFoundException {
    final InternalProcessDefinition process = getInternalProcess(processUUID);
    final Map<String, AttachmentDefinition> processAttachments = process.getAttachments();
    final Set<AttachmentDefinition> attachments = new HashSet<AttachmentDefinition>();
    if (!processAttachments.isEmpty()) {
      for (Entry<String, AttachmentDefinition> processAttachment : processAttachments.entrySet()) {
        attachments.add(processAttachment.getValue());
      }
    }
    return attachments;
  }

  public byte[] getResource(ProcessDefinitionUUID definitionUUID, String resourcePath)
  throws ProcessNotFoundException {
    getInternalProcess(definitionUUID);
    ClassLoader classLoader = EnvTool.getClassDataLoader().getProcessClassLoader(definitionUUID);
    
    InputStream in = classLoader.getResourceAsStream(resourcePath);
    byte[] resource = null;
    if (in != null) {
      try {
        resource = Misc.getAllContentFrom(in);
        in.close();
      } catch (IOException e) {
      }
    }
    return resource;
  }

  public Set<LightProcessDefinition> getLightProcesses(Set<ProcessDefinitionUUID> processUUIDs)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUIDs);
    Set<InternalProcessDefinition> processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processUUIDs);
    final Set<LightProcessDefinition> result = new HashSet<LightProcessDefinition>();
    for (final ProcessDefinition p : processes) {
      result.add(new LightProcessDefinitionImpl(p));
    }
    return result;
  }

  public List<LightProcessDefinition> getLightProcesses(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    FacadeUtil.checkArgsNotNull(processUUIDs);
    if(processUUIDs.isEmpty()){
      return Collections.emptyList();
    }
    
    List<InternalProcessDefinition> processes = EnvTool.getAllQueriers(getQueryList()).getProcesses(processUUIDs, fromIndex, pageSize, pagingCriterion);
    final List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
    for (final ProcessDefinition p : processes) {
      result.add(new LightProcessDefinitionImpl(p));
    }
    return result;
  }

  public List<LightProcessDefinition> getAllLightProcessesExcept(Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize) {
    if(processUUIDs == null || processUUIDs.isEmpty()){
      return getLightProcesses(fromIndex, pageSize);
    } else {
      List<InternalProcessDefinition> processes = EnvTool.getAllQueriers(getQueryList()).getProcessesExcept(processUUIDs,fromIndex,pageSize);
      final List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
      for (final ProcessDefinition p : processes) {
        result.add(new LightProcessDefinitionImpl(p));
      }
      return result;
    }
  }

  public List<LightProcessDefinition> getAllLightProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    if(processUUIDs == null || processUUIDs.isEmpty()){
      return getLightProcesses(fromIndex, pageSize, pagingCriterion);
    } else {
      List<InternalProcessDefinition> processes = EnvTool.getAllQueriers(getQueryList()).getProcessesExcept(processUUIDs,fromIndex,pageSize, pagingCriterion);
      final List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
      for (final ProcessDefinition p : processes) {
        result.add(new LightProcessDefinitionImpl(p));
      }
      return result;
    }
  }

  public Set<ProcessDefinitionUUID> getProcessUUIDs(String category)  {
    FacadeUtil.checkArgsNotNull(category);
    if(category.trim().length()==0){
      throw new IllegalArgumentException();
    } else {
      Set<ProcessDefinitionUUID> processUUIDs = EnvTool.getAllQueriers(getQueryList()).getProcessUUIDsFromCategory(category);
      final Set<ProcessDefinitionUUID> result = new HashSet<ProcessDefinitionUUID>();
      for (final ProcessDefinitionUUID uuid : processUUIDs) {
        result.add(new ProcessDefinitionUUID(uuid));
      }
      return result;
    }
  }

  public Set<ActivityDefinitionUUID> getProcessTaskUUIDs(ProcessDefinitionUUID defintionUUID)
  throws ProcessNotFoundException {
    boolean exist = EnvTool.getAllQueriers(getQueryList()).processExists(defintionUUID);
    if (!exist) {
      throw new ProcessNotFoundException("bai_QDAPII_19", defintionUUID);
    }
    return EnvTool.getAllQueriers(getQueryList()).getProcessTaskUUIDs(defintionUUID);
  }

  @Override
  public Date getMigrationDate(ProcessDefinitionUUID processUUID)
      throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID);
    final InternalProcessDefinition process = EnvTool.getAllQueriers(getQueryList()).getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_QDAPII_5", processUUID);
    }
    return process.getMigrationDate();
  }

}
