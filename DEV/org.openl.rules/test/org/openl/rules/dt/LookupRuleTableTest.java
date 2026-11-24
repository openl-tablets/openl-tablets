package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.xls.XlsUtil;

/**
 * The cells of a rule of a lookup whose horizontal value is merged over several columns, such as a brand over its
 * models: the rule has its region, the merged cell of its brand, its model and the value it returns.
 *
 * @author Yury Molchan
 */
class LookupRuleTableTest {

    @Test
    void aRuleUnderAMergedHorizontalValueHasAllItsCells() {
        var module = new RulesEngineFactory<>("test/rules/dt/EPBDS-5322_Merged_Horizontal_Value.xlsx")
                .getCompiledOpenClass()
                .getOpenClass();
        var price = (IDecisionTable) module.getMethods().stream()
                .filter(method -> "price".equals(method.getName()))
                .findFirst()
                .orElseThrow();

        // Porche is merged over C3:E3, above the models 911, Cayenne and Boxster; the rule of USA and Cayenne takes
        // the merged cell of Porche
        assertEquals(List.of("B5", "C3:E3", "D4", "D5"), cellsOf(price, 2));
        // BMW is not merged
        assertEquals(List.of("B6", "F3", "F4", "F6"), cellsOf(price, 7));
    }

    @Test
    void aRuleReturningAnArrayHasAllTheValuesUnderItsKey() {
        var module = new RulesEngineFactory<>("test/rules/dt/EPBDS-11350_Array_Under_Merged_Key.xlsx")
                .getCompiledOpenClass()
                .getOpenClass();
        var rates = (IDecisionTable) module.getMethods().stream()
                .filter(method -> "rates".equals(method.getName()))
                .findFirst()
                .orElseThrow();

        // Low is merged over C3:D3, so the rule of Basic and Low returns the two values under it
        assertEquals(List.of("B4", "C3:D3", "C4", "D4"), cellsOf(rates, 0));
        // High takes one column: the rule returns its one value, and the column it lacks adds no other cell
        assertEquals(List.of("B5", "E3", "E5"), cellsOf(rates, 3));
    }

    private static List<String> cellsOf(IDecisionTable table, int rule) {
        return GridTableUtils.getGridRegions(table.getRuleTable(rule))
                .stream()
                .map(region -> region.getLeft() == region.getRight() && region.getTop() == region.getBottom()
                        ? XlsUtil.xlsCellPresentation(region.getLeft(), region.getTop())
                        : XlsUtil.xlsCellPresentation(region.getLeft(), region.getTop()) + ":"
                                + XlsUtil.xlsCellPresentation(region.getRight(), region.getBottom()))
                .toList();
    }
}
