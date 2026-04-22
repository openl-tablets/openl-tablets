package org.openl.rules.tbasic.compile;

import java.util.LinkedHashSet;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;

public final class ConversionRulesController {

    private static final ConversionRuleBean[] convertionRules = new ConversionRuleBean[]{
            ConversionRuleBean.builder()
                    .operation("SET")
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("SET.action")
                            .nameForDebug("execution"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("VAR")
                    .step(s -> s
                            .operationType("!Declare")
                            .operationParam1("VAR.condition")
                            .operationParam2("VAR.action"))
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("VAR.action")
                            .nameForDebug("initial value"))
                    .step(s -> s
                            .operationType("AssignValue")
                            .operationParam1("VAR.condition"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IF")
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("IF.condition")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_end")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("IF.action")
                            .nameForDebug("execution"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IF")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("IF.condition")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_end")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("IF.children"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IFELSE")
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("IF.condition")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_else")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("IF.action")
                            .nameForDebug("execution"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_end"))
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("ELSE.action")
                            .labelInstruction("gen_label_else")
                            .nameForDebug("execution"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IFELSE")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("IF.condition")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_else")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("IF.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_end"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("ELSE.children")
                            .labelInstruction("gen_label_else"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("WHILE")
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("WHILE.condition")
                            .labelInstruction("gen_label_begin_loop")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_end_loop")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("WHILE.action")
                            .nameForDebug("execution"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_begin_loop"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end_loop"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("WHILE")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("WHILE.condition")
                            .labelInstruction("gen_label_begin_loop")
                            .nameForDebug("condition calculation"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_end_loop")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("WHILE.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_begin_loop"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end_loop"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("FOR EACH")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("Calculate")
                            .operationParam1("FOR EACH.action"))
                    .step(s -> s
                            .operationType("DeclareIterator")
                            .operationParam1("declare iterator")
                            .operationParam2("FOR EACH.condition"))
                    .step(s -> s
                            .operationType("!DeclareArrayElement")
                            .operationParam1("FOR EACH.condition")
                            .operationParam2("FOR EACH.action"))
                    .step(s -> s
                            .operationType("IteratorHasNext")
                            .operationParam1("FOR EACH.condition")
                            .labelInstruction("gen_label_begin_loop"))
                    .step(s -> s
                            .operationType("ConditionalGoto")
                            .operationParam1("gen_label_end_loop")
                            .operationParam2("FALSE"))
                    .step(s -> s
                            .operationType("IteratorNext")
                            .operationParam1("FOR EACH.condition"))
                    .step(s -> s
                            .operationType("AssignVariable")
                            .operationParam1("gen_localVariable")
                            .operationParam2("FOR EACH.condition")
                            .nameForDebug("next"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("FOR EACH.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_begin_loop"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction("gen_label_end_loop"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("GOTO")
                    .step(s -> s
                            .operationType("!CheckLabel")
                            .operationParam1("GOTO.condition"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("GOTO.condition")
                            .nameForDebug("label"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("BREAK")
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_end_loop")
                            .nameForDebug(""))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("CONTINUE")
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1("gen_label_begin_loop")
                            .nameForDebug(""))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("RETURN")
                    .step(s -> s
                            .operationType("Return")
                            .operationParam1("RETURN.condition")
                            .nameForDebug("result"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("SUB")
                    .step(s -> s
                            .operationType("!Subroutine"))
                    .step(s -> s
                            .operationType("Perform")
                            .operationParam1("SUB.action")
                            .nameForDebug("execution"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("SUB")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("!Subroutine"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("SUB.children"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("FUNCTION")
                    .step(s -> s
                            .operationType("!Function")
                            .operationParam1("FUNCTION.action"))
                    .step(s -> s
                            .operationType("Return")
                            .operationParam1("FUNCTION.action")
                            .nameForDebug("execution"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("FUNCTION")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("!Function")
                            .operationParam1("FUNCTION.children:first:RETURN.condition"))
                    .step(s -> s
                            .operationType("!Compile")
                            .operationParam1("FUNCTION.children"))
                    .build()
    };

    public static ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile,
                                                       IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();

        var groupedOperationNames = new LinkedHashSet<String>(nodesToCompile.size());

        for (AlgorithmTreeNode node : nodesToCompile) {
            groupedOperationNames.add(node.getSpecificationKeyword());
        }

        // Resolve the name of the group defined in the Algorithm Specification
        //
        var operationGroupName = whatIsOperationsGroupName(groupedOperationNames);

        // we assume that all the operations are either all multiline or not
        var theFirstNode = nodesToCompile.getFirst();
        var specification = theFirstNode.getSpecification();
        var isMultilineOperation = specification.isMultiline();

        for (ConversionRuleBean conversionRule : convertionRules) {
            if (conversionRule.getOperation()
                    .equals(operationGroupName) && conversionRule.isMultiLine() == isMultilineOperation) {
                return conversionRule;
            }
        }

        // No conversion rule found.
        String errorMessage = "The operations sequence is wrong: %2$s. Operations %1$s must precede the %2$s".formatted(
                specification.getPredecessorOperations(),
                groupedOperationNames);
        IOpenSourceCodeModule errorSource = theFirstNode.getAlgorithmRow()
                .getOperation()
                .asSourceCodeModule();
        BindHelper.processError(errorMessage, errorSource, bindingContext);
        return null;
    }

    private static String whatIsOperationsGroupName(LinkedHashSet<String> groupedOperationNames) {
        return switch (groupedOperationNames) {
            case LinkedHashSet<String> s when s.containsAll(List.of("IF", "ELSE")) -> "IFELSE";
            case LinkedHashSet<String> s when s.containsAll(List.of("IF", "END IF")) -> "IF";
            case LinkedHashSet<String> s when s.containsAll(List.of("WHILE", "END WHILE")) -> "WHILE";
            case LinkedHashSet<String> s when s.containsAll(List.of("FOR EACH", "END FOR EACH")) -> "FOR EACH";
            case LinkedHashSet<String> s when s.containsAll(List.of("SUB", "END SUB")) -> "SUB";
            case LinkedHashSet<String> s when s.containsAll(List.of("FUNCTION", "END FUNCTION")) -> "FUNCTION";
            default -> groupedOperationNames.getFirst();
        };
    }

}
