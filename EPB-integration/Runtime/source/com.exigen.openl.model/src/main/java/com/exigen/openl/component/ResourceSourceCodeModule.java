package com.exigen.openl.component;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.openl.syntax.impl.ASourceCodeModule;
import org.openl.util.RuntimeExceptionWrapper;

public class ResourceSourceCodeModule extends ASourceCodeModule {
	ClassLoader classloader;

	String resourceName;

	String uri;

	public ResourceSourceCodeModule(ClassLoader classloader,
			String resourceName, String uri, int tabSize) {
		this(classloader, resourceName, uri);
		setTabSize(tabSize);

	}

	/**
	 * 
	 */
	public ResourceSourceCodeModule(ClassLoader classloader,
			String resourceName, String uri) {
		this.classloader = classloader;
		this.resourceName = resourceName;
		this.uri = uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenSourceCodeModule#getUri(int)
	 */
	public String getUri(int textpos) {
		try {
			return uri != null ? uri : "file:///" + resourceName;
		} catch (Exception e) {
			throw RuntimeExceptionWrapper.wrap("", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenSourceCodeModule#getByteStream()
	 */
	public InputStream getByteStream() {
		return classloader.getResourceAsStream(resourceName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenSourceCodeModule#getCharacterStream()
	 */
	public Reader getCharacterStream() {
		return new InputStreamReader(getByteStream());

	}

}
