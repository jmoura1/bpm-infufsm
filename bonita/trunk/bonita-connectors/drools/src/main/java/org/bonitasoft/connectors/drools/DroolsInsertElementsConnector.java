package org.bonitasoft.connectors.drools;

import java.util.ArrayList;
import java.util.List;

import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.command.runtime.rule.InsertElementsCommand;
import org.ow2.bonita.connector.core.ConnectorError;

public class DroolsInsertElementsConnector extends DroolsCommandConnector {

  private ArrayList<Object> facts;
  private String outIdentifier;
  private String entryPoint = "default";

  /* (non-Javadoc)
   * @see org.bonitasoft.connectors.drools.common.DroolsConnector#validateSeparateParams(java.util.List)
   */
  @Override
  protected List<ConnectorError> validateCommandParams() {
    final List<ConnectorError> errors = new ArrayList<ConnectorError>();
    if(facts == null || facts.size() == 0){
      errors.add(new ConnectorError("facts", new IllegalArgumentException("Cannot be empty!")));
    }
    return errors;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bonitasoft.connectors.drools.common.DroolsClient#getCommand()
   */
  protected Command getSpecifiedCommand() {
    InsertElementsCommand insertElementsCommand = (InsertElementsCommand) CommandFactory.newInsertElements(facts);
    if (outIdentifier != null && outIdentifier.trim().length() != 0) {
      insertElementsCommand.setOutIdentifier(outIdentifier);
    }
    if(entryPoint == null || entryPoint.trim().length() == 0){
      entryPoint = "default";
    }
    insertElementsCommand.setEntryPoint(entryPoint);
    return insertElementsCommand;
  }

  /**
   * set facts
   * 
   * @param facts
   */
  public void setFacts(final ArrayList<Object> facts) {
    this.facts = facts;
  }

  /**
   * set outIdentifier
   * 
   * @param outIdentifier
   */
  public void setOutIdentifier(final String outIdentifier) {
    this.outIdentifier = outIdentifier;
  }

  /**
   * set entryPoint
   * 
   * @param entryPoint
   */
  public void setEntryPoint(final String entryPoint) {
    this.entryPoint = entryPoint;
  }

}
