package org.ow2.bonita.facade.def;

import java.io.Serializable;

import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.impl.DataFieldDefinitionImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.VariableUtil;

public class InternalDataFieldDefinition extends DataFieldDefinitionImpl {

  private static final long serialVersionUID = 1L;
  
  protected Variable initialValueVariable;
  
  protected InternalDataFieldDefinition() { }
  
  public InternalDataFieldDefinition(final DataFieldDefinition src, final ProcessDefinitionUUID processUUID) {
    super(src);
    setInitialValue(VariableUtil.createVariable(processUUID, src.getName(), src.getInitialValue()));  
  }
  
  public void setInitialValue(Variable initialValue) {
    this.initialValueVariable = initialValue;
    this.clientInitialValue = null;
  }
  
  private Variable getInitialValueVariable() {
    return initialValueVariable;
  }
  
  public Serializable getInitialValue() {
    if (initialValueVariable == null) {
      return null;
    }
    return (Serializable) getInitialValueVariable().getValue();
  }
}
