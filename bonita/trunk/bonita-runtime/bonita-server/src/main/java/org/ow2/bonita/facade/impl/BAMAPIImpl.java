/**
 * Copyright (C) 2009  BonitaSoft S.A.
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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.ow2.bonita.facade.BAMAPI;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.facade.exception.MonitoringException;
import org.ow2.bonita.facade.monitoring.model.JvmMBean;
import org.ow2.bonita.facade.monitoring.model.impl.MBeanUtil;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.util.DateUtil;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class BAMAPIImpl implements BAMAPI {

	private String queryList;
	
  protected BAMAPIImpl(final String queryList) {
  	this.queryList = queryList;
  }

  private String getQueryList() {
  	return this.queryList;
  }
  
  public List<Integer> getNumberOfExecutingCasesPerDay(Date since) {
    Date now = new Date();
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfExecutingCasesPerDay(since, now);
  }

  public List<Integer> getNumberOfFinishedCasesPerDay(Date since) {
    Date now = new Date();
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfFinishedCasesPerDay(since, now);
  }

  public int getNumberOfOpenSteps() {
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfOpenSteps();
  }

  public List<Integer> getNumberOfOpenStepsPerDay(Date since) {
    Date now = new Date();
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfOpenStepsPerDay(since, now);
  }

  public int getNumberOfOverdueSteps() {
    Date now = new Date();
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfOverdueSteps(now);
  }

  public int getNumberOfStepsAtRisk(int remainingDays) {
    if (remainingDays < 0) {
      throw new IllegalArgumentException("The number of remaining days is negative");
    }
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfStepsAtRisk(currentDate, atRisk);
  }

  public int getNumberOfUserOpenSteps() {
    String userId = EnvTool.getUserId();
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfUserOpenSteps(userId);
  }

  public int getNumberOfUserOverdueSteps() {
    Date now = new Date();
    String userId = EnvTool.getUserId();
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfUserOverdueSteps(userId, now);
  }

  public int getNumberOfUserStepsAtRisk(int remainingDays) {
    if (remainingDays < 0) {
      throw new IllegalArgumentException("The number of remaining days is negative");
    }
    String userId = EnvTool.getUserId();
    Date currentDate = new Date();
    Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(currentDate);
    Date atRisk = DateUtil.backTo(beginningOfTheDay, -(remainingDays+1));
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfUserStepsAtRisk(userId, currentDate, atRisk);
  }

  public int getNumberOfFinishedSteps(int priority, Date since) {
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfFinishedSteps(priority, since);
  }

  public int getNumberOfOpenSteps(int priority) {
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfOpenSteps(priority);
  }

  public int getNumberOfUserFinishedSteps(int priority, Date since) {
    String userId = EnvTool.getUserId();
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfUserFinishedSteps(userId, priority, since);
  }

  public int getNumberOfUserOpenSteps(int priority) {
    String userId = EnvTool.getUserId();
    return EnvTool.getJournalQueriers(getQueryList()).getNumberOfUserOpenSteps(userId, priority);
  }

  public List<Long> getProcessInstancesDuration(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getHistoryQueriers(getQueryList()).getProcessInstancesDuration(since, until);
  }

  public List<Long> getProcessInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getHistoryQueriers(getQueryList()).getProcessInstancesDuration(processUUID, since, until);
  }

  public List<Long> getProcessInstancesDuration(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getHistoryQueriers(getQueryList()).getProcessInstancesDurationFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesExecutionTime(since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesExecutionTime(processUUID, since, until);    
  }

  public List<Long> getActivityInstancesExecutionTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesExecutionTimeFromProcessUUIDs(processUUIDs, since, until);
  }
  
  public List<Long> getActivityInstancesExecutionTime(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesExecutionTime(activityUUID, since, until);  
  }

  public List<Long> getActivityInstancesExecutionTimeFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (activityUUIDs == null || activityUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesExecutionTimeFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTime(since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTime(processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTime(taskUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromTaskUUIDs(
      Set<ActivityDefinitionUUID> tasksUUIDs, Date since, Date until) {
    Misc.checkArgsNotNull(tasksUUIDs);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (tasksUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeFromTaskUUIDs(tasksUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      Date since, Date until) {
    Misc.checkArgsNotNull(username);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeOfUser(username, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    Misc.checkArgsNotNull(username, processUUID);    
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeOfUser(username, processUUID, since, until);    
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(
      String username, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    Misc.checkArgsNotNull(username, processUUIDs);    
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    if (processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(username, processUUIDs, since, until);   
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    Misc.checkArgsNotNull(username, taskUUID);    
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeOfUser(username, taskUUID, since, until);    
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(
      String username, Set<ActivityDefinitionUUID> tasksUUIDs, Date since,
      Date until) {
    Misc.checkArgsNotNull(username, tasksUUIDs);    
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }    
    if (tasksUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(username, tasksUUIDs, since, until);   
  }

  public List<Long> getActivityInstancesDuration(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDuration(since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    Misc.checkArgsNotNull(processUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDuration(processUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    Misc.checkArgsNotNull(processUUIDs);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDurationFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    Misc.checkArgsNotNull(activityUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDuration(activityUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    Misc.checkArgsNotNull(activityUUIDs);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    if (activityUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDurationFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, Date since, Date until) {
    Misc.checkArgsNotNull(activityType);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
        
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDurationByActivityType(activityType, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    Misc.checkArgsNotNull(activityType, processUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
        
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDurationByActivityType(activityType, processUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
        
    return EnvTool.getAllQueriers(getQueryList()).getActivityInstancesDurationByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }

  public long getNumberOfCreatedProcessInstances(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
        
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedProcessInstances(since, until);
  }

  public long getNumberOfCreatedProcessInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    Misc.checkArgsNotNull(processUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedProcessInstances(processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstances(Date since, Date until) {
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstances(since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    Misc.checkArgsNotNull(processUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstances(processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    if (processUUIDs == null || processUUIDs.isEmpty()){
      return 0;
    }
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstancesFromProcessUUIDs(processUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    Misc.checkArgsNotNull(activityUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstances(activityUUID, since, until);    
  }

  public long getNumberOfCreatedActivityInstancesFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    if (activityUUIDs == null || activityUUIDs.isEmpty()) {
      return 0;
    }
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstancesFromActivityUUIDs(activityUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, Date since, Date until) {
    Misc.checkArgsNotNull(activityType);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstancesByActivityType(activityType, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until) {
    Misc.checkArgsNotNull(activityType, processUUID);
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstancesByActivityType(activityType, processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    Misc.checkArgsNotNull(activityType);
    if (processUUIDs == null || processUUIDs.isEmpty()) {
      return 0;
    }
    if (since.getTime() > until.getTime()) {
      throw new IllegalArgumentException("The since date is greater than the until date");
    }
    return EnvTool.getAllQueriers(getQueryList()).getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }
  /*
   * (non-Javadoc)
   * 
   * @see org.ow2.bonita.facade.BAMAPI#getMemoryUsage()
   */
  @Override
  public long getCurrentMemoryUsage() throws MonitoringException {
      long result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Long) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "CurrentMemoryUsage");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }
  
  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getMemoryUsagePercentage()
   */
  @Override
  public float getMemoryUsagePercentage() throws MonitoringException {
      float result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Float) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "MemoryUsagePercentage");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }    

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getSystemLoadAverage()
   */
  @Override
  public double getSystemLoadAverage() throws MonitoringException {
      double result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Double) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "SystemLoadAverage");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getUpTime()
   */
  @Override
  public long getUpTime() throws MonitoringException {
      long result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Long) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "UpTime");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getStartTime()
   */
  @Override
  public long getStartTime() throws MonitoringException {
      long result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();

          try {
              result = (Long) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "StartTime");
          } catch (AttributeNotFoundException e) {
              throw new MonitoringException(e.getMessage(),e);
          } catch (javax.management.InstanceNotFoundException e) {
              throw new MonitoringException(e.getMessage(),e);
          } catch (MalformedObjectNameException e) {
              throw new MonitoringException(e.getMessage(),e);
          } catch (MBeanException e) {
              throw new MonitoringException(e.getMessage(),e);
          } catch (ReflectionException e) {
              throw new MonitoringException(e.getMessage(),e);
          } catch (NullPointerException e) {
              throw new MonitoringException(e.getMessage(),e);
          }

      return result;
  }

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getTotalThreadsCpuTime()
   */
  @Override
  public long getTotalThreadsCpuTime() throws MonitoringException {
      long result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Long) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "TotalThreadsCpuTime");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.BAMAPI#getThreadCount()
   */
  @Override
  public int getThreadCount() throws MonitoringException {
      int result = 0;
      final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
      try {
          result = (Integer) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "ThreadCount");
      } catch (MalformedObjectNameException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (MBeanException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (ReflectionException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (AttributeNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      } catch (javax.management.InstanceNotFoundException e) {
          throw new MonitoringException(e.getMessage(),e);
      }
      return result;
  }
    
    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getAvailableProcessors()
     */
    @Override
    public int getAvailableProcessors() throws MonitoringException {
        int result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (Integer) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "AvailableProcessors");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getOSArch()
     */
    @Override
    public String getOSArch() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "OSArch");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;
    }    
    
    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getOSName()
     */
    @Override
    public String getOSName() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "OSName");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;        
    }

    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getOSVersion()
     */
    @Override
    public String getOSVersion() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "OSVersion");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;       
    }

    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getJvmName()
     */
    @Override
    public String getJvmName() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "JvmName");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;    
    }

    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getJvmVendor()
     */
    @Override
    public String getJvmVendor() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "JvmVendor");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;    
    }

    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getJvmVersion()
     */
    @Override
    public String getJvmVersion() throws MonitoringException {
        String result;
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = (String) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "JvmVersion");
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;   
    }
    
    /* (non-Javadoc)
     * @see org.ow2.bonita.facade.BAMAPI#getJvmSystemProperties()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String,String> getJvmSystemProperties() throws MonitoringException {
        Map<String,String> result = new HashMap<String,String>();
        final MBeanServer mbeanServer = MBeanUtil.getMBeanServer();
        try {
            result = ((Map<String,String>) mbeanServer.getAttribute(new ObjectName(JvmMBean.JVM_MBEAN_NAME), "JvmSystemProperties"));
        } catch (MalformedObjectNameException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (MBeanException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (ReflectionException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (AttributeNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        } catch (javax.management.InstanceNotFoundException e) {
            throw new MonitoringException(e.getMessage(),e);
        }
        return result;   
    }
}
