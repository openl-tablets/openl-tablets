package org.openl.rules.dt;

import java.lang.reflect.Array;
import java.util.*;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Created by ymolchan on 05.02.2016.
 */
public class ActionInvoker implements Invokable {

    private final int[] rules;
    private final IBaseAction[] actions;

    ActionInvoker(int[] rules, IBaseAction[] actions) {
        this.rules = rules;
        this.actions = actions;
    }

    private Object addReturnValues(Collection<Object> returnValue, Object[] returnValues, boolean[] f) {
        for (int i = 0; i < returnValues.length; i++) {
            if (f[i]) {
                returnValue.add(returnValues[i]);
            }
        }
        return returnValue;
    }

    private Object addReturnValues(Map<Object, Object> returnValue,
            Object[] returnValues,
            Object[] keyValues,
            boolean[] f) {
        for (int i = 0; i < returnValues.length; i++) {
            if (f[i]) {
                returnValue.put(keyValues[i], returnValues[i]);
            }
        }
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    private Object processReturnValue(Object[] returnValues, Object[] keyValues, boolean[] f, IOpenClass type) {
        if (type.isArray()) {
            int c = 0;
            for (int i = 0; i < returnValues.length; i++) {
                if (f[i]) {
                    c++;
                }
            }
            if (c == 0) {
                return returnValues;
            }
            Object[] ret = (Object[]) Array.newInstance(type.getComponentClass().getInstanceClass(), c);
            int j = 0;
            for (int i = 0; i < returnValues.length; i++) {
                if (f[i]) {
                    ret[j] = returnValues[i];
                    j++;
                }
            }
            return ret;
        } else {
            if (Map.class.equals(type.getInstanceClass())) {
                return addReturnValues(new HashMap<>(), returnValues, keyValues, f);
            }
            if (SortedMap.class.equals(type.getInstanceClass())) {
                return addReturnValues(new TreeMap<>(), returnValues, keyValues, f);
            }
            if (Map.class.isAssignableFrom(type.getInstanceClass())) {
                try {
                    return addReturnValues((Map<Object, Object>) type.getInstanceClass().newInstance(),
                        returnValues,
                        keyValues,
                        f);
                } catch (Exception e) {
                    throw new OpenLRuntimeException(e);
                }
            }
            if (Collection.class.equals(type.getInstanceClass()) || List.class.equals(type.getInstanceClass())) {
                return addReturnValues(new ArrayList<>(), returnValues, f);
            }
            if (Set.class.equals(type.getInstanceClass())) {
                return addReturnValues(new HashSet<>(), returnValues, f);
            }
            if (SortedSet.class.equals(type.getInstanceClass())) {
                return addReturnValues(new TreeSet<>(), returnValues, f);
            }
            if (Collection.class.isAssignableFrom(type.getInstanceClass())) {
                try {
                    return addReturnValues((Collection<Object>) type.getInstanceClass().newInstance(), returnValues, f);
                } catch (Exception e) {
                    throw new OpenLRuntimeException(e);
                }
            }
            throw new OpenLRuntimeException();
        }
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object returnValue = null;
        Object[] keyValues = null;
        Object[] returnValues = null;
        boolean[] f = null;
        boolean isCollectReturn = false;
        IOpenClass type = null;

        for (IBaseAction action : actions) {
            if (action.isCollectReturnAction()) {
                if (returnValues == null) {
                    type = action.getType();
                    if (type.isArray()) {
                        returnValues = (Object[]) Array.newInstance(type.getComponentClass().getInstanceClass(),
                            rules.length);
                    } else {
                        returnValues = new Object[rules.length];
                    }
                    if (f == null) {
                        f = new boolean[rules.length];
                        Arrays.fill(f, false);
                    }
                }
                for (int i = 0; i < rules.length; i++) {
                    Object actionResult = action.executeAction(rules[i], target, params, env);
                    if (returnValues[i] == null && actionResult != null) {
                        returnValues[i] = actionResult;
                        f[i] = true;
                    }
                }
                returnValue = returnValues;
                isCollectReturn = true;
            } else {
                if (action.isCollectReturnKeyAction()) {
                    if (keyValues == null) {
                        keyValues = new Object[rules.length];
                        if (f == null) {
                            f = new boolean[rules.length];
                            Arrays.fill(f, false);
                        }
                    }
                    for (int i = 0; i < rules.length; i++) {
                        Object actionResult = action.executeAction(rules[i], target, params, env);
                        if (keyValues[i] == null && actionResult != null) {
                            keyValues[i] = actionResult;
                            f[i] = true;
                        }
                    }
                } else {
                    int i = 0;
                    Object actionResult = null;
                    for (i = 0; i < rules.length; i++) {
                        if (action.isReturnAction()) {
                            actionResult = action.executeAction(rules[i], target, params, env);
                            if (actionResult != null) {
                                break;
                            }
                        } else {
                            action.executeAction(rules[i], target, params, env);
                        }
                    }
                    if (returnValue == null && (actionResult != null || (i < rules.length))) {
                        returnValue = actionResult;
                        isCollectReturn = false;
                    }
                }
            }
        }
        if (isCollectReturn) {
            return processReturnValue((Object[]) returnValue, keyValues, f, type);
        }
        return returnValue;
    }

    public int[] getRules() {
        return rules;
    }
}
