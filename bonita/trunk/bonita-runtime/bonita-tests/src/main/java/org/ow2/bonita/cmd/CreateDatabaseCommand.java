package org.ow2.bonita.cmd;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hibernate.cfg.Configuration;
import org.ow2.bonita.env.EnvConstants;
import org.ow2.bonita.env.Environment;
import org.ow2.bonita.runtime.event.EventExecutor;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.DbTool;
import org.ow2.bonita.util.EnvTool;

public class CreateDatabaseCommand implements Command<Void> {

  private static final long serialVersionUID = -297802302758846748L;
  protected static final Logger LOG = Logger.getLogger(CreateDatabaseCommand.class.getName());
  private boolean recreate;

  public CreateDatabaseCommand(boolean recreate) {
    this.recreate = recreate;
  }

  public Void execute(Environment environment) throws Exception {
    final EventExecutor eventExecutor = EnvTool.getEventExecutor();
    if (recreate) {
      if (eventExecutor != null && eventExecutor.isActive()) {
        eventExecutor.stop();
      }
      final Map<String, String> hibernateConfigs = new HashMap<String, String>();
      hibernateConfigs.put(EnvConstants.HB_CONFIG_CORE, EnvConstants.HB_SESSION_FACTORY_CORE);
      hibernateConfigs.put(EnvConstants.HB_CONFIG_HISTORY, EnvConstants.HB_SESSION_FACTORY_HISTORY);

      for (final Map.Entry<String, String> e : hibernateConfigs.entrySet()) {
        final Configuration config = (Configuration) environment.get(e.getKey());
        if (config != null) {
          LOG.severe("Initializing DB for configuration: " + e.getKey());
          LOG.severe("  - URL                 : " + DbTool.getDbUrl(config));
          LOG.severe("  - DRIVER              : " + DbTool.getDbDriverClass(config));
          LOG.severe("  - USE QUERY CACHE     : " + DbTool.getDbUseQueryCache(config));
          LOG.severe("  - USE 2ND LEVEL CACHE : " + DbTool.getDbUseSecondLeveleCache(config));

          // recreate db between tests
          DbTool.recreateDb(BonitaConstants.DEFAULT_DOMAIN, e.getKey());
          if ("true".equals(DbTool.getDbUseQueryCache(config))) {
            DbTool.cleanCache(BonitaConstants.DEFAULT_DOMAIN, e.getValue());
          }
        }
      }
    }
    if (eventExecutor != null && !eventExecutor.isActive()) {
      eventExecutor.start();
    }
    return null;
  }

}
