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
package org.ow2.bonita.light.impl;

import java.util.Date;

import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.impl.RuntimeRecordImpl;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.util.Misc;

/**
 * @author Pierre Vigneras, Charles Souillard
 */
public class LightProcessInstanceImpl extends RuntimeRecordImpl implements LightProcessInstance {

  private static final long serialVersionUID = 8366284714927360659L;
  protected ProcessInstanceUUID parentInstanceUUID;
  protected ActivityInstanceUUID parentActivityUUID;
  protected long nb;
  protected long lastUpdate;
  protected InstanceState state;
  protected String endedBy;
  protected long endedDate;
  protected String startedBy;
  protected long startedDate;
  protected boolean isArchived;

  protected LightProcessInstanceImpl() { }

  public LightProcessInstanceImpl(ProcessDefinitionUUID processUUID, ProcessInstanceUUID instanceUUID, ProcessInstanceUUID rootInstanceUUID, long instanceNb) {
    super(processUUID, instanceUUID, rootInstanceUUID);
    this.nb = instanceNb;
    this.state = InstanceState.STARTED;
    this.lastUpdate = System.currentTimeMillis();
    this.isArchived = false;
  }

  public LightProcessInstanceImpl(final ProcessInstance processInstance) {
    super(processInstance);
    if (processInstance.getParentInstanceUUID() != null) {
      this.parentInstanceUUID = new ProcessInstanceUUID(processInstance.getParentInstanceUUID());
    }
    if (processInstance.getParentActivityUUID() != null) {
      this.parentActivityUUID = new ActivityInstanceUUID(processInstance.getParentActivityUUID());
    }

    this.nb = processInstance.getNb();
    this.lastUpdate = Misc.getTime(processInstance.getLastUpdate());
    this.state = processInstance.getInstanceState();
    this.endedBy = processInstance.getEndedBy();
    this.endedDate = Misc.getTime(processInstance.getEndedDate());
    this.startedBy = processInstance.getStartedBy();
    this.startedDate = Misc.getTime(processInstance.getStartedDate());

    this.isArchived = processInstance.isArchived();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj.getClass().equals(this.getClass()))) {
      return false;
    }
    final LightProcessInstanceImpl other = (LightProcessInstanceImpl) obj;
    if (other.getUUID() == null) {
      return getUUID() == null;
    }
    return other.getUUID().equals(getUUID());
  }

  @Override
  public int hashCode() {
    return getUUID().hashCode();
  }

  public String getEndedBy() {
    return this.endedBy;
  }

  public Date getEndedDate() {
    return Misc.getDate(this.endedDate);
  }

  public String getStartedBy() {
    return this.startedBy;
  }

  public Date getStartedDate() {
    return Misc.getDate(this.startedDate);
  }

  public InstanceState getInstanceState() {
    return this.state;
  }

  public ProcessInstanceUUID getParentInstanceUUID() {
    return this.parentInstanceUUID;
  }

  public ActivityInstanceUUID getParentActivityUUID() {
    return this.parentActivityUUID;
  }

  public ProcessInstanceUUID getUUID() {
    return getProcessInstanceUUID();
  }

  public Date getLastUpdate() {
    return Misc.getDate(lastUpdate);
  }

  public long getNb() {
    return this.nb;
  }

  public ProcessInstanceUUID getRootInstanceUUID() {
    return rootInstanceUUID;
  }


  public boolean isArchived() {
    return isArchived;
  }

  @Override
  public String toString() {
    return this.getClass().getName()
    + "[uuid: " + getUUID()
    + ", processDefinitionUUID: " + getProcessDefinitionUUID()
    + ", processUUID: " + getProcessInstanceUUID()
    + ", parentInstanceUUID: " + getParentInstanceUUID()
    + ", parentActivityUUID: " + getParentActivityUUID()
    + ", startedBy: " + getStartedBy()
    + ", endedBy: " + getEndedBy()
    + ", startedDate: " + getStartedDate()
    + ", endedDate: " + getEndedDate()
    + ", rootInstanceUUID: " + getRootInstanceUUID()
    + ", archived:" + isArchived
    + "]";
  }

}
