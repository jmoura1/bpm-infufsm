/**
 * Copyright (C) 2009  BonitaSoft S.A.
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
package org.ow2.bonita.util;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Parsing of Groovy expressions each time is very expensive and leads to class memory exhaustion.
 * This class purpose is to build groovy Script and cache resulting Scripts,
 * which are not thread-safe objects, in a ThreadLocal soft referenced cache.
 */
public class GroovyScriptBuilder {

  private static ThreadLocal<SoftReference<Map<GroovyScriptCacheKey, Script>>> scriptCacheThreadLocal
      = new ThreadLocal<SoftReference<Map<GroovyScriptCacheKey, Script>>>();

  public static Script getScript(String expression, ClassLoader scriptClassLoader) {
    GroovyScriptCacheKey key = new GroovyScriptCacheKey(scriptClassLoader, expression);

    SoftReference<Map<GroovyScriptCacheKey, Script>> softReference = scriptCacheThreadLocal.get();
    Map<GroovyScriptCacheKey, Script> scriptCache=null;
    if (softReference != null) {
      scriptCache = softReference.get();
    }
    Script script = null;
    if (scriptCache == null) {
      scriptCache = new HashMap<GroovyScriptCacheKey, Script>();
      scriptCacheThreadLocal.set(new SoftReference<Map<GroovyScriptCacheKey, Script>>(scriptCache));
    } else {
      script = scriptCache.get(key);
    }


    if (script == null) {
      script = doBuildScript(expression, scriptClassLoader);
      scriptCache.put(key, script);
    }
    return script;
  }

  private static Script doBuildScript(String expression, ClassLoader scriptClassLoader) {
    Script script;
    GroovyShell shell = new GroovyShell(scriptClassLoader);
    URL defaultGroovyMethods = scriptClassLoader.getResource("org/codehaus/groovy/runtime/DefaultGroovyMethods.class");
    String parsedExpression = expression;
    if (defaultGroovyMethods != null && !expression.contains("org.codehaus.groovy.runtime.DefaultGroovyMethods.*")) {
      parsedExpression = "import static org.codehaus.groovy.runtime.DefaultGroovyMethods.*;\n".concat(expression);
    }
    script = shell.parse(parsedExpression);
    return script;
  }

  // a script is related to its initial textual expression but also to its classloader
  private static class GroovyScriptCacheKey {
    private final String expression;
    private final ClassLoader classLoader;

    private GroovyScriptCacheKey(ClassLoader classLoader, String expression) {
      assert classLoader != null;
      assert expression != null;
      this.classLoader = classLoader;
      this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
      if (null == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GroovyScriptCacheKey that = (GroovyScriptCacheKey) o;

      if (!classLoader.equals(that.classLoader)) return false;
      if (!expression.equals(that.expression)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = expression.hashCode();
      result = 31 * result + classLoader.hashCode();
      return result;
    }
  }
}