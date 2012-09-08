package org.ow2.bonita.facade.def;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ow2.bonita.facade.def.element.ConnectorDefinition;
import org.ow2.bonita.facade.def.element.impl.ConnectorDefinitionImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.VariableUtil;

public class InternalConnectorDefinition extends ConnectorDefinitionImpl {

  protected long dbid;
  private static final long serialVersionUID = 1L;
  
  protected Map<String, ConnectorParameters> variableParameters;
  
  protected InternalConnectorDefinition() { }
  
  public InternalConnectorDefinition(final ConnectorDefinition src, final ProcessDefinitionUUID processUUID) {
    super(src);
    for (Map.Entry<String, Object[]> entries : src.getParameters().entrySet()) {
      String key = entries.getKey();
      Object[] parameters = entries.getValue();
      List<Variable> variables = new ArrayList<Variable>();
      for (Object parameter : parameters) {
        variables.add(VariableUtil.createVariable(processUUID, key, parameter));
      }
      addParameter(key, new ConnectorParameters(key, variables));
    }
    this.clientParameters = null;
  }
  
  private void addParameter(String key, ConnectorParameters value) {
    if (this.variableParameters == null) {
      this.variableParameters = new HashMap<String, ConnectorParameters>();
    }
    this.variableParameters.put(key, value); 
  }
  
  private Map<String, Object[]> getVariableParameters() {
    if (variableParameters == null) {
      return null;
    }
    Map<String, Object[]> result = new HashMap<String, Object[]>();
    for (Map.Entry<String, ConnectorParameters> entry : variableParameters.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getVariableValues());
    }
    return result;
  }
  
  public Map<String, Object[]> getParameters() {
    if (getVariableParameters() != null) {
      return getVariableParameters();
    }
    return Collections.emptyMap();
  }

  public long getDbid() {
    return dbid;
  }

}
