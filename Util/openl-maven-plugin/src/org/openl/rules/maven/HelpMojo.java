package org.openl.rules.maven;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Displays help information about openl-maven-plugin and lists its goals.
 * <p>
 * Hand-written replacement for the goal that {@code maven-plugin-plugin:helpmojo} would otherwise
 * generate, so the help page can carry an OpenL-specific overview and tips. Usage:
 * <ul>
 *     <li>{@code mvn openl:help} — overview plus every goal</li>
 *     <li>{@code mvn openl:help -Dgoal=migrate} — a single goal</li>
 *     <li>{@code mvn openl:help -Ddetail} — also list each goal's parameters</li>
 * </ul>
 *
 * @author Yury Molchan
 * @since 6.1.0
 */
@Mojo(name = "help", requiresProject = false, threadSafe = true)
public final class HelpMojo extends AbstractMojo {

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor plugin;

    /**
     * Show help for this single goal only (e.g. {@code -Dgoal=migrate}). Unset means "every goal".
     */
    @Parameter(property = "goal")
    private String goal;

    /**
     * When {@code true}, also list the parameters of each goal shown.
     */
    @Parameter(property = "detail", defaultValue = "false")
    private boolean detail;

    @Override
    public void execute() {
        var nl = System.lineSeparator();
        var sb = new StringBuilder(nl);

        sb.append(plugin.getName()).append(' ').append(plugin.getVersion()).append(nl).append(nl);

        // ------------------------------------------------------------------------------------------
        // OpenL-specific overview. Edit this block to change the prose shown by `mvn openl:help`.
        // ------------------------------------------------------------------------------------------
        sb.append("""
                  Maven plugin for compiling, testing, packaging, verifying and migrating
                  OpenL Tablets rule projects. The goals act only on OpenL modules — those that
                  declare openl-maven-plugin in their build or carry a rules.xml; any other module
                  in the reactor is skipped.

                """);

        // The 'help' goal itself is plumbing — never list it.
        var mojos = plugin.getMojos().stream()
                .filter(m -> !"help".equals(m.getGoal()))
                .sorted(Comparator.comparing(MojoDescriptor::getGoal))
                .toList();

        if (goal != null && !goal.isBlank()) {
            var selected = mojos.stream().filter(m -> goal.equals(m.getGoal())).findFirst().orElse(null);
            if (selected == null) {
                sb.append("Unknown goal '").append(goal).append("'. Available goals: ");
                sb.append(String.join(", ", mojos.stream().map(MojoDescriptor::getGoal).toList()));
                sb.append(nl);
            } else {
                writeGoal(sb, selected, nl);
            }
        } else {
            sb.append("This plugin has ").append(mojos.size())
                    .append(mojos.size() == 1 ? " goal:" : " goals:").append(nl).append(nl);
            mojos.forEach(m -> writeGoal(sb, m, nl));
        }

        // ------------------------------------------------------------------------------------------
        // Footer tips.
        // ------------------------------------------------------------------------------------------
        sb.append("""
                Run `mvn openl:help -Dgoal=<goal> -Ddetail` to see the parameters of a single goal.

                """);

        getLog().info(sb.toString());
    }

    private void writeGoal(StringBuilder sb, MojoDescriptor mojo, String nl) {
        sb.append("  ").append(mojo.getFullGoalName()).append(nl);
        wrapText(sb, mojo.getDescription(), "    ", nl);
        if (detail && mojo.getParameters() != null) {
            var editable = mojo.getParameters().stream()
                    .filter(org.apache.maven.plugin.descriptor.Parameter::isEditable)
                    .toList();
            if (!editable.isEmpty()) {
                sb.append(nl).append("    Available parameters:").append(nl);
                editable.forEach(p -> writeParameter(sb, p, nl));
            }
        }
        sb.append(nl);
    }

    private static void writeParameter(StringBuilder sb, org.apache.maven.plugin.descriptor.Parameter p,
                                       String nl) {
        sb.append(nl).append("      ").append(p.getName());
        var defaultValue = p.getDefaultValue();
        if (defaultValue != null && !defaultValue.isBlank()) {
            sb.append(" (Default: ").append(defaultValue).append(')');
        }
        sb.append(nl);
        wrapText(sb, p.getDescription(), "      ", nl);
        if (p.isRequired()) {
            sb.append("        Required: Yes").append(nl);
        }
        var userProperty = userProperty(p);
        if (userProperty != null) {
            sb.append("        User property: ").append(userProperty).append(nl);
        }
        if ("migrators".equals(p.getName())) {
            writeMigrateCatalogue(sb, nl);
        }
    }

    /**
     * The migrator catalogue body: for each migrator (alphabetical) an "{@code  <id>  —  <commitMessage>}"
     * header followed by its description and a blank separator line. Shared with {@link HelpMojo}, which
     * inlines it under the {@code migrate} goal.
     * <p>
     * Each migrator authors its description as a text block — line breaks AND the leading six-space indent
     * are baked in, so {@code String#lines()} preserves the full layout and no extra padding is added here.
     */
    private static void writeMigrateCatalogue(StringBuilder sb, String nl) {
        sb.append("""

                    Available migrators (select with -Dopenl.migrate.migrators=<id>):

                """);
        for (var m : MigrateMojo.allMigratorsAlphabetical()) {
            sb.append("      ").append(m.getId()).append("  —  ").append(m.getCommitMessage()).append(nl);
            m.getDescription().lines().forEach(line -> sb.append("        ").append(line).append(nl));
            sb.append(nl);
        }
    }

    private static String userProperty(org.apache.maven.plugin.descriptor.Parameter p) {
        var expression = p.getExpression();
        if (expression != null && expression.startsWith("${") && expression.endsWith("}")) {
            return expression.substring(2, expression.length() - 1);
        }
        return null;
    }

    private static void wrapText(StringBuilder sb, String description, String prefix, String nl) {
        if (StringUtils.isNotBlank(description)) {
            description.lines()
                    // Column width descriptions are word-wrapped to before per-context indentation is applied.
                    .flatMap(l -> WordUtils.wrap(l, 90).lines())
                    .forEach(l -> sb.append(prefix).append(l).append(nl));
        }
    }

}
