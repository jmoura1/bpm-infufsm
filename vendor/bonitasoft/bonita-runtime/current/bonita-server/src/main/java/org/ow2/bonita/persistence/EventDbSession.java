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
package org.ow2.bonita.persistence;

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
 * @author Charles Souillard, Matthieu Chaffotte
 */
public interface EventDbSession extends DbSession {

	Set<IncomingEventInstance> getIncomingEvents(ProcessInstanceUUID instanceUUID);
	Set<IncomingEventInstance> getIncomingEvents(ActivityDefinitionUUID activityUUID);
	Set<IncomingEventInstance> getIncomingEvents(ActivityInstanceUUID activityUUID);
	Set<IncomingEventInstance> getIncomingEvents();
	Set<IncomingEventInstance> getIncomingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID activityUUID);
	IncomingEventInstance getIncomingEvent(long incomingId);
	IncomingEventInstance getIncomingEvent(ProcessInstanceUUID instanceUUID, String name);
	Set<IncomingEventInstance> getBoundaryIncomingEvents(ActivityInstanceUUID activityUUID);
	Set<IncomingEventInstance> getPermanentIncomingEvents(ActivityDefinitionUUID activityUUID);

	Set<EventCouple> getEventsCouples();

	Set<OutgoingEventInstance> getOutgoingEvents(ProcessInstanceUUID instanceUUID);
	Set<OutgoingEventInstance> getOutgoingEvents();
  Set<OutgoingEventInstance> getOutgoingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID activityUUID);
  OutgoingEventInstance getOutgoingEvent(long outgoingId);
  Set<OutgoingEventInstance> getBoundaryOutgoingEvents(ActivityInstanceUUID activityUUID);

  Set<OutgoingEventInstance> getOverdueEvents();
  Set<EventInstance> getConsumedEvents();

  Long getNextDueDate();
  Set<IncomingEventInstance> getActivityIncomingEvents(ActivityInstanceUUID activityUUID);

  Set<IncomingEventInstance> getSignalIncomingEvents(String signal);

  IncomingEventInstance getSignalStartIncomingEvent(final List<String> processName, final String signalCode);
  List<IncomingEventInstance> getMessageStartIncomingEvents(final Set<String> processNames);

}
