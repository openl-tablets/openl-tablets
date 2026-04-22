package org.openl.rules.tbasic.compile;

import java.util.LinkedHashSet;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;

public final class ConversionRulesController {

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
        IOpenSourceCodeModule errorSource = theFirstNode.getAlgorithmRow().getOperation().asSourceCodeModule();
        BindHelper.processError(errorMessage, errorSource, bindingContext);
        return null;
    }


    private static final ConversionRuleBean[] convertionRules = new ConversionRuleBean[]{
            new ConversionRuleBean("SET", false, List.of(
                    new ConversionRuleStep("Perform", "SET.action", null, null, "execution"))),
            new ConversionRuleBean("VAR", false, List.of(
                    new ConversionRuleStep("!Declare", "VAR.condition", "VAR.action", null, null),
                    new ConversionRuleStep("Calculate", "VAR.action", null, null, "initial value"),
                    new ConversionRuleStep("AssignValue", "VAR.condition", null, null, null))),
            new ConversionRuleBean("IF", false, List.of(
                    new ConversionRuleStep("Calculate", "IF.condition", null, null, "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_end", "FALSE", null, null),
                    new ConversionRuleStep("Perform", "IF.action", null, null, "execution"),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end", null))),
            new ConversionRuleBean("IF", true, List.of(
                    new ConversionRuleStep("Calculate", "IF.condition", null, null, "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_end", "FALSE", null, null),
                    new ConversionRuleStep("!Compile", "IF.children", null, null, null),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end", null))),
            new ConversionRuleBean("IFELSE", false, List.of(
                    new ConversionRuleStep("Calculate", "IF.condition", null, null, "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_else", "FALSE", null, null),
                    new ConversionRuleStep("Perform", "IF.action", null, null, "execution"),
                    new ConversionRuleStep("Goto", "gen_label_end", null, null, null),
                    new ConversionRuleStep("Perform", "ELSE.action", null, "gen_label_else", "execution"),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end", null))),
            new ConversionRuleBean("IFELSE", true, List.of(
                    new ConversionRuleStep("Calculate", "IF.condition", null, null, "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_else", "FALSE", null, null),
                    new ConversionRuleStep("!Compile", "IF.children", null, null, null),
                    new ConversionRuleStep("Goto", "gen_label_end", null, null, null),
                    new ConversionRuleStep("!Compile", "ELSE.children", null, "gen_label_else", null),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end", null))),
            new ConversionRuleBean("WHILE", false, List.of(
                    new ConversionRuleStep("Calculate", "WHILE.condition", null, "gen_label_begin_loop", "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_end_loop", "FALSE", null, null),
                    new ConversionRuleStep("Perform", "WHILE.action", null, null, "execution"),
                    new ConversionRuleStep("Goto", "gen_label_begin_loop", null, null, null),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end_loop", null))),
            new ConversionRuleBean("WHILE", true, List.of(
                    new ConversionRuleStep("Calculate", "WHILE.condition", null, "gen_label_begin_loop", "condition calculation"),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_end_loop", "FALSE", null, null),
                    new ConversionRuleStep("!Compile", "WHILE.children", null, null, null),
                    new ConversionRuleStep("Goto", "gen_label_begin_loop", null, null, null),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end_loop", null))),
            new ConversionRuleBean("FOR EACH", true, List.of(
                    new ConversionRuleStep("Calculate", "FOR EACH.action", null, null, null),
                    new ConversionRuleStep("DeclareIterator", "declare iterator", "FOR EACH.condition", null, null),
                    new ConversionRuleStep("!DeclareArrayElement", "FOR EACH.condition", "FOR EACH.action", null, null),
                    new ConversionRuleStep("IteratorHasNext", "FOR EACH.condition", null, "gen_label_begin_loop", null),
                    new ConversionRuleStep("ConditionalGoto", "gen_label_end_loop", "FALSE", null, null),
                    new ConversionRuleStep("IteratorNext", "FOR EACH.condition", null, null, null),
                    new ConversionRuleStep("AssignVariable", "gen_localVariable", "FOR EACH.condition", null, "next"),
                    new ConversionRuleStep("!Compile", "FOR EACH.children", null, null, null),
                    new ConversionRuleStep("Goto", "gen_label_begin_loop", null, null, null),
                    new ConversionRuleStep("Nop", null, null, "gen_label_end_loop", null))),
            new ConversionRuleBean("GOTO", false, List.of(
                    new ConversionRuleStep("!CheckLabel", "GOTO.condition", null, null, null),
                    new ConversionRuleStep("Goto", "GOTO.condition", null, null, "label"))),
            new ConversionRuleBean("BREAK", false, List.of(
                    new ConversionRuleStep("Goto", "gen_label_end_loop", null, null, ""))),
            new ConversionRuleBean("CONTINUE", false, List.of(
                    new ConversionRuleStep("Goto", "gen_label_begin_loop", null, null, ""))),
            new ConversionRuleBean("RETURN", false, List.of(
                    new ConversionRuleStep("Return", "RETURN.condition", null, null, "result"))),
            new ConversionRuleBean("SUB", false, List.of(
                    new ConversionRuleStep("!Subroutine", null, null, null, null),
                    new ConversionRuleStep("Perform", "SUB.action", null, null, "execution"))),
            new ConversionRuleBean("SUB", true, List.of(
                    new ConversionRuleStep("!Subroutine", null, null, null, null),
                    new ConversionRuleStep("!Compile", "SUB.children", null, null, null))),
            new ConversionRuleBean("FUNCTION", false, List.of(
                    new ConversionRuleStep("!Function", "FUNCTION.action", null, null, null),
                    new ConversionRuleStep("Return", "FUNCTION.action", null, null, "execution"))),
            new ConversionRuleBean("FUNCTION", true, List.of(
                    new ConversionRuleStep("!Function", "FUNCTION.children:first:RETURN.condition", null, null, null),
                    new ConversionRuleStep("!Compile", "FUNCTION.children", null, null, null)))

    };

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
