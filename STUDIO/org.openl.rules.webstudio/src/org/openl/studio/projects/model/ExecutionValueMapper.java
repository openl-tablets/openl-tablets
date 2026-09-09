package org.openl.studio.projects.model;

import java.util.Arrays;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.common.utils.SpreadsheetResultBean;
import org.openl.studio.config.SafeSchemaGenerator;
import org.openl.types.IOpenClass;

/**
 * Writes the values of a run or a test the way OpenL Rule Services publishes them.
 *
 * <p>A spreadsheet result carries no properties of its own to read or write, so it travels as the bean class OpenL
 * generates for the spreadsheet. Written as it stands it comes out as the engine's internal row and column tables,
 * which no client can read back.
 *
 * <p>A value of any other type is written as it stands.
 */
@RequiredArgsConstructor
public class ExecutionValueMapper {

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final @Nullable SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy;

    /**
     * Converts a value to the shape it is published in.
     *
     * <p>Use it when the declared type of the value is unknown, such as for the result of a run or for the value of
     * a test assertion. The spreadsheet a result belongs to is then read from the result itself.
     *
     * @param value value to publish
     * @return the published value, or {@code null} when there is no value
     */
    public @Nullable Object convert(@Nullable Object value) {
        return SpreadsheetResult.convertSpreadsheetResult(value, sprNamingStrategy);
    }

    /**
     * Writes a value already converted by {@link #convert(Object)}.
     *
     * <p>A value that is absent is left out rather than written as {@code null}.
     *
     * @param convertedValue published value
     * @return the written value, or {@code null} when there is no value
     */
    public @Nullable JsonNode writeConverted(@Nullable Object convertedValue) {
        return convertedValue == null ? null : objectMapper.valueToTree(convertedValue);
    }

    /**
     * Describes a value already converted by {@link #convert(Object)}.
     *
     * <p>The schema describes the class the value is written as, so that every property of the written value is
     * described by the schema.
     *
     * @param convertedValue published value
     * @return the schema of the value, or {@code null} when there is no value or the schema cannot be generated
     */
    public @Nullable ObjectNode schemaOf(@Nullable Object convertedValue) {
        return convertedValue == null ? null : SafeSchemaGenerator.generate(schemaGenerator, convertedValue.getClass());
    }

    /**
     * Lays a spreadsheet result out the way its spreadsheet is written.
     *
     * <p>Use it for a value whose declared type is unknown, next to {@link #convert(Object)}. Every cell is
     * written the same way the value itself is, so a client shows the same values in the grid and in the tree.
     *
     * @param value value to lay out
     * @return the layout of the value, or {@code null} when the value is not a spreadsheet result
     */
    public @Nullable SpreadsheetResultView spreadsheetOf(@Nullable Object value) {
        if (!(value instanceof SpreadsheetResult spreadsheet) || spreadsheet.getRowNames() == null
                || spreadsheet.getColumnNames() == null || spreadsheet.getResults() == null) {
            return null;
        }
        var columns = Arrays.stream(spreadsheet.getColumnNames()).toList();
        var rows = Arrays.stream(spreadsheet.getRowNames()).toList();
        var cells = IntStream.range(0, rows.size())
                .mapToObj(row -> IntStream.range(0, columns.size())
                        .mapToObj(column -> writeConverted(convert(spreadsheet.getValue(row, column))))
                        .toList())
                .toList();
        return new SpreadsheetResultView(columns, rows, cells);
    }

    /**
     * Writes an execution parameter together with the schema describing it.
     *
     * <p>A parameter declares its type, so the spreadsheet a value belongs to is read from the declaration and both
     * the value and the schema follow the bean class of that spreadsheet.
     *
     * @param param       parameter to write
     * @param description display name of the parameter
     * @return the written parameter
     */
    public ParameterValue writeParameter(ParameterWithValueDeclaration param, @Nullable String description) {
        return writeParameter(param, description, true);
    }

    /**
     * Writes an execution parameter, with or without the schema describing it.
     *
     * <p>A value that is only shown, never edited, is written without the schema.
     *
     * @param param         parameter to write
     * @param description   display name of the parameter
     * @param includeSchema whether to describe the parameter type
     * @return the written parameter
     */
    public ParameterValue writeParameter(ParameterWithValueDeclaration param,
                                         @Nullable String description,
                                         boolean includeSchema) {
        var spreadsheetResult = SpreadsheetResultBean.of(param.getType());
        var value = spreadsheetResult == null
                ? param.getValue()
                : SpreadsheetResult.convertSpreadsheetResult(param.getValue(), spreadsheetResult.beanClass(),
                        param.getType(), sprNamingStrategy);
        return ParameterValue.builder()
                .name(param.getName())
                .value(objectMapper.valueToTree(value))
                .schema(includeSchema ? schemaOfType(param.getType()) : null)
                .description(description)
                .build();
    }

    /**
     * Writes an execution parameter for a list of values.
     *
     * <p>A plain value is written as it stands. A value with inner structure is written as a lazy reference
     * without the value, the way the trace publishes the values of a frame.
     *
     * <p>The reference carries no id. A client addresses the value by the parameter name within its case.
     *
     * @param param       parameter to write
     * @param description display name of the parameter
     * @return the written parameter, or its lazy reference
     */
    public ParameterValue writeParameterLazily(ParameterWithValueDeclaration param, @Nullable String description) {
        if (param.getValue() == null || param.getType().isSimple()) {
            return writeParameter(param, description, false).toBuilder().lazy(false).build();
        }
        return ParameterValue.builder()
                .name(param.getName())
                .description(description)
                .lazy(true)
                .build();
    }

    /**
     * Describes a declared parameter before it is executed.
     *
     * <p>The description carries the name of the parameter, the display name of its type, the schema of the
     * values it accepts and the value it starts with. The starting value holds the defaults the datatype declares,
     * or nothing when the type declares none.
     *
     * <p>A spreadsheet result parameter is described by the bean class OpenL Rule Services publishes for it. That
     * is the shape the run and trace APIs read back.
     *
     * @param name         name of the parameter
     * @param type         declared type of the parameter
     * @param defaultValue value the parameter starts with, or {@code null} when it starts unset
     * @return the described parameter
     */
    public ParameterValue describeParameter(String name, IOpenClass type, @Nullable Object defaultValue) {
        return writeParameter(new ParameterWithValueDeclaration(name, defaultValue, type),
                type.getDisplayName(INamedThing.SHORT), true);
    }

    private @Nullable ObjectNode schemaOfType(IOpenClass type) {
        var spreadsheetResult = SpreadsheetResultBean.of(type);
        return SafeSchemaGenerator.generate(schemaGenerator,
                spreadsheetResult != null ? spreadsheetResult.beanClass() : type.getInstanceClass());
    }
}
