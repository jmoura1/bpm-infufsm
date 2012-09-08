package org.bonitasoft.connectors.scripting;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ow2.bonita.connector.core.ConnectorError;

public class ComplexShellConnector extends AbstractShellConnector {

  private java.util.ArrayList<String> command;
  private static final Log logger = LogFactory.getLog(ComplexShellConnector.class.getClass());


  @Override
  protected List<ConnectorError> validateValues() {
    return null;
  }

  public void setCommand(java.util.ArrayList<String> command) {
    this.command = command;
  }

  protected Process executeShellCommand() throws IOException{
    Runtime rt = Runtime.getRuntime();
    String[] commandArray = (String[]) (command.toArray(new String[0]));
    for (int i = 0; i < commandArray.length; i++) {
      logger.warn(i + " : " + commandArray[i]);
    }
    Process process = rt.exec(commandArray);
    return process;
  }
}
