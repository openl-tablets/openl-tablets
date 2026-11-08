package org.openl.studio.projects.service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellReference;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.FilePlace;
import org.openl.studio.projects.model.project.status.LocalizationFileMessageSource;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.model.project.status.MessageSource;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;
import org.openl.studio.projects.model.project.status.TextSpot;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.TablesByLocation;

@Service
@RequiredArgsConstructor
public class DetailedMessageDescriptionMapperImpl implements DetailedMessageDescriptionMapper {

    private static final Comparator<DetailedMessageDescription> BY_SEVERITY_AND_ID = Comparator
            .<DetailedMessageDescription, Severity>comparing(m -> m.source().severity())
            .thenComparingLong(m -> m.source().id());

    @Override
    public List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model) {
        // A big project raises thousands of messages. Resolving each one against the model's tables and
        // modules independently rescanned every table twice per message; instead, index the tables and
        // modules once and look each message up against those indexes.
        var module = model.getModuleInfo();
        var projectFolder = module == null || module.getProject() == null
                ? null
                : module.getProject().getProjectFolder();
        return mapSorted(messages, new MessageLocator(model, projectFolder, MessagePositions.FILE));
    }

    @Override
    public List<DetailedMessageDescription> mapFileMessages(Collection<OpenLMessage> messages,
                                                            Path projectFolder,
                                                            MessagePositions positions) {
        return mapSorted(messages, new MessageLocator(null, projectFolder, positions));
    }

    private static List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages,
                                                              MessageLocator locator) {
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

        /** The compiled project a message about a table is looked up in, none for the messages about files. */
        private final @Nullable ProjectModel model;
        /**
         * Which table a message was raised in. Indexed only when a message is about a table: the messages about
         * the files of the project need no index, and are mapped while the project is still compiling.
         */
        @Getter(value = AccessLevel.PRIVATE, lazy = true)
        private final TablesByLocation tables = TablesByLocation.of(Objects.requireNonNull(model));
        /** Where a table lives — a message can come from a module of a project this one depends on. */
        @Getter(value = AccessLevel.PRIVATE, lazy = true)
        private final TableModules tableModules = TableModules.ofWorkspaceAddressed(Objects.requireNonNull(model));
        /** The folder of the project being compiled, which the path of a file it raised a message about is told in. */
        private final @Nullable Path projectFolder;
        /** What the places of a message about a file are counted from. */
        private final MessagePositions positions;

        MessageLocator(@Nullable ProjectModel model, @Nullable Path projectFolder, MessagePositions positions) {
            this.model = model;
            this.projectFolder = projectFolder;
            this.positions = positions;
        }

        MessageSource resolve(OpenLMessage message) {
            var sourceLocation = message.getSourceLocation();
            if (sourceLocation == null) {
                return null;
            }
            if (message instanceof OpenLFileMessage fileMessage) {
                return fileSource(fileMessage);
            }
            var location = new XlsUrlParser(sourceLocation);
            var where = getTableModules().locationOf(sourceLocation);
            var node = getTables().find(location);
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
         * The line of a file a message is about, with the key and the language it names, and the piece of the
         * text of the key it is about, when it is about one.
         *
         * <p>The path is told from the project folder, so a screen can open the file of the project; a file
         * outside the folder, or in an archive, is named by its address alone.
         */
        private LocalizationFileMessageSource fileSource(OpenLFileMessage message) {
            var spots = message.getPieces()
                    .stream()
                    .map(piece -> new TextSpot(piece.start(), piece.end()))
                    .toList();
            var places = message.getPlaces()
                    .stream()
                    .map(place -> new FilePlace(place.line(), place.column(), place.endLine(), place.endColumn()))
                    .toList();
            return LocalizationFileMessageSource.builder()
                    .path(pathOf(message.getFile()))
                    .line(message.getLine())
                    .key(message.getKey())
                    .language(message.getLanguage())
                    .spots(positions == MessagePositions.TEXT && !spots.isEmpty() ? spots : null)
                    .places(positions == MessagePositions.FILE && !places.isEmpty() ? places : null)
                    .build();
        }

        private String pathOf(Path file) {
            var absolute = file.toAbsolutePath().normalize();
            var folder = projectFolder == null ? null : projectFolder.toAbsolutePath().normalize();
            if (folder == null || !absolute.startsWith(folder)) {
                return file.toUri().toString();
            }
            return folder.relativize(absolute)
                    .toString()
                    .replace('\\', '/');
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
