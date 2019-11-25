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

    private Object addReturnValues(Collection<Object> returnValue, Object returnValues, boolean[] f) {
        int returnValuesLength = Array.getLength(returnValues);
        for (int i = 0; i < returnValuesLength; i++) {
            if (f[i] && Array.get(returnValues, i) != null) {
                returnValue.add(Array.get(returnValues, i));
            }
        }
        return returnValue;
    }

    private Object addReturnValues(Map<Object, Object> returnValue,
            Object returnValues,
            Object keyValues,
            boolean[] f) {
        int returnValuesLength = Array.getLength(returnValues);
        for (int i = 0; i < returnValuesLength; i++) {
            if (f[i] && Array.get(keyValues, i) != null && Array.get(returnValues, i) != null) {
                returnValue.put(Array.get(keyValues, i), Array.get(returnValues, i));
            }
        }
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    private Object processReturnValue(Object returnValues, Object keyValues, boolean[] f, IOpenClass type) {
        if (type.isArray()) {
            int c = 0;
            for (int i = 0; i < f.length; i++) {
                if (f[i]) {
                    c++;
                }
            }
            int returnValuesLength = Array.getLength(returnValues);
            Object ret;
            if (c == 0) {
                int retLength = 0;
                for (int i = 0; i < returnValuesLength; i++) {
                    if (Array.get(returnValues, i) != null) {
                        retLength++;
                        ;
                    }
                }
                ret = Array.newInstance(type.getComponentClass().getInstanceClass(), retLength);
            } else {
                ret = Array.newInstance(type.getComponentClass().getInstanceClass(), c);
            }
            int j = 0;
            for (int i = 0; i < returnValuesLength; i++) {
                if ((f[i] || c == 0) && Array.get(returnValues, i) != null) {
                    Array.set(ret, j, Array.get(returnValues, i));
                    j++;
                }
            }
            return ret;
        } else {
            if (Map.class == type.getInstanceClass()) {
                return addReturnValues(new HashMap<>(), returnValues, keyValues, f);
            }
            if (SortedMap.class == type.getInstanceClass()) {
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
            if (Collection.class == type.getInstanceClass() || List.class == type.getInstanceClass()) {
                return addReturnValues(new ArrayList<>(), returnValues, f);
            }
            if (Set.class == type.getInstanceClass()) {
                return addReturnValues(new HashSet<>(), returnValues, f);
            }
            if (SortedSet.class == type.getInstanceClass()) {
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
        Object retVal = null;
        Object keyValues = null;
        Object returnValues = null;
        boolean[] f = null;
        boolean isCollectReturn = false;
        IOpenClass type = null;

        for (IBaseAction action : actions) {
            if (action.isCollectReturnAction()) {
                if (returnValues == null) {
                    type = action.getType();
                    if (type.isArray()) {
                        returnValues = Array.newInstance(type.getComponentClass().getInstanceClass(), rules.length);
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
                    if (actionResult != null && Array.get(returnValues, i) == null) {
                        Array.set(returnValues, i, actionResult);
                        f[i] = true;
                    }
                }
                retVal = returnValues;
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
                        if (actionResult != null && Array.get(keyValues, i) == null) {
                            Array.set(keyValues, i, actionResult);
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
                    if (retVal == null && (actionResult != null || i < rules.length)) {
                        retVal = actionResult;
                        isCollectReturn = false;
                    }
                }
            }
        }
        if (isCollectReturn) {
            return processReturnValue(retVal, keyValues, f, type);
        }
        return retVal;
    }

    public int[] getRules() {
        return rules;
    }
}
