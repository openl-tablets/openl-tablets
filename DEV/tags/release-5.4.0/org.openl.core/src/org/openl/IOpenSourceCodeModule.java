/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import java.io.InputStream;
import java.io.Reader;

/**
 * IOpenSourceCodeModule is an abstraction of rules source code.
 * 
 * @author snshor
 */
public interface IOpenSourceCodeModule {

    InputStream getByteStream();

    Reader getCharacterStream();

    String getCode();

    /**
     * 
     * @return relative start position within a module
     */
    int getStartPosition();

    int getTabSize();

    String getUri(int textpos);

}
