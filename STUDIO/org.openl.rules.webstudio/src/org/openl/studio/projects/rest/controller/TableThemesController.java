package org.openl.studio.projects.rest.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.studio.projects.service.tables.theme.TableThemeService;

/**
 * The table themes OpenL Studio offers, for a screen to let the reader choose one.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/table-themes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Table Themes (BETA)", description = "Experimental table themes API")
public class TableThemesController {

    private final TableThemeService tableThemeService;

    @GetMapping
    @Operation(summary = "table-themes.list.summary", description = "table-themes.list.desc")
    public List<TableThemeView> getTableThemes() {
        return tableThemeService.getThemes();
    }
}
