/**
 * Copyright (C) 2007  Bull S. A. S.
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
 * Modified by Charles Souillard, Matthieu Chaffotte, Nicolas Chabanoles, Elias Ricken de Medeiros
 *  - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.EventNotFoundException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.TaskNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.identity.impl.GroupImpl;
import org.ow2.bonita.facade.identity.impl.RoleImpl;
import org.ow2.bonita.facade.identity.impl.UserImpl;
import org.ow2.bonita.facade.paging.ActivityInstanceCriterion;
import org.ow2.bonita.facade.paging.ProcessInstanceCriterion;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.CatchingEvent;
import org.ow2.bonita.facade.runtime.CatchingEvent.Position;
import org.ow2.bonita.facade.runtime.CatchingEvent.Type;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.VariableUpdate;
import org.ow2.bonita.facade.runtime.impl.ActivityInstanceImpl;
import org.ow2.bonita.facade.runtime.impl.AttachmentInstanceImpl;
import org.ow2.bonita.facade.runtime.impl.CaseImpl;
import org.ow2.bonita.facade.runtime.impl.CatchingEventImpl;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.runtime.impl.ProcessInstanceImpl;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CatchingEventUUID;
import org.ow2.bonita.facade.uuid.DocumentUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.light.impl.LightActivityInstanceImpl;
import org.ow2.bonita.light.impl.LightProcessDefinitionImpl;
import org.ow2.bonita.light.impl.LightProcessInstanceImpl;
import org.ow2.bonita.runtime.event.EventConstants;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.search.DocumentResult;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.SearchQueryBuilder;
import org.ow2.bonita.search.index.Index;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.CopyTool;
import org.ow2.bonita.util.DateUtil;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.TransientData;
import org.xml.sax.SAXException;

/**
 * @author Pierre Vigneras
 */
public class QueryRuntimeAPIImpl implements QueryRuntimeAPI {

  private String queryList;

  protected QueryRuntimeAPIImpl(final String queryList) {
    this.queryList = queryList;
  }

  private String getQueryList() {
    return this.queryList;
  }

  public int getNumberOfParentProcessInstances() {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    int count = 0;
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
      if (visibleProcessUUIDs != null) {
        count = EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstances(visibleProcessUUIDs);
      }
    } else {
      count = EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstances();
    }
    return count;
  }

  public int getNumberOfProcessInstances() {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    int count = 0;
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
      if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
        count = EnvTool.getAllQueriers(getQueryList()).getNumberOfProcessInstances(visibleProcessUUIDs);
      }
    } else {
      count = EnvTool.getAllQueriers(getQueryList()).getNumberOfProcessInstances();
    }
    return count;
  }

  public ProcessInstance getProcessInstance(final ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    final InternalProcessInstance instance = getInternalProcessInstanceWithAttachments(instanceUUID);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_QRAPII_1", instanceUUID);
    }
    return new ProcessInstanceImpl(instance);
  }

  public LightProcessInstance getLightProcessInstance(ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    final ProcessInstance result = getInternalProcessInstanceWithoutAttachements(instanceUUID);
    if (result == null) {
      throw new InstanceNotFoundException("bai_QRAPII_1", instanceUUID);
    }
    return new LightProcessInstanceImpl(result);
  }  

  private InternalProcessInstance getInternalProcessInstanceWithAttachments(final ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    final InternalProcessInstance internalProcessInstance = getInternalProcessInstanceWithoutAttachements(instanceUUID);
    if (internalProcessInstance != null && internalProcessInstance.getNbOfAttachments() > 0) {
      bindAttachementsToInternalProcessInstance(internalProcessInstance);
    }
    return internalProcessInstance;
  }

  private void bindAttachementsToInternalProcessInstance(final InternalProcessInstance internalProcessInstance) {
    if (internalProcessInstance != null) {
      final int nbOfAttachments = internalProcessInstance.getNbOfAttachments();
      if (nbOfAttachments > 0) {
        final DocumentationManager manager = EnvTool.getDocumentationManager();
        final List<AttachmentInstance> allAttachmentVersions = DocumentService.getAllAttachmentVersions(manager, internalProcessInstance.getProcessInstanceUUID());
        for (AttachmentInstance attachmentInstance : allAttachmentVersions) {
          internalProcessInstance.addAttachment(attachmentInstance);
        }
      }
    }
  }

  private InternalProcessInstance getInternalProcessInstanceWithoutAttachements(
      final ProcessInstanceUUID instanceUUID) {
    FacadeUtil.checkArgsNotNull(instanceUUID);
    Querier querier = EnvTool.getAllQueriers(getQueryList());
    final InternalProcessInstance result = querier.getProcessInstance(instanceUUID);
    return result;
  }

  public Set<ProcessInstance> getProcessInstances() {
    Set<InternalProcessInstance> processes = getInternalProcessInstancesWithAttachements();
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<LightProcessInstance> getLightProcessInstances() {
    Set<InternalProcessInstance> processes = getInternalProcessInstancesWithoutAttachements();
    final Set<LightProcessInstance> result = new HashSet<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  private Set<InternalProcessInstance> getInternalProcessInstancesWithoutAttachements() {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessInstance> processes = new HashSet<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances(visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances();
    }
    return processes;
  }
  
  private Set<InternalProcessInstance> getInternalProcessInstancesWithAttachements() {
    final Set<InternalProcessInstance> processes = getInternalProcessInstancesWithoutAttachements();
    bindAttachementsToInternalProcessInstances(processes);
    return processes;
  }

  private void bindAttachementsToInternalProcessInstances(final Set<InternalProcessInstance> processes) {
    for (final InternalProcessInstance internalProcessInstance : processes) {
      bindAttachementsToInternalProcessInstance(internalProcessInstance);
    }
  }

  public List<LightProcessInstance> getLightProcessInstances(int fromIndex, int pageSize) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances(visibleProcessUUIDs, fromIndex, pageSize);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances(fromIndex, pageSize);
    }

    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightProcessInstances(int fromIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances(visibleProcessUUIDs, fromIndex, pageSize, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstances(fromIndex, pageSize, pagingCriterion);
    }

    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstances(int fromIndex, int pageSize) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    List<InternalProcessInstance> records = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstances(fromIndex ,pageSize);
    for (final ProcessInstance record : records) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstances(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Misc.checkArgsNotNull(processUUIDs);
    if (processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    List<InternalProcessInstance> processes = getParentProcessInstances(processUUIDs, fromIndex, pageSize, pagingCriterion);    

    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;      
  }

  private List<InternalProcessInstance> getParentProcessInstances(Set<ProcessDefinitionUUID> processUUIDs, int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null || visibleProcessUUIDs.isEmpty()) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstances(visibleProcessUUIDs, 
              startingIndex, pageSize, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstances(processUUIDs,  
          startingIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesExcept(
      Set<ProcessDefinitionUUID> exceptions, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {    
    if (exceptions == null || exceptions.isEmpty()) {
      return getLightParentProcessInstances(fromIndex, pageSize, pagingCriterion);
    }

    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null || visibleProcessUUIDs.isEmpty()) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.removeAll(exceptions);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstances(visibleProcessUUIDs, 
              fromIndex, pageSize, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesExcept(exceptions,  
          fromIndex, pageSize, pagingCriterion);
    }

    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;  
  }

  public List<LightProcessInstance> getLightParentProcessInstances(
      int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    List<InternalProcessInstance> records = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstances(fromIndex ,pageSize, pagingCriterion);
    for (final ProcessInstance record : records) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<ProcessInstance> getUserInstances() {
    final Set<InternalProcessInstance> processes = getUserProcessInstances();
    //bind attachements from document service
    bindAttachementsToInternalProcessInstances(processes);
    
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<LightProcessInstance> getLightUserInstances() {
    Set<InternalProcessInstance> processes = getUserProcessInstances();
    final Set<LightProcessInstance> result = new HashSet<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentUserInstances(int startingIndex, int pageSize) {
    List<InternalProcessInstance> processes = getUserParentProcessInstances(startingIndex, pageSize);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = getUserParentProcessInstances(fromIndex, pageSize, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,remainingDays, fromIndex, pageSize);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String userId, int remainingDays, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
        userId,remainingDays, fromIndex, pageSize, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithOverdueTasks(userId, 
        fromIndex, pageSize, ProcessInstanceCriterion.DEFAULT);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithOverdueTasks(userId, 
        fromIndex, pageSize, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, int fromIndex, 
      int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithOverdueTasks(
              userId, currentDate, fromIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithOverdueTasks(userId, 
          currentDate, fromIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, 
      int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithOverdueTasks(userId,
              currentDate, fromIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithOverdueTasks(userId, 
          currentDate, fromIndex, pageSize,processUUIDs, pagingCriterion);
    }
    return processes;
  }


  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId,int remainingDays, int startingIndex, int pageSize) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, startingIndex, pageSize);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String userId,int remainingDays, int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
              userId,currentDate, atRisk, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
          userId,currentDate, atRisk, startingIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId,int remainingDays, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, startingIndex, pageSize, processUUIDs);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId,
      int remainingDays, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
              userId,currentDate, atRisk, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
          userId,currentDate, atRisk, startingIndex, pageSize, processUUIDs, pagingCriterion);
    }
    return processes;
  }


  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithInvolvedUser(
        userId, fromIndex, pageSize, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    if(processUUIDs== null || processUUIDs.isEmpty()){
      return Collections.emptyList();
    }
    List<InternalProcessInstance> internalProcesses = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize, processUUIDs);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : internalProcesses) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(
      String userId, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    if(processUUIDs== null || processUUIDs.isEmpty()){
      return Collections.emptyList();
    }
    List<InternalProcessInstance> internalProcesses = EnvTool.getAllQueriers(getQueryList())
    .getParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize, processUUIDs, pagingCriterion);

    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : internalProcesses) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUser(userId, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUser(userId);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Date currentDate = new Date();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithOverdueTasks(userId,currentDate, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithOverdueTasks(userId,currentDate);
    }
  }	

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(userId, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(userId);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(String userId, String category) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<ProcessDefinitionUUID> targetedProcesses = EnvTool.getAllQueriers(getQueryList()).getProcessUUIDsFromCategory(category);
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        final Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        visibleProcessUUIDs.retainAll(targetedProcesses);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(userId, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(userId, targetedProcesses);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithStartedBy(userId, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithStartedBy(userId);
    }
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int startingIndex, int pageSize) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, startingIndex, pageSize);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
      String userId, int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(
              userId, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(
          userId, startingIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null || visibleProcessUUIDs.isEmpty()) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, startingIndex, pageSize, processUUIDs);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, 
      int startingIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null || visibleProcessUUIDs.isEmpty()) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, 
              startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithActiveUser(userId, 
          startingIndex, pageSize, processUUIDs, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int startingIndex, int pageSize) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithInvolvedUser(userId, startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithInvolvedUser(userId, startingIndex, pageSize);
    }
    return processes;
  }

  private List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, 
      int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithInvolvedUser(
              userId, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentProcessInstancesWithInvolvedUser(
          userId, startingIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getUserParentProcessInstances(int startingIndex, int pageSize) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), startingIndex, pageSize);
    }
    return processes;
  }

  private List<InternalProcessInstance> getUserParentProcessInstances(int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), 
              startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), 
          startingIndex, pageSize, pagingCriterion);
    }
    return processes;
  }

  private List<InternalProcessInstance> getUserParentProcessInstances(int startingIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), startingIndex, pageSize, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), startingIndex, pageSize, processUUIDs);
    }
    return processes;
  }

  private List<InternalProcessInstance> getUserParentProcessInstances(int startingIndex, int pageSize, 
      Set<ProcessDefinitionUUID> processUUIDs, ProcessInstanceCriterion pagingCriterion) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    List<InternalProcessInstance> processes = new ArrayList<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), 
              startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getParentUserInstances(EnvTool.getUserId(), 
          startingIndex, pageSize, processUUIDs, pagingCriterion);
    }
    return processes;
  }

  private Set<InternalProcessInstance> getUserProcessInstances() {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessInstance> processes = new HashSet<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getUserInstances(EnvTool.getUserId(), visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getUserInstances(EnvTool.getUserId());
    }
    return processes;
  }

  public Set<ProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs) {
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    if (instanceUUIDs == null || instanceUUIDs.isEmpty()) {
      return result;
    }
    for (final InternalProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(instanceUUIDs)) {
      bindAttachementsToInternalProcessInstance(record);
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<LightProcessInstance> getLightProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs) {
    final Set<LightProcessInstance> result = new HashSet<LightProcessInstance>();
    if (instanceUUIDs == null || instanceUUIDs.isEmpty()) {
      return result;
    }
    for (final ProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(instanceUUIDs)) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    if (instanceUUIDs == null || instanceUUIDs.isEmpty()) {
      return result;
    }
    for (final ProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(instanceUUIDs, fromIndex, pageSize)) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightProcessInstances(
      Set<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    if (pageSize <= 0) {
      return result;
    }
    if (instanceUUIDs == null || instanceUUIDs.isEmpty()) {
      return result;
    }
    for (final ProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstancesWithInstanceUUIDs(
        instanceUUIDs, fromIndex, pageSize, pagingCriterion)) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<ProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates) {
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    if (activityStates == null || activityStates.isEmpty()) {
      return result;
    }

    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessInstance> processes = new HashSet<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstancesWithTaskState(activityStates, visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstancesWithTaskState(activityStates);
    }
    for (final InternalProcessInstance record : processes) {
      bindAttachementsToInternalProcessInstance(record);
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<ProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates) {
    Misc.checkArgsNotNull(instanceStates);
    if (instanceStates.isEmpty()) {
      throw new IllegalArgumentException(ExceptionManager.getInstance().getMessage("bai_QRAPII_15"));
    }
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<InternalProcessInstance> processes = new HashSet<InternalProcessInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName,
            RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstancesWithInstanceStates(instanceStates,
              visibleProcessUUIDs);
        }
      }
    } else {
      processes = EnvTool.getAllQueriers(getQueryList()).getProcessInstancesWithInstanceStates(instanceStates);
    }
    for (final InternalProcessInstance record : processes) {
      bindAttachementsToInternalProcessInstance(record);
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<ProcessInstance> getProcessInstances(final ProcessDefinitionUUID processUUID) {
    final Set<ProcessInstance> result = new HashSet<ProcessInstance>();
    for (final InternalProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(processUUID)) {
      bindAttachementsToInternalProcessInstance(record);
      result.add(new ProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<LightProcessInstance> getLightProcessInstances(ProcessDefinitionUUID processUUID) {
    final Set<LightProcessInstance> result = new HashSet<LightProcessInstance>();
    for (final ProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(processUUID)) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public Set<LightProcessInstance> getLightWeightProcessInstances(Set<ProcessDefinitionUUID> processUUIDs) {
    final Set<LightProcessInstance> result = new HashSet<LightProcessInstance>();
    for (final ProcessInstance record : EnvTool.getAllQueriers(getQueryList()).getProcessInstances(processUUIDs)) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public ActivityInstance getActivityInstance(final ProcessInstanceUUID instanceUUID, final String activityId, final String iterationId, final String activityInstanceId, final String loopId)
  throws ActivityNotFoundException, InstanceNotFoundException {
    final ActivityInstance result =
      EnvTool.getAllQueriers(getQueryList()).getActivityInstance(instanceUUID, activityId, iterationId, activityInstanceId, loopId);
    if (result == null) {
      if (EnvTool.getAllQueriers(getQueryList()).getProcessInstance(instanceUUID) == null) {
        throw new InstanceNotFoundException("bai_QRAPII_2", instanceUUID);
      }
      throw new ActivityNotFoundException("bai_QRAPII_3", instanceUUID, activityId, iterationId);
    }
    return new ActivityInstanceImpl(result);
  }

  public boolean canExecuteTask(ActivityInstanceUUID taskUUID)
  throws TaskNotFoundException {
    TaskInstance task = getTask(taskUUID);
    if (task == null) {
      return false;
    }
    if (!task.getState().equals(ActivityState.READY)) {
      return false;
    }
    String userId = EnvTool.getUserId();
    if (task.isTaskAssigned()) {
      return task.getTaskUser().equals(userId);
    }
    return task.getTaskCandidates().contains(userId);
  }

  public Set<ActivityInstance> getActivityInstances(final ProcessInstanceUUID instanceUUID) {
    final Set<ActivityInstance> result = new HashSet<ActivityInstance>();
    for (final ActivityInstance record : EnvTool.getAllQueriers(getQueryList()).getActivityInstances(instanceUUID)) {
      result.add(new ActivityInstanceImpl(record));
    }
    return result;
  }

  public List<LightActivityInstance> getLightActivityInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID) {
    final List<LightActivityInstance> result = new ArrayList<LightActivityInstance>();
    List<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstancesFromRoot(rootInstanceUUID);
    for (final InternalActivityInstance record : activities) {
      result.add(new LightActivityInstanceImpl(record));
    }
    return result;
  }

  public Map<ProcessInstanceUUID, List<LightActivityInstance>> getLightActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, ActivityState state) {
    Map<ProcessInstanceUUID, List<LightActivityInstance>> result = new HashMap<ProcessInstanceUUID, List<LightActivityInstance>>();
    List<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstancesFromRoot(rootInstanceUUIDs, state);
    for (InternalActivityInstance activity : activities) {
      final ProcessInstanceUUID instanceUUID = activity.getRootInstanceUUID();
      if (!result.containsKey(instanceUUID)) {
        result.put(instanceUUID, new ArrayList<LightActivityInstance>());
      }
      result.get(instanceUUID).add(new LightActivityInstanceImpl(activity));
    }
    return result;
  }

  public Map<ProcessInstanceUUID, List<LightActivityInstance>> getLightActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs) {
    Map<ProcessInstanceUUID, List<LightActivityInstance>> result = new HashMap<ProcessInstanceUUID, List<LightActivityInstance>>();
    List<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstancesFromRoot(rootInstanceUUIDs);
    for (InternalActivityInstance activity : activities) {
      final ProcessInstanceUUID instanceUUID = activity.getRootInstanceUUID();
      if (!result.containsKey(instanceUUID)) {
        result.put(instanceUUID, new ArrayList<LightActivityInstance>());
      }
      result.get(instanceUUID).add(new LightActivityInstanceImpl(activity));
    }
    return result;
  }

  public Map<ProcessInstanceUUID, LightActivityInstance> getLightLastUpdatedActivityInstanceFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, boolean considerSystemTaks) {
    Map<ProcessInstanceUUID, LightActivityInstance> result = new HashMap<ProcessInstanceUUID, LightActivityInstance>();
    Map<ProcessInstanceUUID, InternalActivityInstance> temp = EnvTool.getAllQueriers(getQueryList()).getLastUpdatedActivityInstanceFromRoot(rootInstanceUUIDs, considerSystemTaks);
    for (Map.Entry<ProcessInstanceUUID, InternalActivityInstance> entry : temp.entrySet()) {
      result.put(entry.getKey(), new LightActivityInstanceImpl(entry.getValue()));
    }
    return result;
  }

  public List<LightTaskInstance> getLightTaskInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID) {
    final List<LightTaskInstance> result = new ArrayList<LightTaskInstance>();
    for (final InternalActivityInstance record : EnvTool.getAllQueriers(getQueryList()).getActivityInstancesFromRoot(rootInstanceUUID)) {
      if (record.isTask()) {
        result.add(new LightActivityInstanceImpl(record));
      }
    }
    return result;
  }

  public Map<ProcessInstanceUUID, List<LightTaskInstance>> getLightTaskInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs) {
    Map<ProcessInstanceUUID, List<LightTaskInstance>> result = new HashMap<ProcessInstanceUUID, List<LightTaskInstance>>();
    for (ProcessInstanceUUID instanceUUID : rootInstanceUUIDs) {
      result.put(instanceUUID, getLightTaskInstancesFromRoot(instanceUUID));
    }
    return result;
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    final Set<LightActivityInstance> result = new HashSet<LightActivityInstance>();
    final Set<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstances(instanceUUID); 
    for (final ActivityInstance record : activities) {
      result.add(new LightActivityInstanceImpl(record));
    }
    return result;
  }

  public List<LightActivityInstance> getLightActivityInstances(
      ProcessInstanceUUID instanceUUID, int fromIdex, int pageSize,
      ActivityInstanceCriterion pagingCriterion)
      throws InstanceNotFoundException {
    final List<LightActivityInstance> result = new ArrayList<LightActivityInstance>();
    final List<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList())
    .getActivityInstances(instanceUUID, fromIdex, pageSize, pagingCriterion); 
    for (final ActivityInstance record : activities) {
      result.add(new LightActivityInstanceImpl(record));
    }
    return result;
  }

  public Set<ActivityInstance> getActivityInstances(final ProcessInstanceUUID instanceUUID, final String activityId)
  throws ActivityNotFoundException {
    final Set<ActivityInstance> result = new HashSet<ActivityInstance>();
    for (final ActivityInstance record 
        : EnvTool.getAllQueriers(getQueryList()).getActivityInstances(instanceUUID)) {
      if (record.getActivityName().equals(activityId)) {
        result.add(new ActivityInstanceImpl(record));
      }
    }
    if (result.isEmpty()) {
      throw new ActivityNotFoundException("bai_QRAPII_4", instanceUUID, activityId);
    }
    return result;
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, String activityName)
  throws InstanceNotFoundException, ActivityNotFoundException {
    final Set<LightActivityInstance> result = new HashSet<LightActivityInstance>();
    Set<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstances(instanceUUID);
    for (final InternalActivityInstance record : activities) {
      if (record.getActivityName().equals(activityName)) {
        result.add(new LightActivityInstanceImpl(record));
      }
    }
    if (result.isEmpty()) {
      throw new ActivityNotFoundException("bai_QRAPII_4", instanceUUID, activityName);
    }
    return result;
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, String activityName, String iterationId)
  {
    final Set<LightActivityInstance> result = new HashSet<LightActivityInstance>();
    final Set<InternalActivityInstance> activities = EnvTool.getAllQueriers(getQueryList()).getActivityInstances(instanceUUID, activityName, iterationId);
    for (final ActivityInstance record : activities) {
      result.add(new LightActivityInstanceImpl(record));
    }
    return result;
  }

  public Set<TaskInstance> getTasks(final ProcessInstanceUUID instanceUUID) {
    final Set<TaskInstance> result = new HashSet<TaskInstance>();
    for (final TaskInstance record : EnvTool.getAllQueriers(getQueryList()).getTaskInstances(instanceUUID)) {
      result.add(new ActivityInstanceImpl(record));
    }
    return result;
  }

  public Set<LightTaskInstance> getLightTasks(ProcessInstanceUUID instanceUUID) throws InstanceNotFoundException {
    final Set<LightTaskInstance> result = new HashSet<LightTaskInstance>();
    for (final TaskInstance record : EnvTool.getAllQueriers(getQueryList()).getTaskInstances(instanceUUID)) {
      result.add(new LightActivityInstanceImpl(record));
    }
    return result;
  }

  public Set<LightTaskInstance> getLightTasks(ProcessInstanceUUID instanceUUID, Set<String> taskNames) {
    final Set<LightTaskInstance> result = new HashSet<LightTaskInstance>();
    if (taskNames != null && !taskNames.isEmpty()) {
      for (final TaskInstance record : EnvTool.getAllQueriers(getQueryList()).getTaskInstances(instanceUUID, taskNames)) {
        result.add(new LightActivityInstanceImpl(record));
      }
    }
    return result;
  }

  public Collection<TaskInstance> getTaskList(final ProcessInstanceUUID instanceUUID, final ActivityState taskState) throws InstanceNotFoundException {
    return getTaskListUser(instanceUUID, EnvTool.getUserId(), taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(final ProcessInstanceUUID instanceUUID, final ActivityState taskState) throws InstanceNotFoundException {
    return getLightTaskListUser(instanceUUID, EnvTool.getUserId(), taskState);
  }

  public Collection<TaskInstance> getTaskList(ProcessInstanceUUID instanceUUID, Collection<ActivityState> taskStates) throws InstanceNotFoundException {
    FacadeUtil.checkArgsNotNull(instanceUUID, taskStates);
    final Collection<TaskInstance> todos = new HashSet<TaskInstance>();
    for (ActivityState taskState : taskStates) {
      Collection<TaskInstance> tasks = getTaskListUser(instanceUUID, EnvTool.getUserId(), taskState);
      if (tasks != null) {
        todos.addAll(tasks);
      }
    }
    return todos;
  }

  public Collection<LightTaskInstance> getLightTaskList(ProcessInstanceUUID instanceUUID, Collection<ActivityState> taskStates) throws InstanceNotFoundException {
    FacadeUtil.checkArgsNotNull(instanceUUID, taskStates);
    final Collection<LightTaskInstance> todos = new HashSet<LightTaskInstance>();
    for (ActivityState taskState : taskStates) {
      Collection<LightTaskInstance> tasks = getLightTaskListUser(instanceUUID, EnvTool.getUserId(), taskState);
      if (tasks != null) {
        todos.addAll(tasks);
      }
    }
    return todos;
  }

  public Collection<TaskInstance> getTaskList(final ActivityState taskState) {
    return getTaskListUser(EnvTool.getUserId(), taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(final ActivityState taskState) {
    return getLightTaskListUser(EnvTool.getUserId(), taskState);
  }

  public TaskInstance getTask(final ActivityInstanceUUID taskUUID) throws TaskNotFoundException {
    final TaskInstance taskInstance = EnvTool.getAllQueriers(getQueryList()).getTaskInstance(taskUUID);
    if (taskInstance == null) {
      throw new TaskNotFoundException("bai_QRAPII_5", taskUUID);
    }
    return new ActivityInstanceImpl(taskInstance);
  }

  public Set<String> getTaskCandidates(ActivityInstanceUUID taskUUID) throws TaskNotFoundException {
    final TaskInstance taskInstance = EnvTool.getAllQueriers(getQueryList()).getTaskInstance(taskUUID);
    if (taskInstance == null) {
      throw new TaskNotFoundException("bai_QRAPII_5", taskUUID);
    }
    return CopyTool.copy(taskInstance.getTaskCandidates());
  }

  public Map<ActivityInstanceUUID, Set<String>> getTaskCandidates(Set<ActivityInstanceUUID> taskUUIDs) throws TaskNotFoundException {
    final Map<ActivityInstanceUUID, Set<String>> result = new HashMap<ActivityInstanceUUID, Set<String>>();
    for (ActivityInstanceUUID taskUUID : taskUUIDs) {
      final TaskInstance taskInstance = EnvTool.getAllQueriers(getQueryList()).getTaskInstance(taskUUID);
      if (taskInstance == null) {
        throw new TaskNotFoundException("bai_QRAPII_5", taskUUID);
      }
      result.put(taskUUID, CopyTool.copy(taskInstance.getTaskCandidates()));  
    }
    return result;
  }

  public Map<String, Object> getActivityInstanceVariables(final ProcessInstanceUUID instanceUUID,
      final String activityId, final String iterationId, final String activityInstanceId, final String loopId) throws ActivityNotFoundException, InstanceNotFoundException {

    final ActivityInstance activityInst = EnvTool.getAllQueriers().getActivityInstance(instanceUUID, activityId, iterationId, activityInstanceId, loopId);

    if (activityInst == null) {
      throw new ActivityNotFoundException("bai_QRAPII_6", instanceUUID, activityId);
    }
    Map<String, Object> variables = activityInst.getLastKnownVariableValues();
    /*Map<String, Object> convertedVariables = new HashMap<String, Object>();
    for (Map.Entry<String, Object> variable : variables.entrySet()) {
      Object value = variable.getValue();
      if (value != null && (value.getClass().getClassLoader() instanceof ProcessClassLoader
      || value.getClass().getClassLoader() instanceof CommonClassLoader)) {
        convertedVariables.put(variable.getKey(), new ObjectVariable((Serializable)value));
      } else {
        convertedVariables.put(variable.getKey(), value);
      }
    }
    return convertedVariables;
     */
    return variables;
  }

  public Object getActivityInstanceVariable(final ProcessInstanceUUID instanceUUID,
      final String activityId, final String iterationId,
      final String activityInstanceId, final String loopId, final String variableId) throws InstanceNotFoundException,
      ActivityNotFoundException, VariableNotFoundException {

    final Map<String, Object> variables = getActivityInstanceVariables(instanceUUID, activityId, iterationId, activityInstanceId, loopId);
    if (variables == null || !variables.containsKey(variableId)) {
      throw new VariableNotFoundException("bai_QRAPII_7", instanceUUID, activityId, variableId);
    }
    return variables.get(variableId);
  }

  public Map<String, Object> getProcessInstanceVariables(final ProcessInstanceUUID instanceUUID) throws InstanceNotFoundException {
    final ProcessInstance processInstance = getInternalProcessInstanceWithoutAttachements(instanceUUID);
    if (processInstance == null) {
      throw new InstanceNotFoundException("bai_QRAPII_8", instanceUUID);
    }
    return processInstance.getLastKnownVariableValues();
  }

  public Map<String, Object> getProcessInstanceVariables(final ProcessInstanceUUID instanceUUID, final Date maxDate)
  throws InstanceNotFoundException {
    //take all initial instance var and for each varupdate being proceed before max date, replace the initial value by the new one  
    final ProcessInstance processInstance = getInternalProcessInstanceWithoutAttachements(instanceUUID);
    final Map<String, Object> instanceInitialVars = processInstance.getInitialVariableValues();
    final Map<String, Object> instanceVarBeforeMaxDate = new HashMap<String, Object>();
    instanceVarBeforeMaxDate.putAll(instanceInitialVars);
    final Map<String, VariableUpdate> maxVarUpdates = new HashMap<String, VariableUpdate>();

    for (VariableUpdate varUpdate : processInstance.getVariableUpdates()) {
      if (varUpdate.getDate().getTime() <= maxDate.getTime()) {
        VariableUpdate currentMax = maxVarUpdates.get(varUpdate.getName()); 
        if (currentMax == null || currentMax.getDate().getTime() <= varUpdate.getDate().getTime()) {
          maxVarUpdates.put(varUpdate.getName(), varUpdate);
          instanceVarBeforeMaxDate.put(varUpdate.getName(), varUpdate.getValue());
        }
      }
    }
    return instanceVarBeforeMaxDate;
  }

  private Object getProcessInstanceVariable(final ProcessInstanceUUID instanceUUID, final String variableId, final Date maxDate)
  throws InstanceNotFoundException, VariableNotFoundException {
    final String variableName = Misc.getVariableName(variableId);
    final String xpath = Misc.getXPath(variableId);

    final Map<String, Object> variables = getProcessInstanceVariables(instanceUUID, maxDate);
    if (variables == null || !variables.containsKey(variableName)) {
      throw new VariableNotFoundException("bai_QRAPII_9", instanceUUID, variableName);
    }
    Object value = variables.get(variableName);
    if (xpath != null && xpath.length() > 0) {
      try {
        return evaluateXPath(xpath, (org.w3c.dom.Document) value);
      } catch (Exception ex) {
        throw new VariableNotFoundException("bai_QRAPII_17", instanceUUID, variableName);
      }
    } else {
      return value;
    }
  }

  public Object getProcessInstanceVariable(final ProcessInstanceUUID instanceUUID, final String variableId)
  throws InstanceNotFoundException, VariableNotFoundException {
    final String variableName = Misc.getVariableName(variableId);
    final String xpath = Misc.getXPath(variableId);
    final Map<String, Object> variables = getProcessInstanceVariables(instanceUUID);
    if (variables == null || !variables.containsKey(variableName)) {
      throw new VariableNotFoundException("bai_QRAPII_10", instanceUUID, variableName);
    }
    Object value = variables.get(variableName);
    if (xpath != null && xpath.length() > 0) {
      try {
        return evaluateXPath(xpath, (org.w3c.dom.Document) value);
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new VariableNotFoundException("bai_QRAPII_17", instanceUUID, variableId);
      }
    } else {
      return value;
    }
  }

  public ActivityInstance getActivityInstance(final ActivityInstanceUUID activityUUID)
  throws ActivityNotFoundException {
    final ActivityInstance activity = EnvTool.getAllQueriers(getQueryList()).getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_QRAPII_11", activityUUID);
    }
    return new ActivityInstanceImpl(activity);
  }

  public ActivityState getActivityInstanceState(final ActivityInstanceUUID activityUUID)
  throws ActivityNotFoundException {
    FacadeUtil.checkArgsNotNull(activityUUID);
    final Querier querier = EnvTool.getAllQueriers(getQueryList());
    ActivityState state = querier.getActivityInstanceState(activityUUID);
    if (state == null) {
      final ActivityInstance activity = querier.getActivityInstance(activityUUID);
      throw new ActivityNotFoundException("bai_QRAPII_3", activity.getProcessInstanceUUID(), activity.getActivityName());
    }
    return state;
  }

  public Object getActivityInstanceVariable(final ActivityInstanceUUID activityUUID, final String variableId)
  throws ActivityNotFoundException, VariableNotFoundException {
    //search in transient variables
    Map<String, Object> transientVariables = TransientData.getActivityTransientVariables(activityUUID);
    if (transientVariables != null && transientVariables.containsKey(variableId)){
      return transientVariables.get(variableId);
    }
    //search in the database persisted variables
    final String variableName = Misc.getVariableName(variableId);
    final String xpath = Misc.getXPath(variableId);
    final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_QRAPII_11", activityUUID);
    }
    final Map<String, Object> variableValues = activity.getLastKnownVariableValues();

    if (!variableValues.containsKey(variableName)) {
      throw new VariableNotFoundException("bai_QRAPII_12", activityUUID, variableName);
    }
    Object value = activity.getLastKnownVariableValues().get(variableName);
    if (xpath != null && xpath.length() > 0) {
      try {
        return evaluateXPath(xpath, (org.w3c.dom.Document) value);
      } catch (Exception ex) {
        throw new VariableNotFoundException("bai_QRAPII_16", activityUUID, variableName);
      }
    } else {
      return value;
    }
  }

  private Object evaluateXPath(final String xpath, final org.w3c.dom.Document doc)
  throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    final XPath xpathEval = XPathFactory.newInstance().newXPath();
    if (isTextExpected(xpath)) {
      return xpathEval.evaluate(xpath, doc);
    } else {
      return xpathEval.evaluate(xpath, doc, XPathConstants.NODE);
    }
  }

  private boolean isTextExpected(String xpath) {
    String[] segments = xpath.split("/");
    String lastSegment = segments[segments.length - 1];
    return lastSegment.equals("text()") || lastSegment.startsWith("@");
  }

  public Map<String, Object> getActivityInstanceVariables(final ActivityInstanceUUID activityUUID)
  throws ActivityNotFoundException {
    Map<String, Object> variables = new HashMap<String, Object>();
    Map<String, Object> transientVariables = TransientData.getActivityTransientVariables(activityUUID);
    if (transientVariables != null) {
      variables.putAll(transientVariables);
    }
    final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_QRAPII_11", activityUUID);
    }    
    Map<String, Object> lastKnownVariables = activity.getLastKnownVariableValues();
    if (lastKnownVariables != null){
      variables.putAll(lastKnownVariables);
    }
    return variables;
  }

  public Collection<TaskInstance> getTaskList(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState)
  throws InstanceNotFoundException {
    return getTaskListUser(instanceUUID, userId, taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState)
  throws InstanceNotFoundException {
    return getLightTaskListUser(instanceUUID, userId, taskState);
  }

  public Collection<TaskInstance> getTaskList(final String userId, final ActivityState taskState) {
    return getTaskListUser(userId, taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(final String userId, final ActivityState taskState) {
    return getLightTaskListUser(userId, taskState);
  }

  public ActivityInstanceUUID getOneTask(ActivityState taskState) {
    final Querier journal = EnvTool.getJournalQueriers(getQueryList());

    boolean access = EnvTool.isRestrictedApplicationAcces();
    TaskInstance task = null;
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          task = journal.getOneTask(EnvTool.getUserId(), taskState, visibleProcessUUIDs);
        }
      }
    } else {
      task = journal.getOneTask(EnvTool.getUserId(), taskState);
    }
    if (task == null) {
      return null;
    }
    return new ActivityInstanceUUID(task.getUUID());
  }

  public ActivityInstanceUUID getOneTask(ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    final Querier journal = EnvTool.getJournalQueriers(getQueryList());
    TaskInstance task = journal.getOneTask(EnvTool.getUserId(), instanceUUID, taskState);
    if (task == null) {
      return null;
    }
    return new ActivityInstanceUUID(task.getUUID());
  }

  public ActivityInstanceUUID getOneTask(ProcessDefinitionUUID processUUID, ActivityState taskState) {
    final Querier journal = EnvTool.getJournalQueriers(getQueryList());
    TaskInstance task = journal.getOneTask(EnvTool.getUserId(), processUUID, taskState);
    if (task == null) {
      return null;
    }
    return new ActivityInstanceUUID(task.getUUID());
  }

  private Collection<TaskInstance> getTaskListUser(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState)
  throws InstanceNotFoundException {
    final Collection<TaskInstance> todos = new ArrayList<TaskInstance>();
    for (final TaskInstance taskActivity : getInternalTaskListUser(instanceUUID, userId, taskState)) {
      todos.add(new ActivityInstanceImpl(taskActivity));
    }
    return todos;
  }

  private Collection<LightTaskInstance> getLightTaskListUser(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState)
  throws InstanceNotFoundException {
    final Collection<LightTaskInstance> todos = new ArrayList<LightTaskInstance>();
    for (final TaskInstance taskActivity : getInternalTaskListUser(instanceUUID, userId, taskState)) {
      todos.add(new LightActivityInstanceImpl(taskActivity));
    }
    return todos;
  }

  private Collection<TaskInstance> getInternalTaskListUser(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState)
  throws InstanceNotFoundException {
    FacadeUtil.checkArgsNotNull(instanceUUID, taskState, userId);
    final ProcessInstance processInstance = getInternalProcessInstanceWithoutAttachements(instanceUUID);
    if (processInstance == null) {
      throw new InstanceNotFoundException("bai_QRAPII_13", instanceUUID);
    }
    final Querier journal = EnvTool.getAllQueriers(getQueryList());
    return journal.getUserInstanceTasks(userId, instanceUUID, taskState);
  }

  private Collection<TaskInstance> getTaskListUser(final String userId, final ActivityState taskState) {
    final Collection<TaskInstance> result = new HashSet<TaskInstance>();
    for (TaskInstance taskInstance : getInternalTaskListUser(userId, taskState)) {
      result.add(new ActivityInstanceImpl(taskInstance));
    }
    return result;
  }

  private Collection<LightTaskInstance> getLightTaskListUser(final String userId, final ActivityState taskState) {
    final Collection<LightTaskInstance> result = new HashSet<LightTaskInstance>();
    for (TaskInstance taskInstance : getInternalTaskListUser(userId, taskState)) {
      result.add(new LightActivityInstanceImpl(taskInstance));
    }
    return result;
  }

  private Collection<TaskInstance> getInternalTaskListUser(final String userId, final ActivityState taskState) {
    FacadeUtil.checkArgsNotNull(userId, taskState);

    boolean access = EnvTool.isRestrictedApplicationAcces();
    Collection<TaskInstance> tasks = new HashSet<TaskInstance>();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          tasks = EnvTool.getAllQueriers(getQueryList()).getUserTasks(userId, taskState, visibleProcessUUIDs);
        }
      }
    } else {
      tasks = EnvTool.getAllQueriers(getQueryList()).getUserTasks(userId, taskState);
    }
    return tasks;
  }

  public Object getVariable(final ActivityInstanceUUID activityUUID, final String variableId)
  throws ActivityNotFoundException, VariableNotFoundException {
    try {
      return getActivityInstanceVariable(activityUUID, variableId);
    } catch (final Throwable e) {
      final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
      if (activity == null) {
        throw new ActivityNotFoundException("bai_QRAPII_11", activityUUID);
      }
      Date maxDate = getMaxDate(activity);
      try {
        return getProcessInstanceVariable(activity.getProcessInstanceUUID(), variableId, maxDate);
      } catch (final InstanceNotFoundException e1) {
        // If activity exists, the process instance must exist too.
        Misc.unreachableStatement();
        return null;
      }
    }
  }

  public Map<String, Object> getVariables(final ActivityInstanceUUID activityUUID)
  throws ActivityNotFoundException {
    final ActivityInstance activity = EnvTool.getAllQueriers(getQueryList()).getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_QRAPII_14", activityUUID);
    }
    Date maxDate = getMaxDate(activity);
    try {
      final Map<String, Object> allVariables = new HashMap<String, Object>();
      final Map<String, Object> localVariables = activity.getLastKnownVariableValues();
      final Map<String, Object> globalVariables = 
        getProcessInstanceVariables(activity.getProcessInstanceUUID(), maxDate);
      //add global first because if some variables are in both local and global
      //we want to keep local value
      allVariables.putAll(globalVariables);
      allVariables.putAll(localVariables);
      return allVariables;
    } catch (final InstanceNotFoundException e) {
      // If activity exists, the process instance must exist too.
      Misc.unreachableStatement();
      return null;
    }
  }

  private Date getMaxDate(ActivityInstance activity) {
    Date endedDate = activity.getEndedDate();
    if (endedDate == null) {
      return new Date();
    }
    return endedDate;
  }

  public Set<String> getAttachmentNames(ProcessInstanceUUID instanceUUID) {
    FacadeUtil.checkArgsNotNull(instanceUUID);
    try {
      final InternalProcessInstance instance = getInternalProcessInstanceWithAttachments(instanceUUID);
      final Set<String> attachmentNames = new HashSet<String>();
      final List<AttachmentInstance> attachments = instance.getAttachments();
      for (AttachmentInstance attachment : attachments) {
        attachmentNames.add(attachment.getName());
      }
      return attachmentNames;
    } catch (InstanceNotFoundException e) {
      throw new BonitaRuntimeException(e);
    }
  }

  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName) {
    FacadeUtil.checkArgsNotNull(instanceUUID, attachmentName);
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final SearchResult result = DocumentService.getDocuments(manager, instanceUUID, attachmentName);
    final List<org.ow2.bonita.services.Document> documents = result.getDocuments();
    if (documents.isEmpty()) {
      return null;
    } else {
      return DocumentService.getAttachmentFromDocument(manager, documents.get(0));
    }
  }

  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName, ActivityInstanceUUID activityUUID)
  throws ActivityNotFoundException {
    ActivityInstance activity = getActivityInstance(activityUUID);
    Date date = null;
    if (!activity.getState().equals(ActivityState.READY)
        && !activity.getState().equals(ActivityState.SUSPENDED)
        && !activity.getState().equals(ActivityState.EXECUTING)) {
      date = activity.getLastStateUpdate().getUpdatedDate();
    } else {
      date = new Date();
    }
    return getLastAttachment(instanceUUID, attachmentName, date);
  }

  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName, Date date) {
    FacadeUtil.checkArgsNotNull(instanceUUID, attachmentName, date);

    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final SearchResult result = DocumentService.getDocuments(manager, instanceUUID, attachmentName);
    final List<org.ow2.bonita.services.Document> documents = result.getDocuments();
    final List<org.ow2.bonita.services.Document> allDocuments = new ArrayList<org.ow2.bonita.services.Document>();
    for (org.ow2.bonita.services.Document document : documents) {
      List<org.ow2.bonita.services.Document> documentVersions;
      try {
        documentVersions = manager.getVersionsOfDocument(document.getId());
        allDocuments.addAll(documentVersions);
      } catch (DocumentNotFoundException e) {
        throw new BonitaRuntimeException(e);
      }
    }
    org.ow2.bonita.services.Document doc = null;
    for (int i = 0; i < allDocuments.size(); i++) {
      org.ow2.bonita.services.Document tmp = allDocuments.get(i);
      long tmpDate = tmp.getCreationDate().getTime();
      if (tmpDate <= date.getTime()) {
        if (doc == null) {
          doc = tmp;
        } else if (doc.getCreationDate().getTime() <= tmpDate) {
          doc = tmp;
        }
      }
    }
    if (doc == null) {
      return null;
    }
    return DocumentService.getAttachmentFromDocument(manager, doc);
  }

  public Collection<AttachmentInstance> getLastAttachments(ProcessInstanceUUID instanceUUID, Set<String> attachmentNames) {
    FacadeUtil.checkArgsNotNull(instanceUUID, attachmentNames);
    Set<AttachmentInstance> result = new HashSet<AttachmentInstance>();
    for (String attachmentName : attachmentNames) {
      AttachmentInstance attachmentInstance = getLastAttachment(instanceUUID, attachmentName);
      if (attachmentInstance != null) {
        result.add(attachmentInstance);
      }
    }
    return result;
  }

  public Collection<AttachmentInstance> getLastAttachments(ProcessInstanceUUID instanceUUID, String regex) {
    FacadeUtil.checkArgsNotNull(instanceUUID, regex);
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    List<AttachmentInstance> matchingAttachments = DocumentService.getAllAttachmentVersions(manager, instanceUUID);
    Map<String, AttachmentInstance> result = new HashMap<String, AttachmentInstance>();
    for (AttachmentInstance attachmentInstance : matchingAttachments) {
      if (attachmentInstance.getName().matches(regex)) {
        result.put(attachmentInstance.getName(), new AttachmentInstanceImpl(attachmentInstance));
      }
    }
    return result.values();
  }

  public List<AttachmentInstance> getAttachments(ProcessInstanceUUID instanceUUID, String attachmentName) {
    FacadeUtil.checkArgsNotNull(instanceUUID, attachmentName);
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    return DocumentService.getAllAttachmentVersions(manager, instanceUUID, attachmentName);
  }

  public byte[] getAttachmentValue(AttachmentInstance attachmentInstance) {
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    try {
      org.ow2.bonita.services.Document document = manager.getDocument(attachmentInstance.getUUID().getValue());
      return manager.getContent(document);
    } catch (DocumentNotFoundException e) {
      throw new BonitaRuntimeException(e);
    }
  }

  public List<Comment> getCommentFeed(ProcessInstanceUUID instanceUUID) {
    List<Comment> comments = EnvTool.getAllQueriers(getQueryList()).getCommentFeed(instanceUUID);
    return new ArrayList<Comment>(comments);
  }

  public List<Comment> getActivityInstanceCommentFeed(ActivityInstanceUUID activityUUID) {
    FacadeUtil.checkArgsNotNull(activityUUID);
    List<Comment> comments = EnvTool.getAllQueriers(getQueryList()).getActivityInstanceCommentFeed(activityUUID);
    return new ArrayList<Comment>(comments);
  }

  public int getNumberOfActivityInstanceComments(ActivityInstanceUUID activityUUID) {
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfActivityInstanceComments(activityUUID);
  }

  public Map<ActivityInstanceUUID, Integer> getNumberOfActivityInstanceComments(Set<ActivityInstanceUUID> activityUUIDs) {
    if(activityUUIDs == null || activityUUIDs.isEmpty()){
      return Collections.emptyMap();
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfActivityInstanceComments(activityUUIDs);
  }

  public int getNumberOfComments(ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfComments(instanceUUID);
  }

  public int getNumberOfProcessInstanceComments(ProcessInstanceUUID instanceUUID) {
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfProcessInstanceComments(instanceUUID);
  }

  public List<Comment> getProcessInstanceCommentFeed(ProcessInstanceUUID instanceUUID) {
    FacadeUtil.checkArgsNotNull(instanceUUID);
    List<Comment> comments = EnvTool.getAllQueriers(getQueryList()).getProcessInstanceCommentFeed(instanceUUID);
    return new ArrayList<Comment>(comments);
  }

  public LightTaskInstance getLightTaskInstance(ActivityInstanceUUID taskUUID)
  throws TaskNotFoundException {
    final TaskInstance taskInstance = EnvTool.getAllQueriers(getQueryList()).getTaskInstance(taskUUID);
    if (taskInstance == null) {
      throw new TaskNotFoundException("bai_QRAPII_5", taskUUID);
    }
    return new LightActivityInstanceImpl(taskInstance);
  }

  public LightActivityInstance getLightActivityInstance(ActivityInstanceUUID activityInstanceUUID)
  throws ActivityNotFoundException {
    final ActivityInstance activityInstance = EnvTool.getAllQueriers(getQueryList()).getActivityInstance(activityInstanceUUID);
    if (activityInstance == null) {
      throw new ActivityNotFoundException("bai_QRAPII_11", activityInstanceUUID);
    }
    return new LightActivityInstanceImpl(activityInstance);
  }

  public int search(SearchQueryBuilder query) {
    Class<?> indexClass = getIndexedClass(query);
    return EnvTool.getAllQueriers(getQueryList()).search(query, indexClass);
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> search(SearchQueryBuilder query, int firstResult, int maxResults) {
    Index index = query.getIndex();
    Class<?> resultClass = index.getResultClass();
    Class<?> indexClass = getIndexedClass(query);
    List<Object> list = EnvTool.getAllQueriers(getQueryList()).search(query, firstResult, maxResults, indexClass);
    if (UserImpl.class.equals(resultClass)) {
      return (List<T>) getUsers(list);
    } else if (LightProcessInstance.class.equals(resultClass)) {
      return (List<T>) getLightProcessInstances(list);
    } else if (LightProcessDefinition.class.equals(resultClass)) {
      return (List<T>) getLightProcessDefinitions(list);
    } else  if (LightActivityInstance.class.equals(resultClass)) {
      return (List<T>) getLightActivityInstances(list);
    } else if (GroupImpl.class.equals(resultClass)) {
      return (List<T>) getGroups(list);
    } else if (RoleImpl.class.equals(resultClass)) {
      return (List<T>) getRoles(list);
    } else if (CaseImpl.class.equals(resultClass)) {
      return (List<T>) getCases(list);
    } else {
      return Collections.emptyList();
    }
  }

  private Class<?> getIndexedClass(SearchQueryBuilder query) {
    Index index = query.getIndex();
    Class<?> resultClass = index.getResultClass();
    Class<?> indexClass = null;
    if (UserImpl.class.equals(resultClass)) {
      indexClass = UserImpl.class;
    } else if (LightProcessInstance.class.equals(resultClass)) {
      indexClass = InternalProcessInstance.class;
    } else if (LightProcessDefinition.class.equals(resultClass)) {
      indexClass = InternalProcessDefinition.class;
    } else if (LightActivityInstance.class.equals(resultClass)) {
      indexClass = InternalActivityInstance.class;
    } else if (GroupImpl.class.equals(resultClass)) {
      indexClass = GroupImpl.class;
    } else if (RoleImpl.class.equals(resultClass)) {
      indexClass = RoleImpl.class;
    } else if (CaseImpl.class.equals(resultClass)) {
      indexClass = CaseImpl.class;
    }
    return indexClass;
  }

  private List<LightActivityInstance> getLightActivityInstances(List<Object> list) {
    List<LightActivityInstance> result = new ArrayList<LightActivityInstance>();
    for (Object object : list) {
      result.add(new LightActivityInstanceImpl((InternalActivityInstance) object));
    }
    return result;
  }

  private List<LightProcessInstance> getLightProcessInstances(List<Object> list) {
    List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (Object object : list) {
      result.add(new LightProcessInstanceImpl((InternalProcessInstance) object));
    }
    return result;
  }

  private List<LightProcessDefinition> getLightProcessDefinitions(List<Object> list) {
    List<LightProcessDefinition> result = new ArrayList<LightProcessDefinition>();
    for (Object object : list) {
      result.add(new LightProcessDefinitionImpl((InternalProcessDefinition) object));
    }
    return result;
  }

  private List<UserImpl> getUsers(List<Object> list) {
    List<UserImpl> result = new ArrayList<UserImpl>();
    for (Object object : list) {
      result.add(new UserImpl((UserImpl) object));
    }
    return result;
  }

  private List<GroupImpl> getGroups(List<Object> list) {
    List<GroupImpl> result = new ArrayList<GroupImpl>();
    for (Object object : list) {
      result.add(new GroupImpl((GroupImpl) object));
    }
    return result;
  }

  private List<RoleImpl> getRoles(List<Object> list) {
    List<RoleImpl> result = new ArrayList<RoleImpl>();
    for (Object object : list) {
      result.add(new RoleImpl((RoleImpl) object));
    }
    return result;
  }

  private List<CaseImpl> getCases(List<Object> list) {
    List<CaseImpl> result = new ArrayList<CaseImpl>();
    for (Object object : list) {
      result.add(new CaseImpl((CaseImpl) object));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(String username, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUser(username, fromIndex, pageSize, processUUIDs);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = null;
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      processes = new ArrayList<InternalProcessInstance>();
    } else {
      processes = getParentProcessInstancesWithActiveUser(username, 
          fromIndex, pageSize, processUUIDs, pagingCriterion);
    }

    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String username, int remainingDays, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username,remainingDays, fromIndex, pageSize, processUUIDs);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String username, int remainingDays, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    if (processUUIDs == null || processUUIDs.isEmpty()){
      return new ArrayList<LightProcessInstance>();
    }

    List<InternalProcessInstance> processes = getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
        username,remainingDays, fromIndex, pageSize, processUUIDs, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(String username, int remainingDays, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username, remainingDays, fromIndex, pageSize, visibleProcesses);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(
      String username, int remainingDays, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {		
    Set<ProcessDefinitionUUID> visibleProcesses = null;		
    if (processUUIDs == null || processUUIDs.isEmpty()){
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDs();
    } else { 
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    }

    return getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
        username, remainingDays, fromIndex, pageSize, visibleProcesses, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserExcept(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getLightParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, visibleProcesses);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserExcept(
      String userId, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Set<ProcessDefinitionUUID> visibleProcesses = null;
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDs();
    } else {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    }
    return getLightParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, visibleProcesses, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUserExcept(String username, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getLightParentProcessInstancesWithInvolvedUser(username, fromIndex, pageSize, visibleProcesses);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUserExcept(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Set<ProcessDefinitionUUID> visibleProcesses = null;
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDs();
    } else {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    }
    return getLightParentProcessInstancesWithInvolvedUser(username, fromIndex, pageSize, visibleProcesses, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(String username, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    List<InternalProcessInstance> processes = getParentProcessInstancesWithOverdueTasks(username, fromIndex, pageSize, processUUIDs, ProcessInstanceCriterion.DEFAULT);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    if (processUUIDs == null || processUUIDs.isEmpty()){
      return Collections.emptyList();
    }

    List<InternalProcessInstance> processes = getParentProcessInstancesWithOverdueTasks(
        username, fromIndex, pageSize, processUUIDs, pagingCriterion);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasksExcept(String username, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getLightParentProcessInstancesWithOverdueTasks(username, fromIndex, pageSize, visibleProcesses);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasksExcept(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Set<ProcessDefinitionUUID> visibleProcesses = null;
    if (processUUIDs == null || processUUIDs.isEmpty()){
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDs();
    } else {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    }
    return getLightParentProcessInstancesWithOverdueTasks(username, fromIndex, pageSize, visibleProcesses, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    List<InternalProcessInstance> processes = getUserParentProcessInstances(fromIndex, pageSize, processUUIDs);
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex,
      int pageSize, Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processes = null;
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      processes = new ArrayList<InternalProcessInstance>();
    } else {
      processes = getUserParentProcessInstances(fromIndex, pageSize, processUUIDs, pagingCriterion);
    }  	
    final List<LightProcessInstance> result = new ArrayList<LightProcessInstance>();
    for (final ProcessInstance record : processes) {
      result.add(new LightProcessInstanceImpl(record));
    }
    return result;
  }

  public List<LightProcessInstance> getLightParentUserInstancesExcept(int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = null;
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDs();
    } else {
      visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    }    
    return getLightParentUserInstances(fromIndex, pageSize, visibleProcesses);
  }

  public List<LightProcessInstance> getLightParentUserInstancesExcept(
      int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getLightParentUserInstances(fromIndex, pageSize, visibleProcesses, pagingCriterion);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUser(username, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUser(username,processUUIDs);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String username, int remainingDays, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username,currentDate, atRisk, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username,currentDate, atRisk,processUUIDs);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(String username, int remainingDays, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username,remainingDays, visibleProcesses);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserExcept(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithActiveUser(username, visibleProcesses);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(username, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(username, processUUIDs);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserExcept(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithInvolvedUser(username, visibleProcesses);
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Date currentDate = new Date();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if(applicationName!=null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithOverdueTasks(username,currentDate, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithOverdueTasks(username,currentDate,processUUIDs);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasksExcept(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithOverdueTasks(username, visibleProcesses);
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        if (visibleProcessUUIDs == null) {
          visibleProcessUUIDs = new HashSet<ProcessDefinitionUUID>();
        }
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithStartedBy(username, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithStartedBy(username, processUUIDs);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithStartedByExcept(String username, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithStartedBy(username, visibleProcesses);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(String username, String category, Set<ProcessDefinitionUUID> processUUIDs) {
    boolean access = EnvTool.isRestrictedApplicationAcces();
    Set<ProcessDefinitionUUID> targetedProcesses = EnvTool.getAllQueriers(getQueryList()).getProcessUUIDsFromCategory(category);
    if (access) {
      String applicationName = EnvTool.getApplicationAccessName();
      if (applicationName != null) {
        Set<ProcessDefinitionUUID> visibleProcessUUIDs = FacadeUtil.getAllowedProcessUUIDsFor(applicationName, RuleType.PROCESS_READ);
        visibleProcessUUIDs.retainAll(targetedProcesses);
        visibleProcessUUIDs.retainAll(processUUIDs);
        if (visibleProcessUUIDs != null && !visibleProcessUUIDs.isEmpty()) {
          return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(username, visibleProcessUUIDs);
        }
      }
      return 0;
    } else {
      targetedProcesses.retainAll(processUUIDs);
      return EnvTool.getAllQueriers(getQueryList()).getNumberOfParentProcessInstancesWithInvolvedUser(username, targetedProcesses);
    }
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategoryExcept(String username, String category, Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> visibleProcesses = EnvTool.getAllQueriers().getAllProcessDefinitionUUIDsExcept(processUUIDs);
    return getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(username, category, visibleProcesses);
  }

  public Set<String> getActiveUsersOfProcessInstance(ProcessInstanceUUID uuid) throws InstanceNotFoundException {
    final ProcessInstance instance = getInternalProcessInstanceWithoutAttachements(uuid);  
    if (instance == null) {
      throw new InstanceNotFoundException("bai_QRAPII_1", uuid);
    }
    return instance.getActiveUsers();
  }

  public Map<ProcessInstanceUUID, Set<String>> getActiveUsersOfProcessInstances(Set<ProcessInstanceUUID> instanceUUIDs) throws InstanceNotFoundException {
    FacadeUtil.checkArgsNotNull(instanceUUIDs);
    HashMap<ProcessInstanceUUID, Set<String>> result = new HashMap<ProcessInstanceUUID, Set<String>>();
    for (ProcessInstanceUUID processInstanceUUID : instanceUUIDs) {
      result.put(processInstanceUUID, getActiveUsersOfProcessInstance(processInstanceUUID));
    }    
    return result;
  }

  public CatchingEvent getEvent(CatchingEventUUID eventUUID) throws EventNotFoundException {
    EventService eventService = EnvTool.getEventService();
    long incomingId = Long.parseLong(eventUUID.getValue());
    IncomingEventInstance incomingEvent = eventService.getIncomingEvent(incomingId);
    if (incomingEvent == null) {
      throw new EventNotFoundException("Event " + incomingId + "does not exist.");
    }
    return getEvent(incomingEvent);
  }

  public Set<CatchingEvent> getEvents() {
    EventService eventService = EnvTool.getEventService();
    Set<IncomingEventInstance> incomingEventInstances = eventService.getIncomingEvents();
    return getEvents(incomingEventInstances);
  }

  public Set<CatchingEvent> getEvents(ProcessInstanceUUID instanceUUID) {
    EventService eventService = EnvTool.getEventService();
    Set<IncomingEventInstance> incomingEventInstances = eventService.getIncomingEvents(instanceUUID);
    return getEvents(incomingEventInstances);
  }

  public Set<CatchingEvent> getEvents(ActivityInstanceUUID activityUUID) {
    EventService eventService = EnvTool.getEventService();
    Set<IncomingEventInstance> incomingEventInstances = eventService.getIncomingEvents(activityUUID);
    return getEvents(incomingEventInstances);
  }

  private Set<CatchingEvent> getEvents(Set<IncomingEventInstance> incomingEventInstances) {
    Set<CatchingEvent> events = new HashSet<CatchingEvent>();
    for (IncomingEventInstance incomingEventInstance : incomingEventInstances) {
      CatchingEvent event = getEvent(incomingEventInstance);
      if (event != null) {
        events.add(event);
      }
    }
    return events;
  }

  private CatchingEvent getEvent(final IncomingEventInstance incomingEventInstance) {
    final String signal = incomingEventInstance.getSignal();
    CatchingEventImpl event = null;
    if (signal.contains(EventConstants.TIMER)) {
      CatchingEventUUID uuid = new CatchingEventUUID(String.valueOf(incomingEventInstance.getId()));
      Position position = null;
      if (signal.contains(EventConstants.START_EVENT)) {
        position = Position.START;
      } else if (signal.contains(EventConstants.INTERMEDIATE_EVENT)
          || signal.equals("end_of_timer")) {
        position = Position.INTERMEDIATE;
      } else if (signal.contains(EventConstants.BOUNDARY_EVENT)) {
        position = Position.BOUNDARY;
      } else if (signal.equals("timer")) {
        position = Position.DEADLINE;
      }
      event = new CatchingEventImpl(uuid, position, Type.TIMER, incomingEventInstance.getEnableTime(), 
          incomingEventInstance.getActivityDefinitionUUID(), incomingEventInstance.getActivityUUID(),
          incomingEventInstance.getInstanceUUID(),incomingEventInstance.getActivityName(), incomingEventInstance.getProcessName());
    }
    return event;
  }

  public byte[] getDocumentContent(final DocumentUUID documentUUID) throws DocumentNotFoundException {
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final String documentId = documentUUID.getValue();
    final org.ow2.bonita.services.Document document = manager.getDocument(documentId);
    if (document == null) {
      throw new DocumentNotFoundException(documentId);
    }
    return manager.getContent(document);
  }

  public DocumentResult searchDocuments(final DocumentSearchBuilder builder, final int fromResult, final int MaxResults) {
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final SearchResult searchResult = manager.search(builder, fromResult, MaxResults);

    final List<org.ow2.bonita.services.Document> searchDocuments = searchResult.getDocuments();
    final List<Document> documents = new ArrayList<Document>();
    for (int i = 0; i < searchDocuments.size(); i++) {
      org.ow2.bonita.services.Document searchDocument = searchDocuments.get(i);
      documents.add(DocumentService.getClientDocument(manager, searchDocument));
    }
    final int count = searchResult.getCount();
    return new DocumentResult(count, documents);
  }

  public Document getDocument(final DocumentUUID documentUUID) throws DocumentNotFoundException {
    FacadeUtil.checkArgsNotNull(documentUUID);
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    return getDocument(documentUUID, manager);
  }

  public List<Document> getDocuments(final List<DocumentUUID> documentUUIDs) throws DocumentNotFoundException {
    FacadeUtil.checkArgsNotNull(documentUUIDs);
    final List<Document> documents = new ArrayList<Document>();
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    for (int i = 0; i < documentUUIDs.size(); i++) {
      final DocumentUUID documentUUID = documentUUIDs.get(i);
      final Document document = getDocument(documentUUID, manager);
      documents.add(document);
    }
    return documents;
  }

  public Document getDocument(final DocumentUUID documentUUID, final DocumentationManager manager) throws DocumentNotFoundException {
    org.ow2.bonita.services.Document document = manager.getDocument(documentUUID.getValue());
    return DocumentService.getClientDocument(manager, document);
  }

  public List<Document> getDocumentVersions(DocumentUUID documentUUID) throws DocumentNotFoundException {
    FacadeUtil.checkArgsNotNull(documentUUID);
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final List<org.ow2.bonita.services.Document> documentVersions = manager.getVersionsOfDocument(documentUUID.getValue());
    final List<Document> documents = new ArrayList<Document>();
    for (int i = 0; i < documentVersions.size(); i++) {
      final org.ow2.bonita.services.Document documentVersion = documentVersions.get(i);
      documents.add(DocumentService.getClientDocument(manager, documentVersion));
    }
    return documents;
  }

}
