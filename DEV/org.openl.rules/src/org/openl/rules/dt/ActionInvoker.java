package org.openl.rules.dt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openl.domain.IIntIterator;
import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.Invokable;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Created by ymolchan on 05.02.2016.
 */
public class ActionInvoker implements Invokable {
    private final List<Integer> firedRules = new ArrayList<>();
    private final IIntIterator rulesIntIterator;
    private final IBaseAction[] actions;
    private final boolean returnEmptyResult;

    ActionInvoker(IIntIterator rulesIntIterator, IBaseAction[] actions, boolean returnEmptyResult) {
        this.rulesIntIterator = rulesIntIterator;
        this.actions = actions;
        this.returnEmptyResult = returnEmptyResult;
    }

    private static Object addReturnValues(Collection<Object> returnValue, Object returnValues, boolean[] f) {
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
            if (f[i] && isValidResult(Array.get(keyValues, i)) && isValidResult(Array.get(returnValues, i))) {
                returnValue.put(Array.get(keyValues, i), Array.get(returnValues, i));
            }
        }
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    private Object processReturnValue(Object returnValues, Object keyValues, boolean[] f, IOpenClass type) {
        if (type.isArray()) {
            int c = 0;
            for (boolean b : f) {
                if (b) {
                    c++;
                }
            }
            int returnValuesLength = Array.getLength(returnValues);
            Object ret;
            if (c == 0) {
                int retLength = 0;
                for (int i = 0; i < returnValuesLength; i++) {
                    if (isValidResult(Array.get(returnValues, i))) {
                        retLength++;
                    }
                }
                ret = Array.newInstance(type.getComponentClass().getInstanceClass(), retLength);
            } else {
                ret = Array.newInstance(type.getComponentClass().getInstanceClass(), c);
            }
            int j = 0;
            for (int i = 0; i < returnValuesLength; i++) {
                if ((f[i] || c == 0) && (isValidResult(Array.get(returnValues, i)))) {
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
            if (ClassUtils.isAssignable(type.getInstanceClass(), Map.class)) {
                try {
                    return addReturnValues((Map<Object, Object>) type.getInstanceClass().getDeclaredConstructor().newInstance(),
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
            if (ClassUtils.isAssignable(type.getInstanceClass(), Collection.class)) {
                try {
                    return addReturnValues((Collection<Object>) type.getInstanceClass().getDeclaredConstructor().newInstance(), returnValues, f);
                } catch (Exception e) {
                    throw new OpenLRuntimeException(e);
                }
            }
            throw new OpenLRuntimeException();
        }
    }

    private boolean isValidResult(Object actionResult) {
        return actionResult != null || returnEmptyResult;
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
                int[] rules = getRules();
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
                executeActionAndWriteValues(target, params, env, returnValues, f, action, rules);
                retVal = returnValues;
                isCollectReturn = true;
            } else if (action.isCollectReturnKeyAction()) {
                int[] rules = getRules();
                if (keyValues == null) {
                    keyValues = new Object[rules.length];
                    if (f == null) {
                        f = new boolean[rules.length];
                        Arrays.fill(f, false);
                    }
                }
                executeActionAndWriteValues(target, params, env, keyValues, f, action, rules);
            } else {
                Object actionResult = null;
                SmartIterator itr = new SmartIterator(firedRules.iterator(), rulesIntIterator);
                List<Integer> newFiredRules = new ArrayList<>();
                while (itr.hasNext()) {
                    boolean g = itr.itr1.hasNext();
                    int rule = itr.next();
                    if (!g) {
                        newFiredRules.add(rule);
                    }
                    if (action.isReturnAction()) {
                        actionResult = action.executeAction(rule, target, params, env);
                        if (isValidResult(actionResult)) {
                            break;
                        }
                    } else {
                        action.executeAction(rule, target, params, env);
                    }
                }
                firedRules.addAll(newFiredRules);
                if (retVal == null && actionResult != null) {
                    retVal = actionResult;
                }
            }
        }
        if (isCollectReturn) {
            return processReturnValue(retVal, keyValues, f, type);
        }
        return retVal;
    }

    private void executeActionAndWriteValues(Object target,
                                             Object[] params,
                                             IRuntimeEnv env,
                                             Object values,
                                             boolean[] f,
                                             IBaseAction action,
                                             int[] rules) {
        for (int i = 0; i < rules.length; i++) {
            Object actionResult = action.executeAction(rules[i], target, params, env);
            if (isValidResult(actionResult) && (Array.get(values, i) == null || !f[i])) {
                Array.set(values, i, actionResult);
                f[i] = true;
            }
        }
    }

    public int[] getRules() {
        while (rulesIntIterator.hasNext()) {
            firedRules.add(rulesIntIterator.nextInt());
        }
        return firedRules.stream().mapToInt(e -> e).toArray();
    }

    private static class SmartIterator {
        Iterator<Integer> itr1;
        IIntIterator itr2;

        public SmartIterator(Iterator<Integer> itr1, IIntIterator itr2) {
            this.itr1 = itr1;
            this.itr2 = itr2;
        }

        public boolean hasNext() {
            return itr1.hasNext() || itr2.hasNext();
        }

        public Integer next() {
            return itr1.hasNext() ? itr1.next() : itr2.next();
        }
    }
}
