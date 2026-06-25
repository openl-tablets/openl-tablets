package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single in-place edit of a table's raw source.
 * <p>
 * The request nests two discriminators: {@code operation} selects the edit and the {@code target} object's
 * {@code type} selects the resource it acts on, e.g.
 * {@code {"operation": "insert", "target": {"type": "row", "position": 2, "cells": [...]}}}.
 * <p>
 * All coordinates are 0-based and refer to the matrix returned by
 * {@code GET /projects/{projectId}/tables/{tableId}?raw=true}. The table type is never interpreted; the action works
 * on any table.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "Single in-place edit of a table's raw source. `operation` selects the edit and the target's "
        + "`type` the resource it acts on.", discriminatorProperty = "operation")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "operation")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RawTableSourceAction.Append.class, name = "append"),
        @JsonSubTypes.Type(value = RawTableSourceAction.Insert.class, name = "insert"),
        @JsonSubTypes.Type(value = RawTableSourceAction.Delete.class, name = "delete"),
        @JsonSubTypes.Type(value = RawTableSourceAction.Update.class, name = "update"),
        @JsonSubTypes.Type(value = RawTableSourceAction.Merge.class, name = "merge"),
        @JsonSubTypes.Type(value = RawTableSourceAction.Unmerge.class, name = "unmerge")
})
public sealed interface RawTableSourceAction
        permits RawTableSourceAction.Append,
        RawTableSourceAction.Insert,
        RawTableSourceAction.Delete,
        RawTableSourceAction.Update,
        RawTableSourceAction.Merge,
        RawTableSourceAction.Unmerge {

    @Schema(name = "Append", description = "Adds a row or column to the end of the table.")
    record Append(@NotNull @Valid AppendTarget target) implements RawTableSourceAction {
    }

    @Schema(name = "Insert", description = "Inserts a row or column at a position.")
    record Insert(@NotNull @Valid InsertTarget target) implements RawTableSourceAction {
    }

    @Schema(name = "Delete", description = "Deletes a row or column at a position.")
    record Delete(@NotNull @Valid DeleteTarget target) implements RawTableSourceAction {
    }

    @Schema(name = "Update", description = "Overwrites a row, a column or a single cell.")
    record Update(@NotNull @Valid UpdateTarget target) implements RawTableSourceAction {
    }

    @Schema(name = "Merge", description = "Merges a rectangular range of cells.")
    record Merge(@NotNull @Valid MergeTarget target) implements RawTableSourceAction {
    }

    @Schema(name = "Unmerge", description = "Unmerges the merged cell that covers a position.")
    record Unmerge(@NotNull @Valid UnmergeTarget target) implements RawTableSourceAction {
    }

}
