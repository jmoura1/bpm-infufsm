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
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 */
package org.ow2.bonita.type.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.ow2.bonita.type.Converter;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.ExceptionManager;

public class SerializableToBytesConverter implements Converter {

  private static final long serialVersionUID = 1L;

  public boolean supports(Object value) {
    return value == null || Serializable.class.isAssignableFrom(value.getClass());
  }

  public Object convert(Object o) {
    byte[] bytes = null;
    ObjectOutputStream oos = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(baos);
      oos.writeObject(o);
      oos.flush();
      bytes = baos.toByteArray();
    } catch (IOException e) {
      String message = ExceptionManager.getInstance().getFullMessage("bp_STBC_1", o);
      throw new BonitaRuntimeException(message, e);
    } finally {
      try {
        if (oos != null) {
          oos.close();
        }
      } catch (IOException e) {
        String message = ExceptionManager.getInstance().getFullMessage("bp_STBC_1", o);
        throw new BonitaRuntimeException(message, e);
      }
    }
    return bytes;
  }

  public Object revert(Object o) {
    byte[] bytes = (byte[]) o;
    ObjectInputStream ois = null;
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ois = new ObjectInputStream(bais) {

        @Override
        protected Class< ? > resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          final String className = desc.getName();
          ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          Class< ? > clazz = Class.forName(className, true, classLoader);
          return clazz;
        }
      };

      Object object = ois.readObject();
      return object;
    } catch (Exception e) {
      String message = ExceptionManager.getInstance().getFullMessage("bp_STBC_2");
      throw new BonitaRuntimeException(message, e);
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException e) {
          String message = ExceptionManager.getInstance().getFullMessage("bp_STBC_2");
          throw new BonitaRuntimeException(message, e);
        }
      }
    }
  }

}
