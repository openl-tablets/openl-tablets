package org.openl.studio.projects.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetCell;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;

/**
 * Reads a value of an execution one level at a time, in the shape it is published in.
 *
 * <p>A level is the lines a value with inner structure opens into: the fields of a bean or of a spreadsheet result,
 * the elements of an array or a collection, the entries of a map. A line carries a plain value as the object mapper
 * writes it. A value with inner structure is only referred to, by its type and by how many lines it opens into.
 * Nothing below the level asked for is read or converted, so a value of any size costs no more than that level.
 *
 * <p>The object mapper decides what opens: a value it writes as a bean does, and so do a spreadsheet result, an
 * array, a collection and a map. A value it writes otherwise, such as a number, a date or text, opens into no lines
 * and is answered as it is written.
 *
 * <p>A bean is laid out the way the object mapper writes it: its fields carry the names and the order it writes them
 * in, and a field it leaves out when it is null is left out here too. A spreadsheet result is laid out the way the
 * bean OpenL Rule Services publishes for it, and one without a bean of its own the way it is published as a map of
 * its cells.
 */
@RequiredArgsConstructor
public class ExecutionValueLevels {

    /** What the values are published with. */
    private final ObjectMapper objectMapper;

    /**
     * A field of a bean or of a spreadsheet result: the name it is written under, how its value is read, and whether
     * it is left out when it is null.
     */
    private record Field(String name, Function<Object, @Nullable Object> reader, boolean leftOutWhenNull) {
    }

    /** A line before it is written: what it is shown as, what opens it, and its value. */
    private record Line(String name, String segment, @Nullable Object value) {
    }

    /** A page of the lines of a value, and how many lines the value opens into. */
    private record Page(int total, List<Line> lines) {
    }

    /** Where a cell of a spreadsheet result stands. */
    private record Cell(int row, int column) {
    }

    /**
     * Whether the object mapper writes a value as one that opens into lines.
     *
     * <p>A spreadsheet result, an array, a collection, a map and a value the object mapper writes as a bean open into
     * lines. Anything else is written as a plain value, and so is an error.
     *
     * @param objectMapper what the value is written with
     * @param value        the value
     */
    public static boolean opensIntoLines(ObjectMapper objectMapper, @Nullable Object value) {
        if (value == null || value instanceof Throwable) {
            return false;
        }
        if (value instanceof SpreadsheetResult || value instanceof Collection<?> || value instanceof Map<?, ?>) {
            return true;
        }
        if (value.getClass().isArray()) {
            // Written as text: the bytes as Base64, the characters as a string.
            return !(value instanceof byte[]) && !(value instanceof char[]);
        }
        return beanSerializerOf(objectMapper, value.getClass()).isPresent();
    }

    /**
     * The value at a path within another one.
     *
     * @param root the value the path starts from
     * @param path the segments of the lines to open, one level each
     * @return the value, or nothing when a segment names no line or the path leads to no value
     */
    public Optional<Object> at(@Nullable Object root, List<String> path) {
        var value = Optional.ofNullable(root);
        for (var segment : path) {
            value = value.flatMap(current -> child(current, segment));
        }
        return value;
    }

    /**
     * A page of the lines of a value.
     *
     * <p>A value that opens into no lines comes as the object mapper writes it, in place of its lines.
     *
     * @param value  the value to open
     * @param offset how many lines to skip
     * @param size   how many lines to write at the most
     */
    public ValueLevel levelOf(Object value, int offset, int size) {
        if (!opensIntoLines(objectMapper, value)) {
            return new ValueLevel(typeOf(value), 0, null, List.of(), plain(value));
        }
        var page = pageOf(value, offset, size);
        return new ValueLevel(typeOf(value),
                page.total(),
                isElements(value) ? Boolean.TRUE : null,
                page.lines().stream().map(this::write).toList(),
                null);
    }

    private Optional<Object> child(Object value, String segment) {
        if (!opensIntoLines(objectMapper, value)) {
            return Optional.empty();
        }
        if (value instanceof Map<?, ?> || isElements(value)) {
            var position = positionOf(segment);
            return position < 0
                    ? Optional.empty()
                    : pageOf(value, position, 1).lines().stream().findFirst().map(Line::value);
        }
        return fieldsOf(value).stream()
                .filter(field -> field.name().equals(segment))
                .findFirst()
                .map(field -> field.reader().apply(value));
    }

    private static int positionOf(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * How many lines a value opens into, counted rather than read: a field is read only when it is left out while
     * it is null.
     */
    private int sizeOf(Object value) {
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return (int) fieldsOf(value).stream()
                .filter(field -> !field.leftOutWhenNull() || field.reader().apply(value) != null)
                .count();
    }

    /**
     * A page of the lines of a value.
     *
     * <p>The elements and entries before the page are walked past rather than copied, so a page of a large
     * collection costs no more than the page. The fields of a bean are read once for the page and its count.
     */
    private Page pageOf(Object value, int offset, int size) {
        if (value.getClass().isArray()) {
            var total = Array.getLength(value);
            return new Page(total, elementsOf(total, index -> Array.get(value, index), offset, size));
        }
        if (value instanceof List<?> list && value instanceof RandomAccess) {
            return new Page(list.size(), elementsOf(list.size(), list::get, offset, size));
        }
        if (value instanceof Collection<?> collection) {
            return new Page(collection.size(), itemsOf(collection, offset, size, ExecutionValueLevels::element));
        }
        if (value instanceof Map<?, ?> map) {
            return new Page(map.size(), itemsOf(map.entrySet(), offset, size, (index, entry) ->
                    new Line(String.valueOf(entry.getKey()), String.valueOf(index), entry.getValue())));
        }
        var fields = writtenFields(value);
        var end = (int) Math.min(fields.size(), (long) offset + size);
        return new Page(fields.size(), fields.subList(Math.min(offset, end), end));
    }

    /** A page of the elements of a value that reaches each of them by its position. */
    private static List<Line> elementsOf(int total, IntFunction<@Nullable Object> elementAt, int offset, int size) {
        var end = (int) Math.min(total, (long) offset + size);
        return IntStream.range(Math.min(offset, end), end)
                .mapToObj(index -> element(index, elementAt.apply(index)))
                .toList();
    }

    /** A page of the items of a collection, each made into a line with its position. */
    private static <T> List<Line> itemsOf(Collection<T> items,
                                         int offset,
                                         int size,
                                         BiFunction<Integer, T, Line> line) {
        var page = new ArrayList<Line>();
        var end = (long) offset + size;
        var iterator = items.iterator();
        for (var index = 0; index < end && iterator.hasNext(); index++) {
            var item = iterator.next();
            if (index >= offset) {
                page.add(line.apply(index, item));
            }
        }
        return page;
    }

    private static Line element(int index, @Nullable Object value) {
        return new Line("[" + index + "]", String.valueOf(index), value);
    }

    /** The fields the object mapper writes of a bean or of a spreadsheet result, with their values. */
    private List<Line> writtenFields(Object value) {
        var lines = new ArrayList<Line>();
        for (var field : fieldsOf(value)) {
            var fieldValue = field.reader().apply(value);
            if (fieldValue != null || !field.leftOutWhenNull()) {
                lines.add(new Line(field.name(), field.name(), fieldValue));
            }
        }
        return lines;
    }

    private List<Field> fieldsOf(Object value) {
        if (value instanceof SpreadsheetResult spreadsheet) {
            var type = spreadsheet.getCustomSpreadsheetResultOpenClass();
            return type == null ? cellsOf(spreadsheet) : spreadsheetFields(type.getBeanClass());
        }
        return writersOf(value.getClass()).stream()
                .map(writer -> new Field(writer.getName(), bean -> read(writer, bean), writer.willSuppressNulls()))
                .toList();
    }

    /** The fields of the bean a spreadsheet result is published as, each read from the cell it stands for. */
    private List<Field> spreadsheetFields(Class<?> beanClass) {
        var fields = new ArrayList<Field>();
        for (var writer : writersOf(beanClass)) {
            var cell = writer.getAnnotation(SpreadsheetCell.class);
            if (cell != null) {
                fields.add(new Field(writer.getName(),
                        spreadsheet -> ((SpreadsheetResult) spreadsheet).getFieldValue(cell.cell()),
                        writer.willSuppressNulls()));
            }
        }
        return fields;
    }

    /**
     * The cells of a spreadsheet result published without a bean of its own, under the names it publishes them with.
     *
     * <p>Such a result is published as a map of its cells. A cell is named after its row in a result of one column,
     * after its column in a result of one row, and after both otherwise; a cell outside the result model is left out.
     * The names are had from the result itself, from a copy of it that holds where each cell stands in place of its
     * value, so no value is read to name the cells.
     */
    private List<Field> cellsOf(SpreadsheetResult spreadsheet) {
        var rows = spreadsheet.getRowNames();
        var columns = spreadsheet.getColumnNames();
        if (rows == null || columns == null) {
            return List.of();
        }
        var located = new SpreadsheetResult(spreadsheet);
        var cells = new Object[rows.length][columns.length];
        for (var row = 0; row < rows.length; row++) {
            for (var column = 0; column < columns.length; column++) {
                cells[row][column] = new Cell(row, column);
            }
        }
        located.setResults(cells);
        var published = located.toMap(false, null);
        var named = objectMapper.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                ? new TreeMap<>(published)
                : published;
        var inclusion = objectMapper.getSerializationConfig()
                .getDefaultPropertyInclusion(Map.class)
                .getContentInclusion();
        var leftOutWhenNull = inclusion != JsonInclude.Include.ALWAYS && inclusion != JsonInclude.Include.USE_DEFAULTS;
        var fields = new ArrayList<Field>();
        named.forEach((name, cell) -> {
            // Typed, not var: on javac 21 a var binding here has an unknown type, which crashes Error Prone.
            if (cell instanceof Cell(int row, int column)) {
                fields.add(new Field(name,
                        result -> ((SpreadsheetResult) result).getValue(row, column),
                        leftOutWhenNull));
            }
        });
        return fields;
    }

    /** The fields the object mapper writes a bean of the class with; none for a class it writes otherwise. */
    private List<BeanPropertyWriter> writersOf(Class<?> type) {
        var writers = new ArrayList<BeanPropertyWriter>();
        beanSerializerOf(objectMapper, type).ifPresent(serializer -> serializer.properties().forEachRemaining(
                property -> {
                    if (property instanceof BeanPropertyWriter writer) {
                        writers.add(writer);
                    }
                }));
        return writers;
    }

    private static Optional<BeanSerializerBase> beanSerializerOf(ObjectMapper objectMapper, Class<?> type) {
        try {
            return objectMapper.getSerializerProviderInstance().findValueSerializer(type)
                    instanceof BeanSerializerBase serializer ? Optional.of(serializer) : Optional.empty();
        } catch (JsonMappingException e) {
            return Optional.empty();
        }
    }

    /** The value of a field, read the way the object mapper reads it to write it. */
    private static @Nullable Object read(BeanPropertyWriter writer, Object bean) {
        try {
            return writer.get(bean);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read the field '%s'".formatted(writer.getName()), e);
        }
    }

    private ValueLine write(Line line) {
        var value = line.value();
        var written = ValueLine.builder().name(line.name()).segment(line.segment());
        if (!opensIntoLines(objectMapper, value)) {
            return written.value(plain(value)).build();
        }
        return written.type(typeOf(value))
                .size(sizeOf(value))
                .elements(isElements(value) ? Boolean.TRUE : null)
                .build();
    }

    private JsonNode plain(@Nullable Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        // An error is written the way the case reports it, not as everything the exception holds.
        return value instanceof Throwable error
                ? TextNode.valueOf(String.valueOf(error))
                : objectMapper.valueToTree(value);
    }

    private static boolean isElements(Object value) {
        return value.getClass().isArray() || value instanceof Collection<?>;
    }

    private static String typeOf(Object value) {
        return ParameterWithValueDeclaration.getParamType(value).getDisplayName(INamedThing.SHORT);
    }
}
