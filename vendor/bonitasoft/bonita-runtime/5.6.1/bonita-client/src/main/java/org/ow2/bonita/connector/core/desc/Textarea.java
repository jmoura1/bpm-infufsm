/**
 * Copyright (C) 2009  Bull S. A. S.
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

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class Textarea extends Widget {

  private int rows;
  private int columns;
  private int maxChar;
  private int maxCharPerRow;
  
  public Textarea(String labelId, Setter setter, int rows,
      int columns, int maxChar, int maxCharPerRow) {
    super(labelId, setter);
    this.rows = rows;
    this.columns = columns;
    this.maxChar = maxChar;
    this.maxCharPerRow = maxCharPerRow;
  }

  public int getRows() {
    return rows;
  }

  public int getColumns() {
    return columns;
  }

  public int getMaxChar() {
    return maxChar;
  }

  public int getMaxCharPerRow() {
    return maxCharPerRow;
  }
  
  @Override
  public boolean equals(Object obj) {
    boolean equals = super.equals(obj);
    if (equals && obj instanceof Textarea) {
      Textarea temp = (Textarea) obj;
      return (temp.getColumns() == this.getColumns()
          && temp.getRows() == this.getRows()
          && temp.getMaxChar() == this.getMaxChar()
          && temp.getMaxCharPerRow() == this.getMaxCharPerRow());
    } else {
      return false;
    }
  }
}
