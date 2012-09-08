/**
 * Copyright (C) 2009 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.forms.server.api.impl.util;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.forms.client.model.FormFieldAvailableValue;
import org.bonitasoft.forms.client.model.FormFieldValue;
import org.bonitasoft.forms.client.model.FormWidget;
import org.bonitasoft.forms.client.model.WidgetType;
import org.bonitasoft.forms.server.exception.FormNotFoundException;
import org.bonitasoft.forms.server.exception.FormServiceProviderNotFoundException;
import org.bonitasoft.forms.server.provider.FormServiceProvider;
import org.bonitasoft.forms.server.provider.impl.util.FormServiceProviderFactory;
import org.bonitasoft.forms.server.provider.impl.util.FormServiceProviderUtil;

/**
 * @author Anthony Birembaut, Haojie Yuan
 *
 */
public class FormFieldValuesUtil {
    
    /**
     * Logger
     */
    private static Logger LOGGER = Logger.getLogger(FormFieldValuesUtil.class.getName());
        
    /**
     * default dateformat pattern
     */
    protected String defaultDateFormatPattern;
    
    /**
     * Default constructor.
     */
    public FormFieldValuesUtil() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Build a field value object from the process definition
     * @param value the value retrieved from the engine
     * @param formWidget the {@link FormWidget} associated with this field value
     * @return a {@link FormFieldValue} object
     */
    public FormFieldValue getFieldValue(final Object value, final FormWidget formWidget, final Locale locale) {        
        FormFieldValue fieldValue = null;
        if (value != null) {
            // deals with the types whose corresponding form fields
            // input and output value is not a string
            if (value instanceof Date) {
                String formatPattern = null;
                if (formWidget.getDisplayFormat() != null && formWidget.getDisplayFormat().length() > 0) {
                    formatPattern = formWidget.getDisplayFormat();
                } else {
                    formatPattern = defaultDateFormatPattern;
                }
                fieldValue = new FormFieldValue((Date)value, Date.class.getName(), formatPattern);
            } else if (value instanceof Collection<?>) {
                //perform several levels of toString on the items of the collection
                fieldValue = new FormFieldValue((Serializable)processCollectionValue(value), Collection.class.getName());
            } else if (value instanceof String && formWidget.getType().equals(WidgetType.DATE)) {
                final String valueStr = (String)value;
                String formatPattern = null;
                if (formWidget.getDisplayFormat() != null && formWidget.getDisplayFormat().length() > 0) {
                    formatPattern = formWidget.getDisplayFormat();
                } else {
                    formatPattern = defaultDateFormatPattern;
                }
                Date dateValue = null;
                if (valueStr.length() > 0) {
                    try {
                        final DateFormat dateFormat = new SimpleDateFormat(formatPattern, locale);
                        dateValue = dateFormat.parse(valueStr);
                    } catch (final ParseException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(Level.SEVERE, "The initial value for widget " + formWidget.getId() + " is not consistent with the pattern " + formatPattern, e);
                        }
                    }
                }
                fieldValue = new FormFieldValue(dateValue, Date.class.getName(), formatPattern);
            } else if (value instanceof String && formWidget.getType().equals(WidgetType.CHECKBOX)) {
                Boolean booleanValue = null;
                try {
                    booleanValue = Boolean.parseBoolean((String)value);
                } catch (final Exception e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, "The initial value for widget " + formWidget.getId() + " is not consistent with a boolean.", e);
                    }
                    booleanValue = Boolean.valueOf(false);
                }
                fieldValue = new FormFieldValue(booleanValue, Boolean.class.getName());
            } else if (value instanceof Serializable) {
                fieldValue = new FormFieldValue((Serializable)value, value.getClass().getName());
            } else {
                fieldValue = new FormFieldValue(value.toString(), String.class.getName());
            }
        } else {
            fieldValue = getFieldNullValue(formWidget);
        }
        return fieldValue;
    }
    
    protected List<Serializable> processCollectionValue(final Object value) {
        final List<Serializable> valueList = new ArrayList<Serializable>();
        final Collection<?> collectionValues = (Collection<?>) value;
        for (final Object collectionValue : collectionValues) {
            if (collectionValue == null) {
                valueList.add(null);
            } else if (collectionValue instanceof Collection<?>) {
                valueList.add((Serializable)processCollectionValue(collectionValue));
            } else if (collectionValue instanceof Serializable) {
                valueList.add((Serializable)collectionValue);
            } else {
                valueList.add(collectionValue.toString());
            }
        }
        return valueList;
    }
    
    protected FormFieldValue getFieldNullValue(final FormWidget formWidget) {
        FormFieldValue fieldValue = null;
        if (formWidget.getType().equals(WidgetType.CHECKBOX)) {
            fieldValue = new FormFieldValue(Boolean.FALSE, Boolean.class.getName());
        } else if (formWidget.getType().equals(WidgetType.LISTBOX_SIMPLE) || formWidget.getType().equals(WidgetType.RADIOBUTTON_GROUP)) {
            if (formWidget.getAvailableValues().size() > 0) {
                fieldValue = new FormFieldValue(formWidget.getAvailableValues().get(0).getValue(), String.class.getName());
            } else {
                fieldValue = new FormFieldValue("", String.class.getName());
            }
        } else if (formWidget.getType().equals(WidgetType.DATE)) {
            String formatPattern = null;
            if (formWidget.getDisplayFormat() != null && formWidget.getDisplayFormat().length() > 0) {
                formatPattern = formWidget.getDisplayFormat();
            } else {
                formatPattern = defaultDateFormatPattern;
            }
            fieldValue = new FormFieldValue(null, Date.class.getName(), formatPattern);
        } else if (formWidget.getType().equals(WidgetType.DURATION)) {
            fieldValue = new FormFieldValue(Long.valueOf(0), Long.class.getName());
        } else {
            fieldValue = new FormFieldValue(null, Serializable.class.getName());
        }
        return fieldValue;
    }
    
    /**
     * Build a FormFieldAvailableValue List from a String {@link Collection}
     * @param collection
     * @return a List of {@link FormFieldAvailableValue}
     */
    protected List<FormFieldAvailableValue> getAvailableValuesFromCollection(final Collection<?> collection) {
        final List<FormFieldAvailableValue> availableValues = new ArrayList<FormFieldAvailableValue>();
        for (final Object availableValue : collection) {
            final String availableValueStr = getStringValue(availableValue);
            availableValues.add(new FormFieldAvailableValue(availableValueStr, availableValueStr));
        }
        return availableValues;
    }
    
    /**
     * Build a FormFieldAvailableValue List from a {@link Map}
     * @param availableValuesMap
     * @return a List of {@link FormFieldAvailableValue}
     */
    protected List<FormFieldAvailableValue> getAvailableValuesFromMap(final Map<?, ?> availableValuesMap) {
        final List<FormFieldAvailableValue> availableValues = new ArrayList<FormFieldAvailableValue>();
        for (final Entry<?, ?> availableValueEntry : availableValuesMap.entrySet()) {
            final Object key = availableValueEntry.getKey();
            final String keyStr = getStringValue(key);
            final Object value = availableValueEntry.getValue();
            final String valueStr = getStringValue(value);
            availableValues.add(new FormFieldAvailableValue(keyStr, valueStr));
        }
        return availableValues;
    }

    /**
     * Build a FormFieldAvailableValue List from a {@link Map} or a {@link List} and set it in the widget
     * @param availableValuesObject  the {@link Map} or {@link List} of values
     * @param widget the widget to set
     * @return a List of {@link FormFieldAvailableValue}
     */
    public List<FormFieldAvailableValue> getAvailableValues(final Object availableValuesObject, final FormWidget widget) {

        List<FormFieldAvailableValue> availableValuesList = new ArrayList<FormFieldAvailableValue>();
        try {
            availableValuesList = getAvailableValues(availableValuesObject);
        } catch (IllegalArgumentException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "The available values expression for widget " + widget.getId() + " should return a Collection or a Map");
            }
        }
        return availableValuesList;
    }
    
    /**
     * Build a FormFieldAvailableValue List from a {@link Map} or a {@link List} and set it in the widget
     * @param availableValuesObject  the {@link Map} or {@link List} of values
     * @param widget the widget to set
     * @return a List of {@link FormFieldAvailableValue}
     * @throws IllegalArgumentException
     */
    protected List<FormFieldAvailableValue> getAvailableValues(final Object availableValuesObject) throws IllegalArgumentException {

        List<FormFieldAvailableValue> availableValuesList = null;
        if (availableValuesObject != null && availableValuesObject instanceof Collection<?>) {
            final Collection<?> availableValuesCollection = (Collection<?>)availableValuesObject;
            availableValuesList = getAvailableValuesFromCollection(availableValuesCollection);
        } else if (availableValuesObject != null && availableValuesObject instanceof Map<?, ?>) {
            final Map<?, ?> availableValuesMap = (Map<?, ?>)availableValuesObject;
            availableValuesList = getAvailableValuesFromMap(availableValuesMap);
        } else {
            throw new IllegalArgumentException();
        }
        return availableValuesList;
    }

    /**
     * Build a FormFieldAvailableValue List of List from a {@link List} of {@link Map} or a {@link List} of {@link List} and set it in the widget
     * @param availableValuesObject  the {@link List} of {@link Map} or {@link List} of {@link List} of values
     * @param widget the widget to set
     * @return a List of List of {@link FormFieldAvailableValue}
     */
    public List<List<FormFieldAvailableValue>> getTableAvailableValues(final Object tableAvailableValuesObject, final FormWidget widget) {

        final List<List<FormFieldAvailableValue>> tableAvailableValuesList = new ArrayList<List<FormFieldAvailableValue>>();
        try {
            final Collection<?> availableValuesObjects = (Collection<?>)tableAvailableValuesObject;
            for (final Object availableValuesObject : availableValuesObjects) {
                tableAvailableValuesList.add(getAvailableValues(availableValuesObject));
            }
        } catch (final ClassCastException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "The available values expression for widget " + widget.getId() + " should return a Collection of Collections or Maps", e);
            }
        }
        return tableAvailableValuesList;
    }
        
    protected String getStringValue(final Object object) {
        if (object != null) {
            return object.toString();
        }
        return null;
    }
    
    /**
     * get display condition
     * @param conditionExpression
     * @param context
     * @throws FormNotFoundException
     * @throws FormServiceProviderNotFoundException 
     */
    protected String getDisplayConditionStr(final String conditionExpression, final Map<String, Object> context) throws FormNotFoundException, FormServiceProviderNotFoundException {
        final FormServiceProvider formServiceProvider = FormServiceProviderFactory.getFormServiceProvider();
        if (conditionExpression != null && conditionExpression.length() > 0) {
            if (Boolean.FALSE.toString().equals(conditionExpression)) {
                return conditionExpression;
            } else {
                return getStringValue(formServiceProvider.resolveExpression(conditionExpression, context));
            }
        }
        return Boolean.TRUE.toString();
    }
    
    /**
     * set the values of the form widget
     * @param formWidget
     * @param previousPagesFields
     * @param locale
     * @param isCurrentValue
     * @param context
     * @throws FormNotFoundException 
     * @throws FormServiceProviderNotFoundException 
     */
    public void setFormWidgetValues(final FormWidget formWidget, final Map<String, Object> context) throws FormNotFoundException, FormServiceProviderNotFoundException {
        final FormServiceProvider formServiceProvider = FormServiceProviderFactory.getFormServiceProvider();
        final Locale locale = (Locale) context.get(FormServiceProviderUtil.LOCALE);
        formWidget.setLabel(getStringValue(formServiceProvider.resolveExpression(formWidget.getLabel(), context)));
        formWidget.setTitle(getStringValue(formServiceProvider.resolveExpression(formWidget.getTitle(), context)));
        if (formWidget.getSubTitle() != null) {
        	final String label = formWidget.getSubTitle().getLabel();
        	if (label != null && label.trim().length() > 0) {
        		formWidget.getSubTitle().setLabel(getStringValue(formServiceProvider.resolveExpression(label, context)));
        	}
        }
        if (formWidget.getPopupToolTip() != null && formWidget.getPopupToolTip().length() > 0) {
        	formWidget.setPopupToolTip(getStringValue(formServiceProvider.resolveExpression(formWidget.getPopupToolTip(), context)));
        }
        formWidget.setDisplayConditionExpression(getDisplayConditionStr(formWidget.getDisplayConditionExpression(), context));
        Object value = null;
        boolean isEditMode = false;
        if (context.get(FormServiceProviderUtil.IS_EDIT_MODE) != null) {
            isEditMode = Boolean.valueOf(context.get(FormServiceProviderUtil.IS_EDIT_MODE).toString());
        }
        if (!isEditMode && formWidget.getVariableBound() != null) {
            value = formServiceProvider.resolveExpression(formWidget.getVariableBound(), context);
        } else if (isEditMode || formWidget.isViewPageWidget()) {
            value = formServiceProvider.resolveExpression(formWidget.getInitialValueExpression(), context);
        }
        if (formWidget.getType().name().startsWith("FILE") || (formWidget.getType().equals(WidgetType.IMAGE) && formWidget.isDisplayAttachmentImage())) {
            String fileName = null;
            String attachmentName = null;
            if (value != null) {
                attachmentName = value.toString();
                fileName = formServiceProvider.getAttachmentFileName(attachmentName, context);
            }
            final FormFieldValue formFieldValue = new FormFieldValue(fileName, File.class.getName());
            formFieldValue.setAttachmentName(attachmentName);
            formWidget.setInitialFieldValue(formFieldValue);
        } else if (!formWidget.getType().name().startsWith("BUTTON")) {
            // convert the value object returned into a FormFieldValue object.
            formWidget.setInitialFieldValue(getFieldValue(value, formWidget, locale));
            //set the available values list from a groovy expression for listboxes, radiobutton groups and checkbox groups
            if (formWidget.getAvailableValuesExpression() != null) {
                final Object availableValuesObject = formServiceProvider.resolveExpression(formWidget.getAvailableValuesExpression(), context);
                if (formWidget.getType().equals(WidgetType.TABLE)) {
                    final List<List<FormFieldAvailableValue>> availableValues = getTableAvailableValues(availableValuesObject, formWidget);
                    formWidget.setTableAvailableValues(availableValues);
                } else {
                    final List<FormFieldAvailableValue> availableValues = getAvailableValues(availableValuesObject, formWidget);
                    formWidget.setAvailableValues(availableValues);
                }
            }
        }
    }
    
    /**
     * set the tables parameters
     * @param formWidget
     * @param context
     * @throws FormNotFoundException 
     * @throws FormServiceProviderNotFoundException 
     */
    @SuppressWarnings("unchecked")
    public void setTablesParams(final FormWidget formWidget, final Map<String, Object> context) throws FormNotFoundException, FormServiceProviderNotFoundException {
        final FormServiceProvider formServiceProvider = FormServiceProviderFactory.getFormServiceProvider();
        if (WidgetType.TABLE.equals(formWidget.getType()) || WidgetType.EDITABLE_GRID.equals(formWidget.getType())) {
            if (formWidget.getValueColumnIndexExpression() != null) {
                String valueColumnIndexStr = null;
                final Object valueColumnIndex = formServiceProvider.resolveExpression(formWidget.getValueColumnIndexExpression(), context);
                if (valueColumnIndex != null) {
                    valueColumnIndexStr = valueColumnIndex.toString();
                }
                formWidget.setValueColumnIndexExpression(valueColumnIndexStr);
            }
            if (formWidget.getMaxColumnsExpression() != null) {
                String maxColumnsStr = null;
                final Object maxColumns = formServiceProvider.resolveExpression(formWidget.getMaxColumnsExpression(), context);
                if (maxColumns != null) {
                    maxColumnsStr = maxColumns.toString();
                }
                formWidget.setMaxColumnsExpression(maxColumnsStr);
            }
            if (formWidget.getMinColumnsExpression() != null) {
                String minColumnsStr = null;
                final Object minColumns = formServiceProvider.resolveExpression(formWidget.getMinColumnsExpression(), context);
                if (minColumns != null) {
                    minColumnsStr = minColumns.toString();
                }
                formWidget.setMinColumnsExpression(minColumnsStr);
            }
            if (formWidget.getMaxRowsExpression() != null) {
                String maxRowsStr = null;
                final Object maxRows = formServiceProvider.resolveExpression(formWidget.getMaxRowsExpression(), context);
                if (maxRows != null) {
                    maxRowsStr = maxRows.toString();
                }
                formWidget.setMaxRowsExpression(maxRowsStr);
            }
            if (formWidget.getMinRowsExpression() != null) {
                String minRowsStr = null;
                final Object minRows = formServiceProvider.resolveExpression(formWidget.getMinRowsExpression(), context);
                if (minRows != null) {
                    minRowsStr = minRows.toString();
                }
                formWidget.setMinRowsExpression(minRowsStr);
            }
            if (formWidget.getVerticalHeaderExpression() != null) {
                final Object verticalHeader = formServiceProvider.resolveExpression(formWidget.getVerticalHeaderExpression(), context);
                List<String> verticalHeaderList = null;
                if (verticalHeader != null) {
                    try {
                        verticalHeaderList = new ArrayList<String>((Collection<String>)verticalHeader);
                    } catch (final ClassCastException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(Level.SEVERE, "The vertical header expression for widget " + formWidget.getId() + " should return a Collection.", e);
                        }
                    }
                }
                formWidget.setVerticalHeader(verticalHeaderList);
            }
            if (formWidget.getHorizontalHeaderExpression() != null) {
                final Object horizontalHeader = formServiceProvider.resolveExpression(formWidget.getHorizontalHeaderExpression(), context);
                List<String> horizontalHeaderList = null;
                if (horizontalHeader != null) {
                    try {
                        horizontalHeaderList = new ArrayList<String>((Collection<String>)horizontalHeader);
                    } catch (final ClassCastException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(Level.SEVERE, "The horizontal header expression for widget " + formWidget.getId() + " should return a Collection.", e);
                        }
                    }
                }
                formWidget.setHorizontalHeader(horizontalHeaderList);
            }
        }
    }
    
}
