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
package org.ow2.bonita.runtime.event;

import java.util.HashSet;
import java.util.Set;

import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class IncomingEventInstance extends EventInstance {

	private static final long serialVersionUID = -2391431532148921303L;

	protected ActivityDefinitionUUID activityDefinitionUUID;
  protected String expression;
  protected String executionUUID;
  protected String signal;
  protected long enableTime;
  protected boolean executionLocked;
  protected String exception;
  protected int retries;
  protected boolean permanent = false;
  protected Set<Long> incompatibleEvents;
  protected ProcessInstanceUUID eventSubProcessRootInstanceUUID;

  public IncomingEventInstance() {}

  public IncomingEventInstance(final String name, final String expression, 
      final ProcessInstanceUUID instanceUUID, final ActivityDefinitionUUID activityDefinitionUUID, 
      final ActivityInstanceUUID activityUUID, final String processName, final String activityName, 
      final String executionUUID, final String signal, final long enableTime, final boolean executionLocked) {
  	super(name, processName, activityName, instanceUUID, activityUUID);
  	this.activityDefinitionUUID = activityDefinitionUUID;
    this.expression = expression;
    this.executionUUID = executionUUID;
    this.signal = signal;
    this.enableTime = enableTime;
    this.executionLocked = executionLocked;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime
        * result
        + ((activityDefinitionUUID == null) ? 0 : activityDefinitionUUID
            .hashCode());
    result = prime * result
        + ((activityName == null) ? 0 : activityName.hashCode());
    result = prime * result
        + ((activityUUID == null) ? 0 : activityUUID.hashCode());
    result = prime * result + (int) (enableTime ^ (enableTime >>> 32));
    result = prime * result + ((exception == null) ? 0 : exception.hashCode());
    result = prime * result + (executionLocked ? 1231 : 1237);
    result = prime * result
        + ((executionUUID == null) ? 0 : executionUUID.hashCode());
    result = prime * result
        + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result
        + ((instanceUUID == null) ? 0 : instanceUUID.hashCode());
    result = prime * result + ((lockOwner == null) ? 0 : lockOwner.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result
        + ((processName == null) ? 0 : processName.hashCode());
    result = prime * result + retries;
    result = prime * result + ((signal == null) ? 0 : signal.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IncomingEventInstance other = (IncomingEventInstance) obj;
    if (activityDefinitionUUID == null) {
      if (other.activityDefinitionUUID != null)
        return false;
    } else if (!activityDefinitionUUID.equals(other.activityDefinitionUUID))
      return false;
    if (activityName == null) {
      if (other.activityName != null)
        return false;
    } else if (!activityName.equals(other.activityName))
      return false;
    if (activityUUID == null) {
      if (other.activityUUID != null)
        return false;
    } else if (!activityUUID.equals(other.activityUUID))
      return false;
    if (enableTime != other.enableTime)
      return false;
    if (exception == null) {
      if (other.exception != null)
        return false;
    } else if (!exception.equals(other.exception))
      return false;
    if (executionLocked != other.executionLocked)
      return false;
    if (executionUUID == null) {
      if (other.executionUUID != null)
        return false;
    } else if (!executionUUID.equals(other.executionUUID))
      return false;
    if (expression == null) {
      if (other.expression != null)
        return false;
    } else if (!expression.equals(other.expression))
      return false;
    if (id != other.id)
      return false;
    if (instanceUUID == null) {
      if (other.instanceUUID != null)
        return false;
    } else if (!instanceUUID.equals(other.instanceUUID))
      return false;
    if (lockOwner == null) {
      if (other.lockOwner != null)
        return false;
    } else if (!lockOwner.equals(other.lockOwner))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (processName == null) {
      if (other.processName != null)
        return false;
    } else if (!processName.equals(other.processName))
      return false;
    if (retries != other.retries)
      return false;
    if (signal == null) {
      if (other.signal != null)
        return false;
    } else if (!signal.equals(other.signal))
      return false;
    return true;
  }
  
  public String getExecutionUUID() {
  	return executionUUID;
  }

  public int getRetries() {
    return retries;
  }
  
  public void setRetries(int retries) {
    this.retries = retries;
  }
  
  public void setException(String exception) {
    this.exception = exception;
  }
  
  public String getException() {
    return exception;
  }
  
	public ActivityDefinitionUUID getActivityDefinitionUUID() {
    return activityDefinitionUUID;
  }

  public String getExpression() {
    return expression;
  }
  
  public String getSignal() {
    return this.signal;
  }
  
  public long getEnableTime() {
    return enableTime;
  }

  public boolean isExecutionLocked() {
    return executionLocked;
  }

  public void setPermanent(boolean permanent) {
    this.permanent = permanent;
  }
  
  public boolean isPermanent() {
    return permanent;
  }

  public void setEventSubProcessRootInstanceUUID(ProcessInstanceUUID eventSubProcessRootInstanceUUID) {
    this.eventSubProcessRootInstanceUUID = eventSubProcessRootInstanceUUID;
  }

  public ProcessInstanceUUID getEventSubProcessRootInstanceUUID() {
    return eventSubProcessRootInstanceUUID;
  }

  @Override
  public String toString() {
    return "IncomingEventInstance [activityDefinitionUUID="
        + activityDefinitionUUID + ", activityName=" + activityName
        + ", activityUUID=" + activityUUID + ", enableTime=" + enableTime
        + ", exception=" + exception + ", executionLocked=" + executionLocked
        + ", executionUUID=" + executionUUID + ", expression=" + expression
        + ", id=" + id + ", instanceUUID=" + instanceUUID + ", eventSubProcessRootInstanceUUID="
        +	eventSubProcessRootInstanceUUID + ", lockOwner="
        + lockOwner + ", name=" + name + ", processName=" + processName
        + ", retries=" + retries + ", signal=" + signal + "]";
  }

  public void setEnableTime(long time) {
    enableTime = time;
  }
  
  public synchronized void addIncompatibleEvent(final long id) {
    if (incompatibleEvents == null) {
      incompatibleEvents = new HashSet<Long>();
    }
    incompatibleEvents.add(id);
  }
}
