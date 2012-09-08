/**
 * Copyright (C) 2006  Bull S. A. S.
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
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.connector.core.desc;

import java.util.Map;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class Enumeration extends Widget {
  
  public static enum Selection { SINGLE, MUTLI };
  private Map<String, String> values;
  private int[] selectedIndices;
  private int lines;
  private Selection selection;
  
  public Enumeration(String labelId, Setter setter, 
      Map<String, String> values, int[] selectedIndices, int lines,
      Selection selection) {
    super(labelId, setter);
    this.values = values;
    this.selectedIndices = selectedIndices;
    this.lines = lines;
    this.selection = selection;
  }

  public Map<String, String> getValues() {
    return values;
  }

  public int[] getSelectedIndices() {
    return selectedIndices;
  }

  public int getLines() {
    return lines;
  }

  public Selection getSelection() {
    return selection;
  }
  
  @Override
  public boolean equals(Object obj) {
    boolean equals = super.equals(obj);
    if (equals && obj instanceof Enumeration) {
      Enumeration temp = (Enumeration) obj;
      return (temp.getLines() == this.getLines()
          && temp.getValues().equals(this.getValues()));
    } else {
      return false;
    }
  }
}
