package org.openl.rules.datatype.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.openl.rules.datatype.gen.bean.writers.*;
import org.openl.util.ClassUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates byte code for simple java bean.
 * 
 * @author DLiauchuk
 *
 */
public class SimpleBeanByteCodeGenerator {

    private final Logger log = LoggerFactory.getLogger(SimpleBeanByteCodeGenerator.class);
    private Class<?> parentClass;
    private LinkedHashMap<String, FieldDescription> beanFields;
    private LinkedHashMap<String, FieldDescription> parentFields;
    private LinkedHashMap<String, FieldDescription> allFields;
    /**
     * list of writers to generate necessary byte code class representation.
     */
    private List<BeanByteCodeWriter> writers = new ArrayList<BeanByteCodeWriter>();
    private byte[] generatedByteCode;
    private String beanName;
    private String beanNameWithPackage;

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g.
     *            <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     */
    public SimpleBeanByteCodeGenerator(String beanName, Map<String, FieldDescription> beanFields) {
        this(beanName, beanFields, null, new LinkedHashMap<String, FieldDescription>());
    }

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g.
     *            <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     */
    public SimpleBeanByteCodeGenerator(String beanName,
            Map<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields) {
        this.beanName = beanName;
        this.beanNameWithPackage = ByteCodeGeneratorHelper.replaceDots(beanName);
        this.beanFields = new LinkedHashMap<String, FieldDescription>(beanFields);
        this.parentFields = new LinkedHashMap<String, FieldDescription>(parentFields);
        this.parentClass = parentClass;

        allFields = new LinkedHashMap<String, FieldDescription>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields);

        initWriters();
    }

    private void initWriters() {
        addWriter(new ClassDescriptionWriter(getBeanNameWithPackage(), parentClass));
        addWriter(new JAXBAnnotationWriter(getBeanNameWithPackage()));
        addWriter(new ProtectedFieldsWriter(beanFields));
        addWriter(new DefaultConstructorWriter(getBeanNameWithPackage(), parentClass, beanFields));
        if (allFields.size() < 256) {
            // Generate constructor with parameters only in case where there are
            // less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            addWriter(new ConstructorWithParametersWriter(getBeanNameWithPackage(),
                parentClass,
                beanFields,
                parentFields,
                allFields));
        }
        addWriter(new GettersWriter(getBeanNameWithPackage(), beanFields));
        addWriter(new SettersWriter(getBeanNameWithPackage(), beanFields));
        addWriter(new ToStringWriter(getBeanNameWithPackage(), allFields));
        addWriter(new EqualsWriter(getBeanNameWithPackage(), allFields));
        addWriter(new HashCodeWriter(getBeanNameWithPackage(), allFields));
    }

    public byte[] generateClassByteCode() {
        if (generatedByteCode == null) {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            for (BeanByteCodeWriter writer : writers) {
                writer.write(classWriter);
            }

            generatedByteCode = classWriter.toByteArray();

//            writeBytesToFile(generatedByteCode);
        }
        return generatedByteCode;
    }

    /**
     * Return loaded to classpath class object
     *
     * @return <code>Class<?></code> descriptor for given byteCode
     */
    public Class<?> generateAndLoadBeanClass() {
        Class<?> resultClass = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            /** try to load bean class from current classloader.
             check if it already presents*/
            if (ByteCodeGeneratorHelper.isClassLoaderContainsClass(classLoader, beanName)) {
                resultClass = classLoader.loadClass(beanName);
                log.debug("Bean {} is using previously loaded", beanName);
                return resultClass;
            } else {
                /** generate byte code*/
                generateClassByteCode();

                /** add the generated byte code to the classloader*/
                resultClass = ClassUtils.defineClass(beanName, generatedByteCode, classLoader);
                log.debug("bean {} is using generated at runtime", beanName);
                return resultClass;
            }
        } catch (Exception ex) {
            log.error("{}", this, ex);
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    protected boolean addWriter(BeanByteCodeWriter writer) {
        if (writer != null) {
            writers.add(writer);
            return true;
        }
        return false;
    }

    protected String getBeanNameWithPackage() {
        return beanNameWithPackage;
    }

    public String toString() {
        if (StringUtils.isNotBlank(beanName)) {
            return String.format("Bean with name: %s", beanName);
        }
        return super.toString();
    }
}
