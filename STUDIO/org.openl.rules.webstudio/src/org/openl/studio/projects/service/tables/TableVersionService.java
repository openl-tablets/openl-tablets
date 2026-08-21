package org.openl.studio.projects.service.tables;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.studio.projects.model.tables.TableVersionsView;

/**
 * The versions of a table: which tables count as versions of one another, which version each stands for, and which
 * version is free for the next one.
 *
 * <p>Tables of one name that answer the same requests are versions of one another, ordered by three numbers — major,
 * minor and variant. Only one of them is active at a time, so a new version has to carry a number none of them
 * carries.
 *
 * @author Vladyslav Pikus
 */
@Service
public class TableVersionService {

    static final String VERSION_PROPERTY = "version";
    static final String ACTIVE_PROPERTY = "active";

    /**
     * The version a table stands for while it declares none.
     *
     * <p>The wizard this dialog replaced stamped that version on a table it versioned for the first time, and
     * offered the next one to the copy, so a first copy of an unversioned table is still offered {@code 0.0.2}.
     */
    static final String INITIAL_VERSION = "0.0.1";

    /**
     * Major, minor and variant: the form the engine orders versions by.
     *
     * <p>Each number is bounded, because a version is ordered by the numbers it is read as: one longer than this
     * cannot be read as a number at all, and the next version after it could not be counted.
     */
    static final Pattern VERSION_FORMAT = Pattern.compile("(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})");

    /** The version the table stands for: the one it declares, or the initial one while it declares none. */
    public String currentVersion(IOpenLTable table) {
        return currentVersion(table.getProperties());
    }

    /**
     * The versions of the table as the copy dialog needs them, or {@code null} for a table that carries none.
     *
     * <p>The group is the table's own: the versions listed are those of the tables answering the requests it answers
     * today, so an author who leaves the dimension properties alone is offered a version that is free.
     *
     * @param table        the table a new version would be made from
     * @param moduleTables the tables compiled together with it
     */
    public @Nullable TableVersionsView describe(IOpenLTable table, TableSyntaxNode[] moduleTables) {
        if (!table.isVersionable()) {
            return null;
        }
        var current = currentVersion(table);
        var taken = taken(table.getName(), dimensionsOf(table.getProperties()), moduleTables);
        taken.add(current);
        return new TableVersionsView(current, next(current, taken), List.copyOf(taken));
    }

    /**
     * The versions already carried by the tables of one name that answer the same requests.
     *
     * <p>A table that declares no version stands for the initial one, so it takes that number just as a table
     * declaring it does.
     *
     * @param name         the name the versions share
     * @param dimensions   the dimension values the group dispatches on
     * @param moduleTables the tables compiled together
     */
    public Set<String> taken(String name, Map<String, Object> dimensions, TableSyntaxNode[] moduleTables) {
        var versions = new LinkedHashSet<String>();
        for (TableSyntaxNode node : moduleTables) {
            if (isVersionOf(node, name, dimensions)) {
                versions.add(currentVersion(node.getTableProperties()));
            }
        }
        return versions;
    }

    /**
     * The first free version after the current one, raised by its variant number.
     *
     * <p>A current version the engine cannot read is counted as none at all, so the next one is offered written the
     * way the engine reads them.
     */
    static String next(String current, Set<String> taken) {
        var matcher = VERSION_FORMAT.matcher(current);
        boolean readable = matcher.matches();
        int major = readable ? Integer.parseInt(matcher.group(1)) : 0;
        int minor = readable ? Integer.parseInt(matcher.group(2)) : 0;
        int variant = readable ? Integer.parseInt(matcher.group(3)) : 0;
        String candidate;
        do {
            variant++;
            candidate = "%d.%d.%d".formatted(major, minor, variant);
        } while (taken.contains(candidate));
        return candidate;
    }

    /**
     * Whether a table answers the same requests as the given dimension values.
     *
     * <p>Dimension properties are what the engine dispatches on, so a table declaring another set of them stands
     * beside the other rather than being another version of it. They are compared the way the engine compares them
     * when it dispatches.
     *
     * <p>Only what the table declares itself is compared: what it inherits from its module or category, the other
     * inherits alike, so it never tells two tables of one module apart.
     */
    public boolean sameGroup(@Nullable ITableProperties properties, Map<String, Object> dimensions) {
        return DimensionPropertiesMethodKey.compareMethodDimensionProperties(dimensionsOf(properties), dimensions);
    }

    /** The dimension values a table declares itself. */
    private Map<String, Object> dimensionsOf(@Nullable ITableProperties properties) {
        return declaredDimensions(properties == null ? Map.of() : properties.getTableProperties());
    }

    /**
     * The dimension values among the ones a request declares, as the engine keeps them.
     *
     * <p>A table's own values are the ones the engine kept — several of them ordered, a date closing a period
     * moved to the end of its day — so a declared value is kept the same way before the two are compared.
     */
    public Map<String, Object> declaredDimensions(Map<String, Object> declared) {
        var values = new HashMap<String, Object>();
        for (var name : TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames()) {
            var value = declared.get(name);
            if (value != null) {
                values.put(name, TableProperties.preprocess(name, value));
            }
        }
        return values;
    }

    /** Whether the node is a version of the named table answering the same requests. */
    private boolean isVersionOf(TableSyntaxNode node, String name, Map<String, Object> dimensions) {
        var member = node.getMember();
        if (member == null || !name.equals(member.getName()) || node.getTableProperties() == null) {
            return false;
        }
        return sameGroup(node.getTableProperties(), dimensions);
    }

    private static String currentVersion(@Nullable ITableProperties properties) {
        var version = properties == null ? null : properties.getVersion();
        return version == null ? INITIAL_VERSION : version;
    }
}
