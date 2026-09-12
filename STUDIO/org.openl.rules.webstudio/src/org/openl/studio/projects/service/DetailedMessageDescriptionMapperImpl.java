package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessageSource;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;
import org.openl.studio.projects.service.tables.TableModules;

@Service
@RequiredArgsConstructor
public class DetailedMessageDescriptionMapperImpl implements DetailedMessageDescriptionMapper {

    private final ProjectIdentifierMapper projectIdentifierMapper;

    private static final Comparator<DetailedMessageDescription> BY_SEVERITY_AND_ID = Comparator
            .<DetailedMessageDescription, org.openl.message.Severity>comparing(m -> m.source().severity())
            .thenComparingLong(m -> m.source().id());

    @Override
    public List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model) {
        // A big project raises thousands of messages. Resolving each one against the model's tables and
        // modules independently rescanned every table twice per message; instead, index the tables and
        // modules once and look each message up against those indexes.
        var locator = new MessageLocator(model, projectIdentifierMapper);
        return messages.stream()
                .map(message -> map(message, locator))
                .sorted(BY_SEVERITY_AND_ID)
                .toList();
    }

    private static DetailedMessageDescription map(OpenLMessage message, MessageLocator locator) {
        var source = MessageDescription.builder()
                .id(message.getId())
                .summary(message.getSummary())
                .severity(message.getSeverity())
                .build();
        return DetailedMessageDescription.builder()
                .source(source)
                .location(locator.resolve(message))
                .stacktrace(hasStacktrace(message) ? Boolean.TRUE : null)
                .build();
    }

    private static boolean hasStacktrace(OpenLMessage message) {
        return message instanceof OpenLErrorMessage errorMessage && errorMessage.getError() != null;
    }

    /**
     * Resolves message locations against indexes built once per project. Two tables can only overlap
     * when they live on the same worksheet, so bucketing tables by workbook and sheet lets each message
     * intersect-test only its own sheet's tables instead of every table in the workspace. The matching
     * node is used directly, avoiding a second lookup by id.
     */
    private static final class MessageLocator {

        private record TableEntry(TableSyntaxNode node, XlsUrlParser location) {
        }

        private final Map<String, List<TableEntry>> tablesBySheet;
        /** Where a table lives — a message can come from a module of a project this one depends on. */
        private final TableModules tableModules;

        MessageLocator(ProjectModel model, ProjectIdentifierMapper projectIdentifierMapper) {
            tablesBySheet = indexTables(model);
            tableModules = TableModules.ofWorkspace(model, projectIdentifierMapper);
        }

        MessageSource resolve(OpenLMessage message) {
            var sourceLocation = message.getSourceLocation();
            if (sourceLocation == null) {
                return null;
            }
            var location = new XlsUrlParser(sourceLocation);
            var where = tableModules.locationOf(sourceLocation);
            var node = findNode(location);
            if (node != null) {
                var tableName = new TableSyntaxNodeAdapter(node).getDisplayName();
                return TableMessageSource.builder()
                        .id(node.getId())
                        .name(tableName)
                        .module(where == null ? null : where.module())
                        .projectId(where == null ? null : where.projectId())
                        .project(where == null ? null : where.projectName())
                        .cell(location.getCell())
                        .build();
            }
            return where != null
                    ? ModuleMessageSource.builder()
                            .name(where.module())
                            .projectId(where.projectId())
                            .project(where.projectName())
                            .build()
                    : null;
        }

        private TableSyntaxNode findNode(XlsUrlParser location) {
            var candidates = tablesBySheet.get(sheetKey(location));
            if (candidates == null) {
                return null;
            }
            for (TableEntry candidate : candidates) {
                if (location.intersects(candidate.location())) {
                    return candidate.node();
                }
            }
            return null;
        }

        private static Map<String, List<TableEntry>> indexTables(ProjectModel model) {
            var index = new HashMap<String, List<TableEntry>>();
            for (TableSyntaxNode node : model.getAllTableSyntaxNodes()) {
                var location = node.getUriParser();
                if (location != null) {
                    index.computeIfAbsent(sheetKey(location), key -> new ArrayList<>()).add(new TableEntry(node, location));
                }
            }
            return index;
        }

        private static String sheetKey(XlsUrlParser location) {
            return location.getWbPath() + '\n' + location.getWbName() + '\n' + location.getWsName();
        }
    }
}
