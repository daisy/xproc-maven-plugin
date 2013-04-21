package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.net.URI;
import java.net.URL;

public abstract class utils {
	
	public static String asURI(Object href) {
		try {
			if (href instanceof File)
				return asURI(((File)href).toURI());
			if (href instanceof URL) {
				URL url = (URL)href;
				if (url.getProtocol().equals("jar"))
					return "jar:" + asURI(new URI(null, url.getAuthority(), url.getPath(), url.getQuery(), url.getRef()));
				else
					return asURI(new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef())); }
			if (href instanceof URI)
				return ((URI)href).toASCIIString(); }
		catch (Exception e) {}
		throw new RuntimeException("Object can not be converted to URI: " + href);
	}
	
	public static String resolve(String href, String base) {
		try {
			if (base == null)
				return href;
			else if (base.startsWith("jar:"))
				return new URL(new URL(base), href).toString();
			else
				return new URI(base).resolve(new URI(href)).toASCIIString(); }
		catch (Exception e) { throw new RuntimeException(e); }
	}
}
