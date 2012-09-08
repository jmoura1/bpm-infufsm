/**
 * Copyright (C) 2009  BonitaSoft S.A..
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
package org.ow2.bonita.connector.core.desc;

import java.util.List;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class Array extends Widget {

  private int cols;
  private int rows;
  private boolean fixedCols;
  private boolean fixedRows;
  private List<String> colsCaptions;

  public Array(String labelId, Setter setter, int cols, int rows,
      boolean fixedCols, boolean fixedRows, List<String> colsCaptions) {
    super(labelId, setter);
    this.cols = cols;
    this.rows = rows;
    this.fixedCols = fixedCols;
    this.fixedRows = fixedRows;
    this.colsCaptions = colsCaptions;
  }

  public int getCols() {
    return cols;
  }

  public int getRows() {
    return rows;
  }

  public boolean isFixedCols() {
    return fixedCols;
  }

  public boolean isFixedRows() {
    return fixedRows;
  }

  public List<String> getColsCaptions() {
    return colsCaptions;
  }

  @Override
  public boolean equals(Object obj) {
    boolean equals = super.equals(obj);
    if (equals && obj instanceof Array) {
      Array temp = (Array) obj;
      return (temp.getCols() == this.getCols() && temp.getRows() == this.getRows()
          && temp.isFixedCols() == this.isFixedCols() && temp.isFixedRows() == this.isFixedRows()
          && temp.getColsCaptions().equals(this.getColsCaptions()));
    } else {
      return false;
    }
  }

}
