package com.exigen.openl.component;

public interface ErrorListener {

	void parsingError(String message, Throwable cause, String location);

	void bindingError(String message, Throwable cause, String location);

}
