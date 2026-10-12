package org.openl.rules.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

class ValidationHandlerTest {

    public static class Bean {
        private final String value;

        public Bean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ChildBean extends Bean {
        public ChildBean(String value) {
            super(value);
        }

        public String getExtra() {
            return "bad";
        }
    }

    @Test
    void checksVocabularyFieldsOfDatatypeSubclasses() {
        var signature = beanSignature("value");
        var bean = new ChildBean("bad");

        assertThrows(OutsideOfValidDomainException.class, () -> validate(signature, null, bean));
    }

    @Test
    void checksVocabularyFieldsOfTheSameDatatypeFromAnotherClassLoader() throws Exception {
        var signature = beanSignature("value");
        var bean = beanFromAnotherClassLoader();

        assertThrows(OutsideOfValidDomainException.class, () -> validate(signature, null, bean));
    }

    @Test
    void readsCapitalizedDatatypeFieldsFromAnotherClassLoader() throws Exception {
        var signature = beanSignature("Value");
        var bean = beanFromAnotherClassLoader();

        assertThrows(OutsideOfValidDomainException.class, () -> validate(signature, null, bean));
    }

    @Test
    void checksFieldsDeclaredOnlyByTheRuntimeSubclass() {
        var parentType = parentType();
        var module = mock(XlsModuleOpenClass.class);
        when(parentType.getModule()).thenReturn(module);
        var childType = childType();
        when(module.findType(ChildBean.class.getName())).thenReturn(childType);
        var signature = signatureOf(parentType);
        var bean = new ChildBean("good");

        assertThrows(OutsideOfValidDomainException.class, () -> validate(signature, null, bean));
    }

    @Test
    void findsSubclassesDeclaredInADependentModule() {
        var parentType = parentType();
        var parentModule = mock(XlsModuleOpenClass.class);
        when(parentType.getModule()).thenReturn(parentModule);
        var dependentModule = mock(XlsModuleOpenClass.class);
        var childType = childType();
        when(dependentModule.findType(ChildBean.class.getName())).thenReturn(childType);
        var signature = signatureOf(parentType);
        var bean = new ChildBean("good");

        assertThrows(OutsideOfValidDomainException.class, () -> validate(signature, dependentModule, bean));
    }

    @Test
    void skipsFieldsMissingFromAMatchingBean() throws Exception {
        var signature = beanSignature("missing");
        var bean = beanFromAnotherClassLoader();

        assertDoesNotThrow(() -> validate(signature, null, bean));
    }

    @Test
    void ignoresArgumentsTheSignatureDoesNotDeclare() {
        var signature = signatureOf(vocabulary());

        assertDoesNotThrow(() -> validate(signature, null, null, "bad"));
    }

    private static DatatypeOpenClass parentType() {
        var parentType = mock(DatatypeOpenClass.class);
        doReturn(Bean.class).when(parentType).getInstanceClass();
        return parentType;
    }

    private static DatatypeOpenClass childType() {
        var childType = mock(DatatypeOpenClass.class);
        doReturn(ChildBean.class).when(childType).getInstanceClass();
        var extra = mock(IOpenField.class);
        var allowed = vocabulary();
        when(extra.getType()).thenReturn(allowed);
        when(extra.get(any(), any())).thenReturn("bad");
        when(childType.getFields()).thenReturn(List.of(extra));
        return childType;
    }

    private static IMethodSignature signatureOf(IOpenClass type) {
        var signature = mock(IMethodSignature.class);
        when(signature.getNumberOfParameters()).thenReturn(1);
        when(signature.getParameterType(0)).thenReturn(type);
        return signature;
    }

    private static Object beanFromAnotherClassLoader() throws Exception {
        var name = Bean.class.getName();
        var loader = new ClassLoader(Bean.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(className)) {
                    return super.loadClass(className, resolve);
                }
                var loaded = findLoadedClass(className);
                if (loaded != null) {
                    return loaded;
                }
                try (var stream = Bean.class.getResourceAsStream("ValidationHandlerTest$Bean.class")) {
                    if (stream == null) {
                        throw new ClassNotFoundException(className);
                    }
                    var bytes = stream.readAllBytes();
                    return defineClass(className, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(className, e);
                }
            }
        };
        return loader.loadClass(name).getConstructor(String.class).newInstance("bad");
    }

    private static IMethodSignature beanSignature(String fieldName) {
        var field = mock(IOpenField.class);
        when(field.getName()).thenReturn(fieldName);
        var allowed = vocabulary();
        when(field.getType()).thenReturn(allowed);
        when(field.get(any(), any())).thenAnswer(invocation -> ((Bean) invocation.getArgument(0)).getValue());
        var datatype = mock(DatatypeOpenClass.class);
        doReturn(Bean.class).when(datatype).getInstanceClass();
        when(datatype.getFields()).thenReturn(List.of(field));
        return signatureOf(datatype);
    }

    private static void validate(IMethodSignature signature, IOpenClass module, Object... args) {
        ValidationHandler.validateArguments(signature, module, new SimpleRulesVM().getRuntimeEnv(), args);
    }

    private static DomainOpenClass vocabulary() {
        @SuppressWarnings("unchecked")
        var domain = mock(IDomain.class);
        when(domain.selectObject("bad")).thenReturn(false);
        when(domain.iterator()).thenReturn(List.of("good").iterator());
        return new DomainOpenClass("AllowedValue", JavaOpenClass.STRING, domain, null, null);
    }
}
