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
import java.util.Set;
import java.util.TreeSet;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.hook.DefaultTestHook;
import org.ow2.bonita.util.BonitaException;

/**
 * Test. Deploys a xpdl process.
 *
 * @author Marc Blachon, Charles Souillard
 */
public class ActivityAutomaticVariableTest extends APITestCase {

  public void testActivityAutomaticVariable() throws BonitaException {
    URL xpdlUrl = this.getClass().getResource("activityAutomaticVariable_1.0.xpdl");
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveFromXpdl(xpdlUrl,
        DefaultTestHook.class));
    
    ProcessDefinitionUUID processUUID = process.getUUID();

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);

    checkExecutedOnce(instanceUUID, new String[]{"a", "b", "initial"});
    Collection<ActivityInstance> acts = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "b");
    assertEquals(1, acts.size());
    ActivityInstanceUUID actUUID = acts.iterator().next().getUUID();
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(actUUID, "act_str1");
      fail("variable act_str1 should not exist");
    } catch (VariableNotFoundException e) {
      // OK
    } 
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(actUUID, "act_enum_stat");
      fail("variable act_enum_stat should not exist");
    } catch (VariableNotFoundException e) {
      // OK
    }

    Set<String> expectedKeys = new TreeSet<String>();
    expectedKeys.add("act_enum_stat");
    expectedKeys.add("act_str1");

    acts = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "a");
    assertEquals(1, acts.size());
    actUUID = acts.iterator().next().getUUID();
    assertEquals(expectedKeys, getQueryRuntimeAPI().getActivityInstanceVariables(actUUID).keySet());
    Object actStr1 = getQueryRuntimeAPI().getActivityInstanceVariable(actUUID, "act_str1");
    Object actEnumStat = getQueryRuntimeAPI().getActivityInstanceVariable(actUUID, "act_enum_stat");

    assertNotNull(actStr1);
    assertNotNull(actEnumStat);
    assertEquals("initial value", actStr1);

    String enu = (String) actEnumStat;
    assertEquals(enu, "v1");

    getRuntimeAPI().deleteProcessInstance(instanceUUID);
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

}
