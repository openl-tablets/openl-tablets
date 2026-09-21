package org.openl.studio.projects.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.excel.parser.ExcelUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.constraints.RegexpValueConstraint;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.PropertyDefinitionView;
import org.openl.studio.projects.model.PropertyValueView;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

/**
 * Describes what a project holds: the properties its tables may carry, and the worksheets of one of its modules.
 *
 * @author Yury Molchan
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectMetadataService {

    private static final Comparator<PropertyDefinitionView> BY_NAME = Comparator
            .comparing(PropertyDefinitionView::name, String.CASE_INSENSITIVE_ORDER);

    /**
     * Every property of the dictionary.
     *
     * <p>What a table may carry however it comes by it — written on the table, inherited from a Properties table or
     * stamped by OpenL Studio — so a search across tables of every kind may narrow by any of them.
     */
    private static final List<PropertyDefinitionView> ALL = dictionary(
            Arrays.stream(DefaultPropertyDefinitions.getDefaultDefinitions()));

    /** The properties each kind of table may declare, resolved once per kind: the definitions never change. */
    private static final Map<String, List<PropertyDefinitionView>> BY_TABLE_TYPE = new ConcurrentHashMap<>();

    private final ProjectFilesService projectFilesService;
    private final ProjectFileRootFactory projectFileRootFactory;

    /**
     * Properties a kind of table may declare, or every property there is.
     *
     * <p>With a table type, these are the properties a table of that kind may declare — a Properties table for the
     * tables of its scope, every other kind on itself. Without one, these are every property a table may carry,
     * however it comes by it.
     *
     * @param tableType public table kind, or {@code null} for every property of the dictionary
     */
    public List<PropertyDefinitionView> getProperties(@Nullable String tableType) {
        if (StringUtils.isBlank(tableType)) {
            return ALL;
        }
        var internalType = OpenLTableUtils.getTableTypeItems().inverse().get(tableType);
        if (internalType == null) {
            throw new BadRequestException("project.properties.table-type.message", new Object[]{tableType});
        }
        return BY_TABLE_TYPE.computeIfAbsent(internalType, type -> dictionary(declaredAt(type).stream()
                .flatMap(level -> Arrays.stream(
                        TablePropertyDefinitionUtils.getDefaultDefinitionsForTable(type, level, true)))));
    }

    /**
     * The levels a kind of table declares its properties at.
     *
     * <p>A Properties table carries no properties of its own: what it declares applies to the tables of its Global,
     * Module or Category scope. Every other kind declares its properties on itself. A system property, which
     * OpenL Studio stamps rather than the author typing it, is not among what a kind may declare.
     */
    private static List<InheritanceLevel> declaredAt(String internalType) {
        return XlsNodeTypes.XLS_PROPERTIES.toString().equals(internalType)
                ? List.of(InheritanceLevel.GLOBAL, InheritanceLevel.MODULE, InheritanceLevel.CATEGORY)
                : List.of(InheritanceLevel.TABLE);
    }

    /** The definitions as a dialog offers them: each once, by name, the deprecated ones left out. */
    private static List<PropertyDefinitionView> dictionary(Stream<TablePropertyDefinition> definitions) {
        return definitions.filter(definition -> definition.getDeprecation() == null)
                .map(ProjectMetadataService::describe)
                .distinct()
                .sorted(BY_NAME)
                .toList();
    }

    /**
     * Worksheets of a module, read from the module's workbook itself.
     *
     * <p>The file is read rather than the compiled module, so the sheets of a module that does not compile are
     * listed too, and listing them costs no compilation.
     */
    public List<String> getSheets(RulesProject project, String modulePath) {
        var resource = projectFilesService.getResource(projectFileRootFactory.of(project), modulePath, null);
        ExcelUtils.configureZipBombDetection();
        try (var content = resource.getContent(); var workbook = WorkbookFactory.create(content)) {
            return IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName).toList();
        } catch (IOException | ProjectException | RuntimeException e) {
            log.warn("Cannot read the workbook of module '{}'.", modulePath, e);
            throw new BadRequestException("project.module.workbook.message", new Object[]{modulePath});
        }
    }

    /**
     * How the property is presented and what a value of it looks like.
     *
     * <p>The property is described the way the Table Details editor names it — its display name and its group — so
     * a dialog offering it reads the same as the rest of OpenL Studio, and the dimensional ones can be told apart
     * from the rest.
     *
     * <p>An array of an enum is the same choice offered several times over: a dimension property such as
     * {@code state} holds a comma-separated list of the values its enum names.
     */
    private static PropertyDefinitionView describe(TablePropertyDefinition definition) {
        var type = definition.getType() == null ? null : definition.getType().getInstanceClass();
        var multiple = type != null && type.isArray();
        var element = multiple ? type.getComponentType() : type;
        if (element != null && element.isEnum()) {
            var codes = EnumUtils.getNames(element);
            var displayValues = EnumUtils.getValues(element);
            return describe(definition, "enum", multiple, IntStream.range(0, codes.length)
                    .mapToObj(index -> new PropertyValueView(codes[index], displayValues[index]))
                    .toList());
        }
        return describe(definition, element == null ? "text" : scalarType(element), multiple, List.of());
    }

    private static PropertyDefinitionView describe(TablePropertyDefinition definition, String type, boolean multiple,
            List<PropertyValueView> values) {
        return new PropertyDefinitionView(definition.getName(),
                definition.getDisplayName(),
                definition.getGroup(),
                type,
                multiple,
                definition.isDimensional(),
                definition.getDefaultValue(),
                pattern(definition),
                values);
    }

    /**
     * The regular expression a value of the property must match, or {@code null} when it states none.
     *
     * <p>It is the same expression the compiler validates the property with, so a dialog refusing a value refuses
     * exactly what the module would refuse.
     */
    private static @Nullable String pattern(TablePropertyDefinition definition) {
        var constraints = definition.getConstraints();
        if (constraints == null) {
            return null;
        }
        return constraints.getAll()
                .stream()
                .filter(RegexpValueConstraint.class::isInstance)
                .map(constraint -> ((RegexpValueConstraint) constraint).getRegexp())
                .findFirst()
                .orElse(null);
    }

    /** What a value that is not one of a list looks like. */
    private static String scalarType(Class<?> element) {
        if (Date.class == element) {
            return "date";
        }
        return Boolean.class == element ? "boolean" : "text";
    }
}
