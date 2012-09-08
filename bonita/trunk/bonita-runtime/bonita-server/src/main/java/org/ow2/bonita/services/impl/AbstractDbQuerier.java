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
 * Modified by Charles Souillard, Matthieu Chaffotte, Elias Ricken de Medeiros - BonitaSoft S.A.
 **/
package org.ow2.bonita.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.paging.ActivityInstanceCriterion;
import org.ow2.bonita.facade.paging.ProcessDefinitionCriterion;
import org.ow2.bonita.facade.paging.ProcessInstanceCriterion;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.impl.CategoryImpl;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CategoryUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.persistence.QuerierDbSession;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.search.SearchQueryBuilder;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

public class AbstractDbQuerier implements Querier {

  private String persistenceServiceName;

  public AbstractDbQuerier(String persistenceServiceName) {
    super();
    this.persistenceServiceName = persistenceServiceName;
  }

  protected String getPersistenceServiceName() {
    return persistenceServiceName;
  }
  
  protected QuerierDbSession getDbSession() {
    return EnvTool.getQuerierDbSession(persistenceServiceName);
  }

  public int getNumberOfProcesses() {
    return getDbSession().getNumberOfProcesses();
  }
  
  public int getNumberOfParentProcessInstances() {
    return getDbSession().getNumberOfParentProcessInstances();
  }

  public int getNumberOfProcessInstances() {
    return getDbSession().getNumberOfProcessInstances();
  }
  
  public InternalActivityDefinition getActivity(ActivityDefinitionUUID activityDefinitionUUID) {
    Misc.checkArgsNotNull(activityDefinitionUUID);
    return getDbSession().getActivityDefinition(activityDefinitionUUID);
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName) {
    Misc.checkArgsNotNull(instanceUUID, activityName);
    Set<InternalActivityInstance> activityInstances = getDbSession().getActivityInstances(instanceUUID, activityName);
    if (activityInstances != null) {
      return activityInstances;
    }
    return Collections.emptySet();
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName, String iterationId) {
    Misc.checkArgsNotNull(instanceUUID, activityName, iterationId);
    Set<InternalActivityInstance> activityInstances = getDbSession().getActivityInstances(instanceUUID, activityName, iterationId);
    if (activityInstances != null) {
      return activityInstances;
    }
    return Collections.emptySet();
  }

  public InternalActivityInstance getActivityInstance(ProcessInstanceUUID instanceUUID, String activityName, String iterationId, String activityInstanceId, String loopId) {
    return getDbSession().getActivityInstance(instanceUUID, activityName, iterationId, activityInstanceId, loopId);
  }

  public ActivityState getActivityInstanceState(ActivityInstanceUUID activityInstanceUUID) {
    Misc.checkArgsNotNull(activityInstanceUUID);
    return getDbSession().getActivityInstanceState(activityInstanceUUID);
  }

  public InternalActivityInstance getActivityInstance(ActivityInstanceUUID activityInstanceUUID) {
    Misc.checkArgsNotNull(activityInstanceUUID);
    return getDbSession().getActivityInstance(activityInstanceUUID);
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID) {
    Misc.checkArgsNotNull(instanceUUID);
    Set<InternalActivityInstance> activityInstances = getDbSession().getActivityInstances(instanceUUID);
    if (activityInstances != null) {
      return activityInstances;
    }
    return new HashSet<InternalActivityInstance>();
  }
  
  public List<InternalActivityInstance> getActivityInstances(
			ProcessInstanceUUID instanceUUID, int fromIndex, int pageSize,
			ActivityInstanceCriterion pagingCriterion) {
  	Misc.checkArgsNotNull(instanceUUID);
    List<InternalActivityInstance> activityInstances = getDbSession().getActivityInstances(instanceUUID, fromIndex, pageSize, pagingCriterion);
    if (activityInstances != null) {
      return activityInstances;
    }
    return new ArrayList<InternalActivityInstance>();
	}

  public List<InternalActivityInstance> getActivityInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID) {
    Misc.checkArgsNotNull(rootInstanceUUID);
    return getDbSession().getActivityInstancesFromRoot(rootInstanceUUID);
  }
  
  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs) {
    Misc.checkArgsNotNull(rootInstanceUUIDs);
    return getDbSession().getActivityInstancesFromRoot(rootInstanceUUIDs);
  }

  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, ActivityState state) {
    Misc.checkArgsNotNull(rootInstanceUUIDs, state);
    return getDbSession().getActivityInstancesFromRoot(rootInstanceUUIDs, state);
  }

  public Map<ProcessInstanceUUID, InternalActivityInstance> getLastUpdatedActivityInstanceFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, boolean considerSystemTaks) {
    Misc.checkArgsNotNull(rootInstanceUUIDs);
    return getDbSession().getLastUpdatedActivityInstanceFromRoot(rootInstanceUUIDs, considerSystemTaks);
  }
  
  public InternalProcessInstance getProcessInstance(ProcessInstanceUUID instanceUUID) {
    Misc.checkArgsNotNull(instanceUUID);
    final InternalProcessInstance instance = getDbSession().getProcessInstance(instanceUUID);
    return instance;
  }

  public Set<ProcessInstanceUUID> getParentInstancesUUIDs() {
    Set<InternalProcessInstance> parentInstances = getParentInstances();
    Set<ProcessInstanceUUID> result = new HashSet<ProcessInstanceUUID>();
    for (InternalProcessInstance instance : parentInstances) {
      result.add(instance.getUUID());
    }
    return result;
  }
  
  public Set<InternalProcessInstance> getProcessInstances() {
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstances();
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }   

  public List<InternalProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize) {
    List<InternalProcessInstance> dbInstances = getDbSession().getProcessInstances(instanceUUIDs, fromIndex, pageSize);
    if (dbInstances == null) {
      return Collections.emptyList();
    }
    return dbInstances;
  }
  
  public List<InternalProcessInstance> getProcessInstancesWithInstanceUUIDs(
			Set<ProcessInstanceUUID> instanceUUIDs, int fromIndex,
			int pageSize, ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> dbInstances = getDbSession().getProcessInstancesWithInstanceUUIDs(
  			instanceUUIDs, fromIndex, pageSize, pagingCriterion);
    if (dbInstances == null) {
      return Collections.emptyList();
    }
    return dbInstances;
	}
  
  public List<InternalProcessInstance> getMostRecentProcessInstances(int maxResults, long time) {
  	return getDbSession().getMostRecentProcessInstances(maxResults, time);
  }
  
	public List<InternalProcessInstance> getMostRecentProcessInstances(
			int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
		return getDbSession().getMostRecentProcessInstances(maxResults, time, pagingCriterion);
	}
  
  public List<InternalProcessInstance> getMostRecentParentProcessInstances(int maxResults, long time) {
  	return getDbSession().getMostRecentParentProcessInstances(maxResults, time);
  }
  
  public List<InternalProcessInstance> getMostRecentParentProcessInstances(
			int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
  	return getDbSession().getMostRecentParentProcessInstances(maxResults, time, pagingCriterion);
	}
  
  public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time) {
  	return getDbSession().getMostRecentMatchingProcessInstances(instanceUUIDs, maxResults, time);
  }
  
	public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(
			Set<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time,
			ProcessInstanceCriterion pagingCriterion) {
		return getDbSession().getMostRecentMatchingProcessInstances(instanceUUIDs, maxResults, time, pagingCriterion);
	}
  
  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, int maxResults, long time) {
  	return getDbSession().getMostRecentProcessesProcessInstances(definitionUUIDs, maxResults, time);
  }
  
  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, 
  		int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
  	return getDbSession().getMostRecentProcessesProcessInstances(definitionUUIDs, maxResults, time, pagingCriterion);
  }
  
  public List<InternalProcessDefinition> getProcesses(int fromIndex, int pageSize) {
    return getDbSession().getProcesses(fromIndex, pageSize);
  }

  public List<InternalProcessDefinition> getProcesses(int fromIndex,
      int pageSize, ProcessDefinitionCriterion pagingCriterion) {
    return getDbSession().getProcesses(fromIndex, pageSize, pagingCriterion);
  }
  
  public List<InternalProcessInstance> getProcessInstances(int fromIndex, int pageSize) {
    return getDbSession().getProcessInstances(fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getProcessInstances(int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    return getDbSession().getProcessInstances(fromIndex, pageSize, pagingCriterion);
  }
  
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex, int pageSize) {
    return getDbSession().getParentProcessInstances(fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex,
			int pageSize, ProcessInstanceCriterion paginCriterion) {
  	return getDbSession().getParentProcessInstances(fromIndex, pageSize, paginCriterion);		
	}

  public List<InternalProcessInstance> getParentProcessInstances(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    return getDbSession().getParentProcessInstances(processUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public List<InternalProcessInstance> getParentProcessInstancesExcept(
      Set<ProcessDefinitionUUID> exceptions, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    return getDbSession().getParentProcessInstancesExcept(exceptions, fromIndex, pageSize, pagingCriterion);
  }
  
  public Set<InternalProcessInstance> getParentInstances() {
    Set<InternalProcessInstance> dbInstances = getDbSession().getParentInstances();
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs) {
    if (instanceUUIDs == null || instanceUUIDs.isEmpty()) {
      return Collections.emptySet();
    }
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstances(instanceUUIDs);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstances(ProcessDefinitionUUID processUUID, InstanceState instanceState) {
    Misc.checkArgsNotNull(processUUID);
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstances(processUUID, instanceState);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  private Execution getExecOnNode(final Execution exec,
      final ActivityInstanceUUID activityInstanceUUID) {
    Misc.checkArgsNotNull(exec, activityInstanceUUID);
    if (exec.getExecutions() == null || exec.getExecutions().isEmpty()) {
      if (exec.getNode() != null && exec.getActivityInstanceUUID() != null
          && exec.getActivityInstanceUUID().equals(activityInstanceUUID)) {
        return exec;
      }
    } else {
      for (final Execution child : exec.getExecutions()) {
        final Execution found = getExecOnNode(
            (Execution) child, activityInstanceUUID);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  public Execution getExecutionOnActivity(
      final ProcessInstanceUUID instanceUUID,
      final ActivityInstanceUUID activityUUID) {
    Misc.checkArgsNotNull(instanceUUID, activityUUID);

    final InternalProcessInstance instance = (InternalProcessInstance) getProcessInstance(instanceUUID);
    if (instance != null) {
      return getExecOnNode(instance.getRootExecution(), activityUUID);
    }
    return getDbSession().getExecutionPointingOnNode(activityUUID);
  }
  
  public Set<Execution> getExecutions(ProcessInstanceUUID instanceUUID) {
  	Misc.checkArgsNotNull(instanceUUID);
  	return getDbSession().getExecutions(instanceUUID);
  }

  public Execution getExecutionWithEventUUID(final String eventUUID) {
    Misc.checkArgsNotNull(eventUUID);
    return getDbSession().getExecutionWithEventUUID(eventUUID);
  }

  public Set<InternalProcessInstance> getUserInstances(String userId) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getUserInstances(userId);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }
  
  public Set<InternalProcessInstance> getUserInstances(String userId, Date minStartDate) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getUserInstances(userId, minStartDate);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }
  
  public Set<InternalProcessInstance> getUserParentInstances(String userId, Date minStartDate) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getUserParentInstances(userId, minStartDate);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }
  
  public Set<InternalProcessInstance> getUserInstancesExcept(String userId, Set<ProcessInstanceUUID> myCases) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getUserInstancesExcept(userId, myCases);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstancesWithTaskState(activityStates);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstancesWithInstanceStates(instanceStates);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstances(ProcessDefinitionUUID processUUID) {
    Misc.checkArgsNotNull(processUUID);
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstances(processUUID);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public InternalProcessDefinition getProcess(String processId, String version) {
    Misc.checkArgsNotNull(processId, version);
    return getDbSession().getProcess(processId, version);
  }

  public InternalProcessDefinition getProcess(ProcessDefinitionUUID processUUID) {
    Misc.checkArgsNotNull(processUUID);
    return getDbSession().getProcess(processUUID);
  }

  public Set<InternalProcessDefinition> getProcesses() {
    Set<InternalProcessDefinition> processes = getDbSession().getProcesses();
    if (processes == null) {
      return Collections.emptySet();
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(String processId) {
    Misc.checkArgsNotNull(processId);
    Set<InternalProcessDefinition> processes = getDbSession().getProcesses(processId);
    if (processes == null) {
      return Collections.emptySet();
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(ProcessState processState) {
    Misc.checkArgsNotNull(processState);
    Set<InternalProcessDefinition> processes = getDbSession().getProcesses(processState);
    if (processes == null) {
      return Collections.emptySet();
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(String processId, ProcessState processState) {
    Misc.checkArgsNotNull(processId, processState);
    Set<InternalProcessDefinition> processes = getDbSession().getProcesses(processId, processState);
    if (processes == null) {
      return Collections.emptySet();
    }
    return processes;
  }

  public TaskInstance getTaskInstance(ActivityInstanceUUID taskUUID) {
    Misc.checkArgsNotNull(taskUUID);
    return getDbSession().getTaskInstance(taskUUID);
  }

  public Set<TaskInstance> getTaskInstances(ProcessInstanceUUID instanceUUID) {
    Misc.checkArgsNotNull(instanceUUID);
    Set<TaskInstance> tasks = getDbSession().getTaskInstances(instanceUUID);
    if (tasks == null) {
      return Collections.emptySet();
    }
    return tasks;
  }

  public Set<TaskInstance> getTaskInstances(ProcessInstanceUUID instanceUUID, Set<String> taskNames) {
    Misc.checkArgsNotNull(instanceUUID);
    Set<TaskInstance> tasks = getDbSession().getTaskInstances(instanceUUID, taskNames);
    if (tasks == null) {
      return Collections.emptySet();
    }
    return tasks;
  }

  public Set<TaskInstance> getUserInstanceTasks(String userId, ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    Misc.checkArgsNotNull(userId, instanceUUID, taskState);
    Set<TaskInstance> tasks = getDbSession().getUserInstanceTasks(userId, instanceUUID, taskState);
    if (tasks == null) {
      return Collections.emptySet();
    }
    return tasks;
  }
  
  public TaskInstance getOneTask(String userId, ProcessDefinitionUUID processUUID, ActivityState taskState) {
    return getDbSession().getOneTask(userId, processUUID, taskState);
  }
  
  public TaskInstance getOneTask(String userId, ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    return getDbSession().getOneTask(userId, instanceUUID, taskState);
  }
  
  public TaskInstance getOneTask(String userId, ActivityState taskState) {
    return getDbSession().getOneTask(userId, taskState);
  }

  public Set<TaskInstance> getUserTasks(String userId, ActivityState taskState) {
    Collection<ActivityState> taskStates = new HashSet<ActivityState>();
    taskStates.add(taskState);
    return getUserTasks(userId, taskStates);
  }

  public Set<TaskInstance> getUserTasks(String userId, Collection<ActivityState> taskStates) {
    Misc.checkArgsNotNull(userId, taskStates);
    Set<TaskInstance> tasks = getDbSession().getUserTasks(userId, taskStates);
    if (tasks == null) {
      return Collections.emptySet();
    }
    return tasks;
  }

  /*
   *SPECIFIC
   */
  public String getLastProcessVersion(String processName) {
    Misc.checkArgsNotNull(processName);
    return getDbSession().getLastProcessVersion(processName);
  }

  public long getLastProcessInstanceNb(ProcessDefinitionUUID processUUID) {
    Misc.checkArgsNotNull(processUUID);
    return getDbSession().getLastProcessInstanceNb(processUUID);
  }

  public InternalProcessDefinition getLastDeployedProcess(String processId, ProcessState processState) {
    Misc.checkArgsNotNull(processId, processState);
    return getDbSession().getLastProcess(processId, processState);
  }

  public List<Integer> getNumberOfFinishedCasesPerDay(Date since, Date to) {
    return getDbSession().getNumberOfFinishedCasesPerDay(since, to);
  }

  public List<Integer> getNumberOfExecutingCasesPerDay(Date since, Date to) {
    return getDbSession().getNumberOfExecutingCasesPerDay(since, to);
  }

  public int getNumberOfOpenSteps() {
    return getDbSession().getNumberOfOpenSteps();
  }

  public List<Integer> getNumberOfOpenStepsPerDay(Date since, Date to) {
    return getDbSession().getNumberOfOpenStepsPerDay(since, to);
  }

  public int getNumberOfOverdueSteps(Date currentDate) {
    return getDbSession().getNumberOfOverdueSteps(currentDate);
  }

  public int getNumberOfStepsAtRisk(Date beginningOfTheDay, Date atRisk) {
    return getDbSession().getNumberOfStepsAtRisk(beginningOfTheDay, atRisk);
  }

  public int getNumberOfUserOpenSteps(String userId) {
    return getDbSession().getNumberOfUserOpenSteps(userId);
  }

  public int getNumberOfUserOverdueSteps(String userId, Date currentDate) {
    return getDbSession().getNumberOfUserOverdueSteps(userId, currentDate);
  }

  public int getNumberOfUserStepsAtRisk(String userId, Date beginningOfTheDay, Date atRisk) {
    return getDbSession().getNumberOfUserStepsAtRisk(userId, beginningOfTheDay, atRisk);
  }

  public int getNumberOfFinishedSteps(int priority, Date since) {
    return getDbSession().getNumberOfFinishedSteps(priority, since);
  }

  public int getNumberOfOpenSteps(int priority) {
    return getDbSession().getNumberOfOpenSteps(priority);
  }

  public int getNumberOfUserFinishedSteps(String userId, int priority, Date since) {
    return getDbSession().getNumberOfUserFinishedSteps(userId, priority, since);
  }

  public int getNumberOfUserOpenSteps(String userId, int priority) {
    return getDbSession().getNumberOfUserOpenSteps(userId, priority);
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs) {
    return getDbSession().getProcesses(definitionUUIDs);
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    return getDbSession().getProcesses(definitionUUIDs, processState);
  }

  public List<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize) {
    return getDbSession().getProcesses(definitionUUIDs, fromIndex, pageSize);
  }

  public List<InternalProcessDefinition> getProcesses(
      Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    return getDbSession().getProcesses(definitionUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public InternalProcessDefinition getLastDeployedProcess(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    Misc.checkArgsNotNull(processState);
    return getDbSession().getLastProcess(definitionUUIDs, processState);
  }
  
  public Set<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    return getDbSession().getProcessInstances(definitionUUIDs);
  }

  public Set<InternalProcessInstance> getUserInstances(String userId, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getUserInstances(userId, definitionUUIDs);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public int getNumberOfProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    return getDbSession().getNumberOfProcessInstances(definitionUUIDs);
  }

  public int getNumberOfParentProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    return getDbSession().getNumberOfParentProcessInstances(definitionUUIDs);
  }

  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstancesWithTaskState(activityStates, definitionUUIDs);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Set<InternalProcessInstance> dbInstances = getDbSession().getProcessInstancesWithInstanceStates(instanceStates, visibleProcessUUIDs);
    if (dbInstances == null) {
      return Collections.emptySet();
    }
    return dbInstances;
  }

  public TaskInstance getOneTask(String userId, ActivityState taskState, Set<ProcessDefinitionUUID> definitionUUIDs) {
    return getDbSession().getOneTask(userId, taskState, definitionUUIDs);
  }

  public Set<TaskInstance> getUserTasks(String userId, ActivityState taskState, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Collection<ActivityState> taskStates = new HashSet<ActivityState>();
    taskStates.add(taskState);
    Set<TaskInstance> userTasks = getUserTasks(userId, taskStates);
    Set<TaskInstance> filteredTasks = new HashSet<TaskInstance>();
    for (TaskInstance taskInstance : userTasks) {
      if (definitionUUIDs.contains(taskInstance.getProcessDefinitionUUID())) {
        filteredTasks.add(taskInstance);
      }
    }
    return filteredTasks;
  }

  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize) {
    return getDbSession().getProcessInstances(definitionUUIDs, fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs, 
  		int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    return getDbSession().getProcessInstances(definitionUUIDs, fromIndex, pageSize, pagingCriterion);
  }

  public List<InternalProcessDefinition> getProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize) {
    return getDbSession().getProcessesExcept(processUUIDs, fromIndex, pageSize);
  }

  public List<InternalProcessDefinition> getProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    return getDbSession().getProcessesExcept(processUUIDs, fromIndex, pageSize, pagingCriterion);
  }
  
  public int getNumberOfActivityInstanceComments(ActivityInstanceUUID activityUUID) {
    return getDbSession().getNumberOfActivityInstanceComments(activityUUID);
  }
  
  public Map<ActivityInstanceUUID, Integer> getNumberOfActivityInstanceComments(Set<ActivityInstanceUUID> activityUUIDs) {
    return getDbSession().getNumberOfActivityInstanceComments(activityUUIDs);
  }

  public int getNumberOfComments(ProcessInstanceUUID instanceUUID) {
    return getDbSession().getNumberOfComments(instanceUUID);
  }

  public List<Comment> getCommentFeed(ProcessInstanceUUID instanceUUID) {
    return getDbSession().getCommentFeed(instanceUUID);
  }

  public List<Comment> getActivityInstanceCommentFeed(ActivityInstanceUUID activityUUID) {
    return getDbSession().getActivityInstanceCommentFeed(activityUUID);
  }
  
  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDsExcept(Set<ProcessDefinitionUUID> processUUIDs){
    return getDbSession().getAllProcessDefinitionUUIDsExcept(processUUIDs);
  }
  
  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDs(){
    return getDbSession().getAllProcessDefinitionUUIDs();
  }

  public int getNumberOfProcessInstanceComments(ProcessInstanceUUID instanceUUID) {
    return getDbSession().getNumberOfProcessInstanceComments(instanceUUID);
  }

  public List<Comment> getProcessInstanceCommentFeed(ProcessInstanceUUID instanceUUID) {
    return getDbSession().getProcessInstanceCommentFeed(instanceUUID);
  }

  public List<InternalProcessInstance> getParentUserInstances(String userId, int startingIndex, int pageSize) {
	 List<InternalProcessInstance> dbInstances = getDbSession().getParentUserInstances(userId, startingIndex, pageSize);
	 if (dbInstances == null) {
	   return Collections.emptyList();
	 }
	 return dbInstances;
  }

	public List<InternalProcessInstance> getParentUserInstances(String userId,
			int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentUserInstances(userId, startingIndex, pageSize, pagingCriterion);
		 if (dbInstances == null) {
		   return Collections.emptyList();
		 }
		 return dbInstances;
	}

  public List<InternalProcessInstance> getParentUserInstances(String userId, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> definitionUUIDs) {
    List<InternalProcessInstance> dbInstances = getDbSession().getParentUserInstances(userId, startingIndex, pageSize,  definitionUUIDs);
	if (dbInstances == null) {
	  return Collections.emptyList();
	}
	return dbInstances;
  }
  
	public List<InternalProcessInstance> getParentUserInstances(String userId,
			int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> definitionUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		
		List<InternalProcessInstance> dbInstances = getDbSession()
				.getParentUserInstances(userId, startingIndex, pageSize,
						definitionUUIDs, pagingCriterion);
		if (dbInstances == null) {
			return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
			String userId, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		List<InternalProcessInstance> dbInstances = getDbSession()
				.getParentProcessInstancesWithActiveUser(userId, startingIndex,
						pageSize, visibleProcessUUIDs);
		if (dbInstances == null) {
			return Collections.emptyList();
		}
		return dbInstances;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
			String userId, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion){
		
		List<InternalProcessInstance> dbInstances = getDbSession()
				.getParentProcessInstancesWithActiveUser(userId, startingIndex,
						pageSize, visibleProcessUUIDs, pagingCriterion);
		if (dbInstances == null) {
			return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int startingIndex, int pageSize) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUser(userId, startingIndex, pageSize);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
			String userId, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUser(
				userId, startingIndex, pageSize, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId,currentDate, atRisk, startingIndex, pageSize,  visibleProcessUUIDs);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
			String userId, Date currentDate, Date atRisk, int startingIndex,
			int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
				userId,currentDate, atRisk, startingIndex, pageSize,  visibleProcessUUIDs, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk, startingIndex, pageSize);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
			String userId, Date currentDate, Date atRisk, int startingIndex,
			int pageSize, ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
				userId, currentDate, atRisk, startingIndex, pageSize, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}
	
	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithOverdueTasks(userId,currentDate, startingIndex, pageSize,  visibleProcessUUIDs);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
			String userId, Date currentDate, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithOverdueTasks(
				userId, currentDate, startingIndex, pageSize,  visibleProcessUUIDs, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithOverdueTasks(userId, currentDate, startingIndex, pageSize);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
			String userId, Date currentDate, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithOverdueTasks(
				userId, currentDate, startingIndex, pageSize, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}
	
	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithInvolvedUser(userId, startingIndex, pageSize,  visibleProcessUUIDs);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
			String userId, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithInvolvedUser(userId, 
				startingIndex, pageSize,  visibleProcessUUIDs, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int startingIndex, int pageSize) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithInvolvedUser(userId, startingIndex, pageSize);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}

	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
			String userId, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> dbInstances = getDbSession().getParentProcessInstancesWithInvolvedUser(
				userId, startingIndex, pageSize, pagingCriterion);
		if (dbInstances == null) {
		  return Collections.emptyList();
		}
		return dbInstances;
	}
	
	public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs){
		return getDbSession().getNumberOfParentProcessInstancesWithActiveUser(userId,visibleProcessUUIDs);
	}
	public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId){
	 return getDbSession().getNumberOfParentProcessInstancesWithActiveUser(userId);
	}
	
	public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		return getDbSession().getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk,visibleProcessUUIDs);
	}

	public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk) {
		return getDbSession().getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk);
	}
	
	public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
		return getDbSession().getNumberOfParentProcessInstancesWithOverdueTasks(userId, currentDate,visibleProcessUUIDs);
	}

	public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate) {
		return getDbSession().getNumberOfParentProcessInstancesWithOverdueTasks(userId, currentDate);
	}
	
	public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs){
	 return getDbSession().getNumberOfParentProcessInstancesWithInvolvedUser(userId,visibleProcessUUIDs);
	}
	public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId){
	 return getDbSession().getNumberOfParentProcessInstancesWithInvolvedUser(userId);
	}
	public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs){
	 return getDbSession().getNumberOfParentProcessInstancesWithStartedBy(userId,visibleProcessUUIDs);
	}
	public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId){
	 return getDbSession().getNumberOfParentProcessInstancesWithStartedBy(userId);
	}

	public Set<Category> getCategories(Collection<String> categoryNames) {
	  return getDbSession().getCategories(categoryNames);
	}

	public Set<Category> getAllCategories() {
		return getDbSession().getAllCategories();
	}

	public Set<Category> getAllCategoriesExcept(Set<String> uuids) {
		return getDbSession().getAllCategoriesExcept(uuids);
	}
	
	public CategoryImpl getCategoryByUUID(String uuid) {
	  return getDbSession().getCategoryByUUID(uuid);
	}
	
	public Set<CategoryImpl> getCategoriesByUUIDs(Set<CategoryUUID> uuids) {
	  return getDbSession().getCategoriesByUUIDs(uuids);
	}

	public Set<ProcessDefinitionUUID> getProcessUUIDsFromCategory(String category) {
		return getDbSession().getProcessUUIDsFromCategory(category);
	}

  public List<Object> search(SearchQueryBuilder query, int firstResult, int maxResults, Class<?> indexClass) {
    return getDbSession().search(query, firstResult, maxResults, indexClass);
  }

  public int search(SearchQueryBuilder query, Class<?> indexClass) {
    return getDbSession().search(query, indexClass);
  }

  public Set<ActivityDefinitionUUID> getProcessTaskUUIDs(ProcessDefinitionUUID definitionUUID) {
    return getDbSession().getProcessTaskUUIDs(definitionUUID);
  }

  public boolean processExists(ProcessDefinitionUUID definitionUUID) {
    return getDbSession().processExists(definitionUUID);
  }

  public List<Long> getProcessInstancesDuration(Date since, Date until) {    
    return getDbSession().getProcessInstancesDuration(since, until);
  }

  public List<Long> getProcessInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getProcessInstancesDuration(processUUID, since, until);
  }

  public List<Long> getProcessInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    return getDbSession().getProcessInstancesDurationFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(Date since, Date until) {    
    return getDbSession().getActivityInstancesExecutionTime(since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getActivityInstancesExecutionTime(processUUID, since, until);
  }

  public List<Long> getActivityInstancesExecutionTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    return getDbSession().getActivityInstancesExecutionTimeFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    return getDbSession().getActivityInstancesExecutionTime(activityUUID, since, until);
  }

  public List<Long> getActivityInstancesExecutionTimeFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    return getDbSession().getActivityInstancesExecutionTimeFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(Date since, Date until) {    
    return getDbSession().getTaskInstancesWaitingTime(since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTime(processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTimeFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTime(taskUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromTaskUUIDs(
      Set<ActivityDefinitionUUID> taskUUIDs, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTimeFromTasksUUIDs(taskUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTimeOfUser(username, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTimeOfUser(username, processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(
      String username, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {    
    return getDbSession().getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(username, processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    return getDbSession().getTaskInstancesWaitingTimeOfUser(username, taskUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(
      String username, Set<ActivityDefinitionUUID> taskUUIDs, Date since,
      Date until) {
    return getDbSession().getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(username, taskUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDuration(Date since, Date until) {
    return getDbSession().getActivityInstancesDuration(since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getActivityInstancesDuration(processUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    return getDbSession().getActivityInstancesDurationFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    return getDbSession().getActivityInstancesDuration(activityUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    return getDbSession().getActivityInstancesDurationFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, Date since, Date until) {
    return getDbSession().getActivityInstancesDurationByActivityType(activityType, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    return getDbSession().getActivityInstancesDurationByActivityType(activityType, processUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    return getDbSession().getActivityInstancesDurationByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }

  public long getNumberOfCreatedProcessInstances(Date since, Date until) {
    return getDbSession().getNumberOfCreatedProcessInstances(since, until);
  }

  public long getNumberOfCreatedProcessInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getNumberOfCreatedProcessInstances(processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstances(Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstances(since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstances(processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstancesFromProcessUUIDs(processUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstances(activityUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstancesFromActivityUUIDs(activityUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, Date since, Date until) {
    return getDbSession().getNumberOfCreatedActivityInstancesByActivityType(activityType, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    return getDbSession().getNumberOfCreatedActivityInstancesByActivityType(activityType, processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    return getDbSession().getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }

  public boolean containsOtherActiveActivities(ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityUUID) {
    return getDbSession().containsOtherActiveActivities(instanceUUID, activityUUID);
  }

}
