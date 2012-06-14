package org.openl.rules.ruleservice.context;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.variation.Variation;
import org.openl.rules.project.instantiation.variation.VariationsPack;
import org.openl.rules.project.instantiation.variation.VariationsResult;

/**
 *FIXME
 * Defines IRulesRuntime context deserialization from XML: new
 * {@link DefaultRulesRuntimeContext} will be used(By default Aegis creates
 * Proxy that does not provide some necessary methods, e.g. <code>clone()</code>
 * ).
 * 
 * @author PUdalau
 */
public class VariationsResultContextBeanType extends BeanType {
    public static class MyMap extends LinkedHashMap<String, Object>{}

    public static final Class TYPE_CLASS = Map.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public VariationsResultContextBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }
    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) throws DatabindingException {
        MyMap map = new MyMap();
        VariationsResult<Class<?>> result = new VariationsResult<Class<?>>();
        for(String varID : result.getCalculatedVariationIDs()){
            map.put(varID, result.getResultForVariation(varID));
        }
        for(String varID : result.getFailedVariationIDs()){
            map.put(varID, result.getFailureErrorForVariation(varID));
        }
        super.writeObject(map, writer, context);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        Map<String, Object> map = (Map<String, Object>) super.readObject(reader, context);
        
        try {
            VariationsResult variationsResult = new VariationsResult();
            for(Entry<String, Object> entry : map.entrySet()){
                if(entry.getValue() instanceof Exception){
                    variationsResult.registerFailure(entry.getKey(), (Exception) entry.getValue());
                }else{
                    variationsResult.registerResults(entry.getKey(), entry.getValue());
                }
            }
            return variationsResult;
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
 }
