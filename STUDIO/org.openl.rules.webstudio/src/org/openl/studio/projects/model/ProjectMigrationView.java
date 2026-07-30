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
     * The {@code rules.xml} scope: the root workbooks a migrate would move and whether it applies.
     *
     * @param movableRootModules the root-level workbooks a migrate would move into {@code rules/};
     *                           populated only when the project has no {@code rules.xml}
     * @param migratable         whether a migrate would change the {@code rules.xml} — move the root
     *                           workbooks and write one, or rewrite an existing one
     */
    @Schema(description = "The rules.xml migration scope")
    public record RulesXmlSection(
            @Parameter(description = """
                    Root-level workbooks a migrate moves into rules/. Populated only when the project \
                    has no rules.xml""")
            List<String> movableRootModules,
            @Parameter(description = """
                    Whether a migrate would move the root workbooks and write a rules.xml, or rewrite \
                    an existing one""")
            boolean migratable) {
    }

    /**
     * The {@code rules-deploy.xml} scope.
     *
     * @param migratable whether the project has a {@code rules-deploy.xml} that a migrate would rewrite to
     *                   the minimal modern form
     */
    @Schema(description = "The rules-deploy.xml migration scope")
    public record RulesDeploySection(
            @Parameter(description = """
                    Whether the project has a rules-deploy.xml that a migrate would rewrite to the \
                    minimal modern form""")
            boolean migratable) {
    }
}
