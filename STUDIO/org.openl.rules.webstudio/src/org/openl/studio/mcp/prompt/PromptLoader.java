package org.openl.studio.mcp.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Prompt template loader and processor.
 * <p>
 * Loads markdown prompt templates from the mcp/prompts/ directory and
 * substitutes context variables to create contextual guidance for AI assistants.
 */
@Component
public class PromptLoader {

    private static final Logger logger = LoggerFactory.getLogger(PromptLoader.class);
    private static final String PROMPTS_LOCATION = "mcp/prompts/";

    /**
     * Load a prompt template by name.
     *
     * @param templateName Name of the template file (without .md extension)
     * @return Formatted prompt
     */
    public String load(String templateName) {
        return load(templateName, new HashMap<>());
    }

    /**
     * Load a prompt template by name with variable substitution.
     *
     * @param templateName Name of the template file (without .md extension)
     * @param context      Variables to substitute in the template
     * @return Formatted prompt with variables substituted
     */
    public String load(String templateName, Map<String, Object> context) {
        try {
            String template = getPromptContent(templateName);
            return substitute(template, context);
        } catch (Exception e) {
            logger.warn("Failed to load prompt '{}': {}", templateName, e.getMessage());
            return "Processing " + templateName + "...\n\nContext: " + context;
        }
    }

    /**
     * Get prompt content from file or cache.
     *
     * @param templateName Name of the template
     * @return Prompt content
     * @throws IOException if file cannot be read
     */
    private String getPromptContent(String templateName) throws IOException {
        String resourcePath = PROMPTS_LOCATION + templateName + ".md";
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IOException("Prompt file not found: " + resourcePath);
        }

        return readResource(resource);
    }

    /**
     * Read content from a ClassPathResource.
     *
     * @param resource The resource to read
     * @return Content as string
     * @throws IOException if resource cannot be read
     */
    private String readResource(ClassPathResource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Substitute variables in a template.
     * <p>
     * Supports:
     * - Simple variables: {variable_name}
     * - Conditional blocks: {if condition}...{end if}
     * - Loops: {for each items}...{end for}
     *
     * @param template Template string with placeholders
     * @param vars     Variables to substitute
     * @return Template with variables substituted
     */
    public String substitute(String template, Map<String, Object> vars) {
        String result = template;

        // Process conditional blocks: {if variable}...{end if}
        result = processConditionals(result, vars);

        // Process loops: {for each items:}...{end for}
        result = processLoops(result, vars);

        // Substitute simple variables: {variable}
        result = substituteVariables(result, vars);

        return result;
    }

    /**
     * Process conditional blocks.
     *
     * @param template The template to process
     * @param vars     Variables to check
     * @return Template with conditionals processed
     */
    private String processConditionals(String template, Map<String, Object> vars) {
        Pattern ifPattern = Pattern.compile("\\{if\\s+(\\w+)}([\\s\\S]*?)\\{end\\s+if}");
        Matcher matcher = ifPattern.matcher(template);
        var sb = new StringBuilder();

        while (matcher.find()) {
            String condition = matcher.group(1);
            String content = matcher.group(2);

            // Check if condition variable is truthy
            Object value = vars.get(condition);
            boolean isTrue = isTruthy(value);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(isTrue ? content : ""));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Process loop blocks.
     *
     * @param template The template to process
     * @param vars     Variables containing arrays
     * @return Template with loops processed
     */
    @SuppressWarnings("unchecked")
    private String processLoops(String template, Map<String, Object> vars) {
        Pattern forPattern = Pattern.compile("\\{for\\s+each\\s+(\\w+):?}([\\s\\S]*?)\\{end\\s+for}");
        Matcher matcher = forPattern.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String arrayName = matcher.group(1);
            String content = matcher.group(2);

            Object arrayObj = vars.get(arrayName);
            String replacement = "";

            if (arrayObj instanceof Iterable<?> array) {
                int index = 1;
                StringBuilder itemsBuilder = new StringBuilder();

                for (Object item : array) {
                    Map<String, Object> itemVars = new HashMap<>(vars);
                    itemVars.put("index", index);

                    if (item instanceof Map) {
                        itemVars.putAll((Map<String, Object>) item);
                    } else {
                        itemVars.put("item", item);
                    }

                    itemsBuilder.append(substituteVariables(content, itemVars)).append("\n");
                    index++;
                }

                if (!itemsBuilder.isEmpty()) {
                    replacement = itemsBuilder.toString().trim();
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Substitute simple variables.
     *
     * @param template The template to process
     * @param vars     Variables to substitute
     * @return Template with variables replaced
     */
    private String substituteVariables(String template, Map<String, Object> vars) {
        Pattern varPattern = Pattern.compile("\\{(\\w+)}");
        Matcher matcher = varPattern.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = vars.get(varName);

            if (value != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            } else {
                // Keep placeholder if variable not found
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Check if a value is truthy.
     *
     * @param value The value to check
     * @return True if value is truthy
     */
    private boolean isTruthy(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean b -> b;
            case Number number -> number.doubleValue() != 0;
            case String s -> !s.isEmpty();
            default -> true;
        };
    }
}
