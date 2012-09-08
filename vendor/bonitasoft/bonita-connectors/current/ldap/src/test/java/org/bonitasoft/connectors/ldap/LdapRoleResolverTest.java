package org.bonitasoft.connectors.ldap;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.connector.core.Connector;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.util.BonitaException;

import junit.framework.TestCase;

public class LdapRoleResolverTest extends TestCase {

  protected static final Logger LOG = Logger.getLogger(LdapRoleResolverTest.class.getName());

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    if (LdapRoleResolverTest.LOG.isLoggable(Level.WARNING)) {
      LdapRoleResolverTest.LOG.warning("======== Starting test: " + this.getClass().getName() + "." + this.getName() + "() ==========");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (LdapRoleResolverTest.LOG.isLoggable(Level.WARNING)) {
      LdapRoleResolverTest.LOG.warning("======== Ending test: " + this.getName() + " ==========");
    }
    super.tearDown();
  }

  public void testValidateConnector() throws BonitaException {
    List<ConnectorError> errors = Connector.validateConnector(LdapRoleResolver.class);
    assertTrue(errors.isEmpty());
  }
}
