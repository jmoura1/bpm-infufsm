/**
 * Copyright (C) 2006  Bull S. A. S.
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
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.def.majorElement.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.ow2.bonita.facade.def.element.AttachmentDefinition;
import org.ow2.bonita.facade.def.element.ConnectorDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.def.element.impl.AttachmentDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.ConnectorDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.IterationDescriptor;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.EventProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ParticipantDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.impl.LightProcessDefinitionImpl;
import org.ow2.bonita.util.CopyTool;
import org.ow2.bonita.util.Misc;

public class ProcessDefinitionImpl extends LightProcessDefinitionImpl implements ProcessDefinition {

  private static final long serialVersionUID = -572795239631090498L;

  protected Set<DataFieldDefinition> dataFields;
  protected Set<ParticipantDefinition> participants;
  protected Set<ActivityDefinition> activities;
  protected Set<AttachmentDefinition> attachments;
  protected Set<String> subProcesses;
  protected List<EventProcessDefinition> eventSubProcesses;

  protected Map<String, String> metadata;
  protected List<HookDefinition> connectors;
  protected Set<IterationDescriptor> iterationDescriptors;

  protected ProcessDefinitionImpl() { }

  public ProcessDefinitionImpl(final String name, final String version) {
    super(name, version);
  }

  public ProcessDefinitionImpl(final ProcessDefinition src) {
    super(src);

    ClassLoader current = Thread.currentThread().getContextClassLoader();
    try {
      ProcessDefinitionImpl srcImpl = (ProcessDefinitionImpl) src;
      Thread.currentThread().setContextClassLoader(srcImpl.getClassLoader(src.getUUID()));

      Set<DataFieldDefinition> dataFields = src.getDataFields();
      if (dataFields != null) {
        this.dataFields = new HashSet<DataFieldDefinition>();
        for (DataFieldDefinition d : dataFields) {
          this.dataFields.add(new DataFieldDefinitionImpl(d));
        }
      }
      List<HookDefinition> connectors = src.getConnectors();
      if (connectors != null) {
        this.connectors = new ArrayList<HookDefinition>();
        for (HookDefinition d : connectors) {
          this.connectors.add(new ConnectorDefinitionImpl(d));
        }
      }
      Set<ParticipantDefinition> participants = src.getParticipants();
      if (participants != null) {
        this.participants = new HashSet<ParticipantDefinition>();
        for (ParticipantDefinition d : participants) {
          this.participants.add(new ParticipantDefinitionImpl(d));
        }
      }
      Set<ActivityDefinition> activities = src.getActivities();
      if (activities != null) {
        this.activities = new HashSet<ActivityDefinition>();
        for (ActivityDefinition d : activities) {
          this.activities.add(new ActivityDefinitionImpl(d));
        }
      }

      this.metadata = new HashMap<String, String>();
      final Map<String, String> meta = src.getMetaData();
      for (Entry<String, String> entry : meta.entrySet()) {
        this.metadata.put(entry.getKey(), entry.getValue());
      }

      final Map<String, AttachmentDefinition> other = src.getAttachments();
      if (!other.isEmpty()) {
        this.attachments = new HashSet<AttachmentDefinition>();
        for (AttachmentDefinition attach : other.values()) {
          this.attachments.add(new AttachmentDefinitionImpl(attach));
        }
      }
      if (src.getIterationDescriptors() != null) {
        this.iterationDescriptors = new HashSet<IterationDescriptor>();
        for (IterationDescriptor id : src.getIterationDescriptors()) {
          this.iterationDescriptors.add(new IterationDescriptor(id));
        }
      }

      List<EventProcessDefinition> eventSubProcesses = src.getEventSubProcesses();
      this.eventSubProcesses = new ArrayList<EventProcessDefinition>();
      for (EventProcessDefinition d : eventSubProcesses) {
        this.eventSubProcesses.add(new EventProcessDefinitionImpl(d));
      }

      this.subProcesses = CopyTool.copy(src.getSubProcesses());
    } finally {
      Thread.currentThread().setContextClassLoader(current);
    }
  }

  protected ClassLoader getClassLoader(ProcessDefinitionUUID processUUID) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return cl;
  }

  @Override
  public String toString() {
    String st = this.getClass().getName()
    + "[uuid: " + getUUID()
    + ", name:" + getName()
    + ", description:" + getDescription()
    + ", version:" + getVersion();
    st +=  "]";
    return st;
  }

  public Set<String> getClassDependencies() {
    Set<String> classDependencies = new HashSet<String>();
    for (ParticipantDefinition participant : getParticipants()) {
      if (participant.getRoleMapper() != null) {
        classDependencies.add(participant.getRoleMapper().getClassName());
      }
    }
    for (ConnectorDefinition connector : getConnectors()) {
      classDependencies.add(connector.getClassName());
    }
    for (ActivityDefinition activity : getActivities()) {
      classDependencies.addAll(activity.getClassDependencies());
    }
    
    return classDependencies;
  }

  public Set<String> getProcessDependencies() {
    Set<String> processDependencies = new HashSet<String>();
    for (ActivityDefinition activity : getActivities()) {
      if (activity.getSubflowProcessName() != null) {
        processDependencies.add(activity.getSubflowProcessName());
      }
    }
    return processDependencies;
  }

  public Set<DataFieldDefinition> getDataFields() {
    if (dataFields == null) {
      return Collections.emptySet();
    }
    return dataFields;
  }
  
  public Set<String> getSubProcesses() {
    if (this.subProcesses == null) {
      return Collections.emptySet();
    }
    return this.subProcesses;
  }
  
  public void setSubProcesses(Set<String> subProcesses) {
    this.subProcesses = subProcesses;
  }

  public Set<ParticipantDefinition> getParticipants() {
    if (participants == null) {
      return Collections.emptySet();
    }
    return participants;
  }

  public Set<ActivityDefinition> getActivities() {
    if (activities == null) {
      return Collections.emptySet();
    }
    return activities;
  }

  public Set<TransitionDefinition> getTransitions() {
    if (activities == null) {
      return Collections.emptySet();
    }
    Set<TransitionDefinition> transitions = new HashSet<TransitionDefinition>();
    for (ActivityDefinition activity : getActivities()) {
      Set<TransitionDefinition> activityTransitions = activity.getOutgoingTransitions();
      for (TransitionDefinition transition : activityTransitions) {
        transitions.add(transition);
      }
    }
    return transitions;
  }

  public Map<String, String> getMetaData() {
    if (metadata == null) {
      return Collections.emptyMap();
    }
    return metadata;
  }

  public String getAMetaData(String key) {
    Misc.checkArgsNotNull(key);
    if (this.metadata == null) {
      return null;
    }
    return metadata.get(key);
  }

  public List<HookDefinition> getConnectors() {
    if (this.connectors == null) {
      return Collections.emptyList();
    }
    return connectors;
  }

  /**
   * SETTERS
   */

  public void setState(ProcessState state) {
    this.state = state; 
  }

  public void setType(ProcessType type) {
    this.type = type;
  }

  public void setUndeployedDate(Date undeployedDate) {
    this.undeployedDate = Misc.getTime(undeployedDate);
  }

  public void setUndeployedBy(String undeployedBy) {
    this.undeployedBy = undeployedBy;
  }

  public void addData(DataFieldDefinition data) {
    if (dataFields == null) {
      dataFields = new HashSet<DataFieldDefinition>();
    }
    this.dataFields.add(data);
  }

  public void addGroup(ParticipantDefinitionImpl group) {
    if (participants == null) {
      participants = new HashSet<ParticipantDefinition>();
    }
    this.participants.add(group);
  }

  public void addActivity(ActivityDefinition activity) {
    if (activities == null) {
      activities = new HashSet<ActivityDefinition>();
    }
    this.activities.add(activity);
  }

  public void setDeployedDate(Date deployedDate) {
    this.deployedDate = Misc.getTime(deployedDate);
  }

  public void setDeployedBy(String deployedBy) {
    this.deployedBy = deployedBy;
  }

  public void deleteAMetaData(String key) {
    Misc.checkArgsNotNull(key);
    metadata.remove(key);
  }

  public void addAMetaData(String key, String value) {
    Misc.checkArgsNotNull(key, value);
    if (this.metadata == null) {
      this.metadata = new HashMap<String, String>();
    }
    this.metadata.put(key, value);
  }

  public void addAttachment(AttachmentDefinition attach) {
    Misc.checkArgsNotNull(attach);
    if (this.attachments == null) {
      this.attachments = new HashSet<AttachmentDefinition>();
    }
    this.attachments.add(attach);
  }

  public void addConnector(HookDefinition connector) {
    if (this.connectors == null) {
      this.connectors = new ArrayList<HookDefinition>();
    }
    this.connectors.add(connector);
  }

  public ActivityDefinition getActivity(final String name) {
    for (ActivityDefinition activity : getActivities()) {
      if (activity.getName().equals(name)) {
        return activity;
      }
    }
    return null;
  }
  
  public DataFieldDefinition getDatafield(final String name) {
    for (DataFieldDefinition datafield : getDataFields()) {
      if (datafield.getName().equals(name)) {
        return datafield;
      }
    }
    return null;
  }

  public AttachmentDefinition getAttachment(String name) {
    return getAttachments().get(name);
  }

  public Map<String, AttachmentDefinition> getAttachments() {
    if (this.attachments == null) {
      return Collections.emptyMap();
    }
    Map<String, AttachmentDefinition> result = new HashMap<String, AttachmentDefinition>();
    for (AttachmentDefinition attach : this.attachments) {
      result.put(attach.getName(), attach);
    }
    return result;
  }

  public Map<String, ActivityDefinition> getFinalActivities() {
    Map<String, ActivityDefinition> result = new HashMap<String, ActivityDefinition>();
    for (ActivityDefinition activity : getActivities()) {
      if (!activity.hasOutgoingTransitions()) {
        result.put(activity.getName(), activity);
      }
    }
    return result;
  }
  
  public Map<String, ActivityDefinition> getInitialActivities() {
    Map<String, ActivityDefinition> result = new HashMap<String, ActivityDefinition>();
    for (ActivityDefinition activity : getActivities()) {
      if (!activity.hasIncomingTransitions()) {
        result.put(activity.getName(), activity);
      }
    }
    return result;
  }  

  public Set<IterationDescriptor> getIterationDescriptors() {
  	if (this.iterationDescriptors == null) {
  		return Collections.emptySet();
  	}
    return iterationDescriptors;
  }

  public void addIterationDescriptors(final IterationDescriptor iterationDescriptor) {
  	if (this.iterationDescriptors == null) {
  		this.iterationDescriptors = new HashSet<IterationDescriptor>();
  	}
    this.iterationDescriptors.add(iterationDescriptor);
  }

  public boolean containsIterationDescriptor(IterationDescriptor itDesc) {
	  for (IterationDescriptor id : getIterationDescriptors()) {
	  	if (id.equals(itDesc)) {
	  		return true;
	  	}
	  }
	  return false;
  }

  public Set<IterationDescriptor> getIterationDescriptors(String activityName) {
  	Set<IterationDescriptor> result = new HashSet<IterationDescriptor>();
  	for (IterationDescriptor id : getIterationDescriptors()) {
	  	if (id.containsNode(activityName)) {
	  		result.add(id);
	  	}
	  }
    return result;
  }

  public void addCategory(String categoryName) {
    if(categoryName!=null && categoryName.trim().length() > 0){
      if(this.categories == null) {
        this.categories = new HashSet<String>();
      }
      this.categories.add(categoryName);
    }
  }

  public void setCategories(Set<String> categoryNames) {
    this.categories = categoryNames;
  }

  public void addEventSubProcess(EventProcessDefinition eventProcess) {
    if (eventSubProcesses == null) {
      eventSubProcesses = new ArrayList<EventProcessDefinition>();
    }
    eventSubProcesses.add(eventProcess);
  }

  public List<EventProcessDefinition> getEventSubProcesses() {
    if (this.eventSubProcesses == null) {
      return Collections.emptyList();
    }
    return eventSubProcesses;
  }

}
