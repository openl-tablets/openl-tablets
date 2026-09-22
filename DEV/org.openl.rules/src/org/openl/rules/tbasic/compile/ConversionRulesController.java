package org.openl.rules.tbasic.compile;

import java.util.LinkedHashSet;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.tbasic.AlgorithmTreeNode;

public final class ConversionRulesController {

    private static final String PERFORM = "Perform";
    private static final String EXECUTION = "execution";
    private static final String CALCULATE = "Calculate";
    private static final String IF_CONDITION = "IF.condition";
    private static final String CONDITION_CALCULATION = "condition calculation";
    private static final String CONDITIONAL_GOTO = "ConditionalGoto";
    private static final String LABEL_END = "gen_label_end";
    private static final String FALSE_VALUE = "FALSE";
    private static final String COMPILE = "!Compile";
    private static final String IF_ELSE = "IFELSE";
    private static final String LABEL_ELSE = "gen_label_else";
    private static final String WHILE = "WHILE";
    private static final String LABEL_BEGIN_LOOP = "gen_label_begin_loop";
    private static final String LABEL_END_LOOP = "gen_label_end_loop";
    private static final String FOR_EACH = "FOR EACH";
    private static final String FOR_EACH_CONDITION = "FOR EACH.condition";
    private static final String FUNCTION = "FUNCTION";

    private ConversionRulesController() {
        // Utility class
    }

    private static final ConversionRuleBean[] convertionRules = new ConversionRuleBean[]{
            ConversionRuleBean.builder()
                    .operation("SET")
                    .step(s -> s
                            .operationType(PERFORM)
                            .operationParam1("SET.action")
                            .nameForDebug(EXECUTION))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("VAR")
                    .step(s -> s
                            .operationType("!Declare")
                            .operationParam1("VAR.condition")
                            .operationParam2("VAR.action"))
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1("VAR.action")
                            .nameForDebug("initial value"))
                    .step(s -> s
                            .operationType("AssignValue")
                            .operationParam1("VAR.condition"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IF")
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1(IF_CONDITION)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_END)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(PERFORM)
                            .operationParam1("IF.action")
                            .nameForDebug(EXECUTION))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("IF")
                    .multiLine(true)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1(IF_CONDITION)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_END)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("IF.children"))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(IF_ELSE)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1(IF_CONDITION)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_ELSE)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(PERFORM)
                            .operationParam1("IF.action")
                            .nameForDebug(EXECUTION))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_END))
                    .step(s -> s
                            .operationType(PERFORM)
                            .operationParam1("ELSE.action")
                            .labelInstruction(LABEL_ELSE)
                            .nameForDebug(EXECUTION))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(IF_ELSE)
                    .multiLine(true)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1(IF_CONDITION)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_ELSE)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("IF.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_END))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("ELSE.children")
                            .labelInstruction(LABEL_ELSE))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(WHILE)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1("WHILE.condition")
                            .labelInstruction(LABEL_BEGIN_LOOP)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_END_LOOP)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(PERFORM)
                            .operationParam1("WHILE.action")
                            .nameForDebug(EXECUTION))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_BEGIN_LOOP))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END_LOOP))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(WHILE)
                    .multiLine(true)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1("WHILE.condition")
                            .labelInstruction(LABEL_BEGIN_LOOP)
                            .nameForDebug(CONDITION_CALCULATION))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_END_LOOP)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("WHILE.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_BEGIN_LOOP))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END_LOOP))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(FOR_EACH)
                    .multiLine(true)
                    .step(s -> s
                            .operationType(CALCULATE)
                            .operationParam1("FOR EACH.action"))
                    .step(s -> s
                            .operationType("DeclareIterator")
                            .operationParam1("declare iterator")
                            .operationParam2(FOR_EACH_CONDITION))
                    .step(s -> s
                            .operationType("!DeclareArrayElement")
                            .operationParam1(FOR_EACH_CONDITION)
                            .operationParam2("FOR EACH.action"))
                    .step(s -> s
                            .operationType("IteratorHasNext")
                            .operationParam1(FOR_EACH_CONDITION)
                            .labelInstruction(LABEL_BEGIN_LOOP))
                    .step(s -> s
                            .operationType(CONDITIONAL_GOTO)
                            .operationParam1(LABEL_END_LOOP)
                            .operationParam2(FALSE_VALUE))
                    .step(s -> s
                            .operationType("IteratorNext")
                            .operationParam1(FOR_EACH_CONDITION))
                    .step(s -> s
                            .operationType("AssignVariable")
                            .operationParam1("gen_localVariable")
                            .operationParam2(FOR_EACH_CONDITION)
                            .nameForDebug("next"))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("FOR EACH.children"))
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_BEGIN_LOOP))
                    .step(s -> s
                            .operationType("Nop")
                            .labelInstruction(LABEL_END_LOOP))
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
                            .operationParam1(LABEL_END_LOOP)
                            .nameForDebug(""))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("CONTINUE")
                    .step(s -> s
                            .operationType("Goto")
                            .operationParam1(LABEL_BEGIN_LOOP)
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
                            .operationType(PERFORM)
                            .operationParam1("SUB.action")
                            .nameForDebug(EXECUTION))
                    .build(),
            ConversionRuleBean.builder()
                    .operation("SUB")
                    .multiLine(true)
                    .step(s -> s
                            .operationType("!Subroutine"))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("SUB.children"))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(FUNCTION)
                    .step(s -> s
                            .operationType("!Function")
                            .operationParam1("FUNCTION.action"))
                    .step(s -> s
                            .operationType("Return")
                            .operationParam1("FUNCTION.action")
                            .nameForDebug(EXECUTION))
                    .build(),
            ConversionRuleBean.builder()
                    .operation(FUNCTION)
                    .multiLine(true)
                    .step(s -> s
                            .operationType("!Function")
                            .operationParam1("FUNCTION.children:first:RETURN.condition"))
                    .step(s -> s
                            .operationType(COMPILE)
                            .operationParam1("FUNCTION.children"))
                    .build()
    };

    public static ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile,
                                                       IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();

        var groupedOperationNames = LinkedHashSet.<String>newLinkedHashSet(nodesToCompile.size());

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
        var errorMessage = "The operations sequence is wrong: %2$s. Operations %1$s must precede the %2$s".formatted(
                specification.getPredecessorOperations(),
                groupedOperationNames);
        var errorSource = theFirstNode.getAlgorithmRow()
                .getOperation()
                .asSourceCodeModule();
        BindHelper.processError(errorMessage, errorSource, bindingContext);
        return null;
    }

    private static String whatIsOperationsGroupName(LinkedHashSet<String> groupedOperationNames) {
        return switch (groupedOperationNames) {
            case LinkedHashSet<String> s when s.containsAll(List.of("IF", "ELSE")) -> IF_ELSE;
            case LinkedHashSet<String> s when s.containsAll(List.of("IF", "END IF")) -> "IF";
            case LinkedHashSet<String> s when s.containsAll(List.of(WHILE, "END WHILE")) -> WHILE;
            case LinkedHashSet<String> s when s.containsAll(List.of(FOR_EACH, "END FOR EACH")) -> FOR_EACH;
            case LinkedHashSet<String> s when s.containsAll(List.of("SUB", "END SUB")) -> "SUB";
            case LinkedHashSet<String> s when s.containsAll(List.of(FUNCTION, "END FUNCTION")) -> FUNCTION;
            default -> groupedOperationNames.getFirst();
        };
    }

}
