package org.daisy.maven.xproc;

public class XProcExecutionException extends RuntimeException {
	
	public XProcExecutionException(String message) {
		super(message);
	}
	
	public XProcExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
