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
package org.ow2.bonita.facade.exception;

import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.ExceptionManager;
/**
 *
 * Thrown if an internal Bonita error occurred.
 * Please contact Bonita by using the mailing list bonita AT ow2.org
 * 
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras
 *
 */
public class BonitaInternalException extends BonitaRuntimeException {

  private static final long serialVersionUID = 2673048366400783426L;

  /**
   * Constructs a BonitaInternalException with the specified detail message and the throwable cause.
   * @param message msg the detail message.
   * @param cause exception causing the abort.
   */
  public BonitaInternalException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a BonitaInternalException with the specified detail message.
   * @param message the detail message.
   */
  public BonitaInternalException(String message) {
    super(message);
  }

  public static BonitaInternalException build(RuntimeException exception) {
    String message = exception.getMessage();
    if (exception instanceof BonitaRuntimeException) {
      return new BonitaInternalException(message, exception);
    }
    String msg = ExceptionManager.getInstance().getMessage("BIE1", message);
    return new BonitaInternalException(msg, exception);
  }

}
