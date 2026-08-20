package org.openl.studio.projects.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.common.utils.SpreadsheetResultBean;
import org.openl.studio.config.SafeSchemaGenerator;

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
        var spreadsheetResult = SpreadsheetResultBean.of(param.getType());
        var value = spreadsheetResult == null
                ? param.getValue()
                : SpreadsheetResult.convertSpreadsheetResult(param.getValue(), spreadsheetResult.beanClass(),
                        param.getType(), sprNamingStrategy);
        return ParameterValue.builder()
                .name(param.getName())
                .value(objectMapper.valueToTree(value))
                .schema(SafeSchemaGenerator.generate(schemaGenerator,
                        spreadsheetResult != null
                                ? spreadsheetResult.beanClass()
                                : param.getType().getInstanceClass()))
                .description(description)
                .build();
    }
}
