/**
 * Copyright (C) 2007  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
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
package org.ow2.bonita.env.descriptor;

import org.ow2.bonita.env.EnvironmentFactory;
import org.ow2.bonita.env.WireContext;

/**
 * the {@link EnvironmentFactory} of the current {@link WireContext}.
 * 
 * @author Tom Baeyens
 * @author Guillaume Porcher (documentation)
 */
public class EnvironmentFactoryDescriptor extends AbstractDescriptor {

  private static final long serialVersionUID = 1L;

  EnvironmentFactory environmentFactory;

  public EnvironmentFactoryDescriptor(EnvironmentFactory environmentFactory) {
    this.environmentFactory = environmentFactory;
  }

  public Object construct(WireContext wireContext) {
    return environmentFactory;
  }
}
