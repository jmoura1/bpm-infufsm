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

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public final class EventConstants {

  public static final String SEPARATOR = "@3^3NT5@";

  public static final String ERROR = "error";
  public static final String MESSAGE = "message";
  public static final String TIMER = "timer";
  public static final String SIGNAL = "signal";

  public static final String START_EVENT = "event.start.";
  public static final String TIMER_START_EVENT = START_EVENT + TIMER;
  public static final String ERROR_START_EVENT = START_EVENT + ERROR;
  public static final String SIGNAL_START_EVENT = START_EVENT + SIGNAL;
  public static final String MESSAGE_START_EVENT = START_EVENT + MESSAGE;

  public static final String BOUNDARY_EVENT = "event.boundary.";
  public static final String TIMER_BOUNDARY_EVENT = BOUNDARY_EVENT + TIMER;
  public static final String MESSAGE_BOUNDARY_EVENT = BOUNDARY_EVENT + MESSAGE;
  public static final String ERROR_BOUNDARY_EVENT = BOUNDARY_EVENT + ERROR;
  public static final String SIGNAL_BOUNDARY_EVENT = BOUNDARY_EVENT + SIGNAL;

  public static final String INTERMEDIATE_EVENT = "event.intermediate.";
  public static final String SIGNAL_INTERMEDIATE_EVENT = INTERMEDIATE_EVENT + SIGNAL;

  private EventConstants() {}

}
