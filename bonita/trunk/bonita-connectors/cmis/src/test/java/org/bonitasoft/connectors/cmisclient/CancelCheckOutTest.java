/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.cmisclient;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.bonitasoft.connectors.cmisclient.common.CMISConnectorUtil;

/**
 * @author Yanyan Liu
 */
public class CancelCheckOutTest extends TestCase {

    protected static final Logger LOG = Logger.getLogger(CancelCheckOutTest.class.getName());

    private String filename;

    private String path;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (CancelCheckOutTest.LOG.isLoggable(Level.WARNING)) {
            CancelCheckOutTest.LOG.warning("======== Starting test: " + this.getClass().getName() + "." + this.getName() + "() ==========");
        }
        filename = CMISConnectorUtil.getFileName();
        path = "/" + filename + ".txt";
    }

    @Override
    protected void tearDown() throws Exception {
        if (CancelCheckOutTest.LOG.isLoggable(Level.WARNING)) {
            CancelCheckOutTest.LOG.warning("======== Ending test: " + this.getName() + " ==========");
        }
        // delete doc
        CMISConnectorUtil.deleteDocByPath(path);
        super.tearDown();
    }

    public void testCancelCheckOutDocument() throws Exception {
        // prepare pwcDocumentId
        final String pWCDocumentId = CMISConnectorUtil.getPWCDocumentId(filename);
        // cancel check out
        final CancelCheckOut cancelCheckOut = new CancelCheckOut();
        CMISConnectorUtil.configConnector(cancelCheckOut);
        cancelCheckOut.setPWCDocumentID(pWCDocumentId);
        cancelCheckOut.execute();
        assertTrue(cancelCheckOut.getIsSucceed());
    }
}
