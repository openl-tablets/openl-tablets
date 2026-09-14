package org.openl.studio.compare.rest;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.model.ComparisonTableView;
import org.openl.studio.compare.model.ComparisonView;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.compare.service.ComparisonMapper;
import org.openl.studio.compare.service.ComparisonRegistry;

/**
 * REST controller for comparing Excel files.
 *
 * <p>A comparison parses both files, so it is started and read in two steps: the start answers with
 * the identifier the comparison is known by, and the result is read once the comparison has
 * finished. Its progress is pushed to the topic {@code /topic/compare/{id}/status} of the user who
 * asked for it, which the client listens to as {@code /user/topic/compare/{id}/status}.
 *
 * <p>A session holds one comparison at a time. Starting another one releases the previous, and so
 * does closing the screen that shows it.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/compare", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Compare")
public class CompareController {

    private static final String XLSX_MEDIATYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLS_MEDIATYPE = "application/vnd.ms-excel";

    private final ComparisonLauncher launcher;
    private final ComparisonRegistry registry;
    private final ComparisonMapper mapper;

    @Operation(summary = "compare.files.summary", description = "compare.files.desc")
    @ApiResponse(responseCode = "202", description = "compare.files.202.desc")
    @ApiResponse(responseCode = "400", description = "compare.file.not-excel.message")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComparisonStartedView compareFiles(
            @Parameter(description = "compare.param.file1.desc", content = @Content(encoding = {
                    @Encoding(contentType = XLSX_MEDIATYPE + ", " + XLS_MEDIATYPE)}))
            @RequestPart("file1") MultipartFile file1,
            @Parameter(description = "compare.param.file2.desc", content = @Content(encoding = {
                    @Encoding(contentType = XLSX_MEDIATYPE + ", " + XLS_MEDIATYPE)}))
            @RequestPart("file2") MultipartFile file2) throws IOException {

        return launcher.start(file1, file2);
    }

    @Operation(summary = "compare.get-result.summary", description = "compare.get-result.desc")
    @ApiResponse(responseCode = "200", description = "compare.get-result.200.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ComparisonView.class)))
    @ApiResponse(responseCode = "404", description = "compare.not-found.message")
    @ApiResponse(responseCode = "409", description = "compare.not-completed.message")
    @GetMapping("/{comparisonId}")
    public ComparisonView getComparison(
            @Parameter(description = "compare.param.comparison-id.desc")
            @PathVariable("comparisonId") String comparisonId) {
        return mapper.toView(comparisonId, resultOf(comparisonId));
    }

    @Operation(summary = "compare.get-table.summary", description = "compare.get-table.desc")
    @ApiResponse(responseCode = "200", description = "compare.get-table.200.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ComparisonTableView.class)))
    @ApiResponse(responseCode = "404", description = "compare.table.not-found.message")
    @ApiResponse(responseCode = "409", description = "compare.not-completed.message")
    @GetMapping("/{comparisonId}/tables/{tableId}")
    public ComparisonTableView getTable(
            @Parameter(description = "compare.param.comparison-id.desc")
            @PathVariable("comparisonId") String comparisonId,
            @Parameter(description = "compare.param.table-id.desc")
            @PathVariable("tableId") String tableId) {
        var table = mapper.toTable(resultOf(comparisonId), tableId);
        if (table == null) {
            throw new NotFoundException("compare.table.not-found.message");
        }
        return table;
    }

    @Operation(summary = "compare.drop.summary", description = "compare.drop.desc")
    @ApiResponse(responseCode = "204", description = "compare.drop.204.desc")
    @DeleteMapping("/{comparisonId}")
    public void drop(
            @Parameter(description = "compare.param.comparison-id.desc")
            @PathVariable("comparisonId") String comparisonId) {
        registry.drop(comparisonId);
    }

    /**
     * What the named comparison found. A comparison this session no longer holds is gone, and one
     * that is still running has nothing to report yet.
     */
    private DiffTreeNode resultOf(String comparisonId) {
        if (!registry.has(comparisonId)) {
            throw new NotFoundException("compare.not-found.message");
        }
        if (!registry.isDone(comparisonId)) {
            throw new ConflictException("compare.not-completed.message");
        }
        var result = registry.result(comparisonId);
        if (result == null) {
            // The comparison is still this session's; it was stopped before it found anything.
            throw new ConflictException("compare.interrupted.message");
        }
        return result;
    }
}
