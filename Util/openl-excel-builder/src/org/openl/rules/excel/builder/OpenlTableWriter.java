package org.openl.rules.excel.builder;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.model.scaffolding.Model;

public interface OpenlTableWriter<T extends Model> {

    Sheet export(Collection<T> models, Sheet sheet);

}
