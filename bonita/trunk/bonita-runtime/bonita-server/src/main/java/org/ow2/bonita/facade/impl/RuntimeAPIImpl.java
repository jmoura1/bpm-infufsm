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
 * Modified by Matthieu Chaffotte, Elias Ricken de Medeiros - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ow2.bonita.connector.core.Connector;
import org.ow2.bonita.connector.core.Filter;
import org.ow2.bonita.connector.core.Mapper;
import org.ow2.bonita.connector.core.RoleResolver;
import org.ow2.bonita.definition.activity.ConnectorExecutor;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.exception.ActivityDefNotFoundException;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.BonitaInternalException;
import org.ow2.bonita.facade.exception.DataFieldNotFoundException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.exception.EventNotFoundException;
import org.ow2.bonita.facade.exception.IllegalTaskStateException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.TaskNotFoundException;
import org.ow2.bonita.facade.exception.UncancellableInstanceException;
import org.ow2.bonita.facade.exception.UndeletableInstanceException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.InitialAttachment;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.impl.CommentImpl;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.uuid.AbstractUUID;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CatchingEventUUID;
import org.ow2.bonita.facade.uuid.DocumentUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.runtime.ActivityManager;
import org.ow2.bonita.runtime.TaskManager;
import org.ow2.bonita.runtime.event.EventConstants;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.Archiver;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.GroovyBindingBuilder;
import org.ow2.bonita.util.GroovyException;
import org.ow2.bonita.util.GroovyUtil;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessUtil;
import org.ow2.bonita.util.TransientData;
import org.ow2.bonita.util.GroovyBindingBuilder.PropagateBinding;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras
 */
public class RuntimeAPIImpl implements RuntimeAPI {

  private static final Logger LOG = Logger.getLogger(RuntimeAPIImpl.class.getName());

  private String queryList;

  protected RuntimeAPIImpl(final String queryList) {
    this.queryList = queryList;
  }

  private String getQueryList() {
    return this.queryList;
  }

  public void enableEventsInFailure(ActivityInstanceUUID activityUUID) {
    final EventService eventService = EnvTool.getEventService();
    eventService.enableEventsInFailureIncomingEvents(activityUUID);
  }

  public void enableEventsInFailure(ProcessInstanceUUID instanceUUID, String activityName) {
    Set<InternalActivityInstance> activities = EnvTool.getAllQueriers().getActivityInstances(instanceUUID, activityName);
    for (InternalActivityInstance activity : activities) {
      enableEventsInFailure(activity.getUUID());
    }
  }

  public void enablePermanentEventInFailure(ActivityDefinitionUUID activityUUID) {
    final EventService eventService = EnvTool.getEventService();
    eventService.enablePermanentEventsInFailure(activityUUID);
  }

  /**
   * Create an instance of the specified process and return the processUUID
   */
  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID)
  throws ProcessNotFoundException {
    try {
      return instantiateProcess(processUUID, null, null);
    } catch (final VariableNotFoundException e) {
      //must never occur
      throw new BonitaRuntimeException(e);
    }
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final ActivityDefinitionUUID activityUUID)
  throws ProcessNotFoundException {
    try {
      return instantiateProcess(processUUID, null, null, activityUUID);
    } catch (final VariableNotFoundException e) {
      //must never occur
      throw new BonitaRuntimeException(e);
    }
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final Map<String, Object> variables)
  throws ProcessNotFoundException, VariableNotFoundException {
    return instantiateProcess(processUUID, variables, null);
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID,
      final Map<String, Object> variables, Collection<InitialAttachment> attachments) throws ProcessNotFoundException, VariableNotFoundException {
    return instantiateProcess(processUUID, variables, attachments, null);
  }

  private ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final Map<String, Object> variables,
      Collection<InitialAttachment> attachments, final ActivityDefinitionUUID activityUUID)
  throws ProcessNotFoundException, VariableNotFoundException {
    FacadeUtil.checkArgsNotNull(processUUID);
    ProcessState state = new QueryDefinitionAPIImpl(this.queryList).getProcess(processUUID).getState();
    if (ProcessState.DISABLED.equals(state)){
      String message = ExceptionManager.getInstance().getFullMessage("bai_RAPII_36", processUUID);
      throw new BonitaRuntimeException(message);
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Starting a new instance of process : " + processUUID);
    }
    final Execution rootExecution = ProcessUtil.createProcessInstance(processUUID, variables, attachments, null, null, activityUUID, null);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Started: " + rootExecution.getInstance());
    }
    ProcessInstance instance = rootExecution.getInstance();
    final ProcessInstanceUUID instanceUUID = instance.getUUID();
    ProcessUtil.startEventSubProcesses(instance);
    rootExecution.getInstance().begin(activityUUID);
    return instanceUUID;
  }

  public void executeTask(ActivityInstanceUUID taskUUID, boolean assignTask)
  throws TaskNotFoundException, IllegalTaskStateException {
    startTask(taskUUID, assignTask);
    finishTask(taskUUID, assignTask);
  }

  public void cancelProcessInstance(final ProcessInstanceUUID instanceUUID) throws InstanceNotFoundException,
  UncancellableInstanceException {
    //if this instance is a child execution, throw an exception
    FacadeUtil.checkArgsNotNull(instanceUUID);
    final InternalProcessInstance instance = FacadeUtil.getInstance(instanceUUID, null);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_1", instanceUUID);
    }
    //if this instance is a child execution, throw an exception
    if (instance == null
        || instance.getParentInstanceUUID() != null
        || !instance.getInstanceState().equals(InstanceState.STARTED)) {
      throw new UncancellableInstanceException("bai_RAPII_2", instanceUUID, instance.getParentInstanceUUID(), instance.getInstanceState());
    }
    instance.cancel();
  }

  public void cancelProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs) throws InstanceNotFoundException, UncancellableInstanceException {
    FacadeUtil.checkArgsNotNull(instanceUUIDs);
    for (ProcessInstanceUUID instanceUUID : instanceUUIDs) {
      cancelProcessInstance(instanceUUID);
    }
  }

  public void deleteProcessInstances(final Collection<ProcessInstanceUUID> instanceUUIDs)
  throws InstanceNotFoundException, UndeletableInstanceException {
    if (instanceUUIDs != null) {
      for (ProcessInstanceUUID instanceUUID : instanceUUIDs) {
        deleteProcessInstance(instanceUUID);
      }
    }
  }

  public void deleteProcessInstance(final ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException, UndeletableInstanceException {
    //if this instance is a child execution, throw an exception
    //if this instance has children, delete them
    FacadeUtil.checkArgsNotNull(instanceUUID);

    final Querier allQueriers = EnvTool.getAllQueriers();
    final Querier journal = EnvTool.getJournalQueriers();
    final Querier history = EnvTool.getHistoryQueriers();

    InternalProcessInstance processInst = journal.getProcessInstance(instanceUUID);

    boolean inHistory = false;
    final boolean inJournal = processInst != null;
    if (!inJournal) {
      processInst = history.getProcessInstance(instanceUUID);
      inHistory = processInst != null;
    }

    if (processInst == null) {
      throw new InstanceNotFoundException("bai_RAPII_3", instanceUUID);
    }
    final ProcessInstanceUUID parentInstanceUUID = processInst.getParentInstanceUUID();
    //check that the parent instance does not exist anymore, else, throw an exception
    if (parentInstanceUUID != null && allQueriers.getProcessInstance(parentInstanceUUID) != null) {
      throw new UndeletableInstanceException("bai_RAPII_4", instanceUUID, parentInstanceUUID);
    }

    EnvTool.getLargeDataRepository().deleteData(Misc.getAttachmentCategories(instanceUUID));
    ProcessUtil.removeAllInstanceEvents(processInst);
    ProcessUtil.removeSubProcessEvents(processInst);

    if (inJournal) {
      final Recorder recorder = EnvTool.getRecorder();
      recorder.remove(processInst);
    } else if (inHistory) {
      final Archiver archiver = EnvTool.getArchiver();
      archiver.remove(processInst);
    }
    final Set<ProcessInstanceUUID> children = processInst.getChildrenInstanceUUID();
    for (final ProcessInstanceUUID child : children) {
      deleteProcessInstance(child);
    }
  }

  public void deleteAllProcessInstances(final Collection<ProcessDefinitionUUID> processUUIDs)
  throws ProcessNotFoundException, UndeletableInstanceException {
    FacadeUtil.checkArgsNotNull(processUUIDs);
    for (ProcessDefinitionUUID processUUID : processUUIDs) {
      deleteAllProcessInstances(processUUID);
    }
  }

  public void deleteAllProcessInstances(final ProcessDefinitionUUID processUUID)
  throws ProcessNotFoundException, UndeletableInstanceException {
    FacadeUtil.checkArgsNotNull(processUUID);
    final Querier querier = EnvTool.getAllQueriers();
    final ProcessDefinition process = querier.getProcess(processUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_5", processUUID);
    }
    deleteAllProcessInstances(process);
  }

  public void deleteAllProcessInstances(final ProcessDefinition process)
  throws ProcessNotFoundException, UndeletableInstanceException {
    FacadeUtil.checkArgsNotNull(process);
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final Querier querier = EnvTool.getAllQueriers();
    Set<InternalProcessInstance> instances = querier.getProcessInstances(processUUID);
    for (final ProcessInstance instance : instances) {
      //deletes only parent instances
      if (instance.getParentInstanceUUID() == null) {
        try {
          deleteProcessInstance(instance.getUUID());
        } catch (final InstanceNotFoundException e) {
          String message = ExceptionManager.getInstance().getFullMessage("bai_RAPII_6");
          throw new BonitaInternalException(message, e);
        }
      }
    }
    instances = querier.getProcessInstances(processUUID);
    if (instances != null && !instances.isEmpty()) {
      final ProcessInstance first = instances.iterator().next();
      throw new UndeletableInstanceException("bai_RAPII_7", first.getUUID(), first.getParentInstanceUUID());
    }
  }

  public void startTask(final ActivityInstanceUUID taskUUID, final boolean assignTask)
  throws TaskNotFoundException, IllegalTaskStateException  {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.start(taskUUID, assignTask);
  }

  public void startActivity(ActivityInstanceUUID activityUUID) throws ActivityNotFoundException {
    //nothing
  }

  public void finishTask(final ActivityInstanceUUID taskUUID, final boolean assignTask)
  throws TaskNotFoundException, IllegalTaskStateException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.finish(taskUUID, assignTask);
  }

  public void suspendTask(final ActivityInstanceUUID taskUUID, final boolean assignTask)
  throws TaskNotFoundException, IllegalTaskStateException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.suspend(taskUUID, assignTask);
  }

  public void resumeTask(final ActivityInstanceUUID taskUUID, final boolean taskAssign)
  throws TaskNotFoundException, IllegalTaskStateException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.resume(taskUUID, taskAssign);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID) throws TaskNotFoundException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.assign(taskUUID);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID, final String userId)
  throws TaskNotFoundException {
    FacadeUtil.checkArgsNotNull(taskUUID, userId);
    TaskManager.assign(taskUUID, userId);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID, final Set<String> candidates)
  throws TaskNotFoundException {
    FacadeUtil.checkArgsNotNull(taskUUID, candidates);
    TaskManager.assign(taskUUID, candidates);
  }

  public void unassignTask(final ActivityInstanceUUID taskUUID) throws TaskNotFoundException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.unAssign(taskUUID);
  }

  private String getDataTypeClassName(final String variableId, final Object variableValue, final AbstractUUID uuid) {
    final boolean mayBeAnXmlDocument = variableValue != null && variableValue instanceof String && (((String)variableValue).trim().startsWith("<")  || ((String)variableValue).trim().startsWith("&lt;"));
    if (mayBeAnXmlDocument) {
      String dataTypeClassName = null;  
      DataFieldDefinition dataFieldDefinition = null; 
      final String dataFieldName = Misc.getVariableName(variableId);
      final APIAccessor accessor = new StandardAPIAccessorImpl();
      final QueryDefinitionAPI queryDefinitionAPI = accessor.getQueryDefinitionAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);

      try {
        if (uuid instanceof ProcessDefinitionUUID) {
          dataFieldDefinition = queryDefinitionAPI.getProcessDataField((ProcessDefinitionUUID) uuid, dataFieldName);
        } else if (uuid instanceof ActivityDefinitionUUID) {
          dataFieldDefinition = queryDefinitionAPI.getActivityDataField((ActivityDefinitionUUID) uuid, dataFieldName);
        }
        //do that only if the string may be a Document        
        dataTypeClassName = dataFieldDefinition.getDataTypeClassName();
      } catch (Exception e) {
        throw new BonitaRuntimeException("unable to find datafield with name: " + dataFieldName + " in " + uuid);
      }
      return dataTypeClassName;
    }
    return null;
  }

  public void setProcessInstanceVariable(final ProcessInstanceUUID instanceUUID, final String variableId, final Object variableValue)
  throws InstanceNotFoundException, VariableNotFoundException {
    InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
    AttachmentInstance attachment = null;
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_20", instanceUUID);
    }
    String variableName = Misc.getVariableName(variableId);
    if (!instance.getLastKnownVariableValues().containsKey(variableName)) {
      final DocumentationManager manager = EnvTool.getDocumentationManager();
      List<AttachmentInstance> attachments = new ArrayList<AttachmentInstance>();
      if (instance.getNbOfAttachments() > 0) {
        attachments = DocumentService.getAllAttachmentVersions(manager, instanceUUID);
      }
      if (attachments.size() == 0) {
        throw new VariableNotFoundException("bai_RAPII_21", instanceUUID, variableName);
      } else {
        attachment = attachments.get(attachments.size() -1);
      }
    }

    Object newValue = variableValue;
    String targetVariable = variableId;
    final String dataTypeClassName = getDataTypeClassName(variableId, variableValue, instance.getProcessDefinitionUUID());
    if (attachment == null){
      if (variableId.contains(BonitaConstants.XPATH_VAR_SEPARATOR)) {
        try {
          targetVariable = Misc.getVariableName(variableId);
          newValue = getXMLValueXPath(variableId, variableValue, null, instanceUUID);
        } catch (Exception e) {
          throw new VariableNotFoundException("bai_RAPII_32", instanceUUID, variableId);
        }
      } else if (variableId.contains(BonitaConstants.JAVA_VAR_SEPARATOR)) {
        try {
          targetVariable = Misc.getVariableName(variableId);
          newValue = getModifiedJavaObject(variableId, variableValue, null, instance);
        } catch (Exception ex) {
          throw new VariableNotFoundException("bai_RAPII_34", instanceUUID, null, variableId);
        }
      } else if (Document.class.getName().equals(dataTypeClassName) && variableValue instanceof String) {
        try {
          newValue = Misc.generateDocument((String) variableValue);
        } catch (Exception e) {
          throw new BonitaRuntimeException("Unable to build a DOM Document from String: " + variableValue);
        }
      }
      EnvTool.getRecorder().recordInstanceVariableUpdated(targetVariable, newValue, instance.getUUID(), EnvTool.getUserId());
    } 
    else {
      if (variableValue instanceof byte []) {
        addAttachment(instanceUUID, attachment.getName(), attachment.getFileName(), (byte []) variableValue);
      } else if (variableValue instanceof AttachmentInstance) {
        AttachmentInstance newAttachment = (AttachmentInstance) variableValue;
        byte[] attachmentValue;
        try {
          final DocumentationManager manager = EnvTool.getDocumentationManager();
          org.ow2.bonita.services.Document document = manager.getDocument(newAttachment.getUUID().getValue());
          attachmentValue = manager.getContent(document);
        } catch (DocumentNotFoundException e) {
          throw new BonitaRuntimeException(e);
        }
        addAttachment(instanceUUID, attachment.getName(), newAttachment.getFileName(), attachmentValue);
      } else {
        String message = ExceptionManager.getInstance().getMessage("bai_RAPII_37");
        throw new IllegalArgumentException(message);
      }
    }
  }

  public void setProcessInstanceVariables(final ProcessInstanceUUID instanceUUID, final  Map<String, Object> variables)
  throws InstanceNotFoundException, VariableNotFoundException {
    for (String key : variables.keySet()){
      setProcessInstanceVariable(instanceUUID, key, variables.get(key));
    }
  }

  public void setActivityInstanceVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Object variableValue)
  throws ActivityNotFoundException, VariableNotFoundException {
    //search the variable in the transient variables
    Map<String, Object> transientVariables = TransientData.getActivityTransientVariables(activityUUID);
    if (transientVariables!= null && transientVariables.containsKey(variableId)){
      TransientData.updateActivityTransientVariableValue(activityUUID, variableId, variableValue);
      return;
    }
    //search in the database
    ActivityInstance activity = EnvTool.getJournalQueriers().getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_RAPII_22", activityUUID);
    }
    final ProcessInstanceUUID instanceUUID = activity.getProcessInstanceUUID();
    final String activityId = activity.getActivityName();
    if (!activity.getLastKnownVariableValues().containsKey(Misc.getVariableName(variableId))) {
      throw new VariableNotFoundException("bai_RAPII_24", instanceUUID, activityId, Misc.getVariableName(variableId));
    }

    final Recorder recorder = EnvTool.getRecorder();
    Object newValue = variableValue;
    String targetVariable = variableId;
    String dataTypeClassName = getDataTypeClassName(variableId, variableValue, activity.getActivityDefinitionUUID());

    if (variableId.contains(BonitaConstants.XPATH_VAR_SEPARATOR)) {
      try {
        targetVariable = Misc.getVariableName(variableId);
        newValue = getXMLValueXPath(variableId, variableValue, activityUUID, null);
      } catch (Exception e) {
        throw new VariableNotFoundException("bai_RAPII_31", instanceUUID, activityId, variableId);
      }
    } else if (variableId.contains(BonitaConstants.JAVA_VAR_SEPARATOR)) {
      try {
        targetVariable = Misc.getVariableName(variableId);
        newValue = getModifiedJavaObject(variableId, variableValue, activity, null);
      } catch (Exception ex) {
        throw new VariableNotFoundException("bai_RAPII_35", instanceUUID, activityId, variableId);
      }
    } else if (Document.class.getName().equals(dataTypeClassName) && variableValue instanceof String) {
      try {
        newValue = Misc.generateDocument((String) variableValue);
      } catch (Exception e) {
        throw new BonitaRuntimeException("Unable to build a DOM Document from String: " + variableValue);
      }
    }
    // local variable updated -> update only current activity
    recorder.recordActivityVariableUpdated(targetVariable, newValue, activityUUID, EnvTool.getUserId());
  }

  public void setActivityInstanceVariables(ActivityInstanceUUID activityUUID, Map<String, Object> variables) throws ActivityNotFoundException, VariableNotFoundException {
    for (String key : variables.keySet()){
      setActivityInstanceVariable(activityUUID, key, variables.get(key));
    }
  }

  private Object getModifiedJavaObject(String variableExpression, Object attributeValue, ActivityInstance activity, ProcessInstance processInstance)
  throws ActivityNotFoundException, VariableNotFoundException, InstanceNotFoundException {
    final String variableName = Misc.getVariableName(variableExpression);
    ActivityInstanceUUID activityUUID = null;
    if (activity != null) {
      activityUUID = activity.getUUID();
    }
    ProcessInstanceUUID processInstanceUUID = null;
    if (processInstance != null) {
      processInstanceUUID = processInstance.getUUID();
    }
    final Object data = getVariable(variableName, activityUUID, processInstanceUUID);
    ProcessDefinitionUUID processDefUUID = null;
    if (processInstanceUUID != null) {
      processDefUUID = processInstance.getProcessDefinitionUUID();
    } else {
      processDefUUID = activity.getProcessDefinitionUUID();
    }
    return getModifiedJavaObject(processDefUUID, variableExpression, data, attributeValue);
  }

  public Object getModifiedJavaObject(ProcessDefinitionUUID processUUID, String variableExpression, Object variableValue, Object attributeValue){
    final String variableName = Misc.getVariableName(variableExpression);
    final String groovyPlaceholderAccessExpression = Misc.getGroovyPlaceholderAccessExpression(variableExpression);
    final String setterName = Misc.getSetterName(variableExpression);
    final ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(processUUID);
    return modifyJavaObject(variableValue, variableName, groovyPlaceholderAccessExpression, setterName, attributeValue, processClassLoader);
  }

  private Object modifyJavaObject(Object data, String variableName, String groovyPlaceholderAccessExpression, String setterName, Object variableValue, ClassLoader classLoader) {
    GroovyShell shell = new GroovyShell(classLoader);
    shell.setProperty(variableName, data);
    shell.setProperty("__variableValue__", variableValue);
    StringBuilder script = new StringBuilder();
    script.append("def __tmp__ =");
    if (groovyPlaceholderAccessExpression != null && groovyPlaceholderAccessExpression.trim().length() > 0) {
      script.append(groovyPlaceholderAccessExpression);
    } else {
      script.append(variableName);
    }
    script.append(";\n");
    script.append("__tmp__.");
    script.append(setterName);
    script.append("(__variableValue__);\n");
    script.append(variableName);
    return shell.evaluate(script.toString());
  }

  private Object getXMLValueXPath(String variableId, Object variableValue, ActivityInstanceUUID activityUUID, ProcessInstanceUUID processInstanceUUID)
  throws Exception {
    final String variableName = Misc.getVariableName(variableId);
    final String xpathExpression = Misc.getXPath(variableId);
    final boolean isAppend = Misc.isXMLAppend(variableId);
    final Document doc = (Document) getVariable(variableName, activityUUID, processInstanceUUID);
    return evaluateXPath(doc, xpathExpression, isAppend, variableValue);
  }

  private Object getVariable(final String variableName, ActivityInstanceUUID activityUUID, ProcessInstanceUUID processInstanceUUID)
  throws ActivityNotFoundException, VariableNotFoundException, InstanceNotFoundException {
    Object oldValue = null;
    QueryRuntimeAPI queryAPI = new StandardAPIAccessorImpl().getQueryRuntimeAPI();
    if (activityUUID != null) {
      oldValue = queryAPI.getVariable(activityUUID, variableName);
    } else {
      oldValue = queryAPI.getProcessInstanceVariable(processInstanceUUID, variableName);
    }
    return oldValue;
  }

  private Document evaluateXPath(final Document doc, final String xpathExpression, final boolean isAppend, Object variableValue)
  throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    Node node = (Node) xpath.compile(xpathExpression).evaluate(doc, XPathConstants.NODE);
    if (isSetAttribute(xpathExpression, variableValue)) {
      if (node == null) { // Create the attribute
        String parentPath = xpathExpression.substring(0, xpathExpression.lastIndexOf('/'));
        String attributeName = xpathExpression.substring(xpathExpression.lastIndexOf('/') + 2) ; // +1 for @
        Node parentNode = (Node) xpath.compile(parentPath).evaluate(doc, XPathConstants.NODE);
        if (parentNode != null && parentNode instanceof Element) {
          Element element = (Element)parentNode;
          if (variableValue instanceof String) {
            element.setAttribute(attributeName, (String)variableValue);
          } else if (variableValue instanceof Attr) {
            element.setAttribute(((Attr) variableValue).getName(), ((Attr) variableValue).getTextContent());
          }
        }
      } else if (node instanceof Attr) { // Set an existing attribute
        if (variableValue instanceof String) {
          node.setTextContent((String)variableValue);
        } else if (variableValue instanceof Attr) {
          node.setTextContent(((Attr)variableValue).getTextContent());
        }
      } else if (node instanceof Element) { // add attribute to an element
        Attr attr = (Attr)variableValue;
        ((Element)node).setAttribute(attr.getName(), attr.getValue());
      }
    } else if (node instanceof Text) {
      node.setTextContent((String)variableValue);
    } else if (node instanceof Element) {
      Node newNode = null;
      if (variableValue instanceof Node) {
        newNode = doc.importNode((Node)variableValue, true);
      } else if (variableValue instanceof String) {
        newNode = doc.importNode(Misc.generateDocument((String)variableValue).getDocumentElement(), true);
      }

      if (isAppend) {
        node.appendChild(newNode);
      } else { // replace
        Node parentNode = node.getParentNode();
        parentNode.removeChild(node);
        parentNode.appendChild(newNode);
      }
    } else if (node == null && xpathExpression.endsWith("/text()") && variableValue instanceof String) {
      String parentPath = xpathExpression.substring(0, xpathExpression.lastIndexOf('/'));
      Node parentNode = (Node) xpath.compile(parentPath).evaluate(doc, XPathConstants.NODE);
      parentNode.appendChild(doc.createTextNode((String)variableValue));
    }
    return doc;
  }

  private boolean isSetAttribute(String xpathExpression, Object variableValue) {
    if (variableValue instanceof Attr) {
      return true;
    } else {
      String[] segments = xpathExpression.split("/");
      return segments[segments.length -1].startsWith("@");
    }
  }

  public void setVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Object variableValue)
  throws ActivityNotFoundException, VariableNotFoundException {
    try {
      setActivityInstanceVariable(activityUUID, variableId, variableValue);
    } catch (final Throwable e) {
      final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
      if (activity == null) {
        throw new ActivityNotFoundException("bai_RAPII_25", activityUUID);
      }
      try {
        setProcessInstanceVariable(activity.getProcessInstanceUUID(), variableId, variableValue);
      } catch (final InstanceNotFoundException e1) {
        // If activity exists, the process instance must exist too.
        Misc.unreachableStatement();
      }
    }
  }

  public void addComment(final ProcessInstanceUUID instanceUUID, final String message, final String userId)
  throws InstanceNotFoundException {
    CommentImpl comment = new CommentImpl(userId, message, instanceUUID);
    addComment(comment, instanceUUID);
  }

  public void addComment(final ActivityInstanceUUID activityUUID, final String message, final String userId)
  throws ActivityNotFoundException, InstanceNotFoundException {
    final ActivityInstance activity = EnvTool.getJournalQueriers().getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_RAPII_28", activityUUID);
    }
    CommentImpl comment = new CommentImpl(userId, message, activityUUID, activity.getProcessInstanceUUID());
    addComment(comment, activity.getProcessInstanceUUID());
  }

  private void addComment(Comment comment, ProcessInstanceUUID instanceUUID)
  throws InstanceNotFoundException {
    final InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_27", instanceUUID);
    }
    instance.addComment(comment);
  }

  @Deprecated
  public void addComment(final ProcessInstanceUUID instanceUUID, final ActivityInstanceUUID activityUUID, final String message, final String userId)
  throws InstanceNotFoundException, ActivityNotFoundException {
    final InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_27", instanceUUID);
    }
    if (activityUUID != null) {
      final ActivityInstance activity = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
      if (activity == null) {
        throw new ActivityNotFoundException("bai_RAPII_28", activityUUID);
      }
    }
    CommentImpl comment;
    if (activityUUID != null) {
      comment = new CommentImpl(userId, message, activityUUID, instanceUUID);
    } else {
      comment = new CommentImpl(userId, message, instanceUUID);
    }
    instance.addComment(comment);
  }

  public void addProcessMetaData(ProcessDefinitionUUID uuid, String key, String value)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(uuid, key, value);
    InternalProcessDefinition process = EnvTool.getAllQueriers().getProcess(uuid);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_29", uuid);
    }
    process.addAMetaData(key, value);
  }

  public void deleteProcessMetaData(ProcessDefinitionUUID uuid, String key)
  throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(uuid, key);
    InternalProcessDefinition process = EnvTool.getAllQueriers().getProcess(uuid);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_29", uuid);
    }
    process.deleteAMetaData(key);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID instanceUUID, boolean propagate)
  throws InstanceNotFoundException, GroovyException {
    return evaluateGroovyExpression(expression, instanceUUID, null, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID instanceUUID, Map<String, Object> context, boolean propagate)
  throws InstanceNotFoundException, GroovyException {
    return GroovyUtil.evaluate(expression, context, instanceUUID, false, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID instanceUUID,
      Map<String, Object> context, boolean useInitialVariableValues, boolean propagate)
  throws InstanceNotFoundException, GroovyException {
    return GroovyUtil.evaluate(expression, context, instanceUUID, useInitialVariableValues, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ActivityInstanceUUID activityUUID, boolean useActivityScope, boolean propagate)
  throws InstanceNotFoundException, ActivityNotFoundException, GroovyException {
    return evaluateGroovyExpression(expression, activityUUID, null, useActivityScope, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ActivityInstanceUUID activityUUID, Map<String, Object> context, boolean useActivityScope, boolean propagate)
  throws InstanceNotFoundException, ActivityNotFoundException, GroovyException {
    return GroovyUtil.evaluate(expression, context, activityUUID, useActivityScope, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ProcessDefinitionUUID processDefinitionUUID)
  throws ProcessNotFoundException, GroovyException {
    return evaluateGroovyExpression(expression, processDefinitionUUID, null);
  }

  public Object evaluateGroovyExpression(String expression, ProcessDefinitionUUID processDefinitionUUID, Map<String, Object> context)
  throws ProcessNotFoundException, GroovyException {
    return GroovyUtil.evaluate(expression, context, processDefinitionUUID, false);
  }
  
  public Map<String, Object> evaluateGroovyExpressions(final Map<String, String> expressions, final ProcessDefinitionUUID processDefinitionUUID, final Map<String, Object> context)
  throws ProcessNotFoundException, GroovyException {
    Misc.checkArgsNotNull(processDefinitionUUID);
    if (expressions == null || expressions.isEmpty()) {
      return Collections.emptyMap();
    }
    ClassLoader ori = Thread.currentThread().getContextClassLoader();
    try {
      ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(processDefinitionUUID);
      Thread.currentThread().setContextClassLoader(processClassLoader);
      final Binding simpleBinding = GroovyBindingBuilder.getSimpleBinding(processDefinitionUUID, null, null, context, true, true);
      final Map<String, Object> results = evaluateGroovyExpressions(expressions, simpleBinding);
      return results;
    } catch (Exception e) {
      throw new GroovyException("Exception while getting binding. ProcessDefinitionUUID: " + processDefinitionUUID, e);
    } finally {
      if (ori != null && ori != Thread.currentThread().getContextClassLoader()) {
        Thread.currentThread().setContextClassLoader(ori);
      }
    }
  }

  public Map<String, Object> evaluateGroovyExpressions(final Map<String, String> expressions, final ActivityInstanceUUID activityUUID, final Map<String, Object> context, final boolean useActivityScope, final boolean propagate)
  throws InstanceNotFoundException, ActivityNotFoundException, GroovyException {
    Misc.checkArgsNotNull(activityUUID);
    if (expressions == null || expressions.isEmpty()) {
      return Collections.emptyMap();
    }
    ClassLoader ori = Thread.currentThread().getContextClassLoader();
    try {
      InternalActivityInstance activityInstance = EnvTool.getAllQueriers().getActivityInstance(activityUUID);
      ProcessDefinitionUUID definitionUUID = activityInstance.getProcessDefinitionUUID();
      ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(definitionUUID);
      Thread.currentThread().setContextClassLoader(processClassLoader);
      final Binding binding = getBinding(definitionUUID, activityInstance.getProcessInstanceUUID(), activityUUID, context, useActivityScope, false, propagate);
      Map<String, Object> results = evaluateGroovyExpressions(expressions, binding);
      propagateVariablesIfNecessary(activityUUID, null, propagate, binding);
      return results;
    } catch (Exception e) {
      throw new GroovyException("Exception while evaluating expression. ActivityInstanceUUID: " + activityUUID, e);
    } finally {
      if (ori != null && ori != Thread.currentThread().getContextClassLoader()) {
        Thread.currentThread().setContextClassLoader(ori);
      }
    }
  }

  private void propagateVariablesIfNecessary(final ActivityInstanceUUID activityUUID,
      final ProcessInstanceUUID instanceUUID, final boolean propagate, final Binding binding) throws GroovyException {
    if (propagate) {
      try {
        GroovyUtil.propagateVariables(((PropagateBinding)binding).getVariablesToPropagate(), activityUUID, instanceUUID);
      } catch (Exception e) {
        throw new GroovyException("Exception while propagating variables", e);
      } 
    }
  }

  private Binding getBinding(final ProcessDefinitionUUID processUUID, final ProcessInstanceUUID instanceUUID, final ActivityInstanceUUID activityUUID, final Map<String, Object> context, final boolean useActiveScope, final boolean useInitialVariableValues, final boolean propagate)
  throws GroovyException {
    Binding binding = null;
    try {
      if (propagate) {
        binding = GroovyBindingBuilder.getPropagateBinding(processUUID, instanceUUID, activityUUID, context, useActiveScope, useInitialVariableValues);
      } else {
        binding = GroovyBindingBuilder.getSimpleBinding(processUUID, instanceUUID, activityUUID, context, useActiveScope, useInitialVariableValues);
      }
    } catch (Exception e) {
      throw new GroovyException("Exception while getting binding", e);
    }
    return binding;
  }

  private Map<String, Object> evaluateGroovyExpressions(final Map<String, String> expressions, Binding binding)
  throws GroovyException, NotSerializableException, ActivityDefNotFoundException, DataFieldNotFoundException, ProcessNotFoundException, IOException, ClassNotFoundException {
    final Map<String, Object> results = new HashMap<String, Object>();
    for (final String expressionName : expressions.keySet()) {
      final String expression = expressions.get(expressionName);
      final Object result = GroovyUtil.evaluate(expression, binding);
      results.put(expressionName, result);
    }
    return results;
  }

  public Map<String, Object> evaluateGroovyExpressions(final Map<String, String> expressions, final ProcessInstanceUUID processInstanceUUID, final Map<String, Object> context, final boolean useInitialVariableValues, final boolean propagate)
  throws InstanceNotFoundException, GroovyException {
    Misc.checkArgsNotNull(processInstanceUUID);
    if (expressions == null || expressions.isEmpty()) {
      return Collections.emptyMap();
    }

    ClassLoader ori = Thread.currentThread().getContextClassLoader();
    try {
      InternalProcessInstance instance = EnvTool.getAllQueriers().getProcessInstance(processInstanceUUID);
      ProcessDefinitionUUID definitionUUID = instance.getProcessDefinitionUUID();
      ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(definitionUUID);
      Thread.currentThread().setContextClassLoader(processClassLoader);
      final Binding binding = getBinding(definitionUUID, processInstanceUUID, null, context, false, useInitialVariableValues, propagate);
      Map<String, Object> results = evaluateGroovyExpressions(expressions, binding);
      propagateVariablesIfNecessary(null, processInstanceUUID, propagate, binding);
      return results;
    } catch (Exception e) {
      throw new GroovyException("Exception while evaluating expression. ProcessInstanceUUID: " + processInstanceUUID, e);
    } finally {
      if (ori != null && ori != Thread.currentThread().getContextClassLoader()) {
        Thread.currentThread().setContextClassLoader(ori);
      }
    }
  }

  public void addAttachment(ProcessInstanceUUID instanceUUID, String name, String fileName, byte[] value) {
    if(value == null && fileName != null) {
        throw new BonitaRuntimeException("The content of the attachment cannot be null");
    }
    try {
      createDocument(name, instanceUUID, fileName, DocumentService.DEFAULT_MIME_TYPE, value);
    } catch (Exception e) {
      final DocumentationManager manager = EnvTool.getDocumentationManager();
      final SearchResult result = DocumentService.getDocuments(manager, instanceUUID, name);
      try {
        addDocumentVersion(result.getDocuments().get(0).getId(), true, fileName, DocumentService.DEFAULT_MIME_TYPE, value);
      } catch (DocumentationCreationException e1) {
        throw new BonitaRuntimeException(e);
      }
    }
  }

  public void addAttachment(ProcessInstanceUUID instanceUUID, String name, String label, String description, String fileName, Map<String, String> metadata, byte[] value) {
    if(value == null && fileName != null) {
      throw new BonitaRuntimeException("The content of the attachment cannot be null");
    }
    String mimeType = metadata.get("content-type");
    if (mimeType == null) {
      mimeType = DocumentService.DEFAULT_MIME_TYPE;
    }
    try {
      createDocument(name, instanceUUID, fileName, mimeType, value);
    } catch (Exception e) {
      final DocumentationManager manager = EnvTool.getDocumentationManager();
      final SearchResult result = DocumentService.getDocuments(manager, instanceUUID, name);
      try {
        addDocumentVersion(result.getDocuments().get(0).getId(), true, fileName, mimeType, value);
      } catch (DocumentationCreationException e1) {
        throw new BonitaRuntimeException(e);
      }
    }
  }

  public void addAttachments(Map<AttachmentInstance, byte[]> attachments) {
    if (attachments != null) {
      for (Entry<AttachmentInstance, byte[]> attachment : attachments.entrySet()) {
        final AttachmentInstance attachmentInstance = attachment.getKey();
        final String name = attachmentInstance.getName();
        final ProcessInstanceUUID instanceUUID = attachmentInstance.getProcessInstanceUUID();
        final String fileName = attachmentInstance.getFileName();
        String mimeType = attachmentInstance.getMetaData().get("content-type");
        if (mimeType == null) {
          mimeType = DocumentService.DEFAULT_MIME_TYPE;
        }
        try {
          createDocument(name, instanceUUID, fileName, mimeType, attachment.getValue());
        } catch (Exception e) {
          throw new BonitaRuntimeException(e);
        }
      }
    }
  }

  public void removeAttachment(ProcessInstanceUUID instanceUUID, String name) throws InstanceNotFoundException {
    FacadeUtil.checkArgsNotNull(instanceUUID, name);
    final InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_1", instanceUUID);
    }
    if(instance.getNbOfAttachments()<=0) {
      throw new BonitaRuntimeException(new DocumentNotFoundException(name));
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final SearchResult result = DocumentService.getDocuments(manager, instanceUUID, name);
    final List<org.ow2.bonita.services.Document> documents = result.getDocuments();
    if (!documents.isEmpty()) {
      org.ow2.bonita.services.Document document = documents.get(0);
      try {
        manager.deleteDocument(document.getId(), true);
        // Keep mapping with number of attachments.
        instance.setNbOfAttachments(instance.getNbOfAttachments()-1);
      } catch (DocumentNotFoundException e) {
        throw new BonitaRuntimeException(e);
      }
    }
  }

  public void setActivityInstancePriority(ActivityInstanceUUID activityInstanceUUID, int priority)
  throws ActivityNotFoundException {
    final ActivityInstance activity = EnvTool.getJournalQueriers().getActivityInstance(activityInstanceUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_RAPII_22", activityInstanceUUID);
    }
    final ProcessInstanceUUID instanceUUID = activity.getProcessInstanceUUID();
    Execution execution = EnvTool.getJournalQueriers().getExecutionOnActivity(instanceUUID, activityInstanceUUID);
    if (execution == null) {
      throw new ActivityNotFoundException("bai_RAPII_23", activityInstanceUUID);
    }
    final Recorder recorder = EnvTool.getRecorder();
    recorder.recordActivityPriorityUpdated(activityInstanceUUID, priority);
  }

  public void deleteEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID actiivtyUUID) {
    final EventService eventService = EnvTool.getEventService();
    Set<OutgoingEventInstance> events = eventService.getOutgoingEvents(eventName, toProcessName, toActivityName, actiivtyUUID);
    if (events != null) {
      for (OutgoingEventInstance event : events) {
        eventService.removeEvent(event);
      }
    }
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID) throws Exception {
    return this.executeConnector(connectorClassName, parameters, definitionUUID, null, null, null, null, true);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters) throws Exception {
    return this.executeConnector(connectorClassName, parameters, null, null, null, null, null, true);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ClassLoader classLoader) throws Exception {
    return this.executeConnector(connectorClassName, parameters, null, null, null, classLoader, null, true);
  }

  private Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityInstanceUUID,
      ClassLoader classLoader, Map<String, Object> context, boolean useCurrentVariableValues) throws Exception {
    ClassLoader baseClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Connector connector = null;
      if (classLoader == null && definitionUUID == null) {
        connector = (Connector) EnvTool.getClassDataLoader().getInstance(null, connectorClassName);
      } else {
        Class<?> objectClass = null;
        if (classLoader == null) {
          ClassLoader processClassLoader = EnvTool.getClassDataLoader().getProcessClassLoader(definitionUUID);
          Thread.currentThread().setContextClassLoader(processClassLoader);
          objectClass = Class.forName(connectorClassName, true, processClassLoader);
        } else {
          Thread.currentThread().setContextClassLoader(classLoader);
          objectClass = Class.forName(connectorClassName, true, classLoader);
        }
        connector = (Connector) objectClass.newInstance();
      }
      if (connector instanceof Mapper) {
        throw new IllegalAccessException(connectorClassName + " is a instance of RoleResolver or Filter");
      }
      return ConnectorExecutor.executeConnector(connector, definitionUUID, instanceUUID, activityInstanceUUID, parameters, context, useCurrentVariableValues);
    } finally {
      Thread.currentThread().setContextClassLoader(baseClassLoader);
    }
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, Map<String, Object> context) throws Exception {
    return this.executeConnector(connectorClassName, parameters, definitionUUID, null, null, null, context, true);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ProcessInstanceUUID processInstanceUUID, Map<String, Object> context, boolean useCurrentVariableValues) throws Exception {
    final ProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(processInstanceUUID);
    return this.executeConnector(connectorClassName, parameters, instance.getProcessDefinitionUUID(), processInstanceUUID, null, null, context, useCurrentVariableValues);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, ActivityInstanceUUID activityInstanceUUID, Map<String, Object> context, boolean useCurrentVariableValues) throws Exception {
    final ActivityInstance activity = EnvTool.getJournalQueriers().getActivityInstance(activityInstanceUUID);
    return this.executeConnector(connectorClassName, parameters, activity.getProcessDefinitionUUID(), null, activityInstanceUUID, null, context, useCurrentVariableValues);
  }

  public Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members)
  throws Exception {
    return this.executeFilter(connectorClassName, parameters, members, null, null);
  }

  public Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members,
      ProcessDefinitionUUID definitionUUID) throws Exception {
    return this.executeFilter(connectorClassName, parameters, members, definitionUUID, null);
  }

  public Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members,
      ClassLoader classLoader) throws Exception {
    return this.executeFilter(connectorClassName, parameters, members, null, classLoader);
  }

  private Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members,
      ProcessDefinitionUUID definitionUUID, ClassLoader classLoader) throws Exception {
    FacadeUtil.checkArgsNotNull(members);
    ClassLoader baseClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Filter connector = null;
      if (classLoader == null) {
        connector = (Filter) EnvTool.getClassDataLoader().getInstance(definitionUUID, connectorClassName);
      } else {
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> objectClass = Class.forName(connectorClassName, true, classLoader);
        connector = (Filter) objectClass.newInstance();
      }
      return ConnectorExecutor.executeFilter(connector, parameters, members);
    } finally {
      Thread.currentThread().setContextClassLoader(baseClassLoader);
    }
  }

  public Set<String> executeRoleResolver(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID)
  throws Exception {
    return this.executeRoleResolver(connectorClassName, parameters, definitionUUID, null);
  }

  public Set<String> executeRoleResolver(String connectorClassName, Map<String, Object[]> parameters) throws Exception {
    return this.executeRoleResolver(connectorClassName, parameters, null, null);
  }

  public Set<String> executeRoleResolver(String connectorClassName, Map<String, Object[]> parameters, ClassLoader classLoader)
  throws Exception {
    return this.executeRoleResolver(connectorClassName, parameters, null, classLoader);
  }

  private Set<String> executeRoleResolver(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, ClassLoader classLoader)
  throws Exception {
    ClassLoader baseClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      RoleResolver connector = null;
      if (classLoader == null) {
        connector = (RoleResolver) EnvTool.getClassDataLoader().getInstance(definitionUUID, connectorClassName);
      } else {
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> objectClass = Class.forName(connectorClassName, true, classLoader);
        connector = (RoleResolver) objectClass.newInstance();
      }
      return ConnectorExecutor.executeRoleResolver(connector, parameters);
    } finally {
      Thread.currentThread().setContextClassLoader(baseClassLoader);
    }
  }

  public void skipTask(ActivityInstanceUUID taskUUID, Map<String, Object> variablesToUpdate)
  throws TaskNotFoundException, IllegalTaskStateException {
    FacadeUtil.checkArgsNotNull(taskUUID);
    TaskManager.skip(taskUUID, variablesToUpdate);
  }
  
  public void skip(ActivityInstanceUUID activityInstanceUUID,
      Map<String, Object> variablesToUpdate) throws ActivityNotFoundException,
      IllegalTaskStateException {
    FacadeUtil.checkArgsNotNull(activityInstanceUUID);
    ActivityManager.skip(activityInstanceUUID, variablesToUpdate);
  }

  public void executeEvent(CatchingEventUUID eventUUID) throws EventNotFoundException {
    EventService eventService = EnvTool.getEventService();
    long id = Long.parseLong(eventUUID.getValue());
    IncomingEventInstance internalEvent = eventService.getIncomingEvent(id);
    final String signal = internalEvent.getSignal();
    if (signal.contains(EventConstants.TIMER)) {
      updateExpirationDate(internalEvent, new Date());
    }
  }

  public void deleteEvent(CatchingEventUUID eventUUID) throws EventNotFoundException {
    EventService eventService = EnvTool.getEventService();
    long id = Long.parseLong(eventUUID.getValue());
    IncomingEventInstance internalEvent = eventService.getIncomingEvent(id);
    if (internalEvent == null) {
      throw new EventNotFoundException("Event " + id + "does not exist.");
    }
    eventService.removeEvent(internalEvent);
    EnvTool.getEventExecutor().refresh();
  }

  public void deleteEvents(Collection<CatchingEventUUID> eventUUIDs) throws EventNotFoundException {
    if (eventUUIDs != null) {
      for (CatchingEventUUID eventUUID : eventUUIDs) {
        deleteEvent(eventUUID);
      }
    }
  }

  public void updateExpirationDate(CatchingEventUUID eventUUID, Date expiration) throws EventNotFoundException {
    EventService eventService = EnvTool.getEventService();
    long id = Long.parseLong(eventUUID.getValue());
    IncomingEventInstance internalEvent = eventService.getIncomingEvent(id);
    if (internalEvent == null) {
      throw new EventNotFoundException("Event " + id + "does not exist.");
    }
    updateExpirationDate(internalEvent, expiration);
  }

  private void updateExpirationDate(IncomingEventInstance internalEvent, Date expiration) throws EventNotFoundException {
    internalEvent.setEnableTime(expiration.getTime());
    EnvTool.getEventExecutor().refresh();
  }

  public void updateActivityExpectedEndDate(final ActivityInstanceUUID activityUUID, Date expectedEndDate) throws ActivityNotFoundException {
    InternalActivityInstance activity = EnvTool.getAllQueriers(getQueryList()).getActivityInstance(activityUUID);
    if (activity == null) {
      throw new ActivityNotFoundException("bai_QRAPII_11", activityUUID);
    }
    activity.setExpectedEndDate(expectedEndDate);
  }

  public org.ow2.bonita.facade.runtime.Document createDocument(final String name, final ProcessInstanceUUID instanceUUID,
      final String fileName, final String mimeType, final byte[] content)
  throws DocumentationCreationException, InstanceNotFoundException {
    final InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
    if (instance == null) {
      throw new InstanceNotFoundException("bai_RAPII_1", instanceUUID);
    } else if (content != null && (fileName == null || mimeType == null)) {
      new DocumentationCreationException("");
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final ProcessDefinitionUUID definitionUUID = instance.getProcessDefinitionUUID();
    org.ow2.bonita.services.Document d = null;
    if (content != null) {
      d = manager.createDocument(name, definitionUUID, instanceUUID, fileName, mimeType, content);
    } else {
      d = manager.createDocument(name, definitionUUID, instanceUUID);
    }
    // Keep mapping of number of attachments
    final int previousNbOfAttachments =instance.getNbOfAttachments();
    if(previousNbOfAttachments<=0) {
      instance.setNbOfAttachments(1);
    } else {
      instance.setNbOfAttachments(previousNbOfAttachments+1);
    }
    
    //update lastUpdateDate date
    instance.updateLastUpdateDate();
    
    return DocumentService.getClientDocument(manager, d);
  }

  public org.ow2.bonita.facade.runtime.Document createDocument(String name,
      ProcessDefinitionUUID processDefinitionUUID, String fileName, String mimeType, byte[] content)
      throws DocumentationCreationException, ProcessNotFoundException {
    final ProcessDefinition process = EnvTool.getAllQueriers().getProcess(processDefinitionUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_29", processDefinitionUUID);
    } else if (content != null && (fileName == null || mimeType == null)) {
      new DocumentationCreationException("");
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    org.ow2.bonita.services.Document d = null;
    if (content != null) {
      d = manager.createDocument(name, processDefinitionUUID, fileName, mimeType, content);
    } else {
      d = manager.createDocument(name, processDefinitionUUID);
    }
    
    return DocumentService.getClientDocument(manager, d);
  }
  
  public org.ow2.bonita.facade.runtime.Document addDocumentVersion(final DocumentUUID documentUUID, final boolean isMajorVersion,
      final String fileName, final String mimeType, final byte[] content) throws DocumentationCreationException {
    return addDocumentVersion(documentUUID.getValue(), isMajorVersion, fileName, mimeType, content);
  }

  private org.ow2.bonita.facade.runtime.Document addDocumentVersion(final String documentId, final boolean isMajorVersion, final String fileName, final String mimeType, final byte[] content)
  throws DocumentationCreationException {
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    org.ow2.bonita.services.Document d = null;
    if (content != null) {
      d = manager.createVersion(documentId, isMajorVersion, fileName, mimeType, content);
    } else {
      d = manager.createVersion(documentId, isMajorVersion);
    }

    final ProcessInstanceUUID instanceUUID = d.getProcessInstanceUUID();
    if (instanceUUID != null) {
      InternalProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
      if (instance == null) {
        instance = EnvTool.getHistoryQueriers().getProcessInstance(instanceUUID);
      }
      if (instance != null) {
        instance.updateLastUpdateDate();
      }
    }
    return DocumentService.getClientDocument(manager, d);
  }

  public void deleteDocuments(final boolean allVersions, final DocumentUUID... documentUUIDs) throws DocumentNotFoundException {
    final DocumentationManager manager = EnvTool.getDocumentationManager();
    final Querier queriers = EnvTool.getAllQueriers();
    if (documentUUIDs != null) {
      org.ow2.bonita.services.Document doc;
      ProcessInstanceUUID processInstanceUUID;
      for (DocumentUUID documentUUID : documentUUIDs) {
        doc = manager.getDocument(documentUUID.getValue());
        processInstanceUUID = doc.getProcessInstanceUUID();
        manager.deleteDocument(documentUUID.getValue(), allVersions);
        if (processInstanceUUID != null) {
          final InternalProcessInstance instance = queriers.getProcessInstance(processInstanceUUID);
          if (instance != null) {
            instance.setNbOfAttachments(instance.getNbOfAttachments() - 1);
          } else {
            LOG.info("When deleting documents, cannot update the process instance because of its deletion");
          }
        }
      }
    }
  }

}
