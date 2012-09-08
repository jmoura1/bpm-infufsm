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
package org.ow2.bonita.facade.runtime.impl;

import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.Label;

/**
 * This class defines the concept of 'Label'. A Label is a human readable text
 * value that can be associated to a 'Case'. This joins the concept of 'key
 * words' that can be associated to documents to help the indexing.
 * 
 * @author Nicolas Chabanoles
 * 
 */
public class LabelImpl extends CategoryImpl implements Label {

  private static final long serialVersionUID = 888850443702378668L;

  protected String ownerName;
  protected String editableCSSStyleName;

  protected boolean isVisible;
  protected boolean hasToBeDisplayed;
  protected int displayOrder;
  protected boolean isSystemLabel;
  
  protected LabelImpl() {
    super();
    // Mandatory for serialization.
  }

  public LabelImpl(String labelName, String ownerName,
      String editableCSSStyleName, String readonlyCSSStyleName,
      String previewCSSStyleName, boolean isVisible, boolean hasToBeDisplayed,
      String iconCSSStyle, int displayOrder,
      boolean isSystemLabel) {
    super(labelName);
    this.ownerName = ownerName;
    this.editableCSSStyleName = editableCSSStyleName;
    this.readonlyCSSStyleName = readonlyCSSStyleName;
    this.previewCSSStyleName = previewCSSStyleName;
    this.isVisible = isVisible;
    this.hasToBeDisplayed = hasToBeDisplayed;
    this.iconCSSStyle = iconCSSStyle;
    this.displayOrder = displayOrder;
    this.isSystemLabel = isSystemLabel;
  }

  public LabelImpl(final Label src) {
    super(src.getName());
    this.ownerName = src.getOwnerName();
    this.editableCSSStyleName = src.getEditableCSSStyleName();
    this.readonlyCSSStyleName = src.getReadonlyCSSStyleName();
    this.previewCSSStyleName = src.getPreviewCSSStyleName();
    this.isVisible = src.isVisible();
    this.hasToBeDisplayed = src.isHasToBeDisplayed();
    this.iconCSSStyle = src.getIconCSSStyle();
    this.displayOrder = src.getDisplayOrder();
    this.isSystemLabel = src.isSystemLabel();

    //to prevent join tables, this set is not copied
    /*
    if (!src.getCases().isEmpty()) {
      this.cases = new HashSet<Case>();
      for (Case case_ : ((LabelImpl)src).getInternalCases()) {
        this.cases.add(new CaseImpl(case_));
      }
    }
    */
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append(this.getClass().getSimpleName());
    sb.append(", name= " + getName());
    sb.append(", owner= " + getOwnerName());
    return sb.toString();
  }


  public String getOwnerName() {
    return this.ownerName;
  }

  public void setOwnerName(String aOwnerName) {
    this.ownerName = aOwnerName;
  }

  public String getEditableCSSStyleName() {
    return this.editableCSSStyleName;
  }

  public void setEditableCSSStyleName(String aEditableCSSStyleName) {
    this.editableCSSStyleName = aEditableCSSStyleName;
  }
  
  public int getDisplayOrder() {
    return this.displayOrder;
  }

  public void setDisplayOrder(int aDisplayOrder) {
    this.displayOrder = aDisplayOrder;
  }

  public boolean isVisible() {
    return this.isVisible;
  }

  public void setVisible(boolean aIsVisible) {
    this.isVisible = aIsVisible;
  }

  public boolean isHasToBeDisplayed() {
    return this.hasToBeDisplayed;
  }

  public void setHasToBeDisplayed(boolean aHasToBeDisplayed) {
    this.hasToBeDisplayed = aHasToBeDisplayed;
  }



  public boolean isSystemLabel() {
    return this.isSystemLabel;
  }

  public void setSystemLabel(boolean aIsSystemLabel) {
    this.isSystemLabel = aIsSystemLabel;
  }
  
  @Override
  public boolean equals(Object anObject) {
    if (anObject == null) {
      return false;
    }
    if (!(anObject instanceof LabelImpl)) {
      return false;
    }
    LabelImpl other = (LabelImpl) anObject;
    return super.equals(other) && getOwnerName().equals(other.getOwnerName());
  }


	@Override
	public int compareTo(Category o) {
		if(o instanceof Label) {
			Label other = (Label)o;
			int thisVal = this.displayOrder;
		    int anotherVal = other.getDisplayOrder();
		    return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
		} else {
			return super.compareTo(o);
		}
	}
}
