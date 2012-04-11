/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.impl.DataBase;
import org.openl.types.IOpenSchema;

/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass {

    IDataBase dataBase = new DataBase();

    /**
     * @param schema
     * @param name
     */
    public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl) {
        super(schema, name, openl);
        this.metaInfo = metaInfo;
    }

    /**
     * @return
     */
    public IDataBase getDataBase() {
        return dataBase;
    }

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) metaInfo;
    }

}
