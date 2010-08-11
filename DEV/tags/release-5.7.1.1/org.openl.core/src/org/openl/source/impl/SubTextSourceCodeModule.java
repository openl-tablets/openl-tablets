/*
 * Created on Jan 13, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 * Contains substring of the base code between start and end positions. End
 * position can have either positive or negative values. If positive, it is the
 * absolute end position from the beginning of the base code, if negative it is
 * the relative position from the end o fthe base code
 *
 */
public class SubTextSourceCodeModule implements IOpenSourceCodeModule {

    IOpenSourceCodeModule baseModule;
    int startPosition;
    int endPosition = 0;

    boolean skipStart = false;

    /**
     *
     */
    public SubTextSourceCodeModule(IOpenSourceCodeModule baseModule, int startPosition) {
        this.baseModule = baseModule;
        this.startPosition = startPosition;
    }

    public SubTextSourceCodeModule(IOpenSourceCodeModule baseModule, int startPosition, int endPosition) {
        this.baseModule = baseModule;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     *
     */

    public InputStream getByteStream() {
        return null;
    }

    public Reader getCharacterStream() {
        return new StringReader(getCode());
    }

    /**
     *
     */

    public String getCode() {
        String code = baseModule.getCode();

        int end = endPosition <= 0 ? code.length() + endPosition : endPosition;
        return code.substring(startPosition, end);
    }

    /**
     *
     */

    public int getStartPosition() {
        return startPosition;
    }

    /**
     *
     */

    public int getTabSize() {
        return baseModule.getTabSize();
    }

    /**
     *
     */

    public String getUri(int textpos) {
        return baseModule.getUri(textpos + startPosition);
    }

}
