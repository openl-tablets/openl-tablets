package org.openl.rules.webstudio.web.jsf.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.el.ValueBinding;


/**
 * Utils for working with JSF components and components' trees.
 *
 * @author Andrey Naumenko
 */
public class ComponentUtils {
    private static final Log log = LogFactory.getLog(ComponentUtils.class);

    /**
     * This method return object property value irrespective of the fact that
     * it's expression(value binding) or not.
     *
     * @param component component
     * @param value property value from component.
     * @param name name of property in value binding map.
     *
     * @return <code>value</code> - if it isn't null, otherwise - return value with name
     *         <code>name</code> from value binding of component <code>component</code>.
     */
    public static Object getObjectValue(UIComponent component, Object value, String name) {
        if (value != null) {
            return value;
        }

        return getObjectFromValueBinding(component, name);
    }

    /**
     * This method return string property value irrespective of the fact that
     * it's expression(value binding) or not.
     *
     * @param component component
     * @param value property value from component.
     * @param name name of property in value binding map.
     *
     * @return <code>value</code> - if it isn't null, otherwise - return value with name
     *         <code>name</code> from value binding of component <code>component</code>.
     */
    public static String getStringValue(UIComponent component, String value, String name) {
        return (String) getObjectValue(component, value, name);
    }

    /**
     * This method return integer property value irrespective of the fact that
     * it's expression(value binding) or not.
     *
     * @param component component
     * @param value property value from component.
     * @param name name of property in value binding map.
     *
     * @return <code>value</code> - if it isn't null, otherwise - return value with name
     *         <code>name</code> from value binding of component <code>component</code>.
     */
    public static Integer getIntegerValue(UIComponent component, Integer value,
        String name) {
        return (Integer) getObjectValue(component, value, name);
    }

    /**
     * This method return boolean property value irrespective of the fact that
     * it's expression(value binding) or not.
     *
     * @param component component
     * @param value property value from component.
     * @param name name of property in value binding map.
     *
     * @return <code>value</code> - if it isn't null, otherwise - return value with name
     *         <code>name</code> from value binding of component <code>component</code>.
     */
    public static Boolean getBooleanValue(UIComponent component, Boolean value,
        String name) {
        return (Boolean) getObjectValue(component, value, name);
    }

    /**
     * Return Object value by name from <code>_valueBindingMap</code>.
     *
     * @param component <code>UIComponent</code> object
     * @param name property name
     *
     * @return Object value from  <code>_valueBindingMap</code> if such item exist,
     *         <code>null</code> - otherwise.
     */
    public static Object getObjectFromValueBinding(UIComponent component, String name) {
        ValueBinding vb = component.getValueBinding(name);
        return (vb != null) ? vb.getValue(getFacesContext()) : null;
    }

    /**
     * Return String value by name from <code>_valueBindingMap</code>.
     *
     * @param component <code>UIComponent</code> object
     * @param name property name
     *
     * @return String value from  <code>_valueBindingMap</code> if such item exist,
     *         <code>null</code> - otherwise.
     */
    public static String getStringFromValueBinding(UIComponent component, String name) {
        return (String) getObjectFromValueBinding(component, name);
    }

    /**
     * Return Throwable value by name from <code>_valueBindingMap</code>.
     *
     * @param component <code>UIComponent</code> object
     * @param name property name
     *
     * @return Throwable value from  <code>_valueBindingMap</code> if such item exist,
     *         <code>null</code> - otherwise.
     */
    public static Throwable getThrowableFromValueBinding(UIComponent component,
        String name) {
        return (Throwable) getObjectFromValueBinding(component, name);
    }

    /**
     * Return Integer value by name from <code>_valueBindingMap</code>.
     *
     * @param component <code>UIComponent</code> object
     * @param name property name
     *
     * @return Integer value from  <code>_valueBindingMap</code> if such item exist,
     *         <code>null</code> - otherwise.
     */
    public static Integer getIntegerFromValueBinding(UIComponent component, String name) {
        return (Integer) getObjectFromValueBinding(component, name);
    }

    /**
     * Return Boolean value by name from <code>_valueBindingMap</code>.
     *
     * @param component <code>UIComponent</code> object
     * @param name property name
     *
     * @return Boolean value from  <code>_valueBindingMap</code> if such item exist,
     *         <code>null</code> - otherwise.
     */
    public static Boolean getBooleanFromValueBinding(UIComponent component, String name) {
        return (Boolean) getObjectFromValueBinding(component, name);
    }

    /**
     * Search UIForm component among parent components.
     *
     * @param component current component
     *
     * @return <code>UIForm</code> component if its exist in parents, <code>null</code> -
     *         otherwise.
     */
    public static UIForm getParentForm(UIComponent component) {
        // find UIForm parent
        UIComponent parent = component.getParent();
        while ((parent != null) && !(parent instanceof UIForm)) {
            parent = parent.getParent();
        }

        return (UIForm) parent;
    }

    /**
     * Get parameters (defined using f:param) for component.
     *
     * @param component UIComponent.
     *
     * @return parameters map (maps String to Object).
     */
    @SuppressWarnings("unchecked")
    public static Map getComponentParameters(UIComponent component) {
        Assert.notNull(component);
        Map params = new HashMap();
        if (component.getChildCount() > 0) {
            for (Iterator it = component.getChildren().iterator(); it.hasNext();) {
                UIComponent child = (UIComponent) it.next();
                if (child instanceof UIParameter) {
                    String name = ((UIParameter) child).getName();
                    Object value = ((UIParameter) child).getValue();
                    params.put(name, value);
                }
            }
        }
        return params;
    }

    /**
     * Searches for a component with a specified id in specified component and
     * all its children.
     *
     * @param component start point.
     * @param id identifier of the target component.
     *
     * @return UIComponent or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static UIComponent findComponentBelow(UIComponent component, String id) {
        UIComponent result = null;
        if (component instanceof NamingContainer) {
            result = component.findComponent(id);
        }
        if (result != null) {
            return result;
        }
        Iterator children = component.getFacetsAndChildren();

        while (children.hasNext()) {
            UIComponent child = (UIComponent) children.next();

            if (child instanceof NamingContainer) {
                result = child.findComponent(id);
            }

            if (result == null) {
                result = findComponentBelow(child, id);
            }

            if (result != null) {
                break;
            }
        }
        return result;
    }

    /**
     * Find all UIComponents with specified class <code>componentClass</code>.
     *
     * @param component start point.
     * @param componentClass class of searched UIComponents.
     *
     * @return list of all components.
     */
    @SuppressWarnings("unchecked")
    public static List findAllComponentsByClass(UIComponent component,
        Class componentClass) {
        if (component == null) {
            component = getFacesContext().getViewRoot();
        }
        List result = new ArrayList();

        List children = component.getChildren();
        for (Iterator i = children.iterator(); i.hasNext();) {
            Object childComponent = i.next();
            if (!(childComponent instanceof UIComponent)) {
                continue;
            }
            if (componentClass.isInstance(childComponent)) {
                result.add(childComponent);
            }
            if (component.getChildCount() > 0) {
                result.addAll(findAllComponentsByClass((UIComponent) childComponent,
                        componentClass));
            }
        }
        return result;
    }

    /**
     * Get parameter value for component.
     *
     * @param component input component.
     * @param parameterName name of parameter.
     *
     * @return parameter value with specified name for this component.
     *
     * @throws IllegalArgumentException when incorrect parameter name for component
     *         specified
     */
    @SuppressWarnings("unchecked")
    public static Object getParameter(UIComponent component, String parameterName) {
        // get parameters for input component
        Map params = ComponentUtils.getComponentParameters(component);
        if (!params.containsKey(parameterName)) {
            // parameter not found
            throw new IllegalArgumentException("Parameter " + parameterName + " for "
                + component.getId() + " not found.");
        }

        return params.get(parameterName);
    }

    /**
     * Get converted value from component with id = <code>value</code> if
     * <code>value</code> is string, otherwise return not modified <code>value</code>.
     * <code>value</code> - must not be null.
     *
     * @param component component for other component(if <code>value</code> - is
     *        component id) search.
     * @param value it is object value(already converted) or component id.
     *
     * @return converted(if necessary) object value.
     *
     * @throws IllegalArgumentException null value specified
     */
    public static Object getParameterObjectValue(UIComponent component, Object value) {
        Object result = value;

        if (value == null) {
            throw new IllegalArgumentException(
                "Parameter value(from expression string) for " + component.getId()
                + " must not be null.");
        } else if (value instanceof String) {
            result = getValueFromComponentById(component, (String) value);
        }

        return result;
    }

    /**
     * Get value from component with specified id.
     *
     * @param convertedComponent component for search component by id.
     * @param componentId target compoennt id.
     *
     * @return converted component value.
     *
     * @throws IllegalArgumentException specified component not exists
     */
    public static Object getValueFromComponentById(UIComponent convertedComponent,
        String componentId) {
        // find component
        UIComponent component = convertedComponent.findComponent(componentId);
        Object convertedValue = null;

        if (component == null) {
            throw new IllegalArgumentException("Can't find component with id = "
                + componentId);
        }

        if (component != null) {
            // get submitted value
            String value = FacesUtils.getRequestParameter(component.getClientId(
                        FacesContext.getCurrentInstance()));
            if (StringUtils.isNotEmpty(value)) {
                try {
                    // get converted value from input field
                    convertedValue = ((UIInput) component).getConverter()
                            .getAsObject(FacesContext.getCurrentInstance(), component,
                                value);
                } catch (ConverterException e) {
                    log.error("Failed conversion for component " + componentId);
                }
            }
        }

        return convertedValue;
    }

    private static FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }
}
