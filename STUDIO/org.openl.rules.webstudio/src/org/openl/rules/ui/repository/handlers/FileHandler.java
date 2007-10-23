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
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

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
    
    public InputStream getContent(FileBean bean) {
        try {
            RFile file = getRFile(bean);
            return file.getContent();
        } catch (Exception e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }
        
        return null;
    }
    
    public InputStream getContent4Version(FileBean bean, String versionName) {
        try {
            RFile file = getRFile(bean);
            return file.getContent4Version(versionName);
        } catch (Exception e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }
        
        return null;
    }

    public boolean revertToVersion(FileBean bean, String versionName) {
        try {
            RFile file = getRFile(bean);
            file.revertToVersion(versionName);
            return true;
        } catch (Exception e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }
        
        return false;
    }
    
    private RFile getRFile(FileBean bean) throws CannotFindEntityException {
        String id = bean.getId();
        REntity entity = getEntityById(id);
        RFile file = (RFile) entity;
        
        return file;
    }
}
