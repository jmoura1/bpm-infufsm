package org.ow2.bonita.facade.runtime.impl;

import java.io.Serializable;
import java.util.Date;

import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.Misc;

public class InternalVariableUpdate extends VariableUpdateImpl {

  private static final long serialVersionUID = -421173883505183097L;
  
  protected Variable variable;
  
  protected InternalVariableUpdate() { }
  
  public InternalVariableUpdate(Date date, String userId, String name, Variable variable) {
    super();
    this.date = Misc.getTime(date);
    this.userId = userId;
    this.name = name;
    this.variable = variable;
  }
  
  private Variable getVariable() {
    return this.variable;
  }
  
  public Serializable getValue() {
    if (getVariable() != null) {
      return (Serializable) getVariable().getValue();
    }
    return null;
  }
  
  public void setValue(Variable variable) {
    this.variable = variable;
    this.clientVariable = null;
  }
}
