package org.ow2.bonita;

import org.ow2.bonita.env.EnvConstants;
import org.ow2.bonita.env.generator.DbHistoryEnvGenerator;
import org.ow2.bonita.env.generator.EnvEntry;
import org.ow2.bonita.env.generator.EnvGenerator;
import org.ow2.bonita.services.impl.CMISDocumentManager;
import org.ow2.bonita.services.impl.XCmisUserProvider;
import org.ow2.bonita.util.Misc;

public class CMISEnvGenerator extends DbHistoryEnvGenerator {

  public CMISEnvGenerator() {
    super();
    addApplicationEntry(getDocumentationManager());
  }

  private EnvEntry getDocumentationManager() {
    final String key = EnvConstants.DOCUMENTATION_MANAGER_DEFAULT_KEY;
    final String binding = "ATOM";
    final String url = "http://192.168.1.212:8387/xcmis/rest/cmisatom";
    String repositoryId = "linux";
    if (isMac()) {
      repositoryId = "mac";
    } else if (isWindows()) {
      repositoryId = "windows";
    }
    final String i = EnvGenerator.INDENT;
    String xml = "<" + key + " name='" + key + "' class='" + CMISDocumentManager.class.getName() + "'>" + Misc.LINE_SEPARATOR;
    xml += i + "<arg><string value='" + binding + "' /></arg>" + Misc.LINE_SEPARATOR;
    xml += i + "<arg><string value='" + url + "' /></arg>" + Misc.LINE_SEPARATOR;
    xml += i + "<arg><string value='" + repositoryId + "' /></arg>" + Misc.LINE_SEPARATOR;
    xml += i + "<arg>" + Misc.LINE_SEPARATOR;
    xml += i + i + "<object class='" + XCmisUserProvider.class.getName() + "'>" + Misc.LINE_SEPARATOR;
    xml += i + i + i + "<constructor>" + Misc.LINE_SEPARATOR;
    xml += i + i + i + i + "<arg><string value='root'/></arg>" + Misc.LINE_SEPARATOR;
    xml += i + i + i + i + "<arg><string value='exo'/></arg>" + Misc.LINE_SEPARATOR;
    xml += i + i + i + "</constructor>" + Misc.LINE_SEPARATOR;
    xml += i + i + "</object>" + Misc.LINE_SEPARATOR;
    xml += i + "</arg>" + Misc.LINE_SEPARATOR;
    xml += "</" + key + ">";
    return new EnvEntry(key, "Implementation of the documentation manager.", xml, true);
  }

  private static String os = System.getProperty("os.name").toLowerCase();

  protected static boolean isWindows() {
    return os.contains("win");
  }

  protected static boolean isMac() {
    return os.contains("mac");
  }

  protected static boolean isUnix() {
    return os.contains("nix") || os.contains("nux");
  }

}
