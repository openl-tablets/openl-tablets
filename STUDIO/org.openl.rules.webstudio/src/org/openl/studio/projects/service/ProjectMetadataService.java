package org.openl.studio.projects.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    /**
     * Properties a Properties table may declare, resolved once because the set never changes.
     *
     * <p>A Properties table applies at Global, Module or Category scope, so a property restricted to the Table level
     * is left out, and so is a system property, which OpenL Studio stamps rather than the author typing it.
     */
    private static final List<PropertyDefinitionView> PROPERTIES = Stream
            .of(InheritanceLevel.GLOBAL, InheritanceLevel.MODULE, InheritanceLevel.CATEGORY)
            .flatMap(level -> Arrays.stream(TablePropertyDefinitionUtils.getDefaultDefinitionsForTable(
                    XlsNodeTypes.XLS_PROPERTIES.toString(), level, true)))
            .filter(definition -> definition.getDeprecation() == null)
            .map(ProjectMetadataService::describe)
            .distinct()
            .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name()))
            .toList();

    private final ProjectFilesService projectFilesService;
    private final ProjectFileRootFactory projectFileRootFactory;

    /**
     * Properties applicable to one place in a workbook.
     *
     * <p>Without a table type, these are the properties a Properties table may declare at Global, Module or Category
     * scope. With a table type, these are the properties the table itself may declare at Table scope.
     *
     * @param tableType public table kind, or {@code null} for the contents of a Properties table
     */
    public List<PropertyDefinitionView> getProperties(@Nullable String tableType) {
        if (StringUtils.isBlank(tableType)) {
            return PROPERTIES;
        }
        var internalType = OpenLTableUtils.getTableTypeItems().inverse().get(tableType);
        if (internalType == null) {
            throw new BadRequestException("project.properties.table-type.message", new Object[]{tableType});
        }
        return Arrays.stream(TablePropertyDefinitionUtils.getDefaultDefinitionsForTable(
                        internalType, InheritanceLevel.TABLE, true))
                .filter(definition -> definition.getDeprecation() == null)
                .map(ProjectMetadataService::describe)
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name()))
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
     * What a value of the property looks like.
     *
     * <p>An array of an enum is the same choice offered several times over: a dimension property such as
     * {@code state} holds a comma-separated list of the values its enum names.
     */
    private static PropertyDefinitionView describe(TablePropertyDefinition definition) {
        var type = definition.getType() == null ? null : definition.getType().getInstanceClass();
        if (type == null) {
            return new PropertyDefinitionView(definition.getName(), "text", false, List.of());
        }
        var element = type.isArray() ? type.getComponentType() : type;
        if (element.isEnum()) {
            var codes = EnumUtils.getNames(element);
            var values = EnumUtils.getValues(element);
            return new PropertyDefinitionView(definition.getName(),
                    "enum",
                    type.isArray(),
                    IntStream.range(0, codes.length)
                            .mapToObj(index -> new PropertyValueView(codes[index], values[index]))
                            .toList());
        }
        return new PropertyDefinitionView(
                definition.getName(),
                scalarType(element),
                type.isArray(),
                List.of());
    }

    /** What a value that is not one of a list looks like. */
    private static String scalarType(Class<?> element) {
        if (Date.class == element) {
            return "date";
        }
        return Boolean.class == element ? "boolean" : "text";
    }
}
