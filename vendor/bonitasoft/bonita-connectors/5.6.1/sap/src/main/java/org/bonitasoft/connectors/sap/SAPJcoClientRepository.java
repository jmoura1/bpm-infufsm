/**
 * Copyright (C) 2009 BonitaSoft S.A.
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
package org.bonitasoft.connectors.sap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sap.mw.jco.JCO.Client;

/**
 * 
 * @author Charles Souillard
 *
 */
public final class SAPJcoClientRepository {

  private SAPJcoClientRepository() { }
  private static final Map<String, Client> jcoClients = new HashMap<String, Client>();
  
  /**
   * Generates a unique Id for this client and add it in the map
   */
  static synchronized String addJcoClient(final Client jcoClient) {
    String uuid = null;
    do {
      uuid = UUID.randomUUID().toString();
    } while (jcoClients.containsKey(uuid));
    jcoClients.put(uuid, jcoClient);
    return uuid;
  }
  
  static synchronized Client getJcoClient(final String uuid) {
    return jcoClients.get(uuid);
  }
  
  static synchronized void removeJcoClient(final String uuid) {
    jcoClients.remove(uuid);
  }
}
