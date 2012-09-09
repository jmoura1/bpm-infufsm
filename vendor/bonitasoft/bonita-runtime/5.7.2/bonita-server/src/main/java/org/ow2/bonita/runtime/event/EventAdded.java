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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Synchronization;

import org.ow2.bonita.persistence.EventDbSession;

public class EventAdded implements Synchronization {

  static final Logger LOG = Logger.getLogger(EventAdded.class.getName());

  private EventDbSession dbSession;
  private long incomingEventInstanceId;
  private final long originalEnableTime;

  public EventAdded(final EventDbSession dbSession, final long incomingEventInstanceId, final long originalEnableTime) {
    this.dbSession = dbSession;
    this.incomingEventInstanceId = incomingEventInstanceId;
    this.originalEnableTime = originalEnableTime;
  }

  public void afterCompletion(int arg0) {
  }

  public void beforeCompletion() {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("notifying event executor of added event");
    }
    final IncomingEventInstance incoming = dbSession.getIncomingEvent(incomingEventInstanceId);
    if (incoming != null) {
      incoming.setEnableTime(originalEnableTime);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Updating EnableTime of event: " + incomingEventInstanceId + " to " + originalEnableTime + "...");
      }
    }
  }

  public String toString() {
    return "event-added";
  }

}
