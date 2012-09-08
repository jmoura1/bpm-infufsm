/**
 * Copyright (C) 2007  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
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
package org.ow2.bonita.integration.var;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.BonitaException;

/**
 * Testing process variables
 */
public class ProcessVariableTest extends APITestCase {

  public void testProcessVariables() throws BonitaException {
    URL xpdlUrl = this.getClass().getResource("simpleProcessVariable_1.0.xpdl");
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveFromXpdl(xpdlUrl));
    
    ProcessDefinitionUUID processUUID = process.getUUID();

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);

    checkExecutedOnce(instanceUUID, new String[]{"a", "b", "initial"});

    // Check variables have been set into variableMap of the execution
    Collection<ActivityInstance> acts = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "b");
    assertEquals(1, acts.size());
    ActivityInstanceUUID actUUID = acts.iterator().next().getUUID();
    String str = (String) getQueryRuntimeAPI().getVariable(actUUID, "str");

    assertNotNull(str);
    assertEquals("initial value", str);

    String enumStat = (String) getQueryRuntimeAPI().getVariable(actUUID, "enum_stat");

    assertNotNull(enumStat);
    assertEquals("iiii", enumStat);

    getRuntimeAPI().deleteProcessInstance(instanceUUID);
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testModifiedVariables() throws BonitaException {
    URL xpdlUrl = this.getClass().getResource("simpleProcessVariable_1.0.xpdl");
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveFromXpdl(xpdlUrl));
    
    ProcessDefinitionUUID processUUID = process.getUUID();

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("str", "modified value");

    HashSet<String> ar = new HashSet<String>();
    ar.add("k1");
    ar.add("k2");
    variables.put("enum_stat", "k1");

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID, variables, null);

    checkExecutedOnce(instanceUUID, new String[]{"a", "b", "initial"});

    Collection<ActivityInstance> acts = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "b");
    assertEquals(1, acts.size());
    ActivityInstanceUUID actUUID = acts.iterator().next().getUUID();
    
    // Check variables have been set into variableMap of the execution
    String str =  (String) getQueryRuntimeAPI().getVariable(actUUID, "str");

    assertNotNull(str);
    assertEquals("modified value", str);

    String enumStat = (String) getQueryRuntimeAPI().getVariable(actUUID, "enum_stat");
    assertNotNull(enumStat);
    assertEquals("k1", enumStat);

    getRuntimeAPI().deleteProcessInstance(instanceUUID);
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }
}
