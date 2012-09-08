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

import java.util.Set;
import java.util.logging.Logger;

import org.ow2.bonita.util.Misc;

/**
 * @author Alejandro Guizar
 */
public class EventExecutorThread implements Runnable {

  private static final Logger LOG = Logger.getLogger(EventExecutorThread.class.getName());

  private EventExecutor eventExecutor;
  private final Set<EventCoupleId> eventCouples;

  public EventExecutorThread(EventExecutor eventExecutor, Set<EventCoupleId> eventCouples) {
    this.eventExecutor = eventExecutor;
    this.eventCouples = eventCouples;
  }
  
  public void run() {	
    if(this.eventCouples != null && !this.eventCouples.isEmpty()) {
      for (EventCoupleId eventCouple : this.eventCouples) {
        try {
          eventExecutor.getCommandService().execute(new ExecuteEventsCouplesCommand(eventCouple, this.eventExecutor));
        } catch (Throwable e) {
          LOG.severe("exception in event block: " + Misc.getStackTraceFrom(e));
        }
      }
    }
    eventExecutor.notifyThreadFinished();
  }

  @Override
  public String toString() {
    StringBuilder stb = new StringBuilder(EventExecutorThread.class.getSimpleName());
    if (this.eventCouples != null && !this.eventCouples.isEmpty()) {
      stb.append(this.eventCouples);
    }
    return stb.toString();
  }
}
