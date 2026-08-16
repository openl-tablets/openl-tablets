package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The values a vocabulary declares, as the dependency graph reports them.
 *
 * <p>Every value keeps the type the table gives it, so a vocabulary of numbers reads as numbers and not as text.
 *
 * <p>The preview is bounded, so a long vocabulary does not weigh the graph down. The complete list of values is read
 * through the table API.
 *
 * @param valueCount    the whole vocabulary is counted, whether previewed or not
 * @param valuesPreview a vocabulary longer than the preview is reported by its first and last values
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "The values a vocabulary declares")
public record DatatypeNodeVocabularyView(

        @Parameter(description = "Type of the values, as the table header declares it, for example String")
        String valueType,

        @Parameter(description = "How many values the vocabulary declares, previewed or not")
        int valueCount,

        @Parameter(description = """
                Up to six values, in the order the table declares them; a longer vocabulary is previewed by its \
                first three and last three values""")
        List<Object> valuesPreview,

        @Parameter(description = "true when the preview leaves values out")
        boolean truncated
) {
}
