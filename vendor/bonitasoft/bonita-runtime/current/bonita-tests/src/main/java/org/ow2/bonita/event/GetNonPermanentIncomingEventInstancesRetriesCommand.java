package org.ow2.bonita.event;

import java.util.HashSet;
import java.util.Set;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;

public class GetNonPermanentIncomingEventInstancesRetriesCommand implements Command<Set<IncomingRetry>> {
  private static final long serialVersionUID = 1L;

	public Set<IncomingRetry> execute(Environment environment) throws Exception {
	  final EventService eventService = EnvTool.getEventService();
	  Set<IncomingRetry> result = new HashSet<IncomingRetry>();
    for (IncomingEventInstance incoming : eventService.getIncomingEvents()) {
      if (!incoming.isPermanent()) {
        result.add(new IncomingRetry(incoming.getProcessName(), incoming.getRetries()));
      }
    }
    return result;
	}
}