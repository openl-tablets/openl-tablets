package org.openl.studio.projects.model.tables;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Abstract base class for table views that contain headers and data rows.
 * Used by both Data and Test table views.
 *
 * @author Vladyslav Pikus
 */
public abstract class AbstractDataView extends TableView {

    private static final int DEFAULT_HEADER_HEIGHT = 2;

    @Schema(description = "Collection of data table headers")
    public final Collection<DataHeaderView> headers;

    @Schema(description = "Collection of data table rows")
    public final Collection<DataRowView> rows;

    protected AbstractDataView(Builder<?> builder) {
        super(builder);
        this.headers = Optional.ofNullable(builder.headers).map(List::copyOf).orElseGet(List::of);
        this.rows = Optional.ofNullable(builder.rows).map(List::copyOf).orElseGet(List::of);
    }

    @Override
    protected int getBodyHeight() {
        return rows.size() + getHeaderHeight();
    }

    @Override
    protected int getBodyWidth() {
        return CollectionUtils.isNotEmpty(headers) ? headers.size() : 0;
    }

    @JsonIgnore
    public int getHeaderHeight() {
        var hasForeignKey = headers.stream().anyMatch(h -> StringUtils.isNotBlank(h.foreignKey));
        return hasForeignKey ? DEFAULT_HEADER_HEIGHT + 1 : DEFAULT_HEADER_HEIGHT;
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
