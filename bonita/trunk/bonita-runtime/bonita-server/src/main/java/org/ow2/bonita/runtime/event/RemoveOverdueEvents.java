package org.ow2.bonita.runtime.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;

public class RemoveOverdueEvents implements Command<Void> {
  
  private static final long serialVersionUID = -479276850307735480L;
  private static final Logger LOG = Logger.getLogger(RemoveOverdueEvents.class.getName());
  
  public Void execute(Environment environment) throws Exception {
    final EventService eventService = EnvTool.getEventService();
    for (EventInstance event : eventService.getOverdueEvents()) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Removing overdue event:" + event);
      }
      eventService.removeEvent(event);
    }
    return null;
  }

}
