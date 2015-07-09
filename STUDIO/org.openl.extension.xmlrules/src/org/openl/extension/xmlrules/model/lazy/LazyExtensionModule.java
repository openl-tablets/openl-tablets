package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Sheet;

public class LazyExtensionModule extends BaseLazyItem<ExtensionModuleInfo> implements ExtensionModule {
    public LazyExtensionModule(XStream xstream, File file, String mainEntryName) {
        super(xstream, file, mainEntryName);
    }

    @Override
    public String getFormatVersion() {
        return getInfo().getFormatVersion();
    }

    @Override
    public String getXlsFileName() {
        return getInfo().getXlsFileName();
    }

    @Override
    public List<Sheet> getSheets() {
        List<Sheet> sheets = new ArrayList<Sheet>();
        for (String entryName : getInfo().getSheetEntries()) {
            sheets.add(new LazySheet(getXstream(), getFile(), entryName));
        }
        return sheets;
    }
}
