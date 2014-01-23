package org.daisy.maven.xproc.xprocspec;

import com.google.common.base.Throwables;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import net.sf.saxon.xpath.XPathFactoryImpl;

import org.daisy.maven.xproc.api.XProcExecutionException;
import org.daisy.maven.xproc.api.XProcEngine;

import org.xml.sax.InputSource;

public class XProcSpecRunner {
	
	private XProcEngine engine;
	
	protected void setXProcEngine(XProcEngine engine) {
		this.engine = engine;
	}
	
	protected void activate() {
		if (engine == null) {
			
			// We are not in an OSGi environment, try with ServiceLoader
			ServiceLoader<XProcEngine> xprocEngines = ServiceLoader.load(XProcEngine.class);
			try {
				engine = xprocEngines.iterator().next();
			} catch (NoSuchElementException e) {
				throw new RuntimeException("Could not find any XProc engines on the classpath.");
			}
		}
	}
	
	public TestResult[] run(File testsDir,
	                        String[] tests,
	                        File reportsDir,
	                        File surefireReportsDir,
	                        File tempDir,
	                        TestLogger logger) {
		
		if (engine == null)
			activate();
		
		if (logger == null)
			logger = new TestLogger() {
				public void info(String message) {}
				public void warn(String message) {}
				public void error(String message) {}
				public void debug(String message) {}};
		
		URI pipeline = asURI(XProcSpecRunner.class.getResource("/content/xml/xproc/xprocspec.xpl"));
		
		reportsDir.mkdirs();
		surefireReportsDir.mkdirs();
		
		TestResult[] testResults = new TestResult[tests.length];
		int i = 0;
		for (String test : tests) {
			Map<String,List<String>> input = new HashMap<String,List<String>>();
			Map<String,String> output = new HashMap<String,String>();
			Map<String,String> options = new HashMap<String,String>();
			input.put("source", Arrays.asList(new String[]{asURI(new File(testsDir, test)).toASCIIString()}));
			String testName = test.replaceAll("\\.xprocspec$", "").replaceAll("[\\./\\\\]", "_");
			File report = new File(reportsDir, testName + ".xml");
			File surefireReport = new File(surefireReportsDir, "TEST-" + testName + ".xml");
			output.put("result", asURI(report).toASCIIString());
			output.put("junit", asURI(surefireReport).toASCIIString());
			options.put("temp-dir", asURI(tempDir) + "/tmp/");
			logger.info("Running: " + testName);
			try {
				engine.run(pipeline.toASCIIString(), input, output, options, null);
				if ((Boolean)evaluateXPath(surefireReport, "number(/testsuites/@errors) > 0", null, Boolean.class)) {
					testResults[i] = TestResult.ERROR(testName);
					logger.info("...ERROR"); }
				else if ((Boolean)evaluateXPath(surefireReport, "number(/testsuites/@failures) > 0", null, Boolean.class)) {
					testResults[i] = TestResult.FAILURE(testName);
					logger.info("...FAILED"); }
				else {
					testResults[i] = TestResult.SUCCESS(testName);
					logger.info("...SUCCESS"); }}
			catch (XProcExecutionException e) {
				testResults[i] = TestResult.ERROR(testName, e);
				logger.info("...ERROR");
				logger.error(e.getMessage());
				Throwable cause = e.getCause();
				if (cause != null)
					logger.debug(Throwables.getStackTraceAsString(cause)); }
			i++;
		}
		
		return testResults;
	}
	
	public static class TestResult {
		public static enum TestState {
			SUCCESS,
			FAILURE,
			ERROR,
			SKIPPED
		}
		public final String name;
		public final TestState state;
		public final String detail;
		private TestResult(String name, TestState state) {
			this(name, state, null);
		}
		private TestResult(String name, TestState state, String detail) {
			this.name = name;
			this.state = state;
			this.detail = detail;
		}
		private static TestResult SUCCESS(String name) {
			return new TestResult(name, TestState.SUCCESS);
		}
		private static TestResult FAILURE(String name) {
			return new TestResult(name, TestState.FAILURE);
		}
		private static TestResult ERROR(String name) {
			return new TestResult(name, TestState.ERROR);
		}
		private static TestResult ERROR(String name, Throwable cause) {
			return new TestResult(name, TestState.ERROR, cause.getMessage());
		}
		@SuppressWarnings("unused")
		private static TestResult SKIPPED(String name) {
			return new TestResult(name, TestState.SKIPPED);
		}
	}
	
	public static interface TestLogger {
		public void info(String message);
		public void warn(String message);
		public void error(String message);
		public void debug(String message);
	}
	
	public static URI asURI(Object o) {
		try {
			if (o instanceof URI)
				return (URI)o;
			if (o instanceof File)
				return asURI(((File)o).toURI());
			if (o instanceof URL) {
				URL url = (URL)o;
				if (url.getProtocol().equals("jar"))
					return new URI("jar:" + new URI(null, url.getAuthority(), url.getPath(), url.getQuery(), url.getRef()).toASCIIString());
				else
					return new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef()); }}
		catch (Exception e) {}
		throw new RuntimeException("Object can not be converted to URI: " + o);
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
