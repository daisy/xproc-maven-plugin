package org.daisy.maven.xproc.xprocspec;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	
	public TestResult[] run(Map<String,File> tests,
	                        File reportsDir,
	                        File surefireReportsDir,
	                        File tempDir,
	                        TestLogger logger) {
		
		if (engine == null)
			activate();
		
		if (logger == null)
			logger = new TestLogger.NULL();
		
		URI pipeline = asURI(XProcSpecRunner.class.getResource("/content/xml/xproc/xprocspec.xpl"));
		
		reportsDir.mkdirs();
		surefireReportsDir.mkdirs();
		
		TestResult[] testResults = new TestResult[tests.size()];
		int i = 0;
		for (String testName : tests.keySet()) {
			Map<String,List<String>> input = new HashMap<String,List<String>>();
			Map<String,String> output = new HashMap<String,String>();
			Map<String,String> options = new HashMap<String,String>();
			File test = tests.get(testName);
			File report = new File(reportsDir, testName + ".xml");
			File surefireReport = new File(surefireReportsDir, "TEST-" + testName + ".xml");
			input.put("source", Arrays.asList(new String[]{asURI(test).toASCIIString()}));
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
	
	public TestResult[] run(File testsDir,
	                        File reportsDir,
	                        File surefireReportsDir,
	                        File tempDir,
	                        TestLogger logger) {
		
		Map<String,File> tests = new HashMap<String,File>();
		for (File file : listXProcSpecFilesRecursively(testsDir))
			tests.put(
				file.getAbsolutePath().substring(testsDir.getAbsolutePath().length() + 1)
					.replaceAll("\\.xprocspec$", "")
					.replaceAll("[\\./\\\\]", "_"),
				file);
		return run(tests, reportsDir, surefireReportsDir, tempDir, logger);
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
		private static int count(TestResult[] results, TestState state) {
			int c = 0;
			for (TestResult result : results)
				if (result.state == state)
					c++;
			return c;
		}
		public static int getFailures(TestResult[] results) {
			return count(results, TestState.FAILURE);
		}
		public static int getErrors(TestResult[] results) {
			return count(results, TestState.ERROR);
		}
	}
	
	public static interface TestLogger {
		public void info(String message);
		public void warn(String message);
		public void error(String message);
		public void debug(String message);
		public static class NULL implements TestLogger {
			public void info(String message) {}
			public void warn(String message) {}
			public void error(String message) {}
			public void debug(String message) {}
		}
		public static class PrintStreamLogger implements TestLogger {
			private final PrintStream stream;
			public PrintStreamLogger(PrintStream stream) {
				this.stream = stream;
			}
			public void info(String message) { stream.println("[INFO] " + message); }
			public void warn(String message) { stream.println("[WARNING] " + message); }
			public void error(String message) { stream.println("[ERROR] " + message); }
			public void debug(String message) { stream.println("[DEBUG] " + message); }
		}
	}
	
	/*
	 * FileUtils.listFiles from Apache Commons IO could be used here as well,
	 * but would introduce another dependency.
	 */
	private static Collection<File> listXProcSpecFilesRecursively(File directory) {
		ImmutableList.Builder<File> builder = new ImmutableList.Builder<File>();
		for (File file : directory.listFiles()) {
			if (file.isDirectory())
				builder.addAll(listXProcSpecFilesRecursively(file));
			else if (file.getName().endsWith(".xprocspec"))
				builder.add(file); }
		return builder.build();
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
				String authority = (url.getPort() != -1) ?
					url.getHost() + ":" + url.getPort() :
					url.getHost();
				return new URI(url.getProtocol(), authority, url.getPath(), url.getQuery(), url.getRef()); }}
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
