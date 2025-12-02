package org.openl.studio.projects.model.tables;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Abstract base class for table views that contain headers and data rows.
 * Used by both Data and Test table views.
 *
 * @author Vladyslav Pikus
 */
public abstract class AbstractDataView extends TableView {

    @Schema(description = "Collection of data table headers")
    public final Collection<DataHeaderView> headers;

    @Schema(description = "Collection of data table rows")
    public final Collection<DataRowView> rows;

    protected AbstractDataView(Builder<?> builder) {
        super(builder);
        this.headers = Optional.ofNullable(builder.headers).map(List::copyOf).orElseGet(List::of);
        this.rows = Optional.ofNullable(builder.rows).map(List::copyOf).orElseGet(List::of);
    }

    public static abstract class Builder<T extends Builder<T>> extends TableView.Builder<T> {
        protected Collection<DataHeaderView> headers;
        protected Collection<DataRowView> rows;

        protected Builder() {
        }

        public T headers(Collection<DataHeaderView> headers) {
            this.headers = headers;
            return self();
        }

        public T rows(Collection<DataRowView> rows) {
            this.rows = rows;
            return self();
        }
    }

}
