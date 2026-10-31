package org.openl.rules.project.migration;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.util.StringUtils;

/**
 * The {@code rules-deploy.xml} content migrations OpenL Studio applies to a deployment descriptor in memory,
 * so a caller that already holds a {@link RulesDeploy} can normalize it without the Maven plugin.
 *
 * <p>These are the same transforms the {@code openl:migrate} goal runs for the deployment descriptor; the
 * goal's migrator classes delegate their transform here so there is one implementation.
 *
 * <p>The empty-tag cleanup is not applied here: it happens for free when the caller re-serializes the
 * descriptor, as the JAXB {@code beforeMarshal} callbacks drop the empty tags.
 */
public final class RulesDeployMigrations {

    private static final String DEFAULT_TYPING_MODE = "jackson.defaultTypingMode";
    private static final String JSON_TYPE_INFO_ID = "jackson.jsonTypeInfoId";
    private static final String SIMPLE_CLASS_NAME_AS_TYPING_PROPERTY_VALUE =
            "jackson.simpleClassNameAsTypingPropertyValue";

    private RulesDeployMigrations() {
    }

    /**
     * Applies the {@code rules-deploy.xml} content migrations to the descriptor.
     *
     * @param rulesDeploy the descriptor read from {@code rules-deploy.xml}; mutated in place
     */
    public static void apply(RulesDeploy rulesDeploy) {
        runtimeContext(rulesDeploy);
        templateClass(rulesDeploy);
        jacksonTyping(rulesDeploy);
    }

    /**
     * Drops {@code <isProvideRuntimeContext>} when it is {@code false} — the runtime default. An explicit
     * {@code true} or an absent value is kept, so a service that relies on the "no runtime context"
     * behaviour keeps working; only the redundant line goes away.
     */
    public static void runtimeContext(RulesDeploy rulesDeploy) {
        if (Boolean.FALSE.equals(rulesDeploy.isProvideRuntimeContext())) {
            rulesDeploy.setProvideRuntimeContext(null);
        }
    }

    /**
     * Renames the legacy {@code interceptingTemplateClassName} slot to {@code annotationTemplateClassName}.
     * A non-blank intercepting value fills an empty annotation slot (move) or is dropped when the annotation
     * slot already holds one (the annotation slot is authoritative). A blank intercepting value is left as
     * is.
     */
    public static void templateClass(RulesDeploy rulesDeploy) {
        if (StringUtils.isBlank(rulesDeploy.getInterceptingTemplateClassName())) {
            return;
        }
        if (StringUtils.isBlank(rulesDeploy.getAnnotationTemplateClassName())) {
            rulesDeploy.setAnnotationTemplateClassName(rulesDeploy.getInterceptingTemplateClassName());
        }
        rulesDeploy.setInterceptingTemplateClassName(null);
    }

    /**
     * Replaces the Jackson typing settings that OpenL Tablets no longer reads.
     *
     * <p>{@code jackson.defaultTypingMode} set to {@code EVERYTHING} becomes {@code NON_FINAL_AND_ENUMS}, the closest
     * mode: it writes the type of every value except a value of a final class, such as {@code Long}. A service with
     * {@code EVERYTHING} does not deploy at all.
     *
     * <p>{@code jackson.simpleClassNameAsTypingPropertyValue} is dropped. When it was {@code true}, it turned the
     * {@code CLASS} type id into {@code NAME}, so {@code jackson.jsonTypeInfoId} becomes {@code NAME} where it is
     * {@code CLASS} or absent, and the service keeps writing and expecting simple class names. An absent type id is
     * taken for {@code CLASS} or {@code NAME}, the default of {@code ruleservice.jackson.jsonTypeInfoId}, as the
     * descriptor does not tell the one the application properties set.
     */
    public static void jacksonTyping(RulesDeploy rulesDeploy) {
        var configuration = rulesDeploy.getConfiguration();
        if (configuration == null) {
            return;
        }
        if (is(configuration.get(DEFAULT_TYPING_MODE), "EVERYTHING")) {
            configuration.put(DEFAULT_TYPING_MODE, "NON_FINAL_AND_ENUMS");
        }
        // the flag was read without trimming the blanks around it
        var simpleClassName = String.valueOf(configuration.remove(SIMPLE_CLASS_NAME_AS_TYPING_PROPERTY_VALUE));
        var typeInfoId = configuration.get(JSON_TYPE_INFO_ID);
        var classId = typeInfoId == null || is(typeInfoId, "CLASS");
        if ("true".equalsIgnoreCase(simpleClassName) && classId) {
            configuration.put(JSON_TYPE_INFO_ID, "NAME");
        }
    }

    /** Whether a configuration value is the text, in any letter case and with any blanks around it. */
    private static boolean is(@Nullable Object value, String text) {
        return value instanceof String string && text.equalsIgnoreCase(string.trim());
    }
}
