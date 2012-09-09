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
import org.ow2.bonita.facade.BAMAPI;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.facade.exception.MonitoringException;
import org.ow2.bonita.facade.internal.RemoteBAMAPI;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.PasswordOwner;
import org.ow2.bonita.identity.auth.RESTUserOwner;
import org.ow2.bonita.identity.auth.UserOwner;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class RemoteBAMAPIImpl implements RemoteBAMAPI{

  private static final long serialVersionUID = 8977547453867812345L;  
  
  protected Map<String, BAMAPI> apis = new HashMap<String, BAMAPI>();

  protected BAMAPI getAPI(final Map<String, String> options) {
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
      apis.put(queryList, new StandardAPIAccessorImpl().getBAMAPI(queryList));
    }
    return apis.get(queryList);
  }
  
  public List<Integer> getNumberOfExecutingCasesPerDay(Date since, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfExecutingCasesPerDay(since);
  }

  public List<Integer> getNumberOfFinishedCasesPerDay(Date since, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfFinishedCasesPerDay(since);
  }

  public int getNumberOfOpenSteps(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfOpenSteps();
  }

  public List<Integer> getNumberOfOpenStepsPerDay(Date since, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfOpenStepsPerDay(since);
  }

  public int getNumberOfOverdueSteps(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfOverdueSteps();
  }

  public int getNumberOfStepsAtRisk(int remainingDays, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfStepsAtRisk(remainingDays);
  }

  public int getNumberOfUserOpenSteps(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUserOpenSteps();
  }

  public int getNumberOfUserOverdueSteps(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUserOverdueSteps();
  }

  public int getNumberOfUserStepsAtRisk(int remainingDays, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUserStepsAtRisk(remainingDays);
  }

  public int getNumberOfFinishedSteps(int priority, Date since, final Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getNumberOfFinishedSteps(priority, since);
  }

  public int getNumberOfOpenSteps(int priority, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfOpenSteps(priority);
  }

  public int getNumberOfUserFinishedSteps(int priority, Date since, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUserFinishedSteps(priority, since);
  }

  public int getNumberOfUserOpenSteps(int priority, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUserOpenSteps(priority);
  }

  public List<Long> getActivityInstancesExecutionTime(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesExecutionTime(since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesExecutionTime(processUUID, since, until);
  }

  public List<Long> getActivityInstancesExecutionTime(
      ActivityDefinitionUUID activityUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesExecutionTime(activityUUID, since, until);
  }

  public List<Long> getActivityInstancesExecutionTimeFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesExecutionTimeFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getActivityInstancesExecutionTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesExecutionTimeFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getProcessInstancesDuration(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcessInstancesDuration(since, until);
  }

  public List<Long> getProcessInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcessInstancesDuration(processUUID, since, until);
  }

  public List<Long> getProcessInstancesDuration(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getProcessInstancesDuration(processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTime(since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTime(processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTime(
      ActivityDefinitionUUID taskUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTime(taskUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeFromProcessUUIDs(processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeFromTaskUUIDs(
      Set<ActivityDefinitionUUID> taskUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeFromTaskUUIDs(taskUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDuration(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDuration(since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDuration(processUUID, since, until);
  }

  public List<Long> getActivityInstancesDuration(
      ActivityDefinitionUUID activityUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDuration(activityUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, Date since, Date until, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getActivityInstancesDurationByActivityType(activityType, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDurationByActivityType(activityType, processUUID, since, until);
  }

  public List<Long> getActivityInstancesDurationByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDurationByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDurationFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDurationFromActivityUUIDs(activityUUIDs, since, until);
  }

  public List<Long> getActivityInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getActivityInstancesDurationFromProcessUUIDs(processUUIDs, since, until);
  }

  public long getNumberOfCreatedProcessInstances(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedProcessInstances(since, until);
  }

  public long getNumberOfCreatedProcessInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedProcessInstances(processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      Date since, Date until, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeOfUser(username, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeOfUser(username, processUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ActivityDefinitionUUID taskUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeOfUser(username, taskUUID, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(
      String username, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(username, processUUIDs, since, until);
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(
      String username, Set<ActivityDefinitionUUID> taskUUIDs, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(username, taskUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstances(Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstances(since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstances(processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstances(
      ActivityDefinitionUUID activityUUID, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstances(activityUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, Date since, Date until, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstancesByActivityType(activityType, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      Type activityType, ProcessDefinitionUUID processUUID, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstancesByActivityType(activityType, processUUID, since, until);
  }

  public long getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(
      Type activityType, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until, Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(activityType, processUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstancesFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstancesFromActivityUUIDs(activityUUIDs, since, until);
  }

  public long getNumberOfCreatedActivityInstancesFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until,
      Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfCreatedActivityInstancesFromProcessUUIDs(processUUIDs, since, until);
  }

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getSystemLoadAverage(java.util.Map)
 */
@Override
public double getSystemLoadAverage(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getSystemLoadAverage();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getMemoryUsage(java.util.Map)
 */
@Override
public long getCurrentMemoryUsage(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getCurrentMemoryUsage();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getMemoryUsagePercentage(java.util.Map)
 */
@Override
public float getMemoryUsagePercentage(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getMemoryUsagePercentage();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getUpTime(java.util.Map)
 */
@Override
public long getUpTime(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getUpTime();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getStartTime(java.util.Map)
 */
@Override
public long getStartTime(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getStartTime();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getTotalThreadsCpuTime(java.util.Map)
 */
@Override
public long getTotalThreadsCpuTime(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getTotalThreadsCpuTime();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getThreadCount(java.util.Map)
 */
@Override
public int getThreadCount(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getThreadCount();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getOSArch(java.util.Map)
 */
@Override
public String getOSArch(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getOSArch();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getOSName(java.util.Map)
 */
@Override
public String getOSName(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getOSName();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getOSVersion(java.util.Map)
 */
@Override
public String getOSVersion(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getOSVersion();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getJvmName(java.util.Map)
 */
@Override
public String getJvmName(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getJvmName();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getJvmVendor(java.util.Map)
 */
@Override
public String getJvmVendor(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getJvmVendor();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getJvmVersion(java.util.Map)
 */
@Override
public String getJvmVersion(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getJvmVersion();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getAvailableProcessors(java.util.Map)
 */
@Override
public int getAvailableProcessors(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getAvailableProcessors();
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.internal.RemoteBAMAPI#getJvmSystemProperties(java.util.Map)
 */
@Override
public Map<String,String> getJvmSystemProperties(Map<String, String> options) throws RemoteException, MonitoringException {
    return getAPI(options).getJvmSystemProperties();
}

}
