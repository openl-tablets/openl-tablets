package org.openl.studio.config;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;

/**
 * Says that a map holds entries, even where its type does not say what they hold.
 *
 * <p>A map whose values have a type of their own is described with that type — see
 * {@link com.github.victools.jsonschema.generator.Option#MAP_VALUES_AS_ADDITIONAL_PROPERTIES}. A datatype
 * declares its fields without one, so an OpenL map is a plain {@code Map}, and a map of anything is left with
 * nothing said about its entries at all: the schema is then a bare object, which reads as a structure with no
 * fields. The form that collects a run's input has no map to offer for it and falls back to editing the whole
 * value as JSON text.
 *
 * <p>So a map that says nothing about its values says at least that it takes them. What each one holds is then
 * the reader's to write, which is what a map of {@code Object} means.
 *
 * <p>Says it only where nothing was said, so a map whose values do have a type of their own keeps that type.
 * That is the option's work, and this override leans on it having run.
 *
 * @author Vladyslav Pikus
 */
public class MapEntriesAttributeOverride implements TypeAttributeOverrideV2 {

    @Override
    public void overrideTypeAttributes(ObjectNode node, TypeScope scope, SchemaGenerationContext context) {
        var entries = context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES);
        if (scope.getType().isInstanceOf(Map.class) && !node.has(entries)) {
            node.put(entries, true);
        }
    }
}
