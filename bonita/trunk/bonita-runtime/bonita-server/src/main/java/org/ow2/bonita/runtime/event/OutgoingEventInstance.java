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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.VariableUtil;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class OutgoingEventInstance extends EventInstance {

	private static final long serialVersionUID = -799578376710809099L;

  protected Map<String, Variable> parameters;
  protected long overdue;

	public OutgoingEventInstance() {}

	public OutgoingEventInstance(final String name, final String processName, final String activityName, final Map<String, Object> parameters,
			final ProcessInstanceUUID instanceUUID, final ActivityInstanceUUID activityUUID, final long overdue) {
		super(name, processName, activityName, instanceUUID, activityUUID);

		if (parameters != null) {
			this.parameters = new HashMap<String, Variable>();
			final ProcessInstance instance = EnvTool.getJournalQueriers().getProcessInstance(instanceUUID);
			for (Entry<String, Object> parameter : parameters.entrySet()) {
        String key = parameter.getKey();
        Object value = parameter.getValue();
        this.parameters.put(key, VariableUtil.createVariable(instance.getProcessDefinitionUUID(), key, value));
      }
		}
		this.overdue = overdue;
	}

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((activityName == null) ? 0 : activityName.hashCode());
    result = prime * result
        + ((activityUUID == null) ? 0 : activityUUID.hashCode());
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result
        + ((instanceUUID == null) ? 0 : instanceUUID.hashCode());
    result = prime * result + ((lockOwner == null) ? 0 : lockOwner.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (int) (overdue ^ (overdue >>> 32));
    result = prime * result
        + ((processName == null) ? 0 : processName.hashCode());
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
    OutgoingEventInstance other = (OutgoingEventInstance) obj;
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
    if (overdue != other.overdue)
      return false;
    if (processName == null) {
      if (other.processName != null)
        return false;
    } else if (!processName.equals(other.processName))
      return false;
    return true;
  }

	public Map<String, Object> getParameters() {
		if (parameters == null) {
			return Collections.emptyMap();
    }
    Map<String, Object> result = new HashMap<String, Object>();
    for (Map.Entry<String, Variable> entry : parameters.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getValue());
    }
    return result;
  }

  public long getOverdue() {
    return overdue;
  }
  
  @Override
  public String toString() {
    return "OutgoingEventInstance [activityName=" + activityName
        + ", activityUUID=" + activityUUID + ", id=" + id + ", instanceUUID="
        + instanceUUID + ", lockOwner=" + lockOwner + ", name=" + name
        + ", overdue=" + overdue + ", parameters=" + parameters
        + ", processName=" + processName + "]";
  }

}
