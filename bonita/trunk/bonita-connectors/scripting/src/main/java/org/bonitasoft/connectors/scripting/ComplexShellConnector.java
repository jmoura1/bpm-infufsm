/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.scripting;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ow2.bonita.connector.core.ConnectorError;

public class ComplexShellConnector extends AbstractShellConnector {

  private java.util.ArrayList<String> command;

  private static final Log LOGGER = LogFactory.getLog(ComplexShellConnector.class.getClass());

  @Override
  protected List<ConnectorError> validateValues() {
    return null;
  }

  public void setCommand(final java.util.ArrayList<String> command) {
    this.command = command;
  }

  @Override
  protected Process executeShellCommand() throws IOException {
    final Runtime rt = Runtime.getRuntime();
    final String[] commandArray = command.toArray(new String[0]);
    for (int i = 0; i < commandArray.length; i++) {
      LOGGER.warn(i + " : " + commandArray[i]);
    }
    final Process process = rt.exec(commandArray);
    return process;
  }

}
