package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.FileBean;
import org.openl.rules.repository.RFile;

public class FileHandler extends BeanHandler {
    public FileHandler(Context context) {
        super(context);
    }

    protected FileBean createBean(RFile file) {
        FileBean fb = new FileBean();
        initBean(fb, file);
        return fb;
    }
}
