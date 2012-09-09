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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.GroovyExpression;
import org.ow2.bonita.util.GroovyUtil;

public class GetEventsCouplesCommand implements Command<Map<ProcessInstanceUUID, Set<EventCoupleId>>> {
  private static final long serialVersionUID = -479276850307735480L;
  static final Logger LOG = Logger.getLogger(GetEventsCouplesCommand.class.getName());

  @Override
  public Map<ProcessInstanceUUID, Set<EventCoupleId>> execute(final Environment environment) throws Exception {
    final EventService eventService = EnvTool.getEventService();
    final Set<EventCouple> eventCouples = eventService.getEventsCouples();
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Found eventCouples in DB " + eventCouples);
    }
    if (eventCouples != null && !eventCouples.isEmpty()) {
      final Map<ProcessInstanceUUID, Set<EventCouple>> validCouples = new HashMap<ProcessInstanceUUID, Set<EventCouple>>();
      final Set<Long> usedIncomings = new HashSet<Long>();
      final Set<Long> usedOutgoings = new HashSet<Long>();
      for (final EventCouple eventCouple : eventCouples) {
        if (!usedIncomings.contains(eventCouple.getIncoming().getId())
            && !usedOutgoings.contains(eventCouple.getOutgoing().getId())) {
          // matcher
          boolean match = true;
          final IncomingEventInstance incoming = eventCouple.getIncoming();
          final OutgoingEventInstance outgoing = eventCouple.getOutgoing();
          final String expression = incoming.getExpression();
          final Map<String, Object> parameters = outgoing.getParameters();
          final String signal = incoming.getSignal();
          if (expression != null && !"event.start.timer".equals(signal)) {
            final String groovyExpression = GroovyExpression.START_DELIMITER + expression
                + GroovyExpression.END_DELIMITER;
            final ActivityInstanceUUID activityUUID = incoming.getActivityUUID();
            if (activityUUID != null) {
              match = (Boolean) GroovyUtil.evaluate(groovyExpression, parameters, activityUUID, false, false);
            } else {
              match = (Boolean) GroovyUtil.evaluate(groovyExpression, parameters);
            }
          }
          if (match) {
            usedIncomings.add(incoming.getId());
            usedOutgoings.add(outgoing.getId());
            addElement(validCouples, incoming.getInstanceUUID(), eventCouple);
            if (LOG.isLoggable(Level.INFO)) {
              LOG.info("Adding eventCouple:[incoming=" + incoming.getId() + " " + incoming.getSignal() + ", outgoing="
                  + outgoing.getId() + "] to the queue");
            }
          } else {
            incoming.addIncompatibleEvent(outgoing.getId());
          }
        }
      }
      if (validCouples.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<ProcessInstanceUUID, Set<EventCoupleId>> result = new HashMap<ProcessInstanceUUID, Set<EventCoupleId>>();
      for (final Entry<ProcessInstanceUUID, Set<EventCouple>> validCouple : validCouples.entrySet()) {
        final Set<EventCoupleId> instanceResult = new HashSet<EventCoupleId>();
        for (final EventCouple eventCouple : validCouple.getValue()) {
          final IncomingEventInstance incoming = eventCouple.getIncoming();
          final OutgoingEventInstance outgoing = eventCouple.getOutgoing();
          incoming.lock();
          outgoing.lock();
          instanceResult.add(new EventCoupleId(incoming.getId(), outgoing.getId()));
        }
        result.put(validCouple.getKey(), instanceResult);
      }

      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("New event couples to execute: " + result.toString());
      }
      return result;
    }
    return null;
  }

  private void addElement(final Map<ProcessInstanceUUID, Set<EventCouple>> validCouples,
      final ProcessInstanceUUID instanceUUID, final EventCouple eventCouple) {
    if (validCouples.get(instanceUUID) == null) {
      validCouples.put(instanceUUID, new HashSet<EventCouple>());
    }
    validCouples.get(instanceUUID).add(eventCouple);
  }

}
