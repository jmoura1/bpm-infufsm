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
package org.ow2.bonita.connector.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.bonita.connector.core.desc.Array;
import org.ow2.bonita.connector.core.desc.Category;
import org.ow2.bonita.connector.core.desc.Checkbox;
import org.ow2.bonita.connector.core.desc.Component;
import org.ow2.bonita.connector.core.desc.CompositeWidget;
import org.ow2.bonita.connector.core.desc.ConnectorDescriptor;
import org.ow2.bonita.connector.core.desc.Enumeration;
import org.ow2.bonita.connector.core.desc.Getter;
import org.ow2.bonita.connector.core.desc.Group;
import org.ow2.bonita.connector.core.desc.Page;
import org.ow2.bonita.connector.core.desc.Password;
import org.ow2.bonita.connector.core.desc.Radio;
import org.ow2.bonita.connector.core.desc.Select;
import org.ow2.bonita.connector.core.desc.Setter;
import org.ow2.bonita.connector.core.desc.Text;
import org.ow2.bonita.connector.core.desc.Textarea;
import org.ow2.bonita.connector.core.desc.Widget;
import org.ow2.bonita.connector.core.desc.WidgetComponent;
import org.ow2.bonita.expression.ExpressionEvaluator;
import org.ow2.bonita.expression.InvalidExpressionException;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public final class ConnectorValidator {

  private ConnectorValidator() {
    super();
  }

  private static boolean isEmpty(String value) {
    return "".equals(value.trim());
  }

  protected static List<ConnectorError> validateRuntime(final Class<? extends Connector> c) {
    ConnectorDescriptor descriptor = ConnectorDescriptorAPI.load(c);
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    if (descriptor != null) {
    	ConnectorError error = null;
      // Setters
      List<Setter> inputs = descriptor.getInputs();
      if (inputs != null) {
        List<String> setterNames = new ArrayList<String>();
        for (Setter setter : inputs) {
          String setterName = setter.getSetterName();
          Object[] parameters = setter.getParameters();
          String required = setter.getRequired();
          String forbidden = setter.getForbidden();
          if (setterName == null) {
            error = new ConnectorError("null",
                new IllegalArgumentException("A setter name is null"));
            errors.add(error);
          } else if (isEmpty(setterName)) {
            error = new ConnectorError("",
                new IllegalArgumentException("A setter name is empty"));
            errors.add(error);
          } else if (!setterName.startsWith("set")) {
            error = new ConnectorError(setterName,
                new IllegalArgumentException("A setter method starts with set"));
            errors.add(error);
          } else {
            String fieldName = Connector.getFieldName(setterName);
            Field field = Connector.getField(c, fieldName);
            if (field == null) {
              error = new ConnectorError(setterName,
                  new IllegalArgumentException("A setter method does not refer to an attribute of " + c.getName()));
              errors.add(error);
            } else {
              if (setterNames.contains(setterName)) {
                error = new ConnectorError(setterName,
                    new IllegalArgumentException("is already set"));
                errors.add(error);
              } else {
                setterNames.add(setterName);
              }
            }

            Class<?>[] paramClass = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
              paramClass[i] = parameters[i].getClass();
            }
            Method m = Connector.getMethod(c, setterName, paramClass);
            if (m == null) {
              String params = formatSetterParameters(setter.getParameters());
              error = new ConnectorError(setterName,
                  new IllegalArgumentException(setterName + params + " does not refer to a method of " + c.getName()));
              errors.add(error);
            } else {
              if (!"void".equals(m.getReturnType().toString())) {
                error = new ConnectorError(setterName,
                    new IllegalArgumentException(setterName + " returns a value so it is not a setter method"));
                errors.add(error);
              }
            }
            if (required != null && required.length() > 0) {
              InvalidExpressionException e = isValidBooleanExpression(required);
              if (e != null) {
                error = new ConnectorError(setterName, e);
                errors.add(error);
              }
            }
            if (forbidden != null && forbidden.length() > 0) {
              InvalidExpressionException e = isValidBooleanExpression(forbidden);
              if (e != null) {
                error = new ConnectorError(setterName, e);
                errors.add(error);
              }
            }
            if (required != null && forbidden != null && required.equals(forbidden)) {
              error = new ConnectorError(setterName,
                  new IllegalArgumentException("Impossible to set a field when required and forbidden are equal"));
              errors.add(error);
            }
          }
        }
      }
      // Getters
      List<Getter> outputs = descriptor.getOutputs();
      if (outputs != null) {
        List<String> getterNames = new ArrayList<String>();
        for (Getter getter : outputs) {
          String getterName = getter.getName();
          if (getterName == null) {
            error = new ConnectorError("null", new IllegalArgumentException("A getter name is null"));
            errors.add(error);
          } else if (isEmpty(getterName)) {
            error = new ConnectorError("", new IllegalArgumentException("A getter name is empty"));
            errors.add(error);
          } else {
            String getterMethod = Connector.getGetterName(getterName);
            if (Connector.getMethod(c, getterMethod, null) == null) {
              error = new ConnectorError(getterName,
                  new IllegalArgumentException(getterName + " does not refer to a method of " + c.getName()));
              errors.add(error);
            } else {
              if (getterNames.contains(getterName)) {
                error = new ConnectorError(getterName, new IllegalArgumentException("is already set"));
                errors.add(error);
              } else {
                getterNames.add(getterName);
              }
              Method m = Connector.getMethod(c, getterMethod, null);
              if ("void".equals(m.getReturnType().toString())) {
                error = new ConnectorError(getterName,
                    new IllegalArgumentException("A getter method do return a value"));
                errors.add(error);
              }
            }
          }
        }
      }
    }
    return errors;
  }

  /**
   * Checks whether the given expression is a valid boolean expression.
   * @param expression the expression to check
   * @return null if the expression is well-formed; an
   * InvalidExpressionException otherwise
   */
  protected static InvalidExpressionException isValidBooleanExpression(
      final String expression) {
    try {
      new ExpressionEvaluator(expression);
      return null;
    } catch (InvalidExpressionException e) {
      return e;
    }
  }

  protected static List<ConnectorError> validateView(final Class<? extends Connector> c) {
    ConnectorDescriptor descriptor = ConnectorDescriptorAPI.load(c);
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    if (descriptor != null) {
    	ConnectorError error = null;
      String connectorId = descriptor.getConnectorId();
      if (connectorId != null && isEmpty(connectorId)) {
        error = new ConnectorError(c.getSimpleName(), new IllegalArgumentException("The connector Id is empty"));
        errors.add(error);
      }

      List<Category> categories = descriptor.getCategories();
      if (categories != null) {
        for (Category category : categories) {
          String categoryName = category.getName();
          if (categoryName == null) {
            error = new ConnectorError(c.getSimpleName(), new IllegalArgumentException("The category name is missing"));
            errors.add(error);
          } else if (isEmpty(categoryName)) {
            error = new ConnectorError(c.getSimpleName(), new IllegalArgumentException("The category name is empty"));
            errors.add(error);
          }
        }
      }
      List<Page> pages = descriptor.getPages();
      if (pages != null) {
        List<String> allIds = new ArrayList<String>();
        List<String> pageIds = new ArrayList<String>();
        for (int i = 0; i < pages.size(); i++) {
          String pageId = pages.get(i).getPageId();
          List<Component> components = pages.get(i).getWidgets();
          if (pageId == null) {
            error = new ConnectorError(c.getSimpleName(),
                new IllegalArgumentException("Page #" + (i + 1) + " is null"));
            errors.add(error);
          } else if(isEmpty(pageId)) {
            error = new ConnectorError(c.getSimpleName(),
                new IllegalArgumentException("Page #" + (i + 1) + " is empty"));
            errors.add(error);
          } else {
            if (pageIds.contains(pageId)) {
              error = new ConnectorError(c.getSimpleName(),
                  new IllegalArgumentException("Another page has already this Id: " + pageId));
              errors.add(error);
            } else {
              pageIds.add(pageId);
            }
            if (pageId != null && !isEmpty(pageId)
                && (components == null || components.isEmpty())) {
              error = new ConnectorError(c.getSimpleName(),
                  new IllegalArgumentException("Page " + pageId + " does not contain any widgets"));
              errors.add(error);
            } else if ((pageId == null || isEmpty(pageId)) && components != null && !components.isEmpty()){
              error = new ConnectorError(c.getSimpleName(),
                  new IllegalArgumentException("Page Id is missing because of Widget declarations"));
              errors.add(error);
            } else {
              Map<String, Boolean> radioList = new HashMap<String, Boolean>();
              for (Component component : components) {
                if (component instanceof Group) {
                  errors.addAll(checkGroup((Group) component, radioList, allIds));
                } else if (component instanceof CompositeWidget) {
                  errors.addAll(checkCompositeWidget((CompositeWidget) component, radioList, allIds));
                } else if (component instanceof WidgetComponent) {
                  errors.addAll(checkWidget((Widget) component, radioList, allIds));
                }
              }
            }
          }
        }
      }
    }
    return errors;
  }

  private static List<ConnectorError> checkGroup(Group group, Map<String, Boolean> radioList, List<String> allIds) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = group.getLabelId();
    error = checkLabelId(labelId, allIds);
    if (error != null) {
      errors.add(error);
    } else {
      List<WidgetComponent> widgets = group.getWidgets();
      if (widgets == null) {
        error = new ConnectorError(labelId,
            new IllegalArgumentException("The group has no widgets"));
        errors.add(error);
      } else {
        for (WidgetComponent widget : widgets) {
          if (widget instanceof CompositeWidget) {
            errors.addAll(checkCompositeWidget((CompositeWidget) widget, radioList, allIds));
          } else if (widget instanceof Widget) {
            errors.addAll(checkWidget((Widget) widget, radioList, allIds));
          }
        }
      }
    }
    return errors;
  }

  private static List<ConnectorError> checkCompositeWidget(CompositeWidget composite, Map<String, Boolean> radioList, List<String> allIds) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = composite.getLabelId();
    Setter setter = composite.getSetter();
    error = checkLabelId(labelId, allIds);
    if (error != null) {
      errors.add(error);
    } else if (setter == null) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The " + composite.getClass().getSimpleName() + " widget does not refer to any setter"));
      errors.add(error);
    } else {
      List<Widget> widgets = composite.getWidgets();
      if (widgets == null) {
        error = new ConnectorError(labelId,
            new IllegalArgumentException("The composite has no widgets"));
        errors.add(error);
      } else {
        for (Widget widget : widgets) {
          errors.addAll(checkWidget(widget, radioList, allIds));
        }
      }
    }
    return errors;
  }

  private static List<ConnectorError> checkWidget(Widget widget, Map<String, Boolean> radioList, List<String> allIds) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = widget.getLabelId();
    Setter setter = widget.getSetter();
    error = checkLabelId(labelId, allIds);
    if (error != null) {
      errors.add(error);
    } else if (setter == null) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The " + widget.getClass().getSimpleName() + " widget does not refer to any setter"));
      errors.add(error);
    } else {
      if (widget instanceof Password) {
        errors.addAll(checkPasswordWidget((Password) widget));
      } else if (widget instanceof Text) {
        errors.addAll(checkTextWidget((Text) widget));
      } else if (widget instanceof Textarea) {
        errors.addAll(checkTextareaWidget((Textarea) widget));
      } else if (widget instanceof Radio) {
        errors.addAll(checkRadioWidget((Radio) widget, radioList));
      } else if (widget instanceof Checkbox) {
        errors.addAll(checkCheckboxWidget((Checkbox) widget));
      } else if (widget instanceof Select) {
        errors.addAll(checkSelectWidget((Select) widget));
      } else if (widget instanceof Enumeration) {
        errors.addAll(checkEnumerationWidget((Enumeration) widget));
      } else if (widget instanceof Array) {
        errors.addAll(checkArrayWidget((Array) widget));
      }
    }
    return errors;
  }

  private static ConnectorError checkLabelId(String labelId, List<String> allIds) {
    ConnectorError error = null;
    if (labelId == null) {
      error = new ConnectorError("null", new IllegalArgumentException("The label Id is null"));
    } else if (isEmpty(labelId)) {
      error = new ConnectorError("", new IllegalArgumentException("The label Id is empty"));
    } else if (allIds.contains(labelId)) {
        error = new ConnectorError(labelId, new IllegalArgumentException("The label Id refers to another group Id, composite widget Id or widget Id"));
    } else {
      allIds.add(labelId);
    }
    return error;
  }

  private static List<ConnectorError> checkArrayWidget(Array array) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = array.getLabelId();
    if (array.isFixedCols()) {
      int cols = array.getCols();
      if (cols < 1) {
        error = new ConnectorError(labelId,
            new IllegalArgumentException("The column number cannot be less than 1"));
        errors.add(error);
      }
      if (array.getColsCaptions() != null) {
        if (cols != array.getColsCaptions().size()) {
          error = new ConnectorError(labelId,
              new IllegalArgumentException("The size of the caption array is different from the columns number"));
          errors.add(error);
        }
      }
      
    }
    if (array.isFixedRows()) { 
      if (array.getRows() < 1) {
        error = new ConnectorError(labelId,
            new IllegalArgumentException("The row number cannot be less than 1"));
        errors.add(error);
      }
    }
    return errors;
  }

  private static List<ConnectorError> checkTextWidget(Text text) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = text.getLabelId();
    if (text.getSize() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The text size cannot be less than 1"));
      errors.add(error);
    }
    if (text.getMaxChar() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The maximum number of characters cannot be less than 1"));
      errors.add(error);
    }
    return errors;
  }

  private static List<ConnectorError> checkPasswordWidget(Password password) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = password.getLabelId();
    if (password.getSize() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The password size cannot be less than 1"));
      errors.add(error);
    }
    if (password.getMaxChar() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The maximum number of characters cannot be less than 1"));
      errors.add(error);
    }
    return errors;
  }

  private static List<ConnectorError> checkTextareaWidget(Textarea textarea) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = textarea.getLabelId();
    if (textarea.getColumns() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The column number of the textarea cannot be less than 1"));
      errors.add(error);
    }
    if (textarea.getRows() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The row number of the textarea cannot be less than 1"));
      errors.add(error);
    }
    if (textarea.getMaxChar() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The maximum number of characters cannot be less than 1"));
      errors.add(error);
    }
    if (textarea.getMaxCharPerRow() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The maximum number of characters per row cannot be less than 1"));
      errors.add(error);
    }
    return errors;
  }

  private static List<ConnectorError> checkRadioWidget(Radio radio, Map<String, Boolean> radioList) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String radioName = radio.getName();
    String labelId = radio.getLabelId();
    if (radioName.length() == 0) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The name of the radio button cannot be empty"));
      errors.add(error);
    }
    if (radio.getValue().length() == 0) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The value of the radio button cannot be empty"));
      errors.add(error);
    }
    Boolean checked = (Boolean) radio.getSetter().getParameters()[0];
    if (radioList.containsKey(radioName)) {
      if (radioList.get(radioName)) {
        if (checked) {
          String err = "Only one radio button can be checked in the group " + radioName;
          error = new ConnectorError(labelId, new IllegalArgumentException(err));
          errors.add(error);
        }
      } else {
        radioList.put(radioName, checked);
      }
    } else {
      radioList.put(radioName, checked);
    }
    return errors;
  }

  private static List<ConnectorError> checkCheckboxWidget(Checkbox check) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = check.getLabelId();
    if (check.getName().length() == 0) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The checkbox name cannot be emtpy"));
      errors.add(error);
    }
    if (check.getValue().length() == 0) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The checkbox value cannot be emtpy"));
      errors.add(error);
    }
    return errors;
  }

  private static List<ConnectorError> checkSelectWidget(Select select) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = select.getLabelId();
    Map<String, String> options = select.getValues();
    if (options.size() == 0) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The select values cannot be empty"));
      errors.add(error);
    } else {
      for (Entry<String, String> option : options.entrySet()) {
        String label = option.getKey();
        if (label.length() == 0) {
          error = new ConnectorError(labelId,
              new IllegalArgumentException("An option label cannot be empty"));
          errors.add(error);
        }
        if (option.getValue().length() == 0) {
          error = new ConnectorError(labelId,
              new IllegalArgumentException("An option value cannot be empty"));
          errors.add(error);
        }
        //top can have an empty label and empty value
      }
    }
    return errors;
  }

  private static List<ConnectorError> checkEnumerationWidget(Enumeration enumeration) {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    ConnectorError error = null;
    String labelId = enumeration.getLabelId();
    if (enumeration.getLines() < 1) {
      error = new ConnectorError(labelId,
          new IllegalArgumentException("The line number cannot be less than 1"));
      errors.add(error);
    }
    Map<String, String> options = enumeration.getValues();
    if (options != null) {
      if (options.size() == 0) {
        error = new ConnectorError(labelId,
            new IllegalArgumentException("The enumeration values cannot be empty"));
        errors.add(error);
      } else {
        for (Entry<String, String> option : options.entrySet()) {
          String label = option.getKey();
          if (label.length() == 0) {
            error = new ConnectorError(labelId,
                new IllegalArgumentException("An option label cannot be empty"));
            errors.add(error);
          }
          if (option.getValue().length() == 0) {
            error = new ConnectorError(labelId,
                new IllegalArgumentException("An option value cannot be empty"));
            errors.add(error);
          }
        }
        int[] indices = enumeration.getSelectedIndices();
        if (indices != null) {
          List<Integer> integers = new ArrayList<Integer>();
          if (indices.length > options.size()) {
            error = new ConnectorError(labelId,
                new IllegalArgumentException("Impossible to have more selected indices than available options"));
            errors.add(error);
          }
          for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0) {
              error = new ConnectorError(labelId,
                  new IllegalArgumentException("Indices cannot contain a negative value"));
              errors.add(error);
            }
            if (indices[i] > options.size()) {
              error = new ConnectorError(labelId,
                  new IllegalArgumentException("An indice cannot be greater than the enumeration"));
              errors.add(error);
            }
            if (integers.contains(indices[i])) {
              error = new ConnectorError(labelId,
                  new IllegalArgumentException("It is not allowed to have two identical indices"));
              errors.add(error);
            } else if(integers.contains(0) && indices.length > 1) {
              error = new ConnectorError(labelId,
                  new IllegalArgumentException("Either 0 or selected indices upper than 0 are allowed, not both"));
              errors.add(error);
            } else{
              integers.add(indices[i]);
            }
          }
        }
      }
    }
    return errors;
  }

  private static String formatSetterParameters(Object[] setterParameters) {
    StringBuilder builder = new StringBuilder("(");
    if (setterParameters != null) {
      int size = setterParameters.length;
      for (int i = 0; i < size - 1; i++) {
        builder.append(getClassName(setterParameters[i])).append(", ");
      }
      builder.append(getClassName(setterParameters[size - 1]));
    }
    builder.append(")");
    return builder.toString();
  }

  private static String getClassName(Object o) {
    String className;
    if (o == null) {
      className = "null";
    } else {
      className = o.getClass().getSimpleName();
    }
    return className;
  }
}
