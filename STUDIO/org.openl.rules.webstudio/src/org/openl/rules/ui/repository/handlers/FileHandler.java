package org.openl.rules.ui.repository.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.CannotFindEntityException;
import org.openl.rules.ui.repository.beans.FileBean;
import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;

import sun.awt.image.ByteInterleavedRaster;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

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
    
    public boolean updateFile(FileBean bean, String newContent) {
        boolean result = false;
        
        InputStream is = null;
        try {
            RFile file = getRFile(bean);
            try {
                is = new ByteArrayInputStream(newContent.getBytes());
                file.setContent(is);
                // even if cannot close stream content of file was set successfully
                result = true;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (Exception e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }
        
        return result;
    }

    private RFile getRFile(FileBean bean) throws CannotFindEntityException {
        String id = bean.getId();
        REntity entity = getEntityById(id);
        RFile file = (RFile) entity;
        
        return file;
    }
}
