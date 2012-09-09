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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Synchronization;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.CommandService;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;

public class EventCoupleExceptionHandler implements Synchronization, Command<Object> {

  private static final Logger LOG = Logger.getLogger(EventCoupleExceptionHandler.class.getName());

  private static final long serialVersionUID = 1L;

  protected transient CommandService commandService;
  protected EventCoupleId eventCouple;
  protected Throwable exception;

  public EventCoupleExceptionHandler(EventCoupleId eventCouple, Throwable exception, CommandService commandService) {
    this.commandService = commandService;
    this.eventCouple = eventCouple;
    this.exception = exception;
  }

  public void beforeCompletion() {
  }

  public void afterCompletion(int status) {
    // after the transaction rolled back, 
    // execute this job exception handler object as a command with 
    // the command service so that this gets done in a separate 
    // transaction
    LOG.severe("starting new transaction for handling event couple exception");
    commandService.execute(this);
    LOG.severe("completed transaction for handling event couple exception");
  }

  public Object execute(Environment environment) throws Exception {
    final EventService eventService = EnvTool.getEventService();
    final Querier journal = EnvTool.getJournalQueriers();
    final EventExecutor eventExecutor = EnvTool.getEventExecutor();
    final IncomingEventInstance incoming = eventService.getIncomingEvent(eventCouple.getIncoming());
    final OutgoingEventInstance outgoing = eventService.getOutgoingEvent(eventCouple.getOutgoing());
    final String executionEventUUID = incoming.getExecutionUUID();

    // serialize the stack trace
    final StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));
    
    if (executionEventUUID != null) {
      final Execution execution = journal.getExecutionWithEventUUID(executionEventUUID);

      if (execution != null && incoming.isExecutionLocked()) {
        if (!Execution.STATE_ACTIVE.equals(execution.getState())) {
          execution.unlock();
        }
        execution.lock(execution.getState());
      }
    }

    int decrementedRetries = incoming.getRetries() - 1;
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Decrementing retries on incoming: " + incoming + ": " + decrementedRetries);
    }
    incoming.setRetries(decrementedRetries);
    incoming.setException(sw.toString());
    incoming.unlock();
    outgoing.unlock();

    eventExecutor.refresh();
    return null;
  }

}
