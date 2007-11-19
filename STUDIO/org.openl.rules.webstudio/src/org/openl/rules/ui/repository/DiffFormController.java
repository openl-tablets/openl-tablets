package org.openl.rules.ui.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.faces.context.FacesContext;

public class DiffFormController {
    private String compareWithVersion;

    public String revertTo() {
        /*
        FileBean activeFile = (FileBean) getContext().getActiveNodeBean();
        FileHandler fh = getContext().getFileHandler();
        boolean success = fh.revertToVersion(activeFile, compareWithVersion);

        refresh();
        return outcome(success);
        */
        return null;
    }

    public String compare() {
        FacesContext context = FacesContext.getCurrentInstance();
        compareWithVersion = getParamValue(context, "compareWithVersion");

        return UiConst.OUTCOME_SUCCESS;
    }

    public String getBaseVersionContent() {
        /*
        FileBean activeFile = (FileBean) getContext().getActiveNodeBean();
        FileHandler fh = getContext().getFileHandler();

        InputStream is = fh.getContent(activeFile);
        return getContentAsString(is);
        */
        return null;
    }

    public String getComparingVersionContent() {
        /*
        FileBean activeFile = (FileBean) getContext().getActiveNodeBean();
        FileHandler fh = getContext().getFileHandler();

        InputStream is = fh.getContent4Version(activeFile, compareWithVersion);
        return getContentAsString(is);
        */
        return null;
    }

    public String getBaseVersionName() {
       // return getContext().getActiveNodeBean().getVersion();
        return null;
    }

    public String getComparingVersionName() {
        return compareWithVersion;
    }

    // --- private

    private String getParamValue(FacesContext context, String name) {
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        return params.get(name);
    }

    private String getContentAsString(InputStream is) {
        if (is == null) {
            return "";
        }

        InputStreamReader isr = null;
        StringBuffer sb = new StringBuffer(1024);

        try {
            isr = new InputStreamReader(is);
            int i = isr.read();
            while (i >= 0) {
                sb.append((char) i);
                i = isr.read();
            }
        } catch (IOException e) {
            //getContext().getMessageQueue().addMessage(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException e) {
                //getContext().getMessageQueue().addMessage(e);
            }
        }

        return sb.toString();
    }
}
