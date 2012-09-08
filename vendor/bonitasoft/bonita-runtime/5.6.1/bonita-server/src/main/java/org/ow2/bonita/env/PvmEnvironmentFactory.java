/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.ow2.bonita.env;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.stream.FileStreamSource;
import org.ow2.bonita.util.stream.InputStreamSource;
import org.ow2.bonita.util.stream.ResourceStreamSource;
import org.ow2.bonita.util.stream.StreamSource;
import org.ow2.bonita.util.stream.StringStreamSource;
import org.ow2.bonita.util.stream.UrlStreamSource;
import org.ow2.bonita.util.xml.Parse;

/**
 * an environment factory that also is the environment-factory context.
 * 
 * <p>
 * This environment factory will produce environments with 2 contexts: the
 * environment-factory context and the block context.
 * </p>
 * 
 * <p>
 * An environment-factory context is build from two wire definitions: the
 * environment-factory wire definition and the environment wire definition.
 * </p>
 * 
 * <p>
 * The environment-factory context itself is build from the environment-factory
 * wire definition. So all objects that are created in this context remain
 * cached for the lifetime of this environment-factory context object.
 * </p>
 * 
 * <p>
 * This environment-factory context is also a environment factory. The produced
 * environments contain 2 contexts: the environment-factory context itself and a
 * new environment context, build from the environment wire definition. For each
 * created environment, a new environment context will be created from the same
 * environment wire definition. Objects in the environment context will live for
 * as long as the environment.
 * </p>
 * 
 * @author Tom Baeyens
 */
public class PvmEnvironmentFactory extends EnvironmentFactory implements
    Context {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger(PvmEnvironmentFactory.class.getName());
  protected static PvmEnvironmentFactoryParser pvmEnvironmentFactoryParser = new PvmEnvironmentFactoryParser();

  protected WireContext environmentFactoryCtxWireContext = null;
  protected WireDefinition environmentCtxWireDefinition = null;

  public PvmEnvironmentFactory() {
  }

  public PvmEnvironmentFactory(String resource) {
    this(resource, null);
  }

  public PvmEnvironmentFactory(String resource, ClassLoader classLoader) {
    this(new ResourceStreamSource(resource, classLoader));
  }

  public PvmEnvironmentFactory(File file) {
    this(new FileStreamSource(file));
  }

  public PvmEnvironmentFactory(URL url) {
    this(new UrlStreamSource(url));
  }

  public PvmEnvironmentFactory(InputStream inputStream) {
    this(new InputStreamSource(inputStream));
  }

  public static PvmEnvironmentFactory parseXmlString(String xmlString) {
    return new PvmEnvironmentFactory(new StringStreamSource(xmlString));
  }

  PvmEnvironmentFactory(StreamSource streamSource) {
    Parse parse = PvmEnvironmentFactoryParser.getInstance().createParse().setDocumentObject(
        this).setStreamSource(streamSource).execute();
    Misc.showProblems(parse.getProblems(), "pvm environment configuration " + streamSource);
  }

  public Environment openEnvironment() {
    PvmEnvironment environment = new PvmEnvironment(this);

    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("opening " + environment);
    }

    // set the classloader
    ClassLoader classLoader = environmentFactoryCtxWireContext.getClassLoader();
    if (classLoader != null) {
      environment.setClassLoader(classLoader);
    }

    // add the environment-factory context
    environment.addContext(environmentFactoryCtxWireContext);

    // add the environment block context
    WireContext environmentContext = new WireContext(
        environmentCtxWireDefinition, Context.CONTEXTNAME_ENVIRONMENT,
        environment, true);
    // add the environment block context to the environment
    environment.addContext(environmentContext);

    try {
      // finish the creation of the environment wire context
      environmentContext.create();

    } catch (RuntimeException e) {
      // On exception, pop environment
      Environment.popEnvironment();
      throw e;
    }
    // if all went well, return the created environment
    return environment;
  }

  public void close() {
    environmentFactoryCtxWireContext.fire(WireContext.EVENT_CLOSE, null);
  }

  // environment-factory context delegation methods
  // ///////////////////////////////////

  public Object get(String key) {
    return environmentFactoryCtxWireContext.get(key);
  }

  public <T> T get(Class<T> type) {
    return environmentFactoryCtxWireContext.get(type);
  }

  public String getName() {
    return environmentFactoryCtxWireContext.getName();
  }

  public boolean has(String key) {
    return environmentFactoryCtxWireContext.has(key);
  }

  public Set<String> keys() {
    return environmentFactoryCtxWireContext.keys();
  }

  public Object set(String key, Object value) {
    return environmentFactoryCtxWireContext.set(key, value);
  }

  // getters and setters //////////////////////////////////////////////////////

  public void setEnvironmentCtxWireDefinition(WireDefinition blockWireDefinition) {
    this.environmentCtxWireDefinition = blockWireDefinition;
  }

  public WireContext getEnvironmentFactoryCtxWireContext() {
    return environmentFactoryCtxWireContext;
  }

  public void setEnvironmentFactoryCtxWireContext(
      WireContext applicationWireContext) {
    this.environmentFactoryCtxWireContext = applicationWireContext;
  }

  public WireDefinition getEnvironmentCtxWireDefinition() {
    return environmentCtxWireDefinition;
  }

}
