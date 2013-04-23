package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

	public static boolean unpack(URL url, File file) {
		if (file.exists()) return false;
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream writer = new FileOutputStream(file);
			url.openConnection();
			InputStream reader = url.openStream();
			byte[] buffer = new byte[153600];
			int bytesRead = 0;
			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[153600]; }
			writer.close();
			reader.close();
			return true; }
		catch (Exception e) {
			throw new RuntimeException("Exception occured during unpacking of file '" + file.getName() + "'", e); }
	}
}
