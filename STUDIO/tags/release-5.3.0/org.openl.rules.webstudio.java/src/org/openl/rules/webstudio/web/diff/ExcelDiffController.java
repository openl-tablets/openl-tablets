package org.openl.rules.webstudio.web.diff;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;

import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.DiffTreeBuilderImpl;
import org.openl.rules.diff.test.ProjectionDifferImpl;
import org.openl.rules.diff.xls.XlsProjectionBuilder;

public class ExcelDiffController extends AbstractDiffController {
    private UploadedFile file1;
    private UploadedFile file2;

    public static final String excelContentTypes[] = {
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    };

    public String compare() {
        uploadFile(file1);
        uploadFile(file2);

        XlsMetaInfo xmi1 = XlsHelper.getXlsMetaInfo(file1.getName());
        XlsMetaInfo xmi2 = XlsHelper.getXlsMetaInfo(file2.getName());

        AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
        AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());

        setDiffTree(builder.compare(p1, p2));
        return null;
    }

    public void validateContentType(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = "Only Excel files can be compared";
        if (value != null && value instanceof UploadedFile) {
            UploadedFile file = (UploadedFile) value;
            String fileContentType = file.getContentType();
            if (ArrayUtils.contains(excelContentTypes, fileContentType)) {
                ((UIInput)toValidate).setValid(true);
                return;
            }
        }
        ((UIInput)toValidate).setValid(false);
        FacesMessage message = new FacesMessage(errorMessage);
        context.addMessage(toValidate.getClientId(context), message);
    }

    private void uploadFile(UploadedFile file) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = file.getInputStream();
            out = new FileOutputStream(file.getName());
            IOUtils.copy(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * @return the file1
     */
    public UploadedFile getFile1() {
        return file1;
    }

    /**
     * @param file1 the file1 to set
     */
    public void setFile1(UploadedFile file1) {
        this.file1 = file1;
    }

    /**
     * @return the file2
     */
    public UploadedFile getFile2() {
        return file2;
    }

    /**
     * @param file2 the file2 to set
     */
    public void setFile2(UploadedFile file2) {
        this.file2 = file2;
    }

}
