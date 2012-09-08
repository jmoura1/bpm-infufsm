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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
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
import org.ow2.bonita.facade.internal.AbstractRemoteRuntimeAPI;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CatchingEventUUID;
import org.ow2.bonita.facade.uuid.DocumentUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.UserOwner;
import org.ow2.bonita.util.GroovyException;

/**
 * 
 * @author Elias Ricken de Medeiros, Matthieu Chaffotte
 *
 */
public abstract class AbstractRemoteRuntimeAPIImpl implements AbstractRemoteRuntimeAPI {

  private static final long serialVersionUID = 4856106680913239049L;

  protected Map<String, RuntimeAPI> apis = new HashMap<String, RuntimeAPI>();

  protected RuntimeAPI getAPI(final Map<String, String> options) {
    final String queryList = options.get(APIAccessor.QUERYLIST_OPTION);
    final String user = options.get(APIAccessor.USER_OPTION);
    final String domain = options.get(APIAccessor.DOMAIN_OPTION);
    UserOwner.setUser(user);
    DomainOwner.setDomain(domain);

    if (!apis.containsKey(queryList)) {
      putAPI(queryList);
    }
    return apis.get(queryList);
  }

  protected void putAPI(final String queryList) {
    apis.put(queryList, new StandardAPIAccessorImpl().getRuntimeAPI(queryList));
  }

  public void enableEventsInFailure(ProcessInstanceUUID instanceUUID, String activityName, final Map<String, String> options) {
    getAPI(options).enableEventsInFailure(instanceUUID, activityName);
  }

  public void enableEventsInFailure(ActivityInstanceUUID activityUUID, final Map<String, String> options) {
    getAPI(options).enableEventsInFailure(activityUUID);
  }

  public void enablePermanentEventInFailure(ActivityDefinitionUUID activityUUID, final Map<String, String> options)
  throws RemoteException {
    getAPI(options).enablePermanentEventInFailure(activityUUID);
  }

  public void startTask(final ActivityInstanceUUID taskUUID, final boolean assignTask, final Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException {
    getAPI(options).startTask(taskUUID, assignTask);
  }

  public void finishTask(final ActivityInstanceUUID taskUUID, final boolean taskAssign, final Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException {
    getAPI(options).finishTask(taskUUID, taskAssign);
  }

  public void resumeTask(final ActivityInstanceUUID taskUUID, final boolean taskAssign, final Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException {
    getAPI(options).resumeTask(taskUUID, taskAssign);
  }

  public void startActivity(ActivityInstanceUUID activityUUID, final Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    getAPI(options).startActivity(activityUUID);
  }

  public void suspendTask(final ActivityInstanceUUID taskUUID, final boolean assignTask, final Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException {
    getAPI(options).suspendTask(taskUUID, assignTask);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID, final Map<String, String> options)
  throws TaskNotFoundException {
    getAPI(options).assignTask(taskUUID);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID, final String actorId, final Map<String, String> options)
  throws TaskNotFoundException {
    getAPI(options).assignTask(taskUUID, actorId);
  }

  public void assignTask(final ActivityInstanceUUID taskUUID, final Set<String> candidates, final Map<String, String> options)
  throws TaskNotFoundException {
    getAPI(options).assignTask(taskUUID, candidates);
  }

  public void unassignTask(final ActivityInstanceUUID taskUUID, final Map<String, String> options)
  throws TaskNotFoundException {
    getAPI(options).unassignTask(taskUUID);
  }

  public void deleteAllProcessInstances(final ProcessDefinitionUUID processUUID, final Map<String, String> options)
  throws ProcessNotFoundException,
  UndeletableInstanceException {
    getAPI(options).deleteAllProcessInstances(processUUID);
  }

  public void executeTask(ActivityInstanceUUID taskUUID, boolean assignTask, final Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException, RemoteException {
    getAPI(options).executeTask(taskUUID, assignTask);
  }

  public void deleteProcessInstance(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, UndeletableInstanceException {
    getAPI(options).deleteProcessInstance(instanceUUID);
  }  

  public void cancelProcessInstance(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, UncancellableInstanceException {
    getAPI(options).cancelProcessInstance(instanceUUID);
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final Map<String, String> options)
  throws ProcessNotFoundException {
    return getAPI(options).instantiateProcess(processUUID);
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final ActivityDefinitionUUID activityUUID, final Map<String, String> options)
  throws ProcessNotFoundException {
    return getAPI(options).instantiateProcess(processUUID, activityUUID);
  }

  public ProcessInstanceUUID instantiateProcess(final ProcessDefinitionUUID processUUID, final Map<String, Object> variables, final Map<String, String> options)
  throws ProcessNotFoundException, VariableNotFoundException {
    return getAPI(options).instantiateProcess(processUUID, variables);
  }

  public void setProcessInstanceVariable(final ProcessInstanceUUID instanceUUID, final String variableId, final Object variableValue, final Map<String, String> options)
  throws InstanceNotFoundException, VariableNotFoundException {
    getAPI(options).setProcessInstanceVariable(instanceUUID, variableId, variableValue);
  }

  public void setProcessInstanceVariables(final ProcessInstanceUUID instanceUUID, final Map<String, Object> variables, final Map<String, String> options)
  throws InstanceNotFoundException, VariableNotFoundException {
    getAPI(options).setProcessInstanceVariables(instanceUUID, variables);
  }

  public void setActivityInstanceVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Object variableValue, final Map<String, String> options)
  throws ActivityNotFoundException, VariableNotFoundException {
    getAPI(options).setActivityInstanceVariable(activityUUID, variableId, variableValue);
  }

  public void setActivityInstanceVariables(
      ActivityInstanceUUID activityUUID,
      Map<String, Object> variables,
      final Map<String, String> options)
  throws ActivityNotFoundException, VariableNotFoundException {
    getAPI(options).setActivityInstanceVariables(activityUUID, variables);
  }

  public void setVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Object variableValue, final Map<String, String> options)
  throws ActivityNotFoundException, VariableNotFoundException {
    getAPI(options).setVariable(activityUUID, variableId, variableValue);
  }

  @Deprecated
  public void addComment(final ProcessInstanceUUID instanceUUID, final  ActivityInstanceUUID activityUUID, final String message, final String userId, final Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException {
    getAPI(options).addComment(instanceUUID, activityUUID, message, userId);
  }

  public void addComment(ProcessInstanceUUID instanceUUID, String message, String userId, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    getAPI(options).addComment(instanceUUID, message, userId);
  }

  public void addComment(ActivityInstanceUUID activityUUID, String message, String userId, final Map<String, String> options)
  throws ActivityNotFoundException, InstanceNotFoundException, RemoteException {
    getAPI(options).addComment(activityUUID, message, userId);
  }

  public void addProcessMetaData(ProcessDefinitionUUID uuid, String key, String value, final Map<String, String> options)
  throws ProcessNotFoundException {
    getAPI(options).addProcessMetaData(uuid, key, value);
  }

  public void deleteProcessMetaData(ProcessDefinitionUUID uuid, String key, final Map<String, String> options)
  throws ProcessNotFoundException {
    getAPI(options).deleteProcessMetaData(uuid, key);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID instanceUUID, boolean propagate, final Map<String, String> options) 
  throws InstanceNotFoundException, GroovyException {
    return getAPI(options).evaluateGroovyExpression(expression, instanceUUID, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID processInstanceUUID, Map<String, Object> context, boolean propagate, final Map<String, String> options)
  throws InstanceNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpression(expression, processInstanceUUID, context, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ActivityInstanceUUID activityUUID, boolean useActivityScope, boolean propagate, final Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException, GroovyException {
    return getAPI(options).evaluateGroovyExpression(expression, activityUUID, useActivityScope, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ActivityInstanceUUID activityInstanceUUID, Map<String, Object> context,
      boolean useActivityScope, boolean propagate, final Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpression(expression, activityInstanceUUID, context, useActivityScope, propagate);
  }

  public Object evaluateGroovyExpression(String expression, ProcessDefinitionUUID processDefinitionUUID, final Map<String, String> options)
  throws ProcessNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpression(expression, processDefinitionUUID);
  }

  public Object evaluateGroovyExpression(String expression, ProcessDefinitionUUID processDefinitionUUID, Map<String, Object> context, final Map<String, String> options)
  throws ProcessNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpression(expression, processDefinitionUUID, context);
  }

  public Map<String, Object> evaluateGroovyExpressions(Map<String, String> expressions,
      ActivityInstanceUUID activityUUID, final Map<String, Object> context, boolean useActivityScope, boolean propagate,
      final Map<String, String> options)
      throws InstanceNotFoundException, ActivityNotFoundException, GroovyException {
    return getAPI(options).evaluateGroovyExpressions(expressions, activityUUID, context, useActivityScope, propagate);
  }

  public Map<String, Object> evaluateGroovyExpressions(Map<String, String> expressions,
      ProcessDefinitionUUID processDefinitionUUID,
      Map<String, Object> context, final Map<String, String> options)
      throws InstanceNotFoundException, GroovyException, ProcessNotFoundException {
    return getAPI(options).evaluateGroovyExpressions(expressions, processDefinitionUUID, context);
  }
  
  public Map<String, Object> evaluateGroovyExpressions(final Map<String, String> expression,
      final ProcessInstanceUUID processInstanceUUID, final Map<String, Object> context,
      final boolean useInitialVariableValues, final boolean propagate, final Map<String, String> options)
        throws InstanceNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpressions(expression, processInstanceUUID, context, useInitialVariableValues, propagate);
  }

  public void addAttachment(ProcessInstanceUUID instanceUUID, String name, String fileName, byte[] value, final Map<String, String> options)
  throws RemoteException {
    getAPI(options).addAttachment(instanceUUID, name, fileName, value);
  }

  public void addAttachment(ProcessInstanceUUID instanceUUID, String name, String label, String description, String fileName, Map<String, String> metadata,
      byte[] value, final Map<String, String> options)
  throws RemoteException {
    getAPI(options).addAttachment(instanceUUID, name, label, description, fileName, metadata, value); 
  }

  public void addAttachments(Map<AttachmentInstance, byte[]> attachments, final Map<String, String> options)
  throws RemoteException {
    getAPI(options).addAttachments(attachments); 
  }

  public void removeAttachment(ProcessInstanceUUID instanceUUID, String name, Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    getAPI(options).removeAttachment(instanceUUID, name);
  }

  public void deleteEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID actiivtyUUID, final Map<String, String> options)
  throws RemoteException {
    getAPI(options).deleteEvents(eventName, toProcessName, toActivityName, actiivtyUUID);
  }

  public Map<String, Object> executeConnector(String connectorClassName,
      Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, final Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters, definitionUUID);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters, final Map<String, String> options)
  throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters);
  }

  public Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members, ProcessDefinitionUUID definitionUUID, final Map<String, String> options)
  throws RemoteException, Exception {
    return getAPI(options).executeFilter(connectorClassName, parameters, members, definitionUUID);
  }

  public Set<String> executeFilter(String connectorClassName, Map<String, Object[]> parameters, Set<String> members, final Map<String, String> options)
  throws RemoteException, Exception {
    return getAPI(options).executeFilter(connectorClassName, parameters, members);
  }

  public Set<String> executeRoleResolver(String connectorClassName,
      Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, final Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeRoleResolver(connectorClassName, parameters, definitionUUID);
  }

  public Set<String> executeRoleResolver(String connectorClassName,
      Map<String, Object[]> parameters, final Map<String, String> options) throws RemoteException, Exception {
    return getAPI(options).executeRoleResolver(connectorClassName, parameters);
  }

  public Map<String, Object> executeConnector(String connectorClassName,
      Map<String, Object[]> parameters, ClassLoader classLoader, final Map<String, String> options) throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters, classLoader);
  }

  public Set<String> executeFilter(String connectorClassName,
      Map<String, Object[]> parameters, Set<String> members, ClassLoader classLoader, final Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeFilter(connectorClassName, parameters, members, classLoader);
  }

  public Set<String> executeRoleResolver(String connectorClassName,
      Map<String, Object[]> parameters, ClassLoader classLoader, final Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeRoleResolver(connectorClassName, parameters, classLoader);
  }

  public void setActivityInstancePriority(ActivityInstanceUUID activityInstanceUUID, int priority, final Map<String, String> options)
  throws ActivityNotFoundException {
    getAPI(options).setActivityInstancePriority(activityInstanceUUID, priority);
  }

  public Map<String, Object> executeConnector(String connectorClassName,
      Map<String, Object[]> parameters, ProcessDefinitionUUID definitionUUID, Map<String, Object> context, final Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters, definitionUUID, context);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters,
      ProcessInstanceUUID processInstanceUUID, Map<String, Object> context, boolean useCurrentVariableValues, Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters, processInstanceUUID, context, useCurrentVariableValues);
  }

  public Map<String, Object> executeConnector(String connectorClassName, Map<String, Object[]> parameters,
      ActivityInstanceUUID activityInstanceUUID, Map<String, Object> context, boolean useCurrentVariableValues, Map<String, String> options)
      throws RemoteException, Exception {
    return getAPI(options).executeConnector(connectorClassName, parameters, activityInstanceUUID, context, useCurrentVariableValues);
  }

  public Object evaluateGroovyExpression(String expression, ProcessInstanceUUID processInstanceUUID,
      Map<String, Object> context, boolean useInitialVariableValues, boolean propagate, Map<String, String> options)
  throws InstanceNotFoundException, GroovyException, RemoteException {
    return getAPI(options).evaluateGroovyExpression(expression, processInstanceUUID, context, useInitialVariableValues, propagate);
  }

  public void skipTask(ActivityInstanceUUID taskUUID, Map<String, Object> variablesToUpdate, Map<String, String> options)
  throws TaskNotFoundException, IllegalTaskStateException, RemoteException {		
    getAPI(options).skipTask(taskUUID, variablesToUpdate);		
  }
  
  public void skip(ActivityInstanceUUID activityInstanceUUID,
      Map<String, Object> variablesToUpdate, Map<String, String> options)
      throws ActivityNotFoundException, IllegalTaskStateException,
      RemoteException {
    getAPI(options).skip(activityInstanceUUID, variablesToUpdate);
  }

  public void executeEvent(CatchingEventUUID eventUUID, Map<String, String> options)
  throws EventNotFoundException, RemoteException {
    getAPI(options).executeEvent(eventUUID);
  }

  public void deleteEvent(CatchingEventUUID eventUUID, Map<String, String> options)
  throws EventNotFoundException, RemoteException {
    getAPI(options).deleteEvent(eventUUID);
  }

  public void deleteEvents(Collection<CatchingEventUUID> eventUUIDs, Map<String, String> options)
  throws EventNotFoundException, RemoteException {
    getAPI(options).deleteEvents(eventUUIDs);
  }

  public void updateExpirationDate(CatchingEventUUID eventUUID, Date expiration, Map<String, String> options)
  throws EventNotFoundException, RemoteException {
    getAPI(options).updateExpirationDate(eventUUID, expiration);
  }

  public Object getModifiedJavaObject(ProcessDefinitionUUID processUUID, String variableExpression,
      Object variableValue, Object attributeValue,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getModifiedJavaObject(processUUID, variableExpression, variableValue, attributeValue);
  }

  public void updateActivityExpectedEndDate(ActivityInstanceUUID activityUUID, Date expectedEndDate,
      Map<String, String> options) throws RemoteException, ActivityNotFoundException {
    getAPI(options).updateActivityExpectedEndDate(activityUUID, expectedEndDate);
  }

  public Document createDocument(String name, ProcessDefinitionUUID processDefinitionUUID, String fileName, String mimeType, byte[] content, Map<String, String> options)
  throws RemoteException, DocumentationCreationException, ProcessNotFoundException {
    return getAPI(options).createDocument(name, processDefinitionUUID, fileName, mimeType, content);
  }
  
  public Document createDocument(String name, ProcessInstanceUUID instanceUUID, String fileName, String mimeType, byte[] content, Map<String, String> options)
  throws RemoteException, DocumentationCreationException, InstanceNotFoundException {
    return getAPI(options).createDocument(name, instanceUUID, fileName, mimeType, content);
  }

  public Document addDocumentVersion(DocumentUUID documentUUID, boolean isMajorVersion, String fileName, String mimeType,
      byte[] content, Map<String, String> options) throws RemoteException, DocumentationCreationException {
    return getAPI(options).addDocumentVersion(documentUUID, isMajorVersion, fileName, mimeType, content);
  }

  public void deleteDocuments(boolean allVersions, DocumentUUID[] documentUUIDs, Map<String, String> options)
      throws RemoteException, DocumentNotFoundException {
    getAPI(options).deleteDocuments(allVersions, documentUUIDs);
  }

}
