package org.openl.rules.excel.builder;

import java.util.Collection;

import org.openl.rules.excel.builder.export.IWritableExtendedGrid;
import org.openl.rules.model.scaffolding.Model;

public interface OpenlTableWriter<T extends Model> {

    IWritableExtendedGrid export(IWritableExtendedGrid gridToFill, Collection<T> models);

}
