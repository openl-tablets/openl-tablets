package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.FileBean;
import org.openl.rules.repository.RFile;

/**
 * Handler for File UI Bean
 * 
 * @author Aleh Bykhavets
 *
 */
public class FileHandler extends BeanHandler {
    public FileHandler(Context context) {
        super(context);
    }

    /**
     * Creates File UI Bean from a repository file.
     * 
     * @param file repository file
     * @return new File UI Bean
     */
    protected FileBean createBean(RFile file) {
        FileBean fb = new FileBean();
        initBean(fb, file);
        return fb;
    }
}
