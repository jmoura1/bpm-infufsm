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
package org.ow2.bonita.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.persistence.EventDbSession;
import org.ow2.bonita.runtime.event.EventAdded;
import org.ow2.bonita.runtime.event.EventCouple;
import org.ow2.bonita.runtime.event.EventInstance;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.EnvTool;

/**
 * 
 * @author Charles Souillard, Matthieu Chaffotte
 *
 */
public class DbThreadEventService implements EventService {

	private String persistenceServiceName;
	private static final Logger LOG = Logger.getLogger(DbThreadEventService.class.getName());
	
  protected DbThreadEventService() { }
  
  public DbThreadEventService(String persistenceServiceName) {
    super();
    this.persistenceServiceName = persistenceServiceName;
  }

  protected EventDbSession getDbSession() {
    return EnvTool.getEventServiceDbSession(persistenceServiceName);
  }
  
  private void refresh() {
    EnvTool.getEventExecutor().refresh();
  }
  
  public void enableEventsInFailureIncomingEvents(ActivityInstanceUUID activityUUID) {
    final Set<IncomingEventInstance> events = getDbSession().getActivityIncomingEvents(activityUUID);
    final int retries = EnvTool.getEventExecutor().getRetries();
    for (IncomingEventInstance event : events) {
      if (event.getRetries() == 0) {
        event.setRetries(retries);
      }
    }
    refresh();
  }
  
  public void enablePermanentEventsInFailure(ActivityDefinitionUUID activityUUID) {
    final Set<IncomingEventInstance> events = getDbSession().getPermanentIncomingEvents(activityUUID);
    final int retries = EnvTool.getEventExecutor().getRetries();
    for (IncomingEventInstance event : events) {
      if (event.getRetries() == 0) {
        event.setRetries(retries);
      }
    }
    refresh();
  }
  
	public void fire(OutgoingEventInstance outgoingEventInstance) {
	  getDbSession().save(outgoingEventInstance);
		if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Firing event outgoing event: " + outgoingEventInstance + "...");
    }
		refresh();
	}
	
	public void subscribe(IncomingEventInstance incomingEventInstance) {
	  final long originalEnableTime = incomingEventInstance.getEnableTime();
	  incomingEventInstance.setEnableTime(Long.MAX_VALUE);
	  final int retries = EnvTool.getEventExecutor().getRetries();
	  incomingEventInstance.setRetries(retries);
	  getDbSession().save(incomingEventInstance);
	  EnvTool.getTransaction().registerSynchronization(new EventAdded(getDbSession(), incomingEventInstance.getId(), originalEnableTime));
		if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Firing event incoming event: " + incomingEventInstance + "...");
    }
		refresh();
	}

	public Set<IncomingEventInstance> getIncomingEvents() {
	  Set<IncomingEventInstance> incomings = getDbSession().getIncomingEvents();
	  if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
	}
	
	public Set<OutgoingEventInstance> getOutgoingEvents() {
	  Set<OutgoingEventInstance> outgoings = getDbSession().getOutgoingEvents();
	  if (outgoings == null) {
      return Collections.emptySet();
    }
    return outgoings;
	}
	
	public Set<OutgoingEventInstance> getOutgoingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID activityUUID) {
	  Set<OutgoingEventInstance> outgoings = getDbSession().getOutgoingEvents(eventName, toProcessName, toActivityName, activityUUID);
	  if (outgoings == null) {
      return Collections.emptySet();
    }
    return outgoings;
	}

	public Set<IncomingEventInstance> getIncomingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID activityUUID) {
	  Set<IncomingEventInstance> incomings = getDbSession().getIncomingEvents(eventName, toProcessName, toActivityName, activityUUID);
	  if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
	}

	public Set<OutgoingEventInstance> getOutgoingEvents(ProcessInstanceUUID instanceUUID) {
	  Set<OutgoingEventInstance> outgoings = getDbSession().getOutgoingEvents(instanceUUID);
	  if (outgoings == null) {
      return Collections.emptySet();
    }
    return outgoings;
	}

	public Set<IncomingEventInstance> getIncomingEvents(ProcessInstanceUUID instanceUUID) {
    Set<IncomingEventInstance> incomings = getDbSession().getIncomingEvents(instanceUUID);
    if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
  }

	public Set<IncomingEventInstance> getIncomingEvents(ActivityDefinitionUUID activityUUID) {
    Set<IncomingEventInstance> incomings = getDbSession().getIncomingEvents(activityUUID);
    if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
  }

  public Set<IncomingEventInstance> getIncomingEvents(ActivityInstanceUUID activityUUID) {
    Set<IncomingEventInstance> incomings = getDbSession().getIncomingEvents(activityUUID);
    if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
  }

  public Set<IncomingEventInstance> getBoundaryIncomingEvents(ActivityInstanceUUID activityUUID) {
    Set<IncomingEventInstance> incomings = getDbSession().getBoundaryIncomingEvents(activityUUID);
    if (incomings == null) {
      return Collections.emptySet();
    }
    return incomings;
  }

  public Set<OutgoingEventInstance> getBoundaryOutgoingEvents(ActivityInstanceUUID activityUUID) {
    Set<OutgoingEventInstance> outgoings = getDbSession().getBoundaryOutgoingEvents(activityUUID);
    if (outgoings == null) {
      return Collections.emptySet();
    }
    return outgoings;
  }

	public Set<EventCouple> getEventsCouples() {
		return getDbSession().getEventsCouples();
	}
	
	public IncomingEventInstance getIncomingEvent(long incomingId) {
	  return getDbSession().getIncomingEvent(incomingId);
	}
	public OutgoingEventInstance getOutgoingEvent(long outgoingId) {
	  return getDbSession().getOutgoingEvent(outgoingId);
	}
	
	public IncomingEventInstance getIncomingEvent(ProcessInstanceUUID instanceUUID, String name) {
	  return getDbSession().getIncomingEvent(instanceUUID, name);
	}

	public void removeFiredEvent(OutgoingEventInstance outgoing) {
		getDbSession().delete(outgoing);
		if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Removing outgoing event: " + outgoing + "...");
    }
	}
	
	public Set<OutgoingEventInstance> getOverdueEvents() {
	  return getDbSession().getOverdueEvents();
	}
	
	public Set<EventInstance> getConsumedEvents() {
    return getDbSession().getConsumedEvents();
  }

	public Set<IncomingEventInstance> getSignalIncomingEvents(String signal) {
    return getDbSession().getSignalIncomingEvents(signal);
  }

	public void removeSubscription(IncomingEventInstance incoming) {
	  getDbSession().delete(incoming);
	  if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Removing incoming event: " + incoming + "...");
    }
	}

	public void removeSubscriptions(ProcessInstanceUUID instanceUUID) {
	  Set<IncomingEventInstance> events = getIncomingEvents(instanceUUID);
	  if (events != null && !events.isEmpty()) {
	  	for (IncomingEventInstance event : events) {
	  		removeSubscription(event);
	  	}
	  }
	}
	
	public void removeSubscriptions(ActivityDefinitionUUID activityUUID) {
	  Set<IncomingEventInstance> events = getIncomingEvents(activityUUID);
    if (events != null && !events.isEmpty()) {
      for (IncomingEventInstance event : events) {
        removeSubscription(event);
      }
    }
	}
	
	public void removeFiredEvents(ProcessInstanceUUID instanceUUID) {
	  Set<OutgoingEventInstance> events = getOutgoingEvents(instanceUUID);
    if (events != null && !events.isEmpty()) {
      for (OutgoingEventInstance event : events) {
        removeFiredEvent(event);
      }
    }
	}
	
  public void removeEvent(EventInstance event) {
    getDbSession().delete(event);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Removing event: " + event + "...");
    }
  }
  
	public Long getNextDueDate() {
	  return getDbSession().getNextDueDate();
	}

	public IncomingEventInstance getSignalStartIncomingEvent(final List<String> processNames, final String signalCode) {
    return getDbSession().getSignalStartIncomingEvent(processNames, signalCode);
  }

  public List<IncomingEventInstance> getMessageStartIncomingEvents(final Set<String> processNames) {
    return getDbSession().getMessageStartIncomingEvents(processNames);
  }

}
