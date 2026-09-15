package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellReference;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
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
import org.openl.studio.projects.service.tables.TablesByLocation;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

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

    /** The piece of a cell's text a message is about. */
    private record Marked(int start, int end) {
    }

    /** A rule a message was raised about, and the piece of it the compiler stopped on. */
    private record Rule(String code, int start, int end) {
    }

    /**
     * The rule the message was raised about, with the piece it is about pinned down.
     *
     * <p>A location that pins no text marks the whole rule, which is what a message about the rule as a whole
     * is about.
     */
    private static @Nullable Rule ruleOf(OpenLMessage message) {
        if (message instanceof OpenLErrorMessage errorMessage) {
            var error = errorMessage.getError();
            return error == null ? null : rule(error.getSourceCode(), error.getLocation());
        }
        if (message instanceof OpenLWarnMessage warnMessage) {
            var source = warnMessage.getSource();
            var module = source.getModule();
            return rule(module == null ? null : module.getCode(), source.getSourceLocation());
        }
        return null;
    }

    private static @Nullable Rule rule(@Nullable String code, @Nullable ILocation where) {
        if (code == null || code.isBlank()) {
            return null;
        }
        if (where == null || !where.isTextLocation()) {
            return new Rule(code, 0, code.length());
        }
        var text = new TextInfo(code);
        var from = where.getStart().getAbsolutePosition(text);
        var to = Math.min(where.getEnd().getAbsolutePosition(text) + 1, code.length());
        return from < 0 || to <= from ? null : new Rule(code, from, to);
    }

    /**
     * Resolves message locations against indexes built once per project, so a project raising thousands of
     * messages does not walk its tables again for each of them.
     */
    private static final class MessageLocator {

        /** Which table a message was raised in. */
        private final TablesByLocation tables;
        /** Where a table lives — a message can come from a module of a project this one depends on. */
        private final TableModules tableModules;

        MessageLocator(ProjectModel model, ProjectIdentifierMapper projectIdentifierMapper) {
            tables = TablesByLocation.of(model);
            tableModules = TableModules.ofWorkspace(model, projectIdentifierMapper);
        }

        MessageSource resolve(OpenLMessage message) {
            var sourceLocation = message.getSourceLocation();
            if (sourceLocation == null) {
                return null;
            }
            var location = new XlsUrlParser(sourceLocation);
            var where = tableModules.locationOf(sourceLocation);
            var node = tables.find(location);
            if (node != null) {
                var tableName = new TableSyntaxNodeAdapter(node).getDisplayName();
                var marked = markedIn(message, node, location);
                return TableMessageSource.builder()
                        .id(node.getId())
                        .name(tableName)
                        .module(where == null ? null : where.module())
                        .projectId(where == null ? null : where.projectId())
                        .project(where == null ? null : where.projectName())
                        .cell(location.getCell())
                        .start(marked == null ? null : marked.start())
                        .end(marked == null ? null : marked.end())
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

        /**
         * Where in the cell's own text the piece the message is about begins and ends.
         *
         * <p>The compiler reports where in the rule it stopped; the rule is written in a cell the screen already
         * draws, so only the two positions cross the wire and the screen marks the piece itself.
         *
         * <p>Answers {@code null} where there is nothing to mark: a message about a table rather than about
         * something written in one of its cells, or a rule the cell it was read from no longer spells out.
         */
        private static @Nullable Marked markedIn(OpenLMessage message, TableSyntaxNode node, XlsUrlParser at) {
            var rule = ruleOf(message);
            if (rule == null || at.getCell() == null) {
                return null;
            }
            var text = cellText(node, at.getCell());
            // The rule is the cell's text without what the workbook writes around it — the '=' a spreadsheet
            // step begins with, say. Where it is not the cell's text at all, there is nothing to mark.
            var offset = text == null ? -1 : text.indexOf(rule.code());
            return offset < 0 ? null : new Marked(offset + rule.start(), offset + rule.end());
        }

        /** The text a cell of the table holds, as the screen draws it. */
        private static @Nullable String cellText(TableSyntaxNode node, String address) {
            var reference = new CellReference(address);
            var cell = node.getGridTable().getGrid().getCell(reference.getCol(), reference.getRow());
            return cell == null ? null : cell.getStringValue();
        }

    }
}
