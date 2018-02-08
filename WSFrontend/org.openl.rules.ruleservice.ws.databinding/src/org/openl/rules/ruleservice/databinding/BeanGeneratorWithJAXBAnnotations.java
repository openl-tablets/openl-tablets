package org.openl.rules.ruleservice.databinding;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 - 2015 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;

import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.CodeEmitter;
import net.sf.cglib.core.Constants;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.core.EmitUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.core.TypeUtils;

@SuppressWarnings("rawtypes")
public class BeanGeneratorWithJAXBAnnotations extends net.sf.cglib.beans.BeanGenerator {

    private static final String $CGLIB_PROP = "$cglib_prop_";
    
	private String xmlTypeName;
    private String xmlTypeNamespace;
    
    public BeanGeneratorWithJAXBAnnotations() {
    	setStrategy(new GeneratorStrategyWithJAXBAnnotations());
	}

    public void setXmlTypeName(String xmlTypeName) {
        this.xmlTypeName = xmlTypeName;
    }

    public void setXmlTypeNamespace(String xmlTypeNamespace) {
        this.xmlTypeNamespace = xmlTypeNamespace;
    }

    private Map props = new HashMap();
    private Class superclass;

    @SuppressWarnings("unchecked")
    @Override
    public void addProperty(String name, Class type) {
        if (props.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate property name \"" + name + "\"");
        }
        props.put(name, Type.getType(type));
        super.addProperty(name, type);
    }

    public void setSuperclass(Class superclass) {
        if (superclass != null && superclass.equals(Object.class)) {
            superclass = null;
        }
        this.superclass = superclass;
    }
    
    public static void add_properties(ClassEmitter ce, String[] names, Type[] types) {
        for (int i = 0; i < names.length; i++) {
            String fieldName = $CGLIB_PROP + names[i];
            ce.declare_field(Constants.ACC_PRIVATE, fieldName, types[i], null);
            add_property(ce, names[i], types[i], fieldName);
        }
    }

    public static void add_property(ClassEmitter ce, String name, Type type, String fieldName) {
        String property = TypeUtils.upperFirst(name);
        CodeEmitter e;
        
        e = ce.begin_method(Constants.ACC_PUBLIC,
                            new Signature("get" + property,
                                          type,
                                          Constants.TYPES_EMPTY),
                            null);
        e.load_this();
        e.getfield(fieldName);
        e.return_value();
        e.end_method();
        
        e = ce.begin_method(Constants.ACC_PUBLIC,
                            new Signature("set" + property,
                                          Type.VOID_TYPE,
                                          new Type[]{ type }),
                            null);
        e.load_this();
        e.load_arg(0);
        e.putfield(fieldName);
        e.return_value();
        e.end_method();
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {
        int size = props.size();
        @SuppressWarnings("unchecked")
        String[] names = (String[]) props.keySet().toArray(new String[size]);
        Type[] types = new Type[size];
        for (int i = 0; i < size; i++) {
            types[i] = (Type) props.get(names[i]);
        }
        ClassEmitter ce = new ClassEmitter(v);
        ce.begin_class(Constants.V1_5,
            Constants.ACC_PUBLIC,
            getClassName(),
            superclass != null ? Type.getType(superclass) : Constants.TYPE_OBJECT,
            null,
            null);

        AnnotationVisitor av1 = ce.visitAnnotation(Type.getDescriptor(XmlRootElement.class), true);
        if (xmlTypeNamespace != null) {
            av1.visit("namespace", xmlTypeNamespace);
        }
        if (xmlTypeName != null) {
            av1.visit("name", xmlTypeName);
        }
        av1.visitEnd();
        AnnotationVisitor av2 = ce.visitAnnotation(Type.getDescriptor(XmlType.class), true);
        if (xmlTypeName != null) {
            av2.visit("name", xmlTypeName);
        }
        if (xmlTypeNamespace != null) {
            av2.visit("namespace", xmlTypeNamespace);
        }
        av2.visitEnd();
        EmitUtils.null_constructor(ce);
        add_properties(ce, names, types);
        ce.end_class();
    }
    
    private static class GeneratorStrategyWithJAXBAnnotations extends DefaultGeneratorStrategy {
		@Override
		protected DebuggingClassWriter getClassVisitor() throws Exception {
			return new DebuggingClassWriterWithJAXBAnnotations();
		}
	}

	private static class DebuggingClassWriterWithJAXBAnnotations extends DebuggingClassWriter {
		public DebuggingClassWriterWithJAXBAnnotations() {
			super(ClassWriter.COMPUTE_MAXS);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			AnnotationVisitor av1 = fv.visitAnnotation(Type.getDescriptor(XmlElement.class), true);
			if (name.startsWith($CGLIB_PROP)) {
				av1.visit("name", name.substring($CGLIB_PROP.length()));	
			}else {
				av1.visit("name", name);
			}
			av1.visitEnd();
			return fv;
		}
	}
}
