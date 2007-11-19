package org.openl.rules.webstudio.services.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.webstudio.services.Service;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.services.ServiceParams;
import org.openl.rules.webstudio.services.ServiceResult;
import org.openl.rules.webstudio.util.IOUtils;


import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.ZipFile;


/**
 * Base class for upload services.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseUploadService implements Service {
    private final static Log log = LogFactory.getLog(BaseUploadService.class);

    /**
     * {@inheritDoc}
     */
    public ServiceResult execute(ServiceParams serviceParams) throws ServiceException {
        UploadServiceParams params = (UploadServiceParams) serviceParams;
        UploadServiceResult result = null;

        UploadedFile uploadFile = params.getFile();
        try {
            if (uploadFile.getSize() == 0) {
                throw new EmptyFileException(uploadFile.getName(), "file-empty");
            }

            if (isZipFile(params)) {
                result = uploadZipFile(params);
                if (log.isDebugEnabled()) {
                    log.debug("Zip file uploaded and unpacked: "
                        + result.getResultFiles());
                }
            } else {
                throw new NotUnzippedFileException();
                /*
                result = uploadNonZipFile(params);
                if (log.isDebugEnabled()) {
                    log.debug("File uploaded to '" + result.getResultFile().getName()
                        + "'");
                }
                */
            }
        } finally {
            //uploadFile.cleanup();
        }

        return result;
    }

    private boolean isZipFile(UploadServiceParams params) {
        File tempFile = null;
        FileInputStream is = null;

        try {
            tempFile = File.createTempFile("upload", "zip");
            saveFile(params, tempFile);

            is = new FileInputStream(tempFile);

            //Check signature. We make it, because ZipFile try open big non-zip files too slow
            boolean isZipSignatureCorrect = (is.read() == 'P') && (is.read() == 'K');
            ZipFile zip = null;
            try {
                if (isZipSignatureCorrect) {
                    zip = new ZipFile(tempFile);

                    return true;
                }
            } finally {
                if (zip != null) {
                    zip.close();
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            IOUtils.closeSilently(is);
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return false;
    }

    private void saveFile(UploadServiceParams params, File tempFile)
        throws FileNotFoundException, IOException
    {
        OutputStream tempOS = new FileOutputStream(tempFile);
        InputStream is = null;
        try {
            is = params.getFile().getInputStream();
            FileCopyUtils.copy(is, tempOS);
        } finally {
            IOUtils.closeSilently(tempOS);
            IOUtils.closeSilently(is);
        }
    }

    private UploadServiceResult uploadZipFile(UploadServiceParams params)
        throws ServiceException
    {
        UploadServiceResult result = new UploadServiceResult();

        try {
            File tempFile = File.createTempFile("upload", "zip");
            try {
                saveFile(params, tempFile);
                unpack(params, result, tempFile);
            } finally {
                tempFile.delete();
            }
            if (log.isDebugEnabled()) {
                log.debug("File '" + params.getFile().getName() + "' unpacked");
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return result;
    }

    /**
     * Unpack uploaded archive file.
     *
     * @param params service parameters
     * @param result service result
     * @param zipFile zippedFile
     *
     * @throws IOException if I/O error occurs
     * @throws ServiceException if error occurs
     */
    protected abstract void unpack(UploadServiceParams params,
        UploadServiceResult result, File zipFile) throws IOException, ServiceException;

    /**
     * Return file object where uploaded file should be stored.
     *
     * @param params service parameters
     * @param fileName name of uploaded file
     *
     * @return where uploaded file should be stored
     *
     * @throws IOException if I/O error occurs
     */
    protected abstract File getFile(UploadServiceParams params, String fileName)
        throws IOException;
}
