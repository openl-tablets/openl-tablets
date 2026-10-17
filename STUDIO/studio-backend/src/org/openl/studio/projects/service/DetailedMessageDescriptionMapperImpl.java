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
import org.openl.util.text.TextInfo;

@Service
@RequiredArgsConstructor
public class DetailedMessageDescriptionMapperImpl implements DetailedMessageDescriptionMapper {

    private static final Comparator<DetailedMessageDescription> BY_SEVERITY_AND_ID = Comparator
            .<DetailedMessageDescription, org.openl.message.Severity>comparing(m -> m.source().severity())
            .thenComparingLong(m -> m.source().id());

    @Override
    public List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model) {
        // A big project raises thousands of messages. Resolving each one against the model's tables and
        // modules independently rescanned every table twice per message; instead, index the tables and
        // modules once and look each message up against those indexes.
        var locator = new MessageLocator(model);
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

    /**
     * Resolves message locations against indexes built once per project, so a project raising thousands of
     * messages does not walk its tables again for each of them.
     */
    private static final class MessageLocator {

        /** Which table a message was raised in. */
        private final TablesByLocation tables;
        /** Where a table lives — a message can come from a module of a project this one depends on. */
        private final TableModules tableModules;

        MessageLocator(ProjectModel model) {
            tables = TablesByLocation.of(model);
            tableModules = TableModules.ofWorkspaceAddressed(model);
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
            if (where == null) {
                return null;
            }
            var line = lineOf(message);
            var path = line == null ? null : tableModules.fileOf(sourceLocation);
            return ModuleMessageSource.builder()
                    .name(where.module())
                    .projectId(where.projectId())
                    .project(where.projectName())
                    .path(path)
                    .line(path == null ? null : line)
                    .build();
        }

        /**
         * The line of a file a message points at, counted from one: a Markdown file that holds no table tells the
         * line its front matter breaks the format on.
         *
         * <p>Answers {@code null} for a message that points at no text.
         */
        private static @Nullable Integer lineOf(OpenLMessage message) {
            var error = message instanceof OpenLErrorMessage errorMessage ? errorMessage.getError() : null;
            if (error == null || error.getSourceCode() == null) {
                return null;
            }
            var where = error.getLocation();
            return where == null || !where.isTextLocation()
                    ? null
                    : where.getStart().getLine(new TextInfo(error.getSourceCode())) + 1;
        }

        /**
         * Where in the cell's own text the piece the message is about begins and ends.
         *
         * <p>The compiler reports the piece in the text it read the rule from, which is the cell's text for a rule
         * written in a cell — a Method table body included, which joins several cells. The screen already draws the
         * cell, so only the two positions cross the wire and the screen marks the piece itself.
         *
         * <p>Answers {@code null} where there is nothing to mark: a message about a table rather than about
         * something written in one of its cells, a blank rule or an empty piece, or a rule the cell does not spell
         * out, as the code a table generates from its cells.
         */
        private static @Nullable Marked markedIn(OpenLMessage message, TableSyntaxNode node, XlsUrlParser at) {
            var range = message.getSourceRange();
            var markable = range != null && !range.code().isBlank() && range.start() < range.end();
            var text = !markable || at.getCell() == null ? null : cellText(node, at.getCell());
            // A text read from somewhere else than the cell itself, as a value trimmed of its blanks, is found in it.
            var offset = text == null ? -1 : text.indexOf(range.code());
            return offset < 0 ? null : new Marked(offset + range.start(), offset + range.end());
        }

        /** The text a cell of the table holds, as the screen draws it. */
        private static @Nullable String cellText(TableSyntaxNode node, String address) {
            var reference = new CellReference(address);
            var cell = node.getGridTable().getGrid().getCell(reference.getCol(), reference.getRow());
            return cell == null ? null : cell.getStringValue();
        }
    }
}
