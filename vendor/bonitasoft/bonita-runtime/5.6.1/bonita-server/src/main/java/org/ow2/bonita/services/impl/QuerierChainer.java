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
 * Modified by Matthieu Chaffotte, Nicolas Chabanoles, Elias Ricken de Medeiros - BonitaSoft S.A.
 **/
package org.ow2.bonita.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.search.SearchQueryBuilder;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.util.ActivityInstanceLastUpdateComparator;
import org.ow2.bonita.util.InternalProcessDefinitionComparator;
import org.ow2.bonita.util.InternalProcessInstanceComparator;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessInstanceEndedDateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceEndedDateComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceLastUpdateComparator;
import org.ow2.bonita.util.ProcessInstanceLastUpdateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceNbComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceNbComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceStartedDateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceStartedDateComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceUUIDComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceUUIDComparatorDesc;

/**
 * @author Guillaume Porcher
 *
 * Chainer for Queriers :
 *  - for methods that returns only one object, search for the first matching object
 *  - for methods that returns a collection: search for all objects in all return queriers.
 */
public class QuerierChainer implements Querier {

  private List<Querier> queriers;

  public QuerierChainer(List<Querier> queriers) {
    this.queriers = queriers;
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName) {
    Set<InternalActivityInstance> set = new HashSet<InternalActivityInstance>();
    for (Querier querier : queriers) {
      Set<InternalActivityInstance> tmp = querier.getActivityInstances(instanceUUID, activityName);
      if (tmp != null && !tmp.isEmpty()) {
        set.addAll(tmp);
      }
    }
    return set;
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName, String iterationId) {
    Set<InternalActivityInstance> set = new HashSet<InternalActivityInstance>();
    for (Querier querier : queriers) {
      Set<InternalActivityInstance> tmp = querier.getActivityInstances(instanceUUID, activityName, iterationId);
      if (tmp != null && !tmp.isEmpty()) {
        set.addAll(tmp);
      }
    }
    return set;
  }

  public int getNumberOfProcesses() {
    int nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfProcesses();
    }
    return nb;
  }
  public int getNumberOfParentProcessInstances() {
    int nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfParentProcessInstances();
    }
    return nb;
  }
  public int getNumberOfProcessInstances() {
    int nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfProcessInstances();
    }
    return nb;
  }
  public InternalProcessInstance getProcessInstance(ProcessInstanceUUID instanceUUID) {
    for (Querier querier : queriers) {
      InternalProcessInstance processInst = querier.getProcessInstance(instanceUUID);
      if (processInst != null) {
        return processInst;
      }
    }
    return null;
  }

  public Set<ProcessInstanceUUID> getParentInstancesUUIDs() {
    Set<ProcessInstanceUUID> processInsts = new HashSet<ProcessInstanceUUID>();
    for (Querier querier : queriers) {
      Set<ProcessInstanceUUID> tmp = querier.getParentInstancesUUIDs();
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getUserInstances(String userId, Date minStartDate) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getUserInstances(userId, minStartDate);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getUserParentInstances(String userId, Date minStartDate) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getUserParentInstances(userId, minStartDate);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public TaskInstance getOneTask(String userId, ProcessDefinitionUUID processUUID, ActivityState taskState) {
    for (Querier querier : queriers) {
      TaskInstance task = querier.getOneTask(userId, processUUID, taskState);
      if (task != null) {
        return task;
      }
    }
    return null;
  }

  public TaskInstance getOneTask(String userId, ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    for (Querier querier : queriers) {
      TaskInstance task = querier.getOneTask(userId, instanceUUID, taskState);
      if (task != null) {
        return task;
      }
    }
    return null;
  }

  public TaskInstance getOneTask(String userId, ActivityState taskState) {
    for (Querier querier : queriers) {
      TaskInstance task = querier.getOneTask(userId, taskState);
      if (task != null) {
        return task;
      }
    }
    return null;
  }

  public Set<InternalProcessInstance> getProcessInstances() {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstances();
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }



  public Set<InternalProcessInstance> getUserInstancesExcept(String userId, Set<ProcessInstanceUUID> myCases) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getUserInstancesExcept(userId, myCases);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstances(
      Collection<ProcessInstanceUUID> instanceUUIDs) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstances(instanceUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getParentInstances() {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getParentInstances();
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstancesWithTaskState(activityStates);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstancesWithInstanceStates(instanceStates);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstances(ProcessDefinitionUUID processUUID) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getProcessInstances(processUUID));
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstances(ProcessDefinitionUUID processUUID, InstanceState instanceState) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getProcessInstances(processUUID, instanceState));
    }
    return processInsts;
  }

  public InternalActivityInstance getActivityInstance(ProcessInstanceUUID instanceUUID,
      String activityId, String iterationId, String activityInstanceId, String loopId) {
    for (Querier querier : queriers) {
      InternalActivityInstance activityInst = querier.getActivityInstance(instanceUUID, activityId, iterationId, activityInstanceId, loopId);
      if (activityInst != null) {
        return activityInst;
      }
    }
    return null;
  }

  public List<InternalActivityInstance> getActivityInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID) {
    List<InternalActivityInstance> activities = new ArrayList<InternalActivityInstance>();
    for (Querier querier : queriers) {
      List<InternalActivityInstance> tmp = querier.getActivityInstancesFromRoot(rootInstanceUUID);
      if (tmp != null && !tmp.isEmpty()) {
        activities.addAll(tmp);
      }
    }
    if (!activities.isEmpty()) {
      Collections.sort(activities, new ActivityInstanceLastUpdateComparator());
    }
    return activities;    
  }

  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs) {
    List<InternalActivityInstance> activities = new ArrayList<InternalActivityInstance>();
    for (Querier querier : queriers) {
      List<InternalActivityInstance> tmp = querier.getActivityInstancesFromRoot(rootInstanceUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        activities.addAll(tmp);
      }
    }
    if (!activities.isEmpty()) {
      Collections.sort(activities, new ActivityInstanceLastUpdateComparator());
    }
    return activities;    
  }

  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, ActivityState state) {
    List<InternalActivityInstance> activities = new ArrayList<InternalActivityInstance>();
    for (Querier querier : queriers) {
      List<InternalActivityInstance> tmp = querier.getActivityInstancesFromRoot(rootInstanceUUIDs, state);
      if (tmp != null && !tmp.isEmpty()) {
        activities.addAll(tmp);
      }
    }
    if (!activities.isEmpty()) {
      Collections.sort(activities, new ActivityInstanceLastUpdateComparator());
    }
    return activities;    
  }

  public Map<ProcessInstanceUUID, InternalActivityInstance> getLastUpdatedActivityInstanceFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, boolean considerSystemTaks) {
    Map<ProcessInstanceUUID, InternalActivityInstance> activities = new HashMap<ProcessInstanceUUID, InternalActivityInstance>();
    for (Querier querier : queriers) {
      Map<ProcessInstanceUUID, InternalActivityInstance> tmp = querier.getLastUpdatedActivityInstanceFromRoot(rootInstanceUUIDs, considerSystemTaks);
      for (Map.Entry<ProcessInstanceUUID, InternalActivityInstance> entry : tmp.entrySet()) {
        activities.put(entry.getKey(), entry.getValue());
      }
    }
    return activities;   
  }

  public long getLastProcessInstanceNb(ProcessDefinitionUUID processUUID) {
    long max = -1;
    for (Querier querier : queriers) {
      long l = querier.getLastProcessInstanceNb(processUUID);
      if (l > max) {
        max = l;
      }
    }
    return max;
  }

  public Execution getExecutionOnActivity(ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityInstanceUUID) {
    for (Querier querier : queriers) {
      final Execution execution = querier.getExecutionOnActivity(instanceUUID, activityInstanceUUID);
      if (execution != null) {
        return execution;
      }
    }
    return null;
  }

  public Execution getExecutionWithEventUUID(String eventUUID) {
    for (Querier querier : queriers) {
      final Execution execution = querier.getExecutionWithEventUUID(eventUUID);
      if (execution != null) {
        return execution;
      }
    }
    return null;
  }

  public Set<Execution> getExecutions(ProcessInstanceUUID instanceUUID) {
    Set<Execution> executions = new HashSet<Execution>();
    for (Querier querier : queriers) {
      executions.addAll(querier.getExecutions(instanceUUID));
    }
    return executions;
  }

  public ActivityState getActivityInstanceState(ActivityInstanceUUID activityInstanceUUID) {
    for (Querier querier : queriers) {
      ActivityState activityState = querier.getActivityInstanceState(activityInstanceUUID);
      if (activityState != null) {
        return activityState;
      }
    }
    return null;
  }

  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID) {
    Set<InternalActivityInstance> activityInsts = new HashSet<InternalActivityInstance>();
    for (Querier querier : queriers) {
      activityInsts.addAll(querier.getActivityInstances(instanceUUID));
      if (!activityInsts.isEmpty()) {
        return activityInsts;
      }
    }
    return activityInsts;
  }
  
  public List<InternalActivityInstance> getActivityInstances(
			ProcessInstanceUUID instanceUUID, int fromIndex, int pageSize,
			ActivityInstanceCriterion pagingCriterion) {
  	List<InternalActivityInstance> activityInsts = new ArrayList<InternalActivityInstance>();
    for (Querier querier : queriers) {
      activityInsts.addAll(querier.getActivityInstances(instanceUUID, fromIndex, pageSize, pagingCriterion));
      if (!activityInsts.isEmpty()) {
        return activityInsts;
      }
    }
    return activityInsts;
	}

  public TaskInstance getTaskInstance(ActivityInstanceUUID taskUUID) {
    for (Querier querier : queriers) {
      TaskInstance activity = querier.getTaskInstance(taskUUID);
      if (activity != null) {
        return activity;
      }
    }
    return null;
  }

  public Set<InternalProcessDefinition> getProcesses() {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> querierProcesses = querier.getProcesses();
      if (querierProcesses != null && !querierProcesses.isEmpty()) {
        processes.addAll(querierProcesses);
      }
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(String processId) {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> querierProcesses = querier.getProcesses(processId);
      if (querierProcesses != null && !querierProcesses.isEmpty()) {
        processes.addAll(querierProcesses);
      }
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(ProcessState processState) {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> querierProcesses = querier.getProcesses(processState);
      if (querierProcesses != null && !querierProcesses.isEmpty()) {
        processes.addAll(querierProcesses);
      }
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(String processId, ProcessState processState) {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> querierProcesses = querier.getProcesses(processId, processState);
      if (querierProcesses != null && !querierProcesses.isEmpty()) {
        processes.addAll(querierProcesses);
      }
    }
    return processes;
  }

  public InternalProcessDefinition getProcess(ProcessDefinitionUUID processUUID) {
    for (Querier querier : queriers) {
      InternalProcessDefinition process = querier.getProcess(processUUID);
      if (process != null) {
        return process;
      }
    }
    return null;
  }

  public InternalProcessDefinition getProcess(String processId, String version) {
    for (Querier querier : queriers) {
      InternalProcessDefinition process = querier.getProcess(processId, version);
      if (process != null) {
        return process;
      }
    }
    return null;
  }

  public String getLastProcessVersion(String processName) {
    Misc.checkArgsNotNull(processName);
    String last = null;
    for (Querier querier : queriers) {
      String version = querier.getLastProcessVersion(processName);
      if (version != null && (last == null || version.compareTo(last) > 0)) {
        last = version;
      }
    }
    return last;
  }

  public InternalProcessDefinition getLastDeployedProcess(String processId, ProcessState processState) {
    Misc.checkArgsNotNull(processId, processState);
    Set<InternalProcessDefinition> processes = getProcesses(processId, processState);
    InternalProcessDefinition lastProcess = null;
    for (InternalProcessDefinition process : processes) {
      if (lastProcess == null) {
        lastProcess = process;
      } else if (process.getDeployedDate().after(lastProcess.getDeployedDate())) {
        lastProcess = process;
      }
    }
    return lastProcess;
  }

  public InternalActivityInstance getActivityInstance(ActivityInstanceUUID activityInstanceUUID) {
    for (Querier querier : queriers) {
      InternalActivityInstance activity = querier.getActivityInstance(activityInstanceUUID);
      if (activity != null) {
        return activity;
      }
    }
    return null;
  }

  public Set<TaskInstance> getTaskInstances(ProcessInstanceUUID instanceUUID) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getTaskInstances(instanceUUID));
      if (!activities.isEmpty()) {
        return activities;
      }
    }
    return activities;
  }

  public Set<TaskInstance> getTaskInstances(ProcessInstanceUUID instanceUUID, Set<String> taskNames) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getTaskInstances(instanceUUID, taskNames));
      if (!activities.isEmpty()) {
        return activities;
      }
    }
    return activities;
  }

  public Set<TaskInstance> getUserInstanceTasks(String userId, ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getUserInstanceTasks(userId, instanceUUID, taskState));
      if (!activities.isEmpty()) {
        return activities;
      }
    }
    return activities;
  }

  public Set<TaskInstance> getUserTasks(String userId, ActivityState taskState) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getUserTasks(userId, taskState));
    }
    return activities;
  }

  public Set<TaskInstance> getUserTasks(String userId, Collection<ActivityState> taskStates) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getUserTasks(userId, taskStates));
    }
    return activities;
  }

  public InternalActivityDefinition getActivity(ActivityDefinitionUUID activityDefinitionUUID) {
    for (Querier querier : queriers) {
      InternalActivityDefinition activity = querier.getActivity(activityDefinitionUUID);
      if (activity != null) {
        return activity;
      }
    }
    return null;
  }

  public int getNumberOfUserOpenSteps(String userId) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfUserOpenSteps(userId);
    }
    return openSteps;
  }

  public List<Integer> getNumberOfFinishedCasesPerDay(Date since, Date to) {
    List<Integer> finishedCases = new ArrayList<Integer>();
    for (Querier querier : queriers) {
      List<Integer> finishedList = querier.getNumberOfFinishedCasesPerDay(since, to);
      if (finishedCases.isEmpty()) {
        for (int i = 0; i < finishedList.size(); i++) {
          finishedCases.add(finishedList.get(i));
        }
      } else {
        for (int i = 0; i < finishedList.size(); i++) {
          finishedCases.set(i, finishedList.get(i) + finishedCases.get(i));
        }
      }
    }
    return finishedCases;
  }

  public List<Integer> getNumberOfExecutingCasesPerDay(Date since, Date to) {
    List<Integer> executingCases = new ArrayList<Integer>();
    for (Querier querier : queriers) {
      List<Integer> executingList = querier.getNumberOfExecutingCasesPerDay(since, to);
      if (executingCases.isEmpty()) {
        for (int i = 0; i < executingList.size(); i++) {
          executingCases.add(executingList.get(i));
        }
      } else {
        for (int i = 0; i < executingList.size(); i++) {
          executingCases.set(i, executingList.get(i) + executingCases.get(i));
        }
      }
    }
    return executingCases;
  }

  public List<Integer> getNumberOfOpenStepsPerDay(Date since, Date to) {
    List<Integer> opensteps = new ArrayList<Integer>();
    for (Querier querier : queriers) {
      List<Integer> openList = querier.getNumberOfOpenStepsPerDay(since, to);
      if (opensteps.isEmpty()) {
        for (int i = 0; i < openList.size(); i++) {
          opensteps.add(openList.get(i));
        }
      } else {
        for (int i = 0; i < openList.size(); i++) {
          opensteps.set(i, openList.get(i) + opensteps.get(i));
        }
      }
    }
    return opensteps;
  }

  public int getNumberOfOverdueSteps(Date currentDate) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfOverdueSteps(currentDate);
    }
    return openSteps;
  }

  public int getNumberOfStepsAtRisk(Date currentDate, Date atRisk) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfStepsAtRisk(currentDate, atRisk);
    }
    return openSteps;
  }

  public int getNumberOfOpenSteps() {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfOpenSteps();
    }
    return openSteps;
  }

  public int getNumberOfUserOverdueSteps(String userId, Date currentDate) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfUserOverdueSteps(userId, currentDate);
    }
    return openSteps;
  }

  public int getNumberOfUserStepsAtRisk(String userId, Date currentDate, Date atRisk) {
    int stepsAtRisk = 0;
    for (Querier querier : queriers) {
      stepsAtRisk += querier.getNumberOfUserStepsAtRisk(userId, currentDate, atRisk);
    }
    return stepsAtRisk;
  }

  public int getNumberOfFinishedSteps(int priority, Date since) {
    int finishedSteps = 0;
    for (Querier querier : queriers) {
      finishedSteps += querier.getNumberOfFinishedSteps(priority, since);
    }
    return finishedSteps;
  }

  public int getNumberOfOpenSteps(int priority) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfOpenSteps(priority);
    }
    return openSteps;
  }

  public int getNumberOfUserFinishedSteps(String userId, int priority, Date since) {
    int finishedSteps = 0;
    for (Querier querier : queriers) {
      finishedSteps += querier.getNumberOfUserFinishedSteps(userId, priority, since);
    }
    return finishedSteps;
  }

  public int getNumberOfUserOpenSteps(String userId, int priority) {
    int openSteps = 0;
    for (Querier querier : queriers) {
      openSteps += querier.getNumberOfUserOpenSteps(userId, priority);
    }
    return openSteps;
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> temp = querier.getProcesses(definitionUUIDs);
      if (temp != null) {
        processes.addAll(temp);
      }
    }
    return processes;
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    Set<InternalProcessDefinition> processes = new HashSet<InternalProcessDefinition>();
    for (Querier querier : queriers) {
      Set<InternalProcessDefinition> temp = querier.getProcesses(definitionUUIDs, processState);
      if (temp != null) {
        processes.addAll(temp);
      }
    }
    return processes;
  }

  public InternalProcessDefinition getLastDeployedProcess(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    Misc.checkArgsNotNull(processState);
    Set<InternalProcessDefinition> processes = getProcesses(definitionUUIDs, processState);
    InternalProcessDefinition lastProcess = null;
    for (InternalProcessDefinition process : processes) {
      if (lastProcess == null) {
        lastProcess = process;
      } else if (process.getDeployedDate().after(lastProcess.getDeployedDate())) {
        lastProcess = process;
      }
    }
    return lastProcess;
  }

  public Set<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getProcessInstances(definitionUUIDs));
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getUserInstances(String userId, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getUserInstances(userId, definitionUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public int getNumberOfProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    int nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfProcessInstances(definitionUUIDs);
    }
    return nb;
  }

  public int getNumberOfParentProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    int nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfParentProcessInstances(definitionUUIDs);
    }
    return nb;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates,
      Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstancesWithTaskState(activityStates, definitionUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getProcessInstancesWithInstanceStates(instanceStates, visibleProcessUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public TaskInstance getOneTask(String userId, ActivityState taskState, Set<ProcessDefinitionUUID> definitionUUIDs) {
    for (Querier querier : queriers) {
      TaskInstance task = querier.getOneTask(userId, taskState, definitionUUIDs);
      if (task != null) {
        return task;
      }
    }
    return null;
  }

  public Set<TaskInstance> getUserTasks(String userId, ActivityState taskState, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Set<TaskInstance> activities = new HashSet<TaskInstance>();
    for (Querier querier : queriers) {
      activities.addAll(querier.getUserTasks(userId, taskState, definitionUUIDs));
    }
    return activities;
  }

  public int getNumberOfActivityInstanceComments(ActivityInstanceUUID activityUUID) {
    int comments = 0;
    for (Querier querier : queriers) {
      comments += querier.getNumberOfActivityInstanceComments(activityUUID);
    }
    return comments;
  }

  public Set<InternalProcessInstance> getUserInstances(String userId) {
    Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    for (Querier querier : queriers) {
      Set<InternalProcessInstance> tmp = querier.getUserInstances(userId);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

  public List<InternalProcessInstance> getParentUserInstances(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentUserInstances(userId, fromIndex, pageSize);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;

  }

	public List<InternalProcessInstance> getParentUserInstances(String userId,
			int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> querierInstances = querier.getParentUserInstances(userId, startingIndex, pageSize, pagingCriterion);
      if (querierInstances != null && !querierInstances.isEmpty()) {
        processInsts.addAll(querierInstances);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize, visibleProcessUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }
  
	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
			String userId, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> querierProcessInsts = querier.getParentProcessInstancesWithActiveUser(userId, 
      		startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
      if (querierProcessInsts != null && !querierProcessInsts.isEmpty()) {
        processInsts.addAll(querierProcessInsts);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUser(userId, fromIndex, pageSize);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
			String userId, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> querierProcessInsts = querier.getParentProcessInstancesWithActiveUser(
      		userId, startingIndex, pageSize, pagingCriterion);
      if (querierProcessInsts != null && !querierProcessInsts.isEmpty()) {
        processInsts.addAll(querierProcessInsts);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk, startingIndex, pageSize,visibleProcessUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
			String userId, Date currentDate, Date atRisk, int startingIndex,
			int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      		userId, currentDate, atRisk, startingIndex, pageSize,visibleProcessUUIDs, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk, startingIndex, pageSize);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
			String userId, Date currentDate, Date atRisk, int startingIndex,
			int pageSize, ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      		userId, currentDate, atRisk, startingIndex, pageSize, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithOverdueTasks(userId, currentDate, startingIndex, pageSize,visibleProcessUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
			String userId, Date currentDate, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> querierInstances = querier.getParentProcessInstancesWithOverdueTasks(userId, 
      		currentDate, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
      if (querierInstances != null && !querierInstances.isEmpty()) {
        processInsts.addAll(querierInstances);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithOverdueTasks(userId, currentDate, startingIndex, pageSize);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
			String userId, Date currentDate, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithOverdueTasks(
      		userId, currentDate, startingIndex, pageSize, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize, visibleProcessUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
			String userId, int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> visibleProcessUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithInvolvedUser(
      		userId, startingIndex, pageSize, visibleProcessUUIDs, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithInvolvedUser(userId, fromIndex, pageSize);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
			String userId, int startingIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentProcessInstancesWithInvolvedUser(
      		userId, startingIndex, pageSize, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getParentUserInstances(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> definitionUUIDs) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentUserInstances(userId, fromIndex, pageSize, definitionUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
  }

	public List<InternalProcessInstance> getParentUserInstances(String userId,
			int startingIndex, int pageSize,
			Set<ProcessDefinitionUUID> definitionUUIDs,
			ProcessInstanceCriterion pagingCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      List<InternalProcessInstance> tmp = querier.getParentUserInstances(userId, startingIndex, 
      		pageSize, definitionUUIDs, pagingCriterion);
      if (tmp != null && !tmp.isEmpty()) {
        processInsts.addAll(tmp);
      }
    }
    return processInsts;
	}

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithActiveUser(userId, visibleProcessUUIDs);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithActiveUser(userId);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(userId, currentDate, atRisk);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithOverdueTasks(userId, currentDate, visibleProcessUUIDs);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithOverdueTasks(userId, currentDate);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithInvolvedUser(userId, visibleProcessUUIDs);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithInvolvedUser(userId);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithStartedBy(userId, visibleProcessUUIDs);
    }
    return result;
  }

  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId) {
    Integer result = 0;
    for (Querier querier : queriers) {
      result += querier.getNumberOfParentProcessInstancesWithStartedBy(userId);
    }
    return result;
  }

  public Map<ActivityInstanceUUID, Integer> getNumberOfActivityInstanceComments(Set<ActivityInstanceUUID> activityUUIDs) {
    Map<ActivityInstanceUUID, Integer> result = new HashMap<ActivityInstanceUUID, Integer>();
    for (Querier querier : queriers) {
      result.putAll(querier.getNumberOfActivityInstanceComments(activityUUIDs));
    }
    return result;
  }

  public int getNumberOfComments(ProcessInstanceUUID instanceUUID) {
    int comments = 0;
    for (Querier querier : queriers) {
      comments += querier.getNumberOfComments(instanceUUID);
    }
    return comments;
  }

  public List<Comment> getCommentFeed(ProcessInstanceUUID instanceUUID) {
    List<Comment> comments = new ArrayList<Comment>();
    for (Querier querier : queriers) {
      comments.addAll(querier.getCommentFeed(instanceUUID));
    }
    return comments;
  }

  public int getNumberOfProcessInstanceComments(ProcessInstanceUUID instanceUUID) {
    int comments = 0;
    for (Querier querier : queriers) {
      comments += querier.getNumberOfProcessInstanceComments(instanceUUID);
    }
    return comments;
  }

  public List<Comment> getProcessInstanceCommentFeed(ProcessInstanceUUID instanceUUID) {
    List<Comment> comments = new ArrayList<Comment>();
    for (Querier querier : queriers) {
      comments.addAll(querier.getProcessInstanceCommentFeed(instanceUUID));
    }
    return comments;
  }

  public List<Comment> getActivityInstanceCommentFeed(ActivityInstanceUUID activityUUID) {
    List<Comment> comments = new ArrayList<Comment>();
    for (Querier querier : queriers) {
      comments.addAll(querier.getActivityInstanceCommentFeed(activityUUID));
    }
    return comments;
  }

  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDs() {
    Set<ProcessDefinitionUUID> set = new HashSet<ProcessDefinitionUUID>();
    for (Querier querier : queriers) {
      Set<ProcessDefinitionUUID> tmp = querier.getAllProcessDefinitionUUIDs();
      if (tmp != null && !tmp.isEmpty()) {
        set.addAll(tmp);
      }
    }

    return set;
  }

  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDsExcept(Set<ProcessDefinitionUUID> processUUIDs) {
    Set<ProcessDefinitionUUID> set = new HashSet<ProcessDefinitionUUID>();
    for (Querier querier : queriers) {
      Set<ProcessDefinitionUUID> tmp = querier.getAllProcessDefinitionUUIDsExcept(processUUIDs);
      if (tmp != null && !tmp.isEmpty()) {
        set.addAll(tmp);
      }
    }

    return set;
  }


  ///////////////////////////////////////////
  ////////// PAGINATION OPERATIONS //////////
  ///////////////////////////////////////////

  /*
	private static class PaginatedMethod {
		private String methodName;
		private Class< ? >[] methodSignature;
		private List<Object> methodParameters;
		public PaginatedMethod(String methodName, Class<?>[] methodSignature, Object... methodParameters) {
			super();
			this.methodName = methodName;
			this.methodSignature = methodSignature;
			this.methodParameters = new ArrayList<Object>();
			if (methodParameters != null) {
				for (Object o : methodParameters) {
					this.methodParameters.add(o);
				}
			}
		}
		public String getMethodName() {
			return methodName;
		}
		public Class<?>[] getMethodSignature() {
			return methodSignature;
		}
		public List<Object> getMethodParameters() {
			return methodParameters;
		}
	}


	@SuppressWarnings("unchecked")
	private List<InternalProcessInstance> getInstancesPaging(PaginatedMethod firstPaginatedMethod, PaginatedMethod secondPaginatedMethod, int fromIndex, int pageSize) {
		try {
			List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();

			for (Querier querier : queriers) {
				if (processInsts.size() < fromIndex + pageSize) {
					//buid a set with at least all the first elements
					final Method firstMethod = querier.getClass().getMethod(firstPaginatedMethod.getMethodName(), firstPaginatedMethod.getMethodSignature());
					processInsts.addAll((List<InternalProcessInstance>)firstMethod.invoke(querier, firstPaginatedMethod.getMethodParameters().toArray()));
				} else {
					//retrieve maximum (maybe less) fromIndex+pageSize elements.
					//All returned elements must be younger or equal to the older elements of the current list
					final long currentOldestUpdate = processInsts.get(processInsts.size() - 1).getLastUpdate().getTime();
					List<Object> parameters = new ArrayList<Object>(secondPaginatedMethod.getMethodParameters());
					parameters.add(currentOldestUpdate);
					final Method secondMethod = querier.getClass().getMethod(secondPaginatedMethod.getMethodName(), secondPaginatedMethod.getMethodSignature());
					final List<InternalProcessInstance> newProcessInstances = (List<InternalProcessInstance>)secondMethod.invoke(querier, parameters.toArray());
					if (!newProcessInstances.isEmpty()) {
						processInsts.addAll(newProcessInstances);
						//sort the list and keep only fromIndex+pageSize elements
						Collections.sort(processInsts, new ProcessInstanceLastUpdateComparator());
						processInsts = Misc.subList(InternalProcessInstance.class, processInsts, 0, fromIndex+pageSize);
					}
				}
			}
			return getInstancesSubset(fromIndex, pageSize, processInsts);
		} catch (Exception e) {
			throw new BonitaRuntimeException("Exception while accessing paginated API: " + e.getMessage(), e);
		}

		//USAGE
		//PaginatedMethod firstPaginatedMethod = new PaginatedMethod("getProcessInstances", new Class[]{int.class, int.class}, 0, fromIndex + pageSize);
		//PaginatedMethod secondPaginatedMethod = new PaginatedMethod("getMostRecentProcessInstances", new Class[]{int.class, long.class}, fromIndex + pageSize);
		//return getInstancesPaging(firstPaginatedMethod, secondPaginatedMethod, fromIndex, pageSize);
	}*/

  public List<InternalProcessInstance> getProcessInstances(int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstances(fromIndex, pageSize));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstances(0, fromIndex + pageSize));
      } else {
        mergePaginatedProcessInstances(processInsts, querier.getMostRecentProcessInstances(fromIndex + pageSize, 
        		getOldestTime(processInsts)), querier, fromIndex, pageSize, ProcessInstanceCriterion.DEFAULT);
      }
    }

    return getInstancesSubset(fromIndex, pageSize, processInsts, ProcessInstanceCriterion.DEFAULT);
    //PaginatedMethod firstPaginatedMethod = new PaginatedMethod("getProcessInstances", new Class[]{int.class, int.class}, 0, fromIndex + pageSize);
    //PaginatedMethod secondPaginatedMethod = new PaginatedMethod("getMostRecentProcessInstances", new Class[]{int.class, long.class}, fromIndex + pageSize);
    //return getInstancesPaging(firstPaginatedMethod, secondPaginatedMethod, fromIndex, pageSize);
  }
  

	/* (non-Javadoc)
	 * @see org.ow2.bonita.services.Querier#getProcessInstances(int, int, org.ow2.bonita.facade.paging.ProcessInstanceCriterion)
	 */
	public List<InternalProcessInstance> getProcessInstances(int fromIndex,
			int pageSize, ProcessInstanceCriterion paginCriterion) {
		List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstances(fromIndex, pageSize, paginCriterion));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstances(0, fromIndex + pageSize, paginCriterion));
      } else {      	
        mergePaginatedProcessInstances(processInsts, querier.getMostRecentProcessInstances(fromIndex + pageSize, 
        		getOldestTime(processInsts), paginCriterion), querier, fromIndex, pageSize, paginCriterion);
      }
    }

    return getInstancesSubset(fromIndex, pageSize, processInsts, paginCriterion);
	}

  private long getOldestTime(final List<InternalProcessInstance> processInsts) {
    final long currentOldestUpdate = processInsts.get(processInsts.size() - 1).getLastUpdate().getTime();
    return currentOldestUpdate;
  }
  
  private void mergePaginatedProcessInstances(List<InternalProcessInstance> processInsts, 
  		final List<InternalProcessInstance> newProcessInstances, final Querier querier, int fromIndex, 
  		int pageSize, ProcessInstanceCriterion pagingCriterion) {
  	
  	Comparator<InternalProcessInstance> comparator = null;
  	switch (pagingCriterion) {
		case LAST_UPDATE_ASC:
			comparator = new ProcessInstanceLastUpdateComparatorAsc();
			break;
		case STARTED_DATE_ASC:
			comparator = new ProcessInstanceStartedDateComparatorAsc();
			break;
		case ENDED_DATE_ASC:
			comparator = new ProcessInstanceEndedDateComparatorAsc();
			break;
		case INSTANCE_NUMBER_ASC:
			comparator = new ProcessInstanceNbComparatorAsc();
			break;
		case INSTANCE_UUID_ASC:
			comparator = new ProcessInstanceUUIDComparatorAsc();
			break;
		case LAST_UPDATE_DESC:
			comparator = new ProcessInstanceLastUpdateComparator();
			break;
		case STARTED_DATE_DESC:
			comparator = new ProcessInstanceStartedDateComparatorDesc();
			break;
		case ENDED_DATE_DESC:
			comparator = new ProcessInstanceEndedDateComparatorDesc();
			break;
		case INSTANCE_NUMBER_DESC:
			comparator = new ProcessInstanceNbComparatorDesc();
			break;
		case INSTANCE_UUID_DESC:
			comparator = new ProcessInstanceUUIDComparatorDesc();
			break;
		case DEFAULT:
			comparator = new ProcessInstanceLastUpdateComparator();
			break;
		}
    //retrieve maximum (maybe less) fromIndex+pageSize elements.
    //All returned elements must be younger or equal to the older elements of the current list		
    if (!newProcessInstances.isEmpty()) {
      processInsts.addAll(newProcessInstances);
      //sort the list and keep only fromIndex+pageSize elements
      Collections.sort(processInsts, comparator);
      processInsts = Misc.subList(InternalProcessInstance.class, processInsts, 0, fromIndex+pageSize);
    }
  }

  public List<InternalProcessInstance> getMostRecentProcessInstances(int maxResults, long time) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentProcessInstances(maxResults, time));
    }
    return processInsts;
  }
  
  public List<InternalProcessInstance> getMostRecentProcessInstances(
			int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentProcessInstances(maxResults, time, pagingCriterion));
    }
    return processInsts;
	}

  public List<InternalProcessInstance> getMostRecentParentProcessInstances(int maxResults, long time) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentParentProcessInstances(maxResults, time));
    }
    return processInsts;
  }
  
  public List<InternalProcessInstance> getMostRecentParentProcessInstances(
			int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentParentProcessInstances(maxResults, time, pagingCriterion));
    }
    return processInsts;
	}
  
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getParentProcessInstances(fromIndex, pageSize));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getParentProcessInstances(0, fromIndex + pageSize));
      } else {
        mergePaginatedProcessInstances(processInsts, querier.getMostRecentParentProcessInstances(fromIndex + pageSize, 
        		getOldestTime(processInsts)), querier, fromIndex, pageSize, ProcessInstanceCriterion.DEFAULT);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, ProcessInstanceCriterion.DEFAULT);
    //PaginatedMethod firstPaginatedMethod = new PaginatedMethod("getParentProcessInstances", new Class[]{int.class, int.class}, 0, fromIndex + pageSize);
    //PaginatedMethod secondPaginatedMethod = new PaginatedMethod("getMostRecentParentProcessInstances", new Class[]{int.class, long.class}, fromIndex + pageSize);
    //return getInstancesPaging(firstPaginatedMethod, secondPaginatedMethod, fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex,
			int pageSize, ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getParentProcessInstances(fromIndex, pageSize, pagingCriterion));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getParentProcessInstances(0, fromIndex + pageSize, pagingCriterion));
      } else {
      	mergePaginatedProcessInstances(processInsts, querier.getMostRecentParentProcessInstances(fromIndex + pageSize, 
        		getOldestTime(processInsts), pagingCriterion), querier, fromIndex, pageSize, pagingCriterion);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, pagingCriterion);
	}

  public List<InternalProcessInstance> getParentProcessInstances(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {    
    if (processUUIDs == null || processUUIDs.isEmpty()){
      return Collections.emptyList();
    }
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getParentProcessInstances(processUUIDs, fromIndex, pageSize, pagingCriterion));
      return processInsts;
    }
    
    for (Querier querier : queriers) {      
        processInsts.addAll(querier.getParentProcessInstances(processUUIDs, fromIndex, pageSize, pagingCriterion));      
    }    
    Collections.sort(processInsts, new InternalProcessInstanceComparator(pagingCriterion));
    
    return Misc.subList(InternalProcessInstance.class, processInsts, 0, pageSize);
  }

  public List<InternalProcessInstance> getParentProcessInstancesExcept(
      Set<ProcessDefinitionUUID> exceptions, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    if (exceptions == null || exceptions.isEmpty()){
      return getParentProcessInstances(fromIndex, pageSize, pagingCriterion);
    }
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getParentProcessInstancesExcept(exceptions, fromIndex, pageSize, pagingCriterion));
      return processInsts;
    }
    
    for (Querier querier : queriers) {      
        processInsts.addAll(querier.getParentProcessInstancesExcept(exceptions, fromIndex, pageSize, pagingCriterion));      
    }    
    Collections.sort(processInsts, new InternalProcessInstanceComparator(pagingCriterion));
    
    return Misc.subList(InternalProcessInstance.class, processInsts, 0, pageSize);
  }

  public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentMatchingProcessInstances(instanceUUIDs, maxResults, time));
    }
    return processInsts;
  }
    
  public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(
			Set<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time,
			ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentMatchingProcessInstances(instanceUUIDs, maxResults, time, pagingCriterion));
    }
    return processInsts;
	}
    
  public List<InternalProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstances(instanceUUIDs, fromIndex, pageSize));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstances(instanceUUIDs, 0, fromIndex + pageSize));
      } else {
        mergePaginatedProcessInstances(processInsts, querier.getMostRecentMatchingProcessInstances(instanceUUIDs, fromIndex + pageSize, 
        		getOldestTime(processInsts)), querier, fromIndex, pageSize, ProcessInstanceCriterion.DEFAULT);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, ProcessInstanceCriterion.DEFAULT);
    //PaginatedMethod firstPaginatedMethod = new PaginatedMethod("getProcessInstances", new Class[]{Collection.class, int.class, int.class}, instanceUUIDs, 0, fromIndex + pageSize);
    //PaginatedMethod secondPaginatedMethod = new PaginatedMethod("getMostRecentMatchingProcessInstances", new Class[]{Collection.class, int.class, long.class}, instanceUUIDs, fromIndex + pageSize);
    //return getInstancesPaging(firstPaginatedMethod, secondPaginatedMethod, fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getProcessInstancesWithInstanceUUIDs(
			Set<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize,
			ProcessInstanceCriterion pagingCriterion) {
  	List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstancesWithInstanceUUIDs(instanceUUIDs, fromIndex, pageSize, pagingCriterion));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstancesWithInstanceUUIDs(instanceUUIDs, 0, fromIndex + pageSize, pagingCriterion));
      } else {
      	mergePaginatedProcessInstances(processInsts, querier.getMostRecentMatchingProcessInstances(instanceUUIDs, fromIndex + pageSize, 
        		getOldestTime(processInsts), pagingCriterion), querier, fromIndex, pageSize, pagingCriterion);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, pagingCriterion);
	}

  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, int maxResults, long time) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentProcessesProcessInstances(definitionUUIDs, maxResults, time));
    }
    return processInsts;
  }
  
  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, 
  		int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    for (Querier querier : queriers) {
      processInsts.addAll(querier.getMostRecentProcessesProcessInstances(definitionUUIDs, maxResults, time, pagingCriterion));
    }
    return processInsts;
  }
  
  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstances(definitionUUIDs, fromIndex, pageSize));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstances(definitionUUIDs, 0, fromIndex + pageSize));
      } else {
        mergePaginatedProcessInstances(processInsts, querier.getMostRecentProcessesProcessInstances(definitionUUIDs, fromIndex + pageSize, 
        		getOldestTime(processInsts)), querier, fromIndex, pageSize, ProcessInstanceCriterion.DEFAULT);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, ProcessInstanceCriterion.DEFAULT);
    //PaginatedMethod firstPaginatedMethod = new PaginatedMethod("getProcessInstances", new Class[]{Collection.class, int.class, int.class}, definitionUUIDs, 0, fromIndex + pageSize);
    //PaginatedMethod secondPaginatedMethod = new PaginatedMethod("getMostRecentProcessesProcessInstances", new Class[]{Collection.class, int.class, long.class}, definitionUUIDs, fromIndex + pageSize);
    //return getInstancesPaging(firstPaginatedMethod, secondPaginatedMethod, fromIndex, pageSize);
  }
  
  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs, 
  		int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    List<InternalProcessInstance> processInsts = new ArrayList<InternalProcessInstance>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processInsts.addAll(querier.getProcessInstances(definitionUUIDs, fromIndex, pageSize, pagingCriterion));
      return processInsts;
    }
    for (Querier querier : queriers) {
      if (processInsts.size() < fromIndex + pageSize) {
        //buid a set with at least all the first elements
        processInsts.addAll(querier.getProcessInstances(definitionUUIDs, 0, fromIndex + pageSize, pagingCriterion));
      } else {
      	mergePaginatedProcessInstances(processInsts, querier.getMostRecentProcessesProcessInstances(
      			definitionUUIDs, fromIndex + pageSize, getOldestTime(processInsts), pagingCriterion), 
      			querier, fromIndex, pageSize, pagingCriterion);
      }
    }
    return getInstancesSubset(fromIndex, pageSize, processInsts, pagingCriterion);    
  }

  private List<InternalProcessInstance> getInstancesSubset(int fromIndex, int pageSize, 
  		List<InternalProcessInstance> processInstances, ProcessInstanceCriterion pagingCriterion) {
    if (processInstances == null || processInstances.isEmpty() || fromIndex > processInstances.size()) {
      return Collections.emptyList();
    }
    int toIndex = fromIndex + pageSize;
    if (toIndex > processInstances.size()) {
      toIndex = processInstances.size();
    }
    Comparator<InternalProcessInstance> comparator = null;    
    switch (pagingCriterion) {
		case LAST_UPDATE_ASC:
			comparator = new ProcessInstanceLastUpdateComparatorAsc();
			break;
		case STARTED_DATE_ASC:
			comparator = new ProcessInstanceStartedDateComparatorAsc();
			break;
		case ENDED_DATE_ASC:
			comparator = new ProcessInstanceEndedDateComparatorAsc();
			break;
		case INSTANCE_NUMBER_ASC:
			comparator = new ProcessInstanceNbComparatorAsc();
			break;
		case INSTANCE_UUID_ASC:
			comparator = new ProcessInstanceUUIDComparatorAsc();
			break;
		case LAST_UPDATE_DESC:
			comparator = new ProcessInstanceLastUpdateComparator();
			break;
		case STARTED_DATE_DESC:
			comparator = new ProcessInstanceStartedDateComparatorDesc();
			break;
		case ENDED_DATE_DESC:
			comparator = new ProcessInstanceEndedDateComparatorDesc();
			break;
		case INSTANCE_NUMBER_DESC:
			comparator = new ProcessInstanceNbComparatorDesc();
			break;
		case INSTANCE_UUID_DESC:
			comparator = new ProcessInstanceUUIDComparatorDesc();
			break;
		case DEFAULT:
			comparator = new ProcessInstanceLastUpdateComparator();
			break;
		}
    Collections.sort(processInstances, comparator);
    return Misc.subList(InternalProcessInstance.class, processInstances, fromIndex, toIndex);
  }

  public List<InternalProcessDefinition> getProcesses(int fromIndex, int pageSize) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcesses(fromIndex, pageSize));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcesses(0, fromIndex + pageSize));
    }
    return getProcessesSubset(fromIndex, pageSize, processes, ProcessDefinitionCriterion.DEFAULT);
  }

  public List<InternalProcessDefinition> getProcesses(int fromIndex,
      int pageSize, ProcessDefinitionCriterion pagingCriterion) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcesses(fromIndex, pageSize, pagingCriterion));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcesses(0, fromIndex + pageSize, pagingCriterion));
    }    
    return getProcessesSubset(fromIndex, pageSize, processes, pagingCriterion);
  }

  public List<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcesses(definitionUUIDs, fromIndex, pageSize));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcesses(definitionUUIDs, 0, fromIndex + pageSize));
    }
    return getProcessesSubset(fromIndex, pageSize, processes, ProcessDefinitionCriterion.DEFAULT);
  }

  public List<InternalProcessDefinition> getProcesses(
      Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcesses(definitionUUIDs, fromIndex, pageSize, pagingCriterion));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcesses(definitionUUIDs, 0, fromIndex + pageSize, pagingCriterion));
    }
    return getProcessesSubset(fromIndex, pageSize, processes, pagingCriterion);
  }

  public List<InternalProcessDefinition> getProcessesExcept(Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcessesExcept(processUUIDs, fromIndex, pageSize));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcessesExcept(processUUIDs, 0, fromIndex + pageSize));
    }
    return getProcessesSubset(fromIndex, pageSize, processes, ProcessDefinitionCriterion.DEFAULT);
  }

  public List<InternalProcessDefinition> getProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    List<InternalProcessDefinition> processes = new ArrayList<InternalProcessDefinition>();
    if (queriers.size() == 1) {
      //can perform directly the query
      final Querier querier = queriers.get(0);
      processes.addAll(querier.getProcessesExcept(processUUIDs, fromIndex, pageSize, pagingCriterion));
      return processes;
    }
    for (Querier querier : queriers) {
      processes.addAll(querier.getProcessesExcept(processUUIDs, 0, fromIndex + pageSize, pagingCriterion));
    }
    return getProcessesSubset(fromIndex, pageSize, processes, pagingCriterion);
  }

  public Set<Category> getCategories(Collection<String> categoryNames) {
    Set<Category> result = new HashSet<Category>();
    for (Querier querier : queriers) {
      result.addAll(querier.getCategories(categoryNames));
    }
    return result;
  }

  public Set<Category> getAllCategories() {
    Set<Category> result = new HashSet<Category>();
    for (Querier querier : queriers) {
      result.addAll(querier.getAllCategories());
    }
    return result;
  }

  public Set<Category> getAllCategoriesExcept(Set<String> uuids) {
    Set<Category> result = new HashSet<Category>();
    for (Querier querier : queriers) {
      result.addAll(querier.getAllCategoriesExcept(uuids));
    }
    return result;
  }
  
  public Set<CategoryImpl> getCategoriesByUUIDs(Set<CategoryUUID> uuids) {
    Set<CategoryImpl> result = new HashSet<CategoryImpl>();
    for (Querier querier : queriers) {
      result.addAll(querier.getCategoriesByUUIDs(uuids));
    }
    return result;
  }
  
  public CategoryImpl getCategoryByUUID(String uuid) {
    CategoryImpl category=null;
    int i = 0;
    Querier querier;
    while (category==null && i<queriers.size()) {
      querier = queriers.get(i);
      category = querier.getCategoryByUUID(uuid);
      i++;
    }
    return category;
  }

  public Set<ProcessDefinitionUUID> getProcessUUIDsFromCategory(String category) {
    Set<ProcessDefinitionUUID> result = new HashSet<ProcessDefinitionUUID>();
    for (Querier querier : queriers) {
      result.addAll(querier.getProcessUUIDsFromCategory(category));
    }
    return result;
  }

  private List<InternalProcessDefinition> getProcessesSubset(int fromIndex, int pageSize, List<InternalProcessDefinition> processes, ProcessDefinitionCriterion pagingCriterion) {
    int toIndex = fromIndex + pageSize;
    if (toIndex > processes.size()) {
      toIndex = processes.size();
    }
    Collections.sort(processes, new InternalProcessDefinitionComparator(pagingCriterion));
    return Misc.subList(InternalProcessDefinition.class, processes, fromIndex, toIndex);
  }

  public List<Object> search(SearchQueryBuilder query, int firstResult, int maxResults, Class<?> indexClass) {
    List<Object> entities = new ArrayList<Object>();
    if (queriers.size() == 1) {
      final Querier querier = queriers.get(0);
      entities.addAll(querier.search(query, firstResult, maxResults, indexClass));
    } else {
      for (Querier querier : queriers) {
        List<Object> temp = querier.search(query, firstResult, maxResults, indexClass);
        entities.addAll(temp);
        if (entities.size() > maxResults) {
          entities = entities.subList(firstResult, maxResults);
          break;
        }        
      }
    }
    return entities;
  }

  public int search(SearchQueryBuilder query, Class<?> indexClass) {
    int count = 0;
    for (Querier querier : queriers) {
      count += querier.search(query, indexClass);
    }
    return count;
  }

  public Set<ActivityDefinitionUUID> getProcessTaskUUIDs(ProcessDefinitionUUID definitionUUID) {
    Set<ActivityDefinitionUUID> result = new HashSet<ActivityDefinitionUUID>();
    for (Querier querier : queriers) {
      result.addAll(querier.getProcessTaskUUIDs(definitionUUID));
    }
    return result;
  }

  public boolean processExists(ProcessDefinitionUUID definitionUUID) {
    for (Querier querier : queriers) {
      boolean querierExists = querier.processExists(definitionUUID);
      if (querierExists) {
        return true;
      }
    }
    return false;
  }

  public List<Long> getProcessInstancesDuration(Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getProcessInstancesDuration(since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getProcessInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getProcessInstancesDuration(processUUID, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getProcessInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getProcessInstancesDurationFromProcessUUIDs(processUUIDs, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesExecutionTime(Date since, Date until) {
    List<Long> executionTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      executionTimes.addAll(querier.getActivityInstancesExecutionTime(since, until));
    }
    Collections.sort(executionTimes);
    return executionTimes;
  }

  public List<Long> getActivityInstancesExecutionTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    List<Long> executionTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      executionTimes.addAll(querier.getActivityInstancesExecutionTime(processUUID, since, until));
    }
    Collections.sort(executionTimes);
    return executionTimes;
  }

  public List<Long> getActivityInstancesExecutionTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    List<Long> executionTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      executionTimes.addAll(querier.getActivityInstancesExecutionTimeFromProcessUUIDs(processUUIDs, since, until));
    }
    Collections.sort(executionTimes);
    return executionTimes;  
  }

  public List<Long> getActivityInstancesExecutionTime(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    List<Long> executionTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      executionTimes.addAll(querier.getActivityInstancesExecutionTime(activityUUID, since, until));
    }
    Collections.sort(executionTimes);
    return executionTimes;
  }

  public List<Long> getActivityInstancesExecutionTimeFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    List<Long> executionTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      executionTimes.addAll(querier.getActivityInstancesExecutionTimeFromActivityUUIDs(activityUUIDs, since, until));
    }
    Collections.sort(executionTimes);
    return executionTimes;
  }

  public List<Long> getTaskInstancesWaitingTime(Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTime(since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTime(processUUID, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeFromProcessUUIDs(processUUIDs, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTime(
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTime(taskUUID, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeFromTaskUUIDs(
      Set<ActivityDefinitionUUID> taskUUIDs, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeFromTaskUUIDs(taskUUIDs, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeOfUser(username, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeOfUser(username, processUUID, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(
      String username, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(username, processUUIDs, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeOfUser(username, taskUUID, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(
      String username, Set<ActivityDefinitionUUID> taskUUIDs, Date since,
      Date until) {
    List<Long> waitingTimes = new ArrayList<Long>();
    for (Querier querier : queriers) {
      waitingTimes.addAll(querier.getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(username, taskUUIDs, since, until));
    }
    Collections.sort(waitingTimes);
    return waitingTimes;
  }

  public List<Long> getActivityInstancesDuration(Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDuration(since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDuration(processUUID, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDurationFromProcessUUIDs(processUUIDs, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDuration(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDuration(activityUUID, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDurationFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDurationFromActivityUUIDs(activityUUIDs, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, Date since, Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDurationByActivityType(activityType, since, until));
    }
    Collections.sort(durations);
    return durations;
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    List<Long> durations = new ArrayList<Long>();
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDurationByActivityType(activityType, processUUID, since, until));
    }
    Collections.sort(durations);
    return durations;  
  }

  public List<Long> getActivityInstancesDurationByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    List<Long> durations = new ArrayList<Long>();
    if (queriers.size() == 1) {
      return queriers.get(0).getActivityInstancesDurationByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
    }
    for (Querier querier : queriers) {
      durations.addAll(querier.getActivityInstancesDurationByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until));
    }
    Collections.sort(durations);
    return durations; 
  }

  public long getNumberOfCreatedProcessInstances(Date since, Date until) {    
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedProcessInstances(since, until);
    }
    return nb; 
  }

  public long getNumberOfCreatedProcessInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedProcessInstances(processUUID, since, until);
    }
    return nb;
  }

  public long getNumberOfCreatedActivityInstances(Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstances(since, until);
    }
    return nb;
  }

  public long getNumberOfCreatedActivityInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstances(processUUID, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstancesFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstancesFromProcessUUIDs(processUUIDs, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstances(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstances(activityUUID, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstancesFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstancesFromActivityUUIDs(activityUUIDs, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, Date since, Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstancesByActivityType(activityType, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstancesByActivityType(activityType, processUUID, since, until);
    }    
    return nb;
  }

  public long getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    long nb = 0;
    for (Querier querier : queriers) {
      nb += querier.getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
    }    
    return nb;
  }

  public boolean containsOtherActiveActivities(ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityUUID) {
    for (Querier querier : queriers) {
      if (querier.containsOtherActiveActivities(instanceUUID, activityUUID)) {
        return true;
      }
    }
    return false;
  }
  
}
