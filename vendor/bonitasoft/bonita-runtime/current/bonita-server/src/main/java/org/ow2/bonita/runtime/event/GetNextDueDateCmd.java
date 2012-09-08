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

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;

public class GetNextDueDateCmd implements Command<Long> {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger(GetNextDueDateCmd.class.getName());

  public Long execute(Environment environment) throws Exception {
    Long nextDueDate = null;
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("getting next due date...");
    }
    EventService eventService = EnvTool.getEventService();
    nextDueDate = eventService.getNextDueDate();
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("next due date is " + nextDueDate);
    }
    return nextDueDate;
  }

}
