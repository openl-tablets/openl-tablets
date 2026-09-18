package org.openl.rules.model.scaffolding;

import java.util.List;

/**
 * A vocabulary datatype: the values a simple type is restricted to, as an OpenAPI {@code enum} declares them.
 *
 * <p>Written as {@code Datatype <name> <type>} with one value per row.
 *
 * @param name   the name of the datatype
 * @param type   the base type of the values: {@code String}, {@code Integer}, ...
 * @param values the values allowed, as they are written in the table
 */
public record VocabularyModel(String name, String type, List<String> values) implements Model {
    @Override
    public String getName() {
        return name;
    }
}
