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
package org.ow2.bonita.runtime.event;

import java.io.Serializable;

import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;

/**
 * 
 * @author Charles Souillard, Matthieu Chaffotte
 *
 */
public abstract class EventInstance implements Serializable {

  private static final long serialVersionUID = -1939488474644018350L;
  public static final String TIMER = "timer";

	protected long id;
	protected String name;
	protected String processName;
	protected String activityName;
	protected ProcessInstanceUUID instanceUUID;
  protected ActivityInstanceUUID activityUUID;
  protected String lockOwner;
  protected boolean consumed;
  protected boolean locked = false;

  protected EventInstance() {
  }

  protected EventInstance(final String name, final String processName, final String activityName,
			final ProcessInstanceUUID instanceUUID, final ActivityInstanceUUID activityUUID) {
		this.name = name;
		this.processName = processName;
		this.activityName = activityName;
		this.activityUUID = activityUUID;
		this.instanceUUID = instanceUUID;
	}

  public String getName() {
    return this.name;
  }

  public String getProcessName() {
    return this.processName;
  }

  public String getActivityName() {
    return this.activityName;
  }

  public long getId() {
    return this.id;
  }

  public ActivityInstanceUUID getActivityUUID() {
    return activityUUID;
  }

  public ProcessInstanceUUID getInstanceUUID() {
    return instanceUUID;
  }

  public void setLockOwner(String lockOwner) {
    this.lockOwner = lockOwner;
  }
  
  public void lock() {
	  this.locked = true;
  }
  
  public void unlock() {
	  this.locked = false;
  }

}
