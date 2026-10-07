package org.openl.rules.tableeditor.model.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A place a merged cell reaches over.
 *
 * <p>The place holds nothing of its own; it stands in for the cell the merge belongs to.
 */
@Getter
@RequiredArgsConstructor
public final class CellModelDelegator implements ICellModel {

    private final CellModel model;
}
