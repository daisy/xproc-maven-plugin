package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import net.sf.saxon.xpath.XPathFactoryImpl;

import org.xml.sax.InputSource;

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
	
	private static XPath xpath = new XPathFactoryImpl().newXPath();
	
	public static Object evaluateXPath(File file, String expression, final Map<String,String> namespaces, Class<?> type) {
		try {
			if (namespaces != null)
				xpath.setNamespaceContext(
					new NamespaceContext() {
						public String getNamespaceURI(String prefix) {
							return namespaces.get(prefix); }
						public String getPrefix(String namespaceURI) {
							for (String prefix : namespaces.keySet())
								if (namespaces.get(prefix).equals(namespaceURI))
									return prefix;
							return null; }
						public Iterator<String> getPrefixes(String namespaceURI) {
							List<String> prefixes = new ArrayList<String>();
							for (String prefix : namespaces.keySet())
								if (namespaces.get(prefix).equals(namespaceURI))
									prefixes.add(prefix);
							return prefixes.iterator(); }});
			else
				xpath.setNamespaceContext(null);
			XPathExpression expr = xpath.compile(expression);
			InputSource source = new InputSource(file.toURI().toURL().openStream());
			if (type.equals(Boolean.class))
				return expr.evaluate(source, XPathConstants.BOOLEAN);
			if (type.equals(String.class))
				return expr.evaluate(source, XPathConstants.STRING);
			else
				throw new RuntimeException("Cannot evaluate to a " + type.getName()); }
		catch (Exception e) {
			throw new RuntimeException("Exception occured during XPath evaluation.", e); }
		
	}
}
