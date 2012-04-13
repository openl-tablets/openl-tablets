package org.openl.rules.liveexcel;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.text.ILocation;

import com.exigen.le.LiveExcel;

public class LiveExcelIdentifierNode extends IdentifierNode {
    
    private LiveExcel liveExcel;

    public LiveExcelIdentifierNode(String type, ILocation location, String identifier, IOpenSourceCodeModule module,
            LiveExcel liveExcel) {
        super(type, location, identifier, module);
        this.liveExcel = liveExcel;
    }

    public LiveExcel getLiveExcel() {
        return liveExcel;
    }
}
