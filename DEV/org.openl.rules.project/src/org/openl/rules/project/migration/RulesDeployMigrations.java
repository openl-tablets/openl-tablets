package org.openl.rules.project.migration;

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
}
