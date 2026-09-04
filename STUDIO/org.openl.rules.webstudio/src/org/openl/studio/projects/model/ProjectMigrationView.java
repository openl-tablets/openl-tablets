package org.openl.studio.projects.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What migrating a project to the current conventions would do, split per {@link MigrationScope}.
 *
 * <p>The {@code rulesXml} section covers the project descriptor: a project without a {@code rules.xml}
 * keeps its Excel workbooks in the project root, where the resolver treats each as a module. Writing a
 * {@code rules.xml} switches the resolver to the {@code rules/}/{@code tests/} default patterns, which no
 * longer match root workbooks, so migrating first moves them under {@code rules/}. A project that already
 * has a {@code rules.xml} is migrated by rewriting it to the minimal modern form.
 *
 * <p>The {@code rulesDeploy} section covers the deployment descriptor {@code rules-deploy.xml}, rewritten
 * to its minimal modern form when present.
 *
 * @param rulesXml    the rules.xml migration scope
 * @param rulesDeploy the rules-deploy.xml migration scope
 */
@Schema(description = "What migrating a project to the current conventions would do, per scope")
public record ProjectMigrationView(
        @Parameter(description = "The rules.xml migration scope")
        RulesXmlSection rulesXml,
        @Parameter(description = "The rules-deploy.xml migration scope")
        RulesDeploySection rulesDeploy) {

    /**
     * The {@code rules.xml} scope: the root workbooks a migrate would move, whether it applies, and the
     * workbooks that block it.
     *
     * @param movableRootModules the root-level workbooks a migrate would move into {@code rules/};
     *                           populated only when the project has no {@code rules.xml}
     * @param migratable         whether a migrate would change the {@code rules.xml} — move the root
     *                           workbooks and write one, or rewrite an existing one because a migration
     *                           changes what it declares; the formatting of the file does not count
     * @param newModules         the workbooks a rewrite would turn into modules that {@code rules.xml} does
     *                           not declare today; when non-empty the migrate is refused, because it would
     *                           change which modules compile
     */
    @Schema(description = "The rules.xml migration scope")
    public record RulesXmlSection(
            @Parameter(description = """
                    Root-level workbooks a migrate moves into rules/. Populated only when the project \
                    has no rules.xml""")
            List<String> movableRootModules,
            @Parameter(description = """
                    Whether a migrate would move the root workbooks and write a rules.xml, or rewrite \
                    an existing one. An existing rules.xml counts only when a migration changes what it \
                    declares; its formatting does not count.""")
            boolean migratable,
            @Parameter(description = """
                    Workbooks a rewrite would turn into modules that rules.xml does not declare today. \
                    When non-empty the rules.xml migrate is refused, because it would change which modules \
                    compile.""")
            List<String> newModules) {
    }

    /**
     * The {@code rules-deploy.xml} scope.
     *
     * @param migratable whether the project has a {@code rules-deploy.xml} that a migration changes; the
     *                   formatting of the file does not count
     */
    @Schema(description = "The rules-deploy.xml migration scope")
    public record RulesDeploySection(
            @Parameter(description = """
                    Whether the project has a rules-deploy.xml that a migrate would rewrite to the \
                    minimal modern form. It counts only when a migration changes what the file \
                    declares; its formatting does not count.""")
            boolean migratable) {
    }
}
