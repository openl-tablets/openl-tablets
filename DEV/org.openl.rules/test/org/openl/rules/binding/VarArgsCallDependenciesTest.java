package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.ExecutableMethod;

class VarArgsCallDependenciesTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/VarArgsCallDependenciesTest.xlsx";

    VarArgsCallDependenciesTest() {
        super(SRC);
    }

    @ParameterizedTest(name = "{0} depends on {1}")
    @CsvSource({
            "callWithSeveralArguments, total",
            "callWithoutArguments, total",
            "callWithArray, total",
            "callForEachArray, total",
            "callForEachFactor, scaled",
            "callOverloaded, rated"
    })
    void calledArrayParameterTableIsDependency(String caller, String callee) {
        assertFalse(getCompiledOpenClass().hasErrors(), () -> getCompiledOpenClass().getAllMessages().toString());

        var dependencies = tables(caller).findFirst().orElseThrow().getDependencies().getRulesMethods();

        assertEquals(sourceUrls(tables(callee)), sourceUrls(dependencies.stream()));
    }

    private Stream<ExecutableMethod> tables(String name) {
        return Arrays.stream(getTableSyntaxNodes())
                .map(TableSyntaxNode::getMember)
                .filter(ExecutableMethod.class::isInstance)
                .map(ExecutableMethod.class::cast)
                .filter(method -> name.equals(method.getName()));
    }

    private static Set<String> sourceUrls(Stream<ExecutableMethod> methods) {
        return methods.map(ExecutableMethod::getSourceUrl).collect(Collectors.toSet());
    }
}
