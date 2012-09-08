/**
 * Copyright (C) 2010 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.ow2.bonita.env.binding;

import org.ow2.bonita.env.descriptor.ContextTypeRefDescriptor;
import org.ow2.bonita.env.descriptor.EventExecutorDescriptor;
import org.ow2.bonita.env.descriptor.IntegerDescriptor;
import org.ow2.bonita.env.descriptor.ObjectDescriptor;
import org.ow2.bonita.env.descriptor.ReferenceDescriptor;
import org.ow2.bonita.env.descriptor.StringDescriptor;
import org.ow2.bonita.env.operation.InvokeOperation;
import org.ow2.bonita.runtime.event.EventExecutor;
import org.ow2.bonita.services.CommandService;
import org.ow2.bonita.util.xml.Parse;
import org.ow2.bonita.util.xml.Parser;
import org.ow2.bonita.util.xml.XmlUtil;
import org.w3c.dom.Element;

/**
 * parses a descriptor for creating a {@link EventExecutor}.
 * 
 * See schema docs for more details.
 * 
 * @author Charles Souillard
 */
public class EventExecutorBinding extends WireDescriptorBinding {

  public EventExecutorBinding() {
    super("event-executor");
  }

  public Object parse(Element element, Parse parse, Parser parser) {
    // create a event executor object
    EventExecutorDescriptor descriptor = new EventExecutorDescriptor();

    if (element.hasAttribute("command-service")) {
      descriptor.addInjection("commandService", new ReferenceDescriptor(element.getAttribute("command-service")));
    } else {
      descriptor.addInjection("commandService", new ContextTypeRefDescriptor(CommandService.class));
    }

    if (element.hasAttribute("name")) {
      descriptor.addInjection("name", new StringDescriptor(element.getAttribute("name")));
    }

    parseIntAttribute(element, "threads", descriptor, "nbrOfThreads", parse);
    parseIntAttribute(element, "idle", descriptor, "idleMillis", parse);
    parseIntAttribute(element, "idle-min", descriptor, "minimumInterval", parse);
    parseIntAttribute(element, "lock", descriptor, "lockMillis", parse);
    parseIntAttribute(element, "retries", descriptor, "retries", parse);

    // by default invoke the start method, unless auto-start is disabled
    if (XmlUtil.attributeBoolean(element, "auto-start", false, parse, true)) {
      InvokeOperation invokeStartOperation = new InvokeOperation();
      invokeStartOperation.setMethodName("start");
      descriptor.addOperation(invokeStartOperation);
      descriptor.setAutoStart(true);
    }
    
    return descriptor;
  }

  private void parseIntAttribute(Element element, String attributeName, ObjectDescriptor descriptor, String fieldName, Parse parse) {
    Integer intValue = XmlUtil.attributeInteger(element, attributeName, false, parse);
    if (intValue != null) {
      descriptor.addInjection(fieldName, new IntegerDescriptor(intValue));
    }
  }

}
