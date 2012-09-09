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
 **/
package org.ow2.bonita.parsing.xpdl.binding;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.parsing.xpdl.XpdlParser;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.DateUtil;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessBuilder;
import org.ow2.bonita.util.XmlConstants;
import org.ow2.bonita.util.xml.Parse;
import org.ow2.bonita.util.xml.Parser;
import org.ow2.bonita.util.xml.TagBinding;
import org.ow2.bonita.util.xml.XmlUtil;
import org.w3c.dom.Element;

/**
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras
 */
public abstract class MajorElementBinding extends TagBinding {

  protected MajorElementBinding(String tagName) {
    super(tagName, XmlConstants.XPDL_1_0_NS, XpdlParser.CATEGORY_MAJOR_ELT);
  }

  protected String getId(Element domElement) {
    String id = XmlUtil.attribute(domElement, "Id");
    if (id == null) {
      String message = ExceptionManager.getInstance().getMessage("bpx_MEB_1", domElement);
      throw new BonitaRuntimeException(message);
    }
    return id;
  }

  protected <T extends Object> T getObject(Class<T> clazz, Parse parse) {
    return parse.findObject(clazz);
  }


  protected Collection<Element> parseXpdlMajorElementList(Element fatherElement,
      String listName, String elementName, Parse parse, Parser parser) {

    Element listElement = XmlUtil.element(fatherElement, listName);
    if (listElement != null) {
      Set<Element> xpdlMajorElements = new HashSet<Element>();
      //Set<String> ids = new HashSet<String>();
      //Set<String> names = new HashSet<String>();
      List<Element> subElements = XmlUtil.elements(listElement, elementName);
      if (subElements != null) {
        for (Element subElement : subElements) {
          Element element = (Element) parser.parseElement(subElement, parse, "majorElements");
          if (element != null) {
            xpdlMajorElements.add(element);
          }
        }
      }
      return xpdlMajorElements;
    }
    return null;
  }

  protected void addData(final Element datafieldElement, final String dataName, final String initialValue, final ProcessBuilder processBuilder, final Parse parse) {
    final Element dataTypeElement = XmlUtil.element(datafieldElement, "DataType");
    if (dataTypeElement != null) {
      Element realDataTypeElement = XmlUtil.element(dataTypeElement);
      if (realDataTypeElement.getLocalName().equals("BasicType")) {
        String type = XmlUtil.attribute(realDataTypeElement, "Type");
        if ("STRING".equals(type)) {
          processBuilder.addStringData(dataName, initialValue);
        } else if ("FLOAT".equals(type)) {
          if (initialValue == null) {
            processBuilder.addDoubleData(dataName);
          } else {
            processBuilder.addDoubleData(dataName, new Double(initialValue)); 
          }
        } else if ("INTEGER".equals(type)) {
          if (initialValue == null) {
            processBuilder.addLongData(dataName);
          } else {
            processBuilder.addLongData(dataName, new Long(initialValue));
          }
        } else if ("BOOLEAN".equals(type)) {
          if (initialValue == null) {
            processBuilder.addBooleanData(dataName);
          } else {
            processBuilder.addBooleanData(dataName, Boolean.valueOf(initialValue));
          }
        } else if ("DATETIME".equals(type)) {
          if (initialValue == null) {
            processBuilder.addDateData(dataName);
          } else {
            processBuilder.addDateData(dataName, DateUtil.parseDate(initialValue));
          }
        } else if ("PERFORMER".equals(type)) {
          processBuilder.addStringData(dataName, initialValue);
        } else {
          parse.addProblem("Unsupported BasicType Type:  " + type);
        }
      } else if (realDataTypeElement.getLocalName().equals("EnumerationType")) {
        Set<String> enumerationValues = new HashSet<String>();
        List<Element> enumerationValueElements = XmlUtil.elements(realDataTypeElement, "EnumerationValue");
        if (enumerationValueElements == null) {
          parse.addProblem("No enumeration specified in EnumerationType. Please specify at least one");
        } else {
          for (Element enumeratiuonValueElement : enumerationValueElements) {
            enumerationValues.add(XmlUtil.attribute(enumeratiuonValueElement, "Name"));
          }
        }
        processBuilder.addEnumData(dataName, enumerationValues, initialValue);
      } else {
        parse.addProblem("Unsupported DataType: " + realDataTypeElement.getLocalName());
      }
    }
  }


  protected String getChildTextContent(Element fatherElement, String childTagName) {
    if (fatherElement == null) {
      return null;
    }
    Element childElement = XmlUtil.element(fatherElement, childTagName);
    if (childElement != null) {
      return childElement.getTextContent().trim();
    }
    return null;
  }

  protected <T extends Enum<T> > T getEnumValue(Class< T > enumType, String valueAsString, T defaultValue) {
    if (valueAsString == null) {
      return defaultValue;
    }
    try {
      return Misc.stringToEnum(enumType, valueAsString);
    } catch (IllegalArgumentException e) {
      String message = ExceptionManager.getInstance().getMessage(
          "bpx_MEB_5", valueAsString, enumType.getName());
      throw new BonitaRuntimeException(message, e);
    }
  }

  protected boolean containsExtendedAttribute(Element domElement, String extendedAttributeName) {
    return getExtendedAttribute(domElement, extendedAttributeName) != null;
  }

  protected Element getExtendedAttribute(Element domElement, String extendedAttributeName) {
    Element extendedAttributesElement = XmlUtil.element(domElement, "ExtendedAttributes");
    if (extendedAttributesElement != null) {
      List<Element> extendedAttributeElements = XmlUtil.elements(extendedAttributesElement, "ExtendedAttribute");
      if (extendedAttributeElements != null) {
        for (Element extendedAttributeElement : extendedAttributeElements) {
          String name = XmlUtil.attribute(extendedAttributeElement, "Name");
          if (name != null && name.equals(extendedAttributeName)) {
            return extendedAttributeElement;
          }
        }
      }
    }
    return null;
  }

  protected Set<Element> getExtendedAttributes(Element domElement, String extendedAttributeName) {
    Set<Element> result = null;
    Element extendedAttributesElement = XmlUtil.element(domElement, "ExtendedAttributes");
    if (extendedAttributesElement != null) {
      List<Element> extendedAttributeElements = XmlUtil.elements(extendedAttributesElement, "ExtendedAttribute");
      if (extendedAttributeElements != null) {
        result = new HashSet<Element>();
        for (Element extendedAttributeElement : extendedAttributeElements) {
          String name = XmlUtil.attribute(extendedAttributeElement, "Name");
          if (name != null && name.equals(extendedAttributeName)) {
            result.add(extendedAttributeElement);
          }
        }
      }
    }
    return result;
  }

}
