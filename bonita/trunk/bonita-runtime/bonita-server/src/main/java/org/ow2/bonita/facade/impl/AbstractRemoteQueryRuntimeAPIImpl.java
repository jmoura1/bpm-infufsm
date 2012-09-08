/**
 * Copyright (C) 2010-2011 BonitaSoft S.A.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.EventNotFoundException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.TaskNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.internal.AbstractRemoteQueryRuntimeAPI;
import org.ow2.bonita.facade.paging.ActivityInstanceCriterion;
import org.ow2.bonita.facade.paging.ProcessInstanceCriterion;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.CatchingEvent;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CatchingEventUUID;
import org.ow2.bonita.facade.uuid.DocumentUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.PasswordOwner;
import org.ow2.bonita.identity.auth.RESTUserOwner;
import org.ow2.bonita.identity.auth.UserOwner;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.search.DocumentResult;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.SearchQueryBuilder;

/**
 * 
 * @author Elias Ricken de Medeiros, Nicolas Chabanoles, Matthieu Chaffotte
 *
 */
public abstract class AbstractRemoteQueryRuntimeAPIImpl implements AbstractRemoteQueryRuntimeAPI {

  protected Map<String, QueryRuntimeAPI> apis = new HashMap<String, QueryRuntimeAPI>();

  protected QueryRuntimeAPI getAPI(final Map<String, String> options) {
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
      putAPI(queryList);
    }
    return apis.get(queryList);
  }

  protected void putAPI(final String queryList) {
    apis.put(queryList, new StandardAPIAccessorImpl().getQueryRuntimeAPI(queryList));
  }

  public Set<String> getTaskCandidates(ActivityInstanceUUID taskUUID, Map<String, String> options)
  throws RemoteException, TaskNotFoundException {
    return getAPI(options).getTaskCandidates(taskUUID);
  }

  public Map<ActivityInstanceUUID, Set<String>> getTaskCandidates(Set<ActivityInstanceUUID> taskUUIDs, Map<String, String> options)
  throws RemoteException, TaskNotFoundException {
    return getAPI(options).getTaskCandidates(taskUUIDs);
  }

  public List<LightProcessInstance> getLightParentProcessInstances(int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstances(fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentProcessInstances(
      int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getLightParentProcessInstances(fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstances(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstances(processUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesExcept(
      Set<ProcessDefinitionUUID> exceptions, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesExcept(exceptions, fromIndex, pageSize, pagingCriterion);
  }

  public int getNumberOfParentProcessInstances(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstances();
  }
  
  public int getNumberOfParentProcessInstances(Set<ProcessDefinitionUUID> processDefinitionUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstances(processDefinitionUUIDs);
  }
  
  public int getNumberOfParentProcessInstancesExcept(Set<ProcessDefinitionUUID> exceptions, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesExcept(exceptions);
  }

  public int getNumberOfProcessInstances(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfProcessInstances();
  }

  public List<LightActivityInstance> getLightActivityInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightActivityInstancesFromRoot(rootInstanceUUID);
  }

  public Map<ProcessInstanceUUID, List<LightActivityInstance>> getLightActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightActivityInstancesFromRoot(rootInstanceUUIDs);
  }

  public List<LightTaskInstance> getLightTaskInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightTaskInstancesFromRoot(rootInstanceUUID);
  }

  public Map<ProcessInstanceUUID, List<LightTaskInstance>> getLightTaskInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightTaskInstancesFromRoot(rootInstanceUUIDs);
  }

  public ActivityInstanceUUID getOneTask(ProcessDefinitionUUID processUUID, ActivityState taskState, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getOneTask(processUUID, taskState);
  }

  public ActivityInstanceUUID getOneTask(ProcessInstanceUUID instanceUUID, ActivityState taskState, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getOneTask(instanceUUID, taskState);
  }

  public ActivityInstanceUUID getOneTask(ActivityState taskState, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getOneTask(taskState);
  }

  public Set<ActivityInstance> getActivityInstances(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getActivityInstances(instanceUUID);
  }

  public Set<ActivityInstance> getActivityInstances(final ProcessInstanceUUID instanceUUID, final String activityId, final Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException, RemoteException {
    return getAPI(options).getActivityInstances(instanceUUID, activityId);
  }

  public ActivityInstance getActivityInstance(final ActivityInstanceUUID activityUUID, final Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    return getAPI(options).getActivityInstance(activityUUID);
  }

  public ProcessInstance getProcessInstance(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getProcessInstance(instanceUUID);
  }

  public Set<ProcessInstance> getProcessInstances(final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getProcessInstances();
  }

  public Boolean canExecuteTask(ActivityInstanceUUID taskUUID, Map<String, String> options)
  throws TaskNotFoundException, RemoteException {
    return getAPI(options).canExecuteTask(taskUUID);
  }

  public Set<ProcessInstance> getProcessInstances(final ProcessDefinitionUUID processUUID, final Map<String, String> options)
  throws ProcessNotFoundException, RemoteException {
    return getAPI(options).getProcessInstances(processUUID);
  }

  public TaskInstance getTask(final ActivityInstanceUUID taskUUID, final Map<String, String> options)
  throws TaskNotFoundException, RemoteException {
    return getAPI(options).getTask(taskUUID);
  }

  public Collection<TaskInstance> getTaskList(final ProcessInstanceUUID instanceUUID, final ActivityState taskState, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getTaskList(instanceUUID, taskState);
  }

  public Collection<TaskInstance> getTaskList(final ProcessInstanceUUID instanceUUID, final String userId, final ActivityState taskState, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getTaskList(instanceUUID, userId, taskState);
  }

  public Collection<TaskInstance> getTaskList(final ActivityState taskState, final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getTaskList(taskState);
  }

  public Collection<TaskInstance> getTaskList(final String userId, final ActivityState taskState, final Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getTaskList(userId, taskState);
  }

  public Set<TaskInstance> getTasks(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getTasks(instanceUUID);
  }

  public Object getActivityInstanceVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Map<String, String> options)
  throws ActivityNotFoundException, VariableNotFoundException, RemoteException {
    return getAPI(options).getActivityInstanceVariable(activityUUID, variableId);
  }

  public ActivityState getActivityInstanceState(ActivityInstanceUUID activityUUID, Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    return getAPI(options).getActivityInstanceState(activityUUID);
  }

  public Object getProcessInstanceVariable(final ProcessInstanceUUID instanceUUID, final String variableId, final Map<String, String> options)
  throws InstanceNotFoundException, VariableNotFoundException, RemoteException {
    return getAPI(options).getProcessInstanceVariable(instanceUUID, variableId);
  }

  public Map<String, Object> getActivityInstanceVariables(final ActivityInstanceUUID activityUUID, final Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    return getAPI(options).getActivityInstanceVariables(activityUUID);
  }

  public Map<String, Object> getProcessInstanceVariables(final ProcessInstanceUUID instanceUUID, final Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getProcessInstanceVariables(instanceUUID);
  }

  public Map<String, Object> getProcessInstanceVariables(ProcessInstanceUUID instanceUUID, Date maxDate, final Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    return getAPI(options).getProcessInstanceVariables(instanceUUID, maxDate);
  }

  public Object getVariable(final ActivityInstanceUUID activityUUID, final String variableId, final Map<String, String> options)
  throws ActivityNotFoundException, VariableNotFoundException, RemoteException {
    return getAPI(options).getVariable(activityUUID, variableId);
  }

  public Map<String, Object> getVariables(final ActivityInstanceUUID activityUUID, final Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException, RemoteException {
    return getAPI(options).getVariables(activityUUID);
  }

  public List<Comment> getCommentFeed(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getCommentFeed(instanceUUID);
  }

  public Set<ProcessInstance> getUserInstances(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getUserInstances();
  }

  @Deprecated
  public Set<String> getAttachmentNames(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getAttachmentNames(instanceUUID);
  }

  @Deprecated
  public List<AttachmentInstance> getAttachments(ProcessInstanceUUID instanceUUID, String attachmentName, Map<String, String> options) 
  throws RemoteException {
    return getAPI(options).getAttachments(instanceUUID, attachmentName);
  }

  @Deprecated
  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLastAttachment(instanceUUID, attachmentName);
  }

  @Deprecated
  public Collection<AttachmentInstance> getLastAttachments(ProcessInstanceUUID instanceUUID, Set<String> attachmentNames, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLastAttachments(instanceUUID, attachmentNames);
  }

  @Deprecated
  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName, ActivityInstanceUUID activityUUID, Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    return getAPI(options).getLastAttachment(instanceUUID, attachmentName, activityUUID);
  }

  @Deprecated
  public AttachmentInstance getLastAttachment(ProcessInstanceUUID instanceUUID, String attachmentName, Date date, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLastAttachment(instanceUUID, attachmentName, date);
  }

  @Deprecated
  public Collection<AttachmentInstance> getLastAttachments(ProcessInstanceUUID instanceUUID, String regex, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLastAttachments(instanceUUID, regex);
  }

  @Deprecated
  public byte[] getAttachmentValue(AttachmentInstance attachmentInstance, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getAttachmentValue(attachmentInstance);
  }

  public Set<LightProcessInstance> getLightUserInstances(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightUserInstances();
  }

  public List<LightProcessInstance> getLightParentUserInstances(int startingIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentUserInstances(startingIndex, pageSize);
  }

  public LightProcessInstance getLightProcessInstance(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightProcessInstance(instanceUUID);
  }

  public Set<LightProcessInstance> getLightProcessInstances(ProcessDefinitionUUID processUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightProcessInstances(processUUID);
  }

  public List<LightProcessInstance> getLightProcessInstances(
      Set<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightProcessInstances(instanceUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public Set<LightProcessInstance> getLightWeightProcessInstances(Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightWeightProcessInstances(processUUIDs);
  }

  public Set<LightProcessInstance> getLightProcessInstances(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightProcessInstances();
  }

  public List<LightProcessInstance> getLightProcessInstances(int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightProcessInstances(fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getLightParentUserInstances(fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightProcessInstances(int fromIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion,
      Map<String, String> options) throws RemoteException {		
    return getAPI(options).getLightProcessInstances(fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays, int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, remainingDays, fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String userId, int remainingDays, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
        userId, remainingDays, fromIndex, pageSize, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(String userId, int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasks(userId, fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasks(userId, fromIndex, pageSize
        , pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(
      String userId, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize, pagingCriterion);
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightActivityInstances(instanceUUID);
  }

  public Set<LightTaskInstance> getLightTasks(ProcessInstanceUUID instanceUUID,Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightTasks(instanceUUID);
  }

  public Set<LightTaskInstance> getLightTasks(ProcessInstanceUUID instanceUUID, Set<String> taskNames, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightTasks(instanceUUID, taskNames);
  }

  public List<Comment> getActivityInstanceCommentFeed(ActivityInstanceUUID activityUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getActivityInstanceCommentFeed(activityUUID);
  }

  public List<Comment> getProcessInstanceCommentFeed(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getProcessInstanceCommentFeed(instanceUUID);
  }

  public int getNumberOfActivityInstanceComments(ActivityInstanceUUID activityUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getNumberOfActivityInstanceComments(activityUUID);
  }

  public Map<ActivityInstanceUUID, Integer> getNumberOfActivityInstanceComments(Set<ActivityInstanceUUID> activityUUIDs, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getNumberOfActivityInstanceComments(activityUUIDs);
  }

  public int getNumberOfComments(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getNumberOfComments(instanceUUID);
  }

  public int getNumberOfProcessInstanceComments(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getNumberOfProcessInstanceComments(instanceUUID);
  }

  public LightTaskInstance getLightTaskInstance(ActivityInstanceUUID taskUUID, Map<String, String> options)
  throws TaskNotFoundException, RemoteException {
    return getAPI(options).getLightTaskInstance(taskUUID);
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, String activityName, Map<String, String> options)
  throws InstanceNotFoundException, ActivityNotFoundException, RemoteException {
    return getAPI(options).getLightActivityInstances(instanceUUID, activityName);
  }

  public Set<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, String activityName,
      String iterationId, Map<String, String> options) throws RemoteException {
    return getAPI(options).getLightActivityInstances(instanceUUID, activityName, iterationId);
  }

  public LightActivityInstance getLightActivityInstance(ActivityInstanceUUID activityInstanceUUID, Map<String, String> options)
  throws ActivityNotFoundException, RemoteException {
    return getAPI(options).getLightActivityInstance(activityInstanceUUID);
  }

  public Collection<LightTaskInstance> getLightTaskList(ActivityState taskState, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightTaskList(taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(ProcessInstanceUUID instanceUUID, ActivityState taskState, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightTaskList(instanceUUID, taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(ProcessInstanceUUID instanceUUID, Collection<ActivityState> taskStates, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightTaskList(instanceUUID, taskStates);
  }

  public Collection<LightTaskInstance> getLightTaskList(ProcessInstanceUUID instanceUUID, String userId, ActivityState taskState, Map<String, String> options)
  throws InstanceNotFoundException, RemoteException {
    return getAPI(options).getLightTaskList(instanceUUID, userId, taskState);
  }

  public Collection<LightTaskInstance> getLightTaskList(String userId, ActivityState taskState, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightTaskList(userId, taskState);
  }

  public Map<ProcessInstanceUUID, List<LightActivityInstance>> getLightActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, ActivityState state, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightActivityInstancesFromRoot(rootInstanceUUIDs, state);
  }

  public Map<ProcessInstanceUUID, LightActivityInstance> getLightLastUpdatedActivityInstanceFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, boolean considerSystemTaks, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightLastUpdatedActivityInstanceFromRoot(rootInstanceUUIDs, considerSystemTaks);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUser(userId);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, remainingDays);
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithOverdueTasks(userId);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUser(userId);
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithStartedBy(userId);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(String userId, String category, Map<String, String> options)
  throws RemoteException{
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(userId, category);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(String userId, Set<ProcessDefinitionUUID> processes, int fromIndex, int pageSize, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize);
  }

  public <T> List<T> search(SearchQueryBuilder query, int firstResult, int maxResults, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).search(query, firstResult, maxResults);
  }

  public int search(SearchQueryBuilder query, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).search(query);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUser(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUser(username, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processes, Map<String, String> options) throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,remainingDays,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String username, int remainingDays, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(username,
        remainingDays, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(String userId, int remainingDays, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(userId,remainingDays,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(
      String username, int remainingDays, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(
        username, remainingDays, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserExcept(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserExcept(userId,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithActiveUserExcept(
      String userId, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithActiveUserExcept(userId, fromIndex,
        pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize, processUUIDs);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUser(
      String userId, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUser(
        userId, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUserExcept(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUserExcept(userId,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithInvolvedUserExcept(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithInvolvedUserExcept(
        username, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasks(userId,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasks(
      String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasks(
        username, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasksExcept(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasksExcept(userId,fromIndex,pageSize,processes);
  }

  public List<LightProcessInstance> getLightParentProcessInstancesWithOverdueTasksExcept(String username, int fromIndex, int pageSize,
      Set<ProcessDefinitionUUID> processUUIDs, ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentProcessInstancesWithOverdueTasksExcept(username, fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, int remainingDays, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,remainingDays,processes);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(String userId, int remainingDays, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateExcept(userId,remainingDays,processes);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndProcessUUIDs(String userId, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUser(userId,processes);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserExcept(String userId, Set<ProcessDefinitionUUID> processes, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUserExcept(userId,processes);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUser(userId,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(String userId, String category, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUserAndCategory(userId,category,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserAndCategoryExcept(String userId, String category, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUserAndCategoryExcept(userId,category,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUserExcept(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithInvolvedUserExcept(userId,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithOverdueTasks(userId,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasksExcept(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithOverdueTasksExcept(userId,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithStartedBy(userId,processUUIDs);
  }

  public Integer getNumberOfParentProcessInstancesWithStartedByExcept(String userId, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithStartedByExcept(userId,processUUIDs);
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentUserInstances(fromIndex, pageSize, processUUIDs);
  }

  public List<LightProcessInstance> getLightParentUserInstances(int fromIndex,
      int pageSize, Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentUserInstances(fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public List<LightProcessInstance> getLightParentUserInstancesExcept(int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getLightParentUserInstancesExcept(fromIndex, pageSize, processUUIDs);
  }

  public List<LightProcessInstance> getLightParentUserInstancesExcept(
      int fromIndex, int pageSize, Set<ProcessDefinitionUUID> processUUIDs,
      ProcessInstanceCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightParentUserInstancesExcept(fromIndex, pageSize, processUUIDs, pagingCriterion);
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String username, Set<ProcessDefinitionUUID> processUUIDs, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getNumberOfParentProcessInstancesWithActiveUser(username, processUUIDs);
  }

  public Set<String> getActiveUsersOfProcessInstance(ProcessInstanceUUID uuid, Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    return getAPI(options).getActiveUsersOfProcessInstance(uuid);
  }

  public Map<ProcessInstanceUUID, Set<String>> getActiveUsersOfProcessInstances(Set<ProcessInstanceUUID> instanceUUIDs, Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    return getAPI(options).getActiveUsersOfProcessInstances(instanceUUIDs);
  }

  public List<LightActivityInstance> getLightActivityInstances(ProcessInstanceUUID instanceUUID, int fromIdex, int pageSize,
      ActivityInstanceCriterion pagingCriterion, Map<String, String> options)
      throws InstanceNotFoundException, RemoteException {	
    return getAPI(options).getLightActivityInstances(instanceUUID, fromIdex, pageSize, pagingCriterion);
  }

  public CatchingEvent getEvent(CatchingEventUUID eventUUID, Map<String, String> options)
  throws RemoteException, EventNotFoundException {
    return getAPI(options).getEvent(eventUUID);
  }

  public Set<CatchingEvent> getEvents(Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getEvents();
  }

  public Set<CatchingEvent> getEvents(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getEvents(instanceUUID);
  }

  public Set<CatchingEvent> getEvents(ActivityInstanceUUID activityUUID, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).getEvents(activityUUID);
  }

  public byte[] getDocumentContent(DocumentUUID documentUUID, Map<String, String> options)
  throws RemoteException, DocumentNotFoundException {
    return getAPI(options).getDocumentContent(documentUUID);
  }

  public DocumentResult searchDocuments(DocumentSearchBuilder builder, int fromResult, int maxResults, Map<String, String> options)
  throws RemoteException {
    return getAPI(options).searchDocuments(builder, fromResult, maxResults);
  }

  public Document getDocument(DocumentUUID documentUUID, Map<String, String> options)
  throws RemoteException, DocumentNotFoundException {
    return getAPI(options).getDocument(documentUUID);
  }

  public List<Document> getDocuments(List<DocumentUUID> documentUUIDs, Map<String, String> options)
  throws RemoteException, DocumentNotFoundException {
    return getAPI(options).getDocuments(documentUUIDs);
  }

  public List<Document> getDocumentVersions(DocumentUUID documentUUID, Map<String, String> options)
  throws RemoteException, DocumentNotFoundException {
    return getAPI(options).getDocumentVersions(documentUUID);
  }

  @Override
  public Set<String> getInvolvedUsersOfProcessInstance(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    return getAPI(options).getInvolvedUsersOfProcessInstance(instanceUUID);
  }

  @Override
  public Set<ProcessInstanceUUID> getChildrenInstanceUUIDsOfProcessInstance(ProcessInstanceUUID instanceUUID, Map<String, String> options)
  throws RemoteException, InstanceNotFoundException {
    return getAPI(options).getChildrenInstanceUUIDsOfProcessInstance(instanceUUID);
  }
}
