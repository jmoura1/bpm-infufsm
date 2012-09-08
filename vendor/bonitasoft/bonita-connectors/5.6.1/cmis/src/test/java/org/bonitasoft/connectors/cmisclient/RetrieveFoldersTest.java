/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.bonitasoft.connectors.cmisclient;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.bonitasoft.connectors.cmisclient.common.CMISConnectorUtil;

/**
 * @author Yanyan Liu
 * 
 */
public class RetrieveFoldersTest extends TestCase {

  public void testRetrieveFolders() throws Exception {
    final RetrieveFolders retrieveFolders = new RetrieveFolders();
    CMISConnectorUtil.configConnector(retrieveFolders);
    final Map<String, String> conditions = new HashMap<String, String>();
    conditions.put("name", "Bonita-upload");
    retrieveFolders.setConditions(conditions);
    retrieveFolders.execute();
    System.out.println(retrieveFolders.getCmisFolders().size());
  }

}
