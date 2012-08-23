/**
 * Copyright (C) 2010 BonitaSoft S.A.
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

package org.bonitasoft.connectors;

import java.util.Set;
import java.util.List;

import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.MultipleInstancesJoinChecker;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.util.AccessorUtil;

/**
 * 
 * @author Rodrigue Le Gall
 * @author Matthieu Chaffotte
 * @author Andrea Charao (mixing VetoJoinChecker.java with PercentageJoinChecker.java)
 *
 */
public class MajorityWithVetoJoinChecker extends MultipleInstancesJoinChecker {

  // DO NOT REMOVE NOR RENAME THIS FIELD
  private double percentage;
  // DO NOT REMOVE NOR RENAME THIS FIELD
  private java.lang.String outputvalue;
  // DO NOT REMOVE NOR RENAME THIS FIELD
  private java.lang.String value;
  // DO NOT REMOVE NOR RENAME THIS FIELD
  private java.lang.String variable;
  // DO NOT REMOVE NOR RENAME THIS FIELD
  private java.lang.String outputvariable;



  @Override
  protected boolean isJoinOK() throws Exception {
    final APIAccessor accessor = new StandardAPIAccessorImpl();
    final QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
		
    // test the veto value
    String currentValue = (String)queryRuntimeAPI.getVariable(getActivitytInstanceUUID(), variable);
    if(currentValue.compareTo(value)==0) {
      // We need to set the output global variable
      final RuntimeAPI runtimeAPI = accessor.getRuntimeAPI();
      runtimeAPI.setProcessInstanceVariable(getProcessInstanceUUID(), outputvariable, outputvalue);
      return true;
    }
		
    // test if all steps are finished
    final Set<LightActivityInstance> activities = queryRuntimeAPI.getLightActivityInstances(getProcessInstanceUUID(), getActivityName(), getIterationId());
    int finishedActivities = 0;
    for (LightActivityInstance activityInstance : activities) {
      if (ActivityState.FINISHED.equals(activityInstance.getState())) {
        finishedActivities ++;
      }
    }
    int numberOfActivities = activities.size();
    return percentage <= (double) finishedActivities / numberOfActivities;
  }

  /**
   * Setter for input argument 'percentage'
   * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
   */
  public void setPercentage(double percentage) {
    this.percentage = percentage;
  }

  /**
   * Setter for input argument 'outputvalue'
   * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
   */
  public void setOutputvalue(java.lang.String outputvalue) {
    this.outputvalue = outputvalue;
  }

  /**
   * Setter for input argument 'value'
   * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
   */
  public void setValue(java.lang.String value) {
    this.value = value;
  }

  /**
   * Setter for input argument 'variable'
   * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
   */
  public void setVariable(java.lang.String variable) {
    this.variable = variable;
  }

  /**
   * Setter for input argument 'outputvariable'
   * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
   */
  public void setOutputvariable(java.lang.String outputvariable) {
    this.outputvariable = outputvariable;
  }

  @Override
  protected List<ConnectorError> validateValues() {
    List<ConnectorError> errors = super.validateValues();
    if (percentage < 0.0) {
      ConnectorError error = new ConnectorError("percentage", new IllegalArgumentException("The percentage cannot be less than 0"));
      errors.add(error);
    } else if (percentage > 1.0) {
      ConnectorError error = new ConnectorError("percentage", new IllegalArgumentException("The percentage cannot be greater than 1"));
      errors.add(error);
    }
    return errors;
  }

}
