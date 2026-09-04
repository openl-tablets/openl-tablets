/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.fast.FastStringReader;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class StringSourceCodeModule implements IOpenSourceCodeModule {

    @Getter
    private final String code;
    @Getter
    private final String uri;

    @Getter
    @Setter
    private Map<String, Object> params;

    @Override
    public InputStream getByteStream() {
        return new ByteArrayInputStream(code.getBytes());
    }

    @Override
    public Reader getCharacterStream() {
        return new FastStringReader(code);
    }

    @Override
    public int getStartPosition() {
        return 0;
    }
}
