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
package org.ow2.bonita.services;

import java.util.List;
import java.util.Set;

import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.runtime.event.EventCouple;
import org.ow2.bonita.runtime.event.EventInstance;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;

/**
 * 
 * @author Charles Souillard, Matthieu Chaffotte
 *
 */
public interface EventService {

	void fire(OutgoingEventInstance outgoing);
	void subscribe(IncomingEventInstance incoming);

	Set<EventCouple> getEventsCouples();

	Set<IncomingEventInstance> getIncomingEvents();
	IncomingEventInstance getIncomingEvent(long incomingId);
	Set<IncomingEventInstance> getIncomingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID actiivtyUUID);
	Set<IncomingEventInstance> getBoundaryIncomingEvents(ActivityInstanceUUID activityUUID);

	Set<OutgoingEventInstance> getOutgoingEvents();
	OutgoingEventInstance getOutgoingEvent(long outgoingId);
	Set<OutgoingEventInstance> getOutgoingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID actiivtyUUID);
	Set<OutgoingEventInstance> getBoundaryOutgoingEvents(ActivityInstanceUUID activityUUID);

  Set<OutgoingEventInstance> getOverdueEvents();
  Set<EventInstance> getConsumedEvents();

  void removeSubscriptions(ProcessInstanceUUID instanceUUID);
  void removeSubscriptions(ActivityDefinitionUUID activityUUID);

  void removeEvent(EventInstance outgoing);

  void removeFiredEvents(ProcessInstanceUUID instanceUUID);
  Long getNextDueDate();
  void enableEventsInFailureIncomingEvents(ActivityInstanceUUID activityUUID);
  Set<OutgoingEventInstance> getOutgoingEvents(ProcessInstanceUUID instanceUUID);
  IncomingEventInstance getIncomingEvent(ProcessInstanceUUID instanceUUID, String name);
  void enablePermanentEventsInFailure(ActivityDefinitionUUID activityUUID);
  Set<IncomingEventInstance> getSignalIncomingEvents(String signal);

  Set<IncomingEventInstance> getIncomingEvents(final ProcessInstanceUUID instanceUUID);
  Set<IncomingEventInstance> getIncomingEvents(final ActivityInstanceUUID activityUUID);

  IncomingEventInstance getSignalStartIncomingEvent(final List<String> processNames, final String signalCode);
  List<IncomingEventInstance> getMessageStartIncomingEvents(final Set<String> processNames);

}
