package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;

/** Reading a value a level at a time gives what writing it whole gives, one level of it. */
class ExecutionValueLevelsTest {

    private static final String SRC = "test/rules/EPBDS-16463/sprTests.xlsx";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutionValueLevels levels = new ExecutionValueLevels(objectMapper);

    /** A driver, the way a datatype is written. */
    public record Driver(String name, int age, List<String> licenses) {
    }

    /** Leaves out the fields that are null, the way a project publishes its datatypes. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public interface NonNullMixIn {
    }

    /** A vehicle whose value is held in a field that is not public, the way a datatype of a project holds it. */
    public static class Vehicle {
        @JsonProperty("model")
        protected String model = "Toyota";
    }

    /**
     * A spreadsheet result opens into the fields of the bean it is published as, in the same order and with the same
     * values; a field holding another spreadsheet result opens the same way in turn.
     */
    @Test
    void opensASpreadsheetResultIntoTheFieldsItIsPublishedWith() {
        var wrapper = spreadsheetOf("WrapperTest");
        var published = objectMapper.valueToTree(SpreadsheetResult.convertSpreadsheetResult(wrapper, null));

        var level = levels.levelOf(wrapper, 0, 100);

        assertSameLevel(published, level);
        var nested = level.lines().stream()
                .filter(line -> line.size() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("WrapperTest returns a spreadsheet result within its own"));
        var inner = levels.at(wrapper, List.of(nested.segment())).orElseThrow();
        assertSameLevel(published.get(nested.name()), levels.levelOf(inner, 0, 100));
    }

    /**
     * A spreadsheet result without a bean of its own opens into its cells under the names it is published with as a
     * map: a row alone in a result of one column, a column alone in a result of one row, both otherwise, and nothing
     * of a cell the result model leaves out.
     */
    @Test
    void opensASpreadsheetResultWithoutABeanIntoTheCellsItIsPublishedWith() {
        var oneColumn = spreadsheet(new Object[][]{{150}, {15}}, new String[]{"Premium", "Tax"}, new String[]{"Value"});
        var oneRow = spreadsheet(new Object[][]{{150, 40}}, new String[]{"Total"}, new String[]{"Life", "Accident"});
        var grid = spreadsheet(new Object[][]{{1, 2}, {3, 4}}, new String[]{"Base", "Total"},
                new String[]{"Life", "Accident"});
        var hidden = new SpreadsheetResult(new Object[][]{{1, 2}, {3, 4}}, new String[]{"Base", "Total"},
                new String[]{"Life", "Hidden"}, new String[]{"Base", "Total"}, new String[]{"Life", null}, Map.of());

        for (var published : List.of(oneColumn, oneRow, grid, hidden)) {
            assertEquals(List.copyOf(published.toMap(false, null).keySet()), names(levels.levelOf(published, 0, 100)));
        }
        assertEquals(15, levels.at(oneColumn, List.of("Tax")).orElseThrow());
        assertEquals(40, levels.at(oneRow, List.of("Accident")).orElseThrow());
        assertEquals(4, levels.at(grid, List.of("Accident_Total")).orElseThrow());
        // The one column of the result model left, its cells are named after their rows.
        assertEquals(3, levels.at(hidden, List.of("Total")).orElseThrow());
    }

    @Test
    void opensABeanIntoTheFieldsTheObjectMapperWrites() {
        var driver = new Driver("Sara", 25, List.of("B", "C"));

        var level = levels.levelOf(driver, 0, 100);

        assertEquals(List.of("name", "age", "licenses"), names(level));
        assertEquals("Sara", level.lines().get(0).value().asText());
        assertEquals(25, level.lines().get(1).value().asInt());
        var licenses = level.lines().get(2);
        assertNull(licenses.value(), "a value with inner structure is only referred to");
        assertEquals(2, licenses.size());
        assertEquals(Boolean.TRUE, licenses.elements());
        assertEquals(List.of("[0]", "[1]"), names(levels.levelOf(levels.at(driver, List.of("licenses")).orElseThrow(),
                0, 100)));
    }

    @Test
    void readsAFieldThatIsNotPublicTheWayTheObjectMapperWritesIt() {
        var level = levels.levelOf(new Vehicle(), 0, 100);

        assertEquals(List.of("model"), names(level));
        assertEquals("Toyota", level.lines().getFirst().value().asText());
    }

    @Test
    void leavesOutTheFieldsTheObjectMapperLeavesOut() {
        var withoutNulls = new ExecutionValueLevels(new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL));
        var driver = new Driver(null, 25, List.of());

        assertEquals(List.of("age", "licenses"), names(withoutNulls.levelOf(driver, 0, 100)));
        assertEquals(List.of("name", "age", "licenses"), names(levels.levelOf(driver, 0, 100)));
        // A class the object mapper writes through a mix-in leaves out what the mix-in leaves out.
        var mixedIn = new ExecutionValueLevels(new ObjectMapper().addMixIn(Driver.class, NonNullMixIn.class));
        assertEquals(List.of("age", "licenses"), names(mixedIn.levelOf(driver, 0, 100)));
    }

    /** A long list is read a page at a time, and every page says how long the list is. */
    @Test
    void readsTheElementsOfALongListAPageAtATime() {
        var ages = IntStream.range(0, 250).boxed().toArray(Integer[]::new);

        var page = levels.levelOf(ages, 100, 100);

        assertEquals(250, page.total());
        assertEquals(100, page.lines().size());
        assertEquals("[100]", page.lines().getFirst().name());
        assertEquals("100", page.lines().getFirst().segment());
        assertEquals(199, page.lines().getLast().value().asInt());
        assertEquals(50, levels.levelOf(ages, 200, 100).lines().size());
        assertTrue(levels.levelOf(ages, 300, 100).lines().isEmpty());
    }

    @Test
    void opensAMapIntoItsEntriesByPosition() {
        var premiums = Map.of("Sara", Map.of("total", 150));

        var level = levels.levelOf(premiums, 0, 100);

        assertEquals(List.of("Sara"), names(level));
        assertEquals("0", level.lines().getFirst().segment());
        assertEquals(150, levels.levelOf(levels.at(premiums, List.of("0")).orElseThrow(), 0, 100)
                .lines().getFirst().value().asInt());
    }

    /** A set and a map are read a page at a time as well, their entries by position. */
    @Test
    void readsASetAndAMapAPageAtATime() {
        var names = new LinkedHashSet<>(List.of("Ann", "Bob", "Cid", "Dan", "Eve"));
        var ages = new LinkedHashMap<String, Integer>();
        names.forEach(name -> ages.put(name, 20 + ages.size()));

        var page = levels.levelOf(names, 2, 2);
        var entries = levels.levelOf(ages, 3, 100);

        assertEquals(5, page.total());
        assertEquals(List.of("[2]", "[3]"), names(page));
        assertEquals("Cid", page.lines().getFirst().value().asText());
        assertEquals(List.of("Dan", "Eve"), names(entries));
        assertEquals("3", entries.lines().getFirst().segment());
        assertEquals(24, levels.at(ages, List.of("4")).orElseThrow());
        assertEquals("Eve", levels.at(names, List.of("4")).orElseThrow());
    }

    @Test
    void findsNothingAlongAPathThatNamesNoLine() {
        var driver = new Driver("Sara", 25, List.of("B"));

        assertTrue(levels.at(driver, List.of("premium")).isEmpty());
        assertTrue(levels.at(driver, List.of("licenses", "5")).isEmpty());
        assertTrue(levels.at(driver, List.of("licenses", "first")).isEmpty());
        assertTrue(levels.at(driver, List.of("name", "0")).isEmpty(), "a plain value opens into nothing");
        assertTrue(levels.at(null, List.of()).isEmpty());
    }

    /**
     * A value the object mapper writes plain opens into no lines, and comes as it is written. OpenL does not count
     * every such type as simple: a value of one can be referred to, and asked for, all the same.
     */
    @Test
    void answersAValueThatOpensIntoNoLinesAsItIsWritten() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        var level = levels.levelOf(id, 0, 100);

        assertFalse(ExecutionValueLevels.opensIntoLines(objectMapper, id));
        assertEquals(0, level.total());
        assertTrue(level.lines().isEmpty());
        assertEquals(id.toString(), level.value().asText());
        assertEquals("Sara", levels.levelOf("Sara", 0, 100).value().asText());
        assertNull(levels.levelOf(new Driver("Sara", 25, List.of()), 0, 100).value(),
                "a value that opens is not written");
    }

    private static void assertSameLevel(JsonNode published, ValueLevel level) {
        var fields = new ArrayList<String>();
        published.fieldNames().forEachRemaining(fields::add);
        assertFalse(fields.isEmpty(), "the value is published with fields");
        assertEquals(fields, names(level), "the fields, in the order they are published");
        assertEquals(fields.size(), level.total());
        for (var line : level.lines()) {
            var written = published.get(line.name());
            if (line.size() == null) {
                assertEquals(written, line.value(), line.name());
            } else {
                assertTrue(written.isContainerNode(), line.name() + " has inner structure");
                assertEquals(written.size(), line.size(), line.name());
            }
        }
    }

    /** A spreadsheet result without a bean of its own, every row and column of it in the result model. */
    private static SpreadsheetResult spreadsheet(Object[][] values, String[] rows, String[] columns) {
        return new SpreadsheetResult(values, rows, columns, rows, columns, Map.of());
    }

    private static List<String> names(ValueLevel level) {
        return level.lines().stream().map(ValueLine::name).toList();
    }

    private static Object spreadsheetOf(String testName) {
        var openClass = new RulesEngineFactory<>(SRC).getCompiledOpenClass().getOpenClassWithErrors();
        var testMethod = openClass.getMethods().stream()
                .filter(TestSuiteMethod.class::isInstance)
                .map(TestSuiteMethod.class::cast)
                .filter(method -> testName.equals(method.getName()))
                .findFirst()
                .orElseThrow();
        var result = new TestSuite(testMethod).invokeSequentially(openClass, 1).getTestUnits().getFirst()
                .getActualResult();
        assertTrue(result instanceof SpreadsheetResult, testName + " returns a spreadsheet result");
        return result;
    }
}
