package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.ConflictSheetStatus;
import org.openl.studio.projects.model.merge.ConflictSheetView;
import org.openl.studio.projects.model.merge.ConflictTableReason;
import org.openl.studio.projects.model.merge.ConflictTextBlock;
import org.openl.studio.projects.model.merge.ConflictTextBlockKind;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeSummaryView;
import org.openl.studio.projects.model.merge.MergedChanges;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.studio.projects.service.tables.read.RawTableReader;

class ProjectsMergeConflictsServiceImplTest {

    private static final String FIRST_FILE = "rules/DeletedOurs.xlsx";
    private static final String SECOND_FILE = "rules/DeletedTheirs.xlsx";
    /** The commits of the conflict: the base, yours and theirs. */
    private static final List<String> COMMITS = List.of("base", "ours", "theirs");

    @Test
    void reportsAvailabilityForEveryConflictedFile() throws Exception {
        var context = createContext();
        var repository = context.repository();
        when(repository.checkHistory(SECOND_FILE, "ours")).thenReturn(file(SECOND_FILE));
        when(repository.checkHistory(FIRST_FILE, "theirs")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(FIRST_FILE, "base")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(SECOND_FILE, "base")).thenReturn(file(SECOND_FILE));

        var result = context.service().getConflictDetails(context.conflict());

        assertTrue(result.oursRevision().exists());
        assertTrue(result.theirsRevision().exists());
        assertFalse(result.fileAvailability().get(FIRST_FILE).ours());
        assertTrue(result.fileAvailability().get(FIRST_FILE).theirs());
        assertTrue(result.fileAvailability().get(FIRST_FILE).base());
        assertTrue(result.fileAvailability().get(SECOND_FILE).ours());
        assertFalse(result.fileAvailability().get(SECOND_FILE).theirs());
        assertTrue(result.fileAvailability().get(SECOND_FILE).base());
        // A workbook is no text, whichever versions hold it.
        assertFalse(result.fileAvailability().get(FIRST_FILE).text());
        assertFalse(result.fileAvailability().get(SECOND_FILE).text());
    }

    @Test
    void tellsWhichConflictedFilesAreText() throws Exception {
        var notes = "rules/notes.txt";
        var latin1 = "rules/latin1.properties";
        var removed = "rules/removed.txt";
        var library = "lib/rates.bin";
        var context = createContext(mock(MergeChangesService.class), "base", notes, latin1, removed, library,
                FIRST_FILE);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");
        givenTextVersions(repository, library, "rate\u0000table");
        for (var commit : COMMITS) {
            when(repository.checkHistory(latin1, commit)).thenReturn(file(latin1));
            givenVersion(repository, latin1, commit, "Pr\u00e4mie=" + commit + "\n", StandardCharsets.ISO_8859_1);
        }
        // Their branch deleted the file: the versions that hold it are text all the same.
        givenTextVersions(repository, removed, "a");
        when(repository.checkHistory(removed, "theirs")).thenReturn(null);
        when(repository.readHistory(removed, "theirs")).thenReturn(null);

        var result = context.service().getConflictDetails(context.conflict());

        assertTrue(result.fileAvailability().get(notes).text());
        // A merge of lines reads UTF-8 only; the screen compares such a file line by line all the same.
        assertFalse(result.fileAvailability().get(latin1).text());
        assertTrue(result.fileAvailability().get(removed).text());
        assertFalse(result.fileAvailability().get(removed).theirs());
        assertFalse(result.fileAvailability().get(library).text());
        // A workbook is told by its name: its versions are not read.
        assertFalse(result.fileAvailability().get(FIRST_FILE).text());
        verify(repository, never()).readHistory(eq(FIRST_FILE), anyString());
    }

    @Test
    void readsEachConflictedFileOnceWhileTheConflictLasts() throws Exception {
        var notes = "rules/notes.txt";
        var context = createContext(mock(MergeChangesService.class), "base", notes);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");

        context.service().getConflictDetails(context.conflict());
        var again = context.service().getConflictDetails(context.conflict());

        assertTrue(again.fileAvailability().get(notes).text());
        for (var commit : COMMITS) {
            verify(repository, times(1)).readHistory(notes, commit);
        }
    }

    @Test
    void tellsAFileWithAVersionThatCannotBeReadAsNoTextUntilItIsRead() throws Exception {
        var notes = "rules/notes.txt";
        var other = "rules/other.txt";
        var context = createContext(mock(MergeChangesService.class), "base", notes, other);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");
        givenTextVersions(repository, other, "b");
        when(repository.readHistory(notes, "theirs"))
                .thenThrow(new IOException("The LFS object is unavailable"))
                .thenAnswer(invocation -> new FileItem(notes,
                        new ByteArrayInputStream("a=theirs\n".getBytes(StandardCharsets.UTF_8))));

        var result = context.service().getConflictDetails(context.conflict());

        // The other resolutions of the file stay offered, and the rest of the conflict is shown as it is.
        assertFalse(result.fileAvailability().get(notes).text());
        assertTrue(result.fileAvailability().get(notes).theirs());
        assertTrue(result.fileAvailability().get(other).text());
        // The failure is not kept: the file is read again the next time, and is text once its versions are read.
        assertTrue(context.service().getConflictDetails(context.conflict()).fileAvailability().get(notes).text());
    }

    @Test
    void propagatesRepositoryFailureInsteadOfReportingDeletedFiles() throws Exception {
        var context = createContext();
        when(context.repository().checkHistory(anyString(), anyString()))
                .thenThrow(new IOException("Repository is unavailable"));

        assertThrows(IOException.class, () -> context.service().getConflictDetails(context.conflict()));
    }

    @Test
    void rejectsDownloadWhenFileDoesNotExistInRequestedRevision() throws Exception {
        var context = createContext();
        when(context.repository().readHistory(FIRST_FILE, "ours")).thenReturn(null);
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class,
                () -> service.getConflictFileItem(conflict, FIRST_FILE, ConflictBase.OURS));

        assertEquals("openl.error.404.project.merge.conflict.file.revision.not.found", exception.getErrorCode());
        assertArrayEquals(new Object[]{"ours", FIRST_FILE}, exception.getArgs());
    }

    @Test
    void mergesAConflictedTextFileLineByLine() throws Exception {
        var context = createContext();
        givenVersion(context.repository(), "base", "a=1\nb=2\nc=3\nd=4\n");
        givenVersion(context.repository(), "ours", "a=1\nb=ours\nc=3\nd=4\n");
        givenVersion(context.repository(), "theirs", "a=1\nb=theirs\nc=3\nd=theirs\n");

        var text = context.service().getConflictText(context.conflict(), FIRST_FILE);

        // Lines a branch changed come with every version of them, lines no branch changed with their text alone.
        assertEquals(List.of(
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "a=1\n", null, null, null),
                new ConflictTextBlock(ConflictTextBlockKind.CONFLICT, null, "b=ours\n", "b=2\n", "b=theirs\n"),
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "c=3\n", null, null, null),
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "d=theirs\n", "d=4\n", "d=4\n", "d=theirs\n")),
                text.blocks());
    }

    @Test
    void tellsThatHistoriesWithNoCommitInCommonHaveNoBaseVersion() throws Exception {
        var context = createContext(mock(MergeChangesService.class), null, FIRST_FILE);
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class,
                () -> service.getConflictFileItem(conflict, FIRST_FILE, ConflictBase.BASE));

        assertEquals("openl.error.404.project.merge.conflict.file.side.not.found", exception.getErrorCode());
        assertArrayEquals(new Object[]{ConflictBase.BASE, FIRST_FILE}, exception.getArgs());
    }

    @Test
    void readsABaseThatDoesNotHoldTheFileAsAnEmptyFile() throws Exception {
        var context = createContext();
        when(context.repository().readHistory(FIRST_FILE, "base")).thenReturn(null);
        givenVersion(context.repository(), "ours", "a=ours\n");
        givenVersion(context.repository(), "theirs", "a=theirs\n");

        var text = context.service().getConflictText(context.conflict(), FIRST_FILE);

        assertEquals(List.of(new ConflictTextBlock(ConflictTextBlockKind.CONFLICT, null, "a=ours\n", "", "a=theirs\n")),
                text.blocks());
    }

    @Test
    void refusesToMergeAFileThatIsNoText() throws Exception {
        var context = createContext();
        givenVersion(context.repository(), "base", "a=1\n");
        givenVersion(context.repository(), "ours", "a=1\u0000\n");
        givenVersion(context.repository(), "theirs", "a=2\n");
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(BadRequestException.class, () -> service.getConflictText(conflict, FIRST_FILE));

        assertEquals("openl.error.400.project.merge.conflict.text.not-text.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{FIRST_FILE}, exception.getArgs());
    }

    @Test
    void refusesToMergeAFileThatIsNotConflicted() throws Exception {
        var context = createContext();
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class, () -> service.getConflictText(conflict, "notes.txt"));

        assertEquals("openl.error.404.project.merge.conflict.file.not.found", exception.getErrorCode());
    }

    @Test
    void readsTheConflictingTablesOfAWorkbook() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> {
            rate(sheet, 1, 20);
            rate(sheet, 2, 3);
        });

        var workbook = context.service().getConflictWorkbook(context.conflict(), FIRST_FILE);

        assertFalse(workbook.palette());
        var sheet = workbook.sheets().getFirst();
        assertEquals(List.of("Rules"), workbook.sheets().stream().map(ConflictSheetView::name).toList());
        assertEquals(ConflictSheetStatus.CONFLICT, sheet.status());
        assertNull(sheet.reason());
        var table = sheet.tables().getFirst();
        assertEquals("Rules/A1:B3", table.id());
        assertEquals("Rules rate", table.name());
        assertEquals(ConflictTableReason.CELLS, table.reason());
        assertEquals("A1:B3", table.base().range());
        assertEquals(3, table.base().rows());
        assertEquals(List.of(), table.base().changedCells());
        // Each branch marks the cells it changed, the ones that do not conflict included.
        assertEquals(List.of("B2"), table.ours().changedCells());
        assertEquals(List.of("B2", "B3"), table.theirs().changedCells());
        assertEquals(20.0, ((Number) table.theirs().cells().get(1).get(1).value()).doubleValue());
        // The cell both branches changed differently is the conflict, in every version.
        assertEquals(List.of("B2"), table.ours().conflictCells());
        assertEquals(List.of("B2"), table.base().conflictCells());
        assertEquals(List.of("B2"), table.theirs().conflictCells());
    }

    @Test
    void marksNoCellBothBranchesChangedAlike() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            rate(sheet, 1, 10);
            rate(sheet, 2, 7);
        }, sheet -> {
            rate(sheet, 1, 20);
            rate(sheet, 2, 7);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(List.of("B2"), table.ours().changedCells());
        assertEquals(List.of("B2"), table.theirs().changedCells());
        assertEquals(List.of("B2"), table.ours().conflictCells());
    }

    @Test
    void namesTheConflictingCellsAlikeInEveryVersionWhenOneMergedThem() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            // The rate of US covered by its label.
            sheet.addMergedRegion(CellRangeAddress.valueOf("A2:B2"));
            rate(sheet, 2, 5);
        }, sheet -> {
            rate(sheet, 1, 20);
            rate(sheet, 2, 7);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(List.of("B2", "B3"), table.ours().conflictCells());
        assertEquals(List.of("B2", "B3"), table.theirs().conflictCells());
    }

    @Test
    void marksNoCellBothBranchesChangedAlikeWhenOneDeletedARow() throws Exception {
        var context = createContext();
        Consumer<Sheet> withMexico = sheet -> {
            text(sheet, 3, 0, "MX");
            rate(sheet, 3, 3);
        };
        givenRates(context.repository(), withMexico, withMexico.andThen(sheet -> {
            rate(sheet, 1, 10);
            rate(sheet, 2, 20);
        }), withMexico.andThen(sheet -> {
            rate(sheet, 1, 10);
            // CA deleted, which moves MX a row up.
            text(sheet, 2, 0, "MX");
            rate(sheet, 2, 3);
            sheet.removeRow(sheet.getRow(3));
        }));

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(ConflictTableReason.ROWS, table.reason());
        // Both branches changed the rate of US alike: only the rate of CA, which their branch deleted, is marked.
        assertEquals(List.of("B3"), table.ours().changedCells());
        assertEquals(List.of(), table.theirs().changedCells());
    }

    @Test
    void leavesOutTheFormattingBothBranchesGaveACell() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            rate(sheet, 1, 10);
            bold(sheet, 2, 0);
        }, sheet -> {
            rate(sheet, 1, 20);
            // The same formatting as your branch gives the cell: left out.
            bold(sheet, 2, 0);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(List.of("B2"), table.ours().changedCells());
        assertFalse(table.ours().formattingChanged());
        assertFalse(table.theirs().formattingChanged());
        assertEquals(List.of("B2"), table.ours().conflictCells());
    }

    @Test
    void tellsTheFormattingABranchChangedInsteadOfMarkingTheCells() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            // The labels written in bold, which keeps the rows they head matched, and a row inserted after CA.
            bold(sheet, 1, 0);
            bold(sheet, 2, 0);
            text(sheet, 3, 0, "MX");
            rate(sheet, 3, 3);
        }, sheet -> {
            rate(sheet, 1, 10);
            bold(sheet, 2, 1);
            // Another row inserted at the same place.
            text(sheet, 3, 0, "BR");
            rate(sheet, 3, 4);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(ConflictTableReason.ROWS, table.reason());
        assertEquals(List.of("A4", "B4"), table.ours().changedCells());
        assertTrue(table.ours().formattingChanged());
        // The row of US does not match the base one, and the label your version has alike is left out.
        assertEquals(List.of("B2", "A4", "B4"), table.theirs().changedCells());
        assertTrue(table.theirs().formattingChanged());
        assertFalse(table.base().formattingChanged());
    }

    @Test
    void tellsTheFontsBothBranchesChangedDifferently() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            var font = sheet.getWorkbook().createFont();
            font.setFontName("Georgia");
            font.setFontHeightInPoints((short) 10);
            var style = sheet.getWorkbook().createCellStyle();
            style.setFont(font);
            sheet.getRow(1).getCell(0).setCellStyle(style);
            rate(sheet, 1, 10);
        }, sheet -> {
            // The default font of the workbook changes the font of every cell written in it.
            sheet.getWorkbook().getFontAt(0).setFontName("Verdana");
            rate(sheet, 1, 20);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        // The rate both branches changed makes the conflict; the fonts alone would not.
        assertEquals(ConflictTableReason.CELLS, table.reason());
        assertEquals(List.of("B2"), table.ours().conflictCells());
        assertTrue(table.ours().formattingChanged());
        assertTrue(table.theirs().formattingChanged());
    }

    @Test
    void marksTheRowsAndTheColumnsABranchInserted() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            // A row inserted after US, which moves CA a row down.
            text(sheet, 2, 0, "MX");
            rate(sheet, 2, 3);
            text(sheet, 3, 0, "CA");
            rate(sheet, 3, 2);
        }, sheet -> {
            // A column of currencies inserted before the rates.
            text(sheet, 1, 1, "USD");
            number(sheet, 1, 2, 1);
            text(sheet, 2, 1, "CAD");
            number(sheet, 2, 2, 2);
        });

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(ConflictTableReason.ROWS_AND_COLUMNS, table.reason());
        assertEquals(List.of("A3", "B3"), table.ours().changedCells());
        assertEquals(List.of("B1", "B2", "B3"), table.theirs().changedCells());
    }

    @Test
    void mergesAWorkbookWithEachConflictTakenFromTheVersionDecided() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            rate(sheet, 1, 10);
            sheet.getWorkbook().createSheet("Notes").createRow(0).createCell(0).setCellValue("ours");
        }, sheet -> {
            rate(sheet, 1, 20);
            rate(sheet, 2, 3);
        });

        var merged = context.service()
                .mergeConflictWorkbook(context.conflict(), FIRST_FILE, Map.of("Rules/A1:B3", ConflictBase.THEIRS));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(merged))) {
            var sheet = workbook.getSheet("Rules");
            assertEquals(20, sheet.getRow(1).getCell(1).getNumericCellValue());
            assertEquals(3, sheet.getRow(2).getCell(1).getNumericCellValue());
            assertEquals("ours", workbook.getSheet("Notes").getRow(0).getCell(0).getStringCellValue());
        }
    }

    /** A merged workbook is staged with the project as the branch the merge changes holds it. */
    @Test
    void formatsTheWorkbookItMergesTableByTable() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        var formatted = new byte[]{7};
        when(context.formatting().format(argThat(onTarget(context)), any(), any()))
                .thenReturn(Map.of(FIRST_FILE, formatted));

        var merged = context.service()
                .mergeConflictWorkbook(context.conflict(), FIRST_FILE, Map.of("Rules/A1:B3", ConflictBase.THEIRS));

        assertArrayEquals(formatted, merged);
    }

    /** A workbook of a save conflict is staged with the copy in the workspace, which holds your changes. */
    @Test
    void formatsTheWorkbookOfASaveConflictWithTheProjectOfTheWorkspace() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        var saveConflict = MergeConflictInfo.builder()
                .details(context.conflict().details())
                .project(context.conflict().project())
                .build();
        var formatted = new byte[]{7};
        when(context.formatting().format(eq(context.conflict().project()), any(), any()))
                .thenReturn(Map.of(FIRST_FILE, formatted));

        var merged = context.service()
                .mergeConflictWorkbook(saveConflict, FIRST_FILE, Map.of("Rules/A1:B3", ConflictBase.THEIRS));

        assertArrayEquals(formatted, merged);
    }

    /** A branch that cannot be read leaves the merged workbook unformatted, but merged. */
    @Test
    void leavesTheWorkbookUnformattedWhenTheBranchCannotBeRead() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        when(context.repository().forBranch("main")).thenThrow(new IOException("The branch is gone"));
        // the versions of the conflict are read from the repository of the branch, as it was
        var designRepository = mock(BranchRepository.class);
        when(designRepository.forBranch("main")).thenReturn(context.repository());
        when(context.designTimeRepository().getRepository("design")).thenReturn(designRepository);

        var merged = context.service()
                .mergeConflictWorkbook(context.conflict(), FIRST_FILE, Map.of("Rules/A1:B3", ConflictBase.THEIRS));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(merged))) {
            assertEquals(20, workbook.getSheet("Rules").getRow(1).getCell(1).getNumericCellValue());
        }
        verify(context.formatting(), never()).format(any(), any(), any());
    }

    /** The project of the branch the merge changes, read by the paths from the root of the repository. */
    private static ArgumentMatcher<AProjectFolder> onTarget(TestContext context) {
        return staged -> staged instanceof AProject && !(staged instanceof RulesProject)
                && staged.getRepository() == context.repository() && "Project".equals(staged.getFolderPath());
    }

    @Test
    void formatsTheWorkbooksTheResolutionMergesByItself() throws Exception {
        var auto = "rules/Auto.xlsx";
        var base = rates(sheet -> {
        });
        var ours = rates(sheet -> rate(sheet, 1, 10));
        var theirs = rates(sheet -> rate(sheet, 2, 20));
        WorkbookDiffResult diff;
        try (var merger = XlsWorkbookMerger.create(new ByteArrayInputStream(base),
                new ByteArrayInputStream(ours),
                new ByteArrayInputStream(theirs))) {
            diff = merger.getDiffResult();
        }
        var context = createContext(mock(MergeChangesService.class),
                "base",
                Map.of(auto, diff),
                FIRST_FILE,
                SECOND_FILE);
        var repository = context.repository();
        givenTheirVersions(repository);
        when(repository.readHistory(auto, "ours")).thenAnswer(invocation -> new FileItem(auto,
                new ByteArrayInputStream(ours)));
        when(repository.readHistory(auto, "theirs")).thenAnswer(invocation -> new FileItem(auto,
                new ByteArrayInputStream(theirs)));
        var formatted = new byte[]{7};
        when(context.formatting().format(argThat(onTarget(context)), any(), any()))
                .thenReturn(Map.of(auto, formatted));
        var resolution = ArgumentCaptor.forClass(ConflictResolveData.class);
        when(repository.merge(eq("feature"), any(), resolution.capture()))
                .thenReturn(new MergeOutcome("before", "after", List.of(), List.of()));

        resolveBothWithTheirs(context);

        var file = StreamSupport.stream(resolution.getValue().getResolvedFiles().spliterator(), false)
                .filter(item -> item.getData().getName().equals(auto))
                .findFirst()
                .orElseThrow();
        assertArrayEquals(formatted, file.getStream().readAllBytes());
    }

    @Test
    void refusesAWorkbookWithAConflictLeftUnresolved() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        var service = context.service();
        var conflict = context.conflict();
        Map<String, ConflictBase> nothing = Map.of();

        var exception = assertThrows(BadRequestException.class,
                () -> service.mergeConflictWorkbook(conflict, FIRST_FILE, nothing));

        assertEquals("openl.error.400.project.merge.conflict.workbook.unresolved.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{FIRST_FILE, "Rules/A1:B3"}, exception.getArgs());
    }

    @Test
    void refusesTablesThatTouchEachOtherAsResolved() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            rate(sheet, 1, 10);
            text(sheet, 3, 0, "MX");
            rate(sheet, 3, 3);
        }, sheet -> {
            rate(sheet, 1, 20);
            text(sheet, 4, 0, "Data Driver drivers");
        });
        var service = context.service();
        var conflict = context.conflict();
        var ours = Map.of("Rules/A1:B3", ConflictBase.OURS);

        var exception = assertThrows(BadRequestException.class,
                () -> service.mergeConflictWorkbook(conflict, FIRST_FILE, ours));

        assertEquals("openl.error.400.project.merge.conflict.workbook.touching.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{"Rules"}, exception.getArgs());
    }

    @Test
    void refusesAFormulaThatWouldReadAnotherWorkbook() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> {
            rate(sheet, 1, 10);
            link(sheet, "Rates.xlsx");
        }, sheet -> {
            link(sheet, "Census.xlsx");
            CellUtil.getCell(CellUtil.getRow(1, sheet), 1).setCellFormula("[1]Census!A1");
        });
        var service = context.service();
        var conflict = context.conflict();
        var theirs = Map.of("Rules/A1:B3", ConflictBase.THEIRS);

        var exception = assertThrows(BadRequestException.class,
                () -> service.mergeConflictWorkbook(conflict, FIRST_FILE, theirs));

        assertEquals("openl.error.400.project.merge.conflict.workbook.link.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{"Rules!B2"}, exception.getArgs());
    }

    @Test
    void marksOnlyTheRowsABranchInsertedInATableLongerThanShown() throws Exception {
        var context = createContext();
        // A rates table of 250 rows below its header.
        Consumer<Sheet> rows = sheet -> {
            for (var row = 1; row <= 250; row++) {
                text(sheet, row, 0, "R" + row);
                rate(sheet, row, row);
            }
        };
        givenRates(context.repository(), rows, rows.andThen(sheet -> {
            rate(sheet, 5, 1000);
            // A row inserted after the tenth row, which moves the rows below it a row down.
            for (var row = 251; row > 10; row--) {
                text(sheet, row, 0, "R" + (row - 1));
                rate(sheet, row, row - 1);
            }
            text(sheet, 10, 0, "NEW");
            rate(sheet, 10, 0);
        }), rows.andThen(sheet -> rate(sheet, 5, 2000)));

        var table = context.service()
                .getConflictWorkbook(context.conflict(), FIRST_FILE)
                .sheets()
                .getFirst()
                .tables()
                .getFirst();

        assertEquals(252, table.ours().rows());
        assertEquals(200, table.ours().cells().size());
        assertEquals(List.of("A6", "B6", "A11", "B11"), table.ours().changedCells());
        assertEquals(List.of("B6"), table.theirs().changedCells());
    }

    @Test
    void refusesTablesThatMoveContentAFormulaRefersTo() throws Exception {
        var context = createContext();
        Consumer<Sheet> notes = sheet -> {
            text(sheet, 5, 0, "Notes");
            text(sheet, 6, 0, "x");
        };
        givenRates(context.repository(), notes, notes.andThen(sheet -> {
            rate(sheet, 1, 10);
            CellUtil.getCell(CellUtil.getRow(0, sheet), 3).setCellFormula("A7");
        }), notes.andThen(sheet -> {
            rate(sheet, 1, 20);
            // The notes move two rows down.
            CellUtil.getRow(5, sheet).removeCell(CellUtil.getCell(CellUtil.getRow(5, sheet), 0));
            CellUtil.getRow(6, sheet).removeCell(CellUtil.getCell(CellUtil.getRow(6, sheet), 0));
            text(sheet, 7, 0, "Notes");
            text(sheet, 8, 0, "x");
        }));
        var service = context.service();
        var conflict = context.conflict();
        var theirs = Map.of("Rules/A1:B3", ConflictBase.THEIRS);

        var exception = assertThrows(BadRequestException.class,
                () -> service.mergeConflictWorkbook(conflict, FIRST_FILE, theirs));

        assertEquals("openl.error.400.project.merge.conflict.workbook.moved.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{"Rules"}, exception.getArgs());
    }

    @Test
    void refusesToTakeThePaletteFromTheBaseVersion() throws Exception {
        var context = createContext();
        var service = context.service();
        var conflict = context.conflict();
        var base = Map.of("/palette", ConflictBase.BASE);

        var exception = assertThrows(BadRequestException.class,
                () -> service.mergeConflictWorkbook(conflict, FIRST_FILE, base));

        assertEquals("openl.error.400.project.merge.conflict.workbook.palette.message", exception.getErrorCode());
    }

    @Test
    void refusesAWorkbookAVersionOfWhichCannotBeRead() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        givenVersion(context.repository(), "ours", "not a workbook");
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(BadRequestException.class,
                () -> service.getConflictWorkbook(conflict, FIRST_FILE));

        assertEquals("openl.error.400.project.merge.conflict.workbook.not-workbook.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{FIRST_FILE}, exception.getArgs());
    }

    @Test
    void refusesToMergeAFileThatIsNoWorkbookTableByTable() throws Exception {
        var context = createContext(mock(MergeChangesService.class), "base", "rules/notes.txt");
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(BadRequestException.class,
                () -> service.getConflictWorkbook(conflict, "rules/notes.txt"));

        assertEquals("openl.error.400.project.merge.conflict.workbook.not-workbook.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{"rules/notes.txt"}, exception.getArgs());
    }

    @Test
    void needsTheBaseVersionToMergeAWorkbookTableByTable() throws Exception {
        var context = createContext();
        givenRates(context.repository(), sheet -> {
        }, sheet -> rate(sheet, 1, 10), sheet -> rate(sheet, 1, 20));
        when(context.repository().readHistory(FIRST_FILE, "base")).thenReturn(null);
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class, () -> service.getConflictWorkbook(conflict, FIRST_FILE));

        assertEquals("openl.error.404.project.merge.conflict.file.revision.not.found", exception.getErrorCode());
    }

    /**
     * Answers the reads of the first conflicted file at the base, your and their commit with a workbook whose sheet
     * Rules holds a rates table at A1, each version edited as given.
     */
    private static void givenRates(BranchRepository repository,
                                   Consumer<Sheet> base,
                                   Consumer<Sheet> ours,
                                   Consumer<Sheet> theirs) throws IOException {
        givenWorkbook(repository, "base", base);
        givenWorkbook(repository, "ours", ours);
        givenWorkbook(repository, "theirs", theirs);
    }

    private static void givenWorkbook(BranchRepository repository, String commit, Consumer<Sheet> edit)
            throws IOException {
        var bytes = rates(edit);
        when(repository.readHistory(FIRST_FILE, commit)).thenAnswer(invocation -> new FileItem(FIRST_FILE,
                new ByteArrayInputStream(bytes)));
    }

    /** A workbook with a table of rates, changed by an edit. */
    private static byte[] rates(Consumer<Sheet> edit) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Rules");
            text(sheet, 0, 0, "Rules rate");
            text(sheet, 1, 0, "US");
            rate(sheet, 1, 1);
            text(sheet, 2, 0, "CA");
            rate(sheet, 2, 2);
            edit.accept(sheet);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void text(Sheet sheet, int row, int column, String value) {
        CellUtil.getCell(CellUtil.getRow(row, sheet), column).setCellValue(value);
    }

    /** Sets the rate of a row of the rates table. */
    private static void rate(Sheet sheet, int row, double value) {
        number(sheet, row, 1, value);
    }

    private static void number(Sheet sheet, int row, int column, double value) {
        CellUtil.getCell(CellUtil.getRow(row, sheet), column).setCellValue(value);
    }

    /** Links the workbook of a sheet to another workbook, which is not kept. */
    private static void link(Sheet sheet, String workbookName) {
        try (var other = new XSSFWorkbook()) {
            ((XSSFWorkbook) sheet.getWorkbook()).linkExternalWorkbook(workbookName, other);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes the text of a cell in bold, the formatting alone changed. */
    private static void bold(Sheet sheet, int row, int column) {
        var font = sheet.getWorkbook().createFont();
        font.setBold(true);
        CellUtil.setFont(CellUtil.getCell(CellUtil.getRow(row, sheet), column), font);
    }

    /** Answers each read of the first conflicted file at a commit with the text, read anew. */
    private static void givenVersion(BranchRepository repository, String commit, String text) throws IOException {
        givenVersion(repository, FIRST_FILE, commit, text);
    }

    /** Answers each read of a conflicted file at a commit with the text, read anew. */
    private static void givenVersion(BranchRepository repository,
                                     String path,
                                     String commit,
                                     String text) throws IOException {
        givenVersion(repository, path, commit, text, StandardCharsets.UTF_8);
    }

    /** Answers each read of a conflicted file at a commit with the text written in the charset, read anew. */
    private static void givenVersion(BranchRepository repository,
                                     String path,
                                     String commit,
                                     String text,
                                     Charset charset) throws IOException {
        when(repository.readHistory(path, commit)).thenAnswer(invocation -> new FileItem(path,
                new ByteArrayInputStream(text.getBytes(charset))));
    }

    /** Every version of the conflict holds the file, as the text {@code <key>=<commit>}. */
    private static void givenTextVersions(BranchRepository repository, String path, String key) throws IOException {
        for (var commit : COMMITS) {
            when(repository.checkHistory(path, commit)).thenReturn(file(path));
            givenVersion(repository, path, commit, key + "=" + commit + "\n");
        }
    }

    @Test
    void aResolvedMergeTellsWhatItChangedAgainstTheProjectsOfTheTargetBefore() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        givenTheirVersions(repository);
        var outcome = new MergeOutcome("before", "after",
                List.of(new FileChange("Project/" + FIRST_FILE, FileChange.Kind.MODIFIED)), List.of());
        when(repository.merge(eq("feature"), any(), any())).thenReturn(outcome);
        var project = mock(AProject.class);
        var before = List.of(project);
        when(mergeChangesService.projectsBefore("design", "main")).thenReturn(before);
        var changes = new MergedChanges("feature", "main", outcome, before, before);
        when(mergeChangesService.changesOf("design", "feature", "main", outcome, before)).thenReturn(changes);
        var summary = new MergeSummaryView("feature", "main", "before", "after", List.of(), 0);
        when(mergeChangesService.summaryOf(context.conflict().project(), changes)).thenReturn(summary);

        var response = resolveBothWithTheirs(context);

        assertEquals(summary, response.summary());
    }

    /**
     * The resolution is committed before the index is asked to publish it, so an index that does not publish it
     * leaves the resolution a success, only without a summary: its projects after the merge would be the ones before.
     */
    @Test
    void anIndexThatDoesNotPublishTheResolutionLeavesItWithoutASummary() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        when(repository.supports().branches()).thenReturn(true);
        when(context.designTimeRepository().refreshBranch("design", "main"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("index is busy")));
        givenTheirVersions(repository);
        when(repository.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));

        var response = resolveBothWithTheirs(context);

        assertEquals(ConflictResolutionStatus.SUCCESS, response.status());
        assertNull(response.summary());
        verify(mergeChangesService, never()).changesOf(any(), any(), any(), any(), any());
    }

    /** A descriptor declaring the module of the resolved workbook already is not written again. */
    @Test
    void aDescriptorDeclaringTheModuleAlreadyIsLeftAsItIs() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "DeletedOurs.xlsx");
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        verify(context.repository(), never()).save(any(FileData.class), any(), any());
        assertEquals("after", outcomes.getValue().after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), outcomes.getValue().changes());
    }

    /**
     * Descriptors whose save tells no revision are left out of the summary: compared at the merge commit, they would
     * show no change.
     */
    @Test
    void descriptorsWhoseSaveTellsNoRevisionAreLeftOutOfTheSummary() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "Other.xlsx");
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        verify(context.repository()).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        assertEquals("after", outcomes.getValue().after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), outcomes.getValue().changes());
    }

    /**
     * A resolution declares the module of a resolved workbook again in a commit of its own: the summary counts the
     * descriptor among the changed files, and its revision after the merge is the commit that wrote it.
     */
    @Test
    void aDescriptorWrittenAfterTheResolutionBelongsToTheMerge() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "Other.xlsx");
        var saved = new FileData();
        saved.setVersion("descriptors");
        when(context.repository().save(any(FileData.class), any(), eq(ChangesetType.DIFF))).thenReturn(saved);
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        var outcome = outcomes.getValue();
        assertEquals("before", outcome.before());
        assertEquals("descriptors", outcome.after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED),
                new FileChange("rules/rules.xml", FileChange.Kind.MODIFIED)), outcome.changes());
    }

    /** Their version of every conflicted file reads empty. */
    private static void givenTheirVersions(BranchRepository repository) throws IOException {
        when(repository.readHistory(anyString(), eq("theirs")))
                .thenAnswer(invocation -> new FileItem(invocation.<String>getArgument(0),
                        new ByteArrayInputStream(new byte[0])));
    }

    /**
     * A resolution of a workbook their rules.xml declares as a module, into a branch whose rules.xml declares the
     * given module.
     */
    private TestContext givenResolutionOfADeclaredModule(MergeChangesService mergeChangesService,
                                                         String targetModule) throws IOException {
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        givenTheirVersions(repository);
        when(repository.readHistory("rules/rules.xml", "theirs")).thenAnswer(invocation -> new FileItem(
                "rules/rules.xml", new ByteArrayInputStream(descriptor("DeletedOurs.xlsx").getBytes())));
        when(repository.read("rules/rules.xml")).thenAnswer(invocation -> new FileItem("rules/rules.xml",
                new ByteArrayInputStream(descriptor(targetModule).getBytes())));
        var resolved = mock(RulesProject.class);
        when(resolved.getRealPath()).thenReturn("rules");
        when(context.workspace().getProjectByPath("design", FIRST_FILE)).thenReturn(Optional.of(resolved));
        when(repository.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));
        return context;
    }

    /** The merge outcome the resolution tells what it changed by. */
    private static ArgumentCaptor<MergeOutcome> capturedOutcomes(MergeChangesService mergeChangesService) {
        var outcomes = ArgumentCaptor.forClass(MergeOutcome.class);
        var changes = mock(MergedChanges.class);
        when(mergeChangesService.changesOf(eq("design"), eq("feature"), eq("main"), outcomes.capture(), any()))
                .thenReturn(changes);
        return outcomes;
    }

    private static ResolveConflictsResponse resolveBothWithTheirs(TestContext context) throws Exception {
        return context.service().resolveConflicts(context.conflict(),
                List.of(new FileConflictResolution(FIRST_FILE, ConflictResolutionStrategy.THEIRS),
                        new FileConflictResolution(SECOND_FILE, ConflictResolutionStrategy.THEIRS)),
                Map.of(),
                "Resolved");
    }

    /**
     * The descriptors of a resolution belong to the branch it merged into: read or written on the default branch of
     * the repository, they would change a branch the merge never touched and miss the one it did.
     */
    @Test
    void theDescriptorsOfAResolutionAreWrittenToTheBranchItMergedInto() throws Exception {
        var context = createContext();
        var repository = context.repository();
        var target = mock(BranchRepository.class);
        var features = repository.supports();
        when(target.supports()).thenReturn(features);
        when(repository.forBranch("main")).thenReturn(target);
        when(repository.readHistory(anyString(), eq("theirs")))
                .thenAnswer(invocation -> new FileItem(invocation.<String>getArgument(0),
                        new ByteArrayInputStream(new byte[0])));
        when(repository.readHistory("rules/rules.xml", "theirs")).thenAnswer(invocation -> new FileItem(
                "rules/rules.xml", new ByteArrayInputStream(descriptor("DeletedOurs.xlsx").getBytes())));
        when(target.read("rules/rules.xml")).thenAnswer(invocation -> new FileItem("rules/rules.xml",
                new ByteArrayInputStream(descriptor("Other.xlsx").getBytes())));
        var resolved = mock(RulesProject.class);
        when(resolved.getRealPath()).thenReturn("rules");
        when(context.workspace().getProjectByPath("design", FIRST_FILE)).thenReturn(Optional.of(resolved));
        when(target.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));

        context.service().resolveConflicts(context.conflict(),
                List.of(new FileConflictResolution(FIRST_FILE, ConflictResolutionStrategy.THEIRS),
                        new FileConflictResolution(SECOND_FILE, ConflictResolutionStrategy.THEIRS)),
                Map.of(),
                "Resolved");

        verify(target).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        verify(repository, never()).read("rules/rules.xml");
        verify(repository, never()).save(any(FileData.class), any(), any());
    }

    private static String descriptor(String module) {
        return """
                <project>
                    <name>Rules</name>
                    <modules>
                        <module>
                            <name>Main</name>
                            <rules-root path="%s"/>
                        </module>
                    </modules>
                </project>
                """.formatted(module);
    }

    private TestContext createContext() throws IOException {
        return createContext(mock(MergeChangesService.class));
    }

    private TestContext createContext(MergeChangesService mergeChangesService) throws IOException {
        return createContext(mergeChangesService, "base", FIRST_FILE, SECOND_FILE);
    }

    /**
     * @param baseCommit      the commit the branches of the conflict parted at, or {@code null} when their histories
     *                        have none in common
     * @param conflictedFiles the files the conflict lists
     */
    private TestContext createContext(MergeChangesService mergeChangesService,
                                      @Nullable String baseCommit,
                                      String... conflictedFiles) throws IOException {
        return createContext(mergeChangesService, baseCommit, Map.of(), conflictedFiles);
    }

    /**
     * @param toAutoResolve the workbooks the conflict merges by itself once it is resolved
     */
    private TestContext createContext(MergeChangesService mergeChangesService,
                                      @Nullable String baseCommit,
                                      Map<String, WorkbookDiffResult> toAutoResolve,
                                      String... conflictedFiles) throws IOException {
        var repository = mock(BranchRepository.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var workspace = mock(UserWorkspace.class);
        var project = mock(RulesProject.class);
        var features = mock(Features.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(features);
        when(repository.forBranch("main")).thenReturn(repository);
        when(project.getRepository()).thenReturn(repository);
        lenient().when(project.getDesignRepository()).thenReturn(repository);
        lenient().when(project.getRealPath()).thenReturn("Project");
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var user = mock(WorkspaceUser.class);
        lenient().when(workspace.getUser()).thenReturn(user);
        var localWorkspace = mock(LocalWorkspace.class);
        lenient().when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(workspace.getProjectByPath(anyString(), anyString())).thenReturn(Optional.empty());
        when(designTimeRepository.getRulesLocation()).thenReturn("");
        when(designTimeRepository.getRepository("design")).thenReturn(repository);

        var diffs = new LinkedHashMap<String, String>();
        Stream.of(conflictedFiles).forEach(file -> diffs.put(file, "diff"));
        var conflictDetails = MergeConflictDetails.builder()
                .diffs(diffs)
                .yourCommit("ours")
                .theirCommit("theirs")
                .baseCommit(baseCommit)
                .toAutoResolve(toAutoResolve)
                .build();
        var conflict = MergeConflictInfo.builder()
                .details(conflictDetails)
                .project(project)
                .mergeBranchFrom("feature")
                .mergeBranchTo("main")
                .currentBranch("main")
                .build();
        // The conflict is kept for the project, as a merge or a save keeps it.
        var conflictsSessionHolder = new ProjectsMergeConflictsSessionHolder();
        conflictsSessionHolder.store(ProjectIds.of("design", "Project"),
                conflict);
        // A workbook a merge combines is left as it is, unless a test formats it.
        var formatting = mock(MergedWorkbookFormatting.class);
        lenient().when(formatting.format(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        var studio = mock(WebStudio.class);
        var service = new ProjectsMergeConflictsServiceImpl(mergeChangesService,
                conflictsSessionHolder,
                new ConflictWorkbookMapper(new RawTableReader()),
                formatting) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }

            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
        return new TestContext(repository, designTimeRepository, workspace, conflict, service, formatting);
    }

    private static FileData file(String name) {
        var file = new FileData();
        file.setName(name);
        return file;
    }

    private record TestContext(BranchRepository repository,
                               DesignTimeRepository designTimeRepository,
                               UserWorkspace workspace,
                               MergeConflictInfo conflict,
                               ProjectsMergeConflictsServiceImpl service,
                               MergedWorkbookFormatting formatting) {
    }
}
