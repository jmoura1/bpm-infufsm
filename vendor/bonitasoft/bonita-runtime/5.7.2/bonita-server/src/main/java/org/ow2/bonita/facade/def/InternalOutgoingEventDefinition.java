package org.ow2.bonita.facade.def;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.bonita.facade.def.element.OutgoingEventDefinition;
import org.ow2.bonita.facade.def.element.impl.OutgoingEventDefinitionImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.VariableUtil;

public class InternalOutgoingEventDefinition extends OutgoingEventDefinitionImpl {

  private static final long serialVersionUID = 1L;
  protected Map<String, Variable> variableParameters;

  protected InternalOutgoingEventDefinition() { }
  
  public InternalOutgoingEventDefinition(final OutgoingEventDefinition src, final ProcessDefinitionUUID processUUID) {
    super(src);
    for (Entry<String, Object> parameter : src.getParameters().entrySet()) {
      String key = parameter.getKey();
      Object value = parameter.getValue();
      addParameter(key, VariableUtil.createVariable(processUUID, key, value));
    }
  }
  
  public void addParameter(String key, Variable value) {
    if (this.variableParameters == null) {
      this.variableParameters = new HashMap<String, Variable>();
    }
    this.variableParameters.put(key, value); 
  }
  
  public Map<String, Object> getVariableParameters() {
    if (variableParameters == null) {
      return null;
    }
    Map<String, Object> result = new HashMap<String, Object>();
    for (Map.Entry<String, Variable> entry : variableParameters.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getValue());
    }
    return result;
  }
  
  public Map<String, Object> getParameters() {
    if (getVariableParameters() != null) {
      return getVariableParameters();
    }
    return Collections.emptyMap();
  }
}
