package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.ConvertationMetadata.NestedType;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;
import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertorUtils {

    public static SpreadsheetResult getRootSpreadSheetResult(CompoundStep compoundStepWithСonvertationMetadata) {
        for (CalculationStep calculationStep : compoundStepWithСonvertationMetadata.getSteps()){
            ConvertationMetadata convertationMetadata = calculationStep.getConvertationMetadata();
            if (convertationMetadata.getSpreadsheetResult() != null){
                return convertationMetadata.getSpreadsheetResult();
            }
        }
        return new SpreadsheetResult();
    }
    
    public static SpreadsheetResult convert(CompoundStep compoundStepWithСonvertationMetadata,
            CompoundStep compoundStep) {

        SpreadsheetResult spreadsheetResult = null;
        Object[][] result = null;
        Map<String, CalculationStep> stepMap = new HashMap<String, CalculationStep>();
        for (CalculationStep calculationStep : compoundStepWithСonvertationMetadata.getSteps()) {
            stepMap.put(calculationStep.getStepName(), calculationStep);
        }

        ObjectToDataOpenCastConvertor convertor = new ObjectToDataOpenCastConvertor();

        for (CalculationStep calculationStep : compoundStep.getSteps()) {
            CalculationStep calculationStepWithСonvertationMetadata = stepMap.get(calculationStep.getStepName());
            if (calculationStepWithСonvertationMetadata == null) {
                continue;
            }
            ConvertationMetadata convertationMetadata = calculationStepWithСonvertationMetadata
                .getConvertationMetadata();
            if (spreadsheetResult == null) { // Copy Spreadsheet
                SpreadsheetResult spr = convertationMetadata.getSpreadsheetResult();
                spreadsheetResult = new SpreadsheetResult(spr.getHeight(), spr.getWidth());
                spreadsheetResult.setColumnNames(spr.getColumnNames());
                spreadsheetResult.setRowNames(spr.getRowNames());
                result = new Object[spr.getHeight()][spr.getWidth()];
            }
            if (calculationStep instanceof CompoundStep && calculationStepWithСonvertationMetadata instanceof CompoundStep) {
                if (NestedType.SINGLE.equals(convertationMetadata.getNestedType())) {
                    SpreadsheetResult spr = convert((CompoundStep) calculationStepWithСonvertationMetadata,
                        (CompoundStep) calculationStep);
                    result[convertationMetadata.getNestedRowIndex()][convertationMetadata.getNestedColumnIndex()] = spr;
                } else {
                    CompoundStep arrayCompoundStep = (CompoundStep) calculationStep;
                    CompoundStep arrayCompoundStepWithConvertationMetadata = (CompoundStep) calculationStepWithСonvertationMetadata;
                    List<SpreadsheetResult> array = new ArrayList<SpreadsheetResult>();
                    if (arrayCompoundStep.getSteps().size() == arrayCompoundStepWithConvertationMetadata.getSteps()
                        .size()) {
                        Iterator<CalculationStep> itr = arrayCompoundStepWithConvertationMetadata.getSteps().iterator();
                        for (CalculationStep cs : arrayCompoundStep.getSteps()) {
                            SpreadsheetResult spr = convert((CompoundStep) itr.next(), (CompoundStep) cs);
                            array.add(spr);
                        }
                        result[convertationMetadata.getNestedRowIndex()][convertationMetadata
                            .getNestedColumnIndex()] = array.toArray(new SpreadsheetResult[] {});
                    }
                }
            }
            for (String property : convertationMetadata.getProperties()) {
                SpreadsheetResultPoint spreadsheetResultPoint = convertationMetadata.getPropert(property);
                Object value = getValue(calculationStep, property);
                Object originalValue = convertationMetadata.getSpreadsheetResult()
                    .getValue(spreadsheetResultPoint.getRowIndex(), spreadsheetResultPoint.getColumnIndex());
                if (originalValue != null) {
                    value = convert(convertor, value, originalValue.getClass());
                }
                result[spreadsheetResultPoint.getRowIndex()][spreadsheetResultPoint.getColumnIndex()] = value;
            }
        }
        spreadsheetResult.setResults(result);
        return spreadsheetResult;
    }

    private static Object convert(ObjectToDataOpenCastConvertor convertor, Object x, Class<?> expectedType) {
        if (x.getClass().isArray() && expectedType.isArray()) {
            int length = Array.getLength(x);
            Object newValue = Array.newInstance(expectedType.getComponentType(), length);
            IOpenCast openCast = convertor.getConvertor(expectedType.getComponentType(),
                x.getClass().getComponentType());
            for (int i = 0; i < length; i++) {
                Object componentValue = Array.get(x, i);
                if (componentValue != null && !ClassUtils.isAssignable(componentValue.getClass(),
                    expectedType.getComponentType())) {
                    try {
                        componentValue = openCast.convert(componentValue);
                    } catch (Exception e) {
                        Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
                        if (log.isDebugEnabled()) {
                            log.debug("Cannot convert value {} to {}",
                                componentValue,
                                expectedType.getComponentType().getName(),
                                e);
                        }
                        return x;
                    }
                }
                Array.set(newValue, i, componentValue);
            }
            return newValue;
        } else {
            if (!ClassUtils.isAssignable(x.getClass(), expectedType)) {
                try {
                    IOpenCast openCast = convertor.getConvertor(expectedType, x.getClass());
                    return openCast.convert(x);
                } catch (Exception e) {
                    Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
                    if (log.isDebugEnabled()) {
                        log.debug("Cannot convert value {} to {}", x, expectedType.getName(), e);
                    }
                }
            }
        }
        return x;
    }

    private static Object getValue(CalculationStep calculationStep, String propertyName) {
        Method getterMethod = getGetterMethod(calculationStep, propertyName);
        try {
            Object value = getterMethod.invoke(calculationStep);
            return value;
        } catch (IllegalAccessException e) {
            Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
            log.warn(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
            log.warn(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    private static Method getGetterMethod(CalculationStep step, String propertyName) {
        Method getterMethod = null;
        String getterrName = ClassUtils.getter(propertyName);
        try {
            getterMethod = step.getClass().getMethod(getterrName);
        } catch (Exception e) {
            try {
                // try to get setter by upper case the first symbol in the
                // column name, and lower
                // case the rest
                //
                getterrName = String.format("get%s%s",
                    propertyName.substring(0, 1).toUpperCase(),
                    propertyName.substring(1).toLowerCase());
                getterMethod = step.getClass().getMethod(getterrName);
            } catch (Exception e1) {
                Logger log = LoggerFactory.getLogger(ConvertorUtils.class);
                log.warn(e1.getMessage(), e1);
            }
        }
        return getterMethod;
    }
}
