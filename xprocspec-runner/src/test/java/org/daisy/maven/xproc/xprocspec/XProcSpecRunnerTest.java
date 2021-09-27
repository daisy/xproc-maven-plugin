package org.daisy.maven.xproc.xprocspec;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import org.daisy.maven.xproc.xprocspec.XProcSpecRunner.Reporter;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.Test;

public class XProcSpecRunnerTest {
	
	private static File testsDir = new File(new File(XProcSpecRunnerTest.class.getResource("/").getPath()), "xprocspec");
	private static File logbackXml = new File(new File(XProcSpecRunnerTest.class.getResource("/").getPath()), "logback.xml");
	private XProcSpecRunner xprocspecRunner;
	private File reportsDir;
	private File surefireReportsDir;
	private File tempDir;
	
	@Rule
	public TestWatcher deleteTempDirsWhenTestSucceeded = new TestWatcher() {
		@Override
		protected void failed(Throwable e, org.junit.runner.Description d) {
			System.out.println(d.getMethodName() + " failed");
			System.out.println("-> reportsDir was: " + reportsDir);
			System.out.println("-> surefireReportsDir was: " + surefireReportsDir);
			System.out.println("-> tempDir was: " + tempDir);
		}
		@Override
		protected void succeeded(org.junit.runner.Description d) {
			reportsDir.deleteOnExit();
			surefireReportsDir.deleteOnExit();
			tempDir.deleteOnExit();
		}
	};
	
	@Before
	public void setup() {
		xprocspecRunner = ServiceLoader.load(XProcSpecRunner.class).iterator().next();
		reportsDir = Files.createTempDir();
		surefireReportsDir = Files.createTempDir();
		tempDir = Files.createTempDir();
		System.setProperty("logback.configurationFile", logbackXml.toURI().toASCIIString());
	}
	
	@Test
	public void testSuccess() {
		Map<String,File> tests = ImmutableMap.of("test_identity", new File(testsDir, "test_identity.xprocspec"),
		                                         "test_throw_error", new File(testsDir, "test_throw_error.xprocspec"));
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_identity"                                                                  + "\n" +
"Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... sec"                + "\n" +
"Running test_throw_error"                                                               + "\n" +
"Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... sec"                + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 4, Failures: 0, Errors: 0, Skipped: 0"                                       + "\n"));
		assertTrue(new File(reportsDir, "test_identity.html").exists());
		assertTrue(new File(surefireReportsDir, "TEST-test_identity.xml").exists());
	}
	
	@Test
	public void testFailure() {
		Map<String,File> tests = ImmutableMap.of(
			// step does not behave as expected
			"test_identity_broken", new File(testsDir, "test_identity_broken.xprocspec")
		);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_identity_broken"                                                           + "\n" +
"Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: ... sec <<< FAILURE!"   + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Failed tests:"                                                                          + "\n" +
"  test_identity_broken"                                                                 + "\n" +
"    Identity"                                                                           + "\n" +
"      * FAILURE: the option 'option.required' should have the value 'option.required-value'" + "\n" +
""                                                                                       + "\n" +
"Tests run: 3, Failures: 1, Errors: 0, Skipped: 0"                                       + "\n"));
		assertTrue(new File(reportsDir, "test_identity_broken.html").exists());
		assertTrue(new File(surefireReportsDir, "TEST-test_identity_broken.xml").exists());
	}
	
	@Test
	public void testError() {
		Map<String,File> tests = ImmutableMap.of(
			// script file doesn't exist
			"test_non_existing", new File(testsDir, "test_non_existing.xprocspec"),
			// xprocspec file doesn't exist
			"non_existing_test", new File(testsDir, "non_existing_test.xprocspec"),
			// step throws an unexpected xproc error
			"test_throw_error_unexpected", new File(testsDir, "test_throw_error_unexpected.xprocspec"),
			// step throws a java error
			"test_throw_java_error", new File(testsDir, "test_throw_java_error.xprocspec")
		);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new File(testsDir, "throw_java_error_implementation_java.xml"),
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_non_existing"                                                              + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Error loading test description"                                                         + "\n" +
"line: 112"                                                                              + "\n" +
"column: 26"                                                                             + "\n" +
""                                                                                       + "\n" +
"                <message> * unable to load script: .../test-classes/xprocspec/non_existing.xpl</message>\n" +
"            "                                                                           + "\n" +
"<x:was...><x:description..."                                                            + "\n" +
"..."                                                                                    + "\n" +
"</x:description></x:was>"                                                               + "\n" +
"Running non_existing_test"                                                              + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Test file does not exist: .../test-classes/xprocspec/non_existing_test.xprocspec"       + "\n" +
"Running test_throw_error_unexpected"                                                    + "\n" +
"Tests run: 2, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Error evaluating assertion"                                                             + "\n" +
"line: 112"                                                                              + "\n" +
"column: 26"                                                                             + "\n" +
""                                                                                       + "\n" +
"                <message>       * port not found: result</message>"                     + "\n" +
"            "                                                                           + "\n" +
"<x:was...><x:description..."                                                            + "\n" +
"..."                                                                                    + "\n" +
"...<c:error code=\"foo\"...><message xmlns:ex=\"http://example.net/ns\">foobar</message></c:error>...</x:description>\n" +
"        </x:was>"                                                                       + "\n" +
"Running test_throw_java_error"                                                          + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Error evaluating assertion"                                                             + "\n" +
"line: 112"                                                                              + "\n" +
"column: 26"                                                                             + "\n" +
""                                                                                       + "\n" +
"                <message>       * port not found: result</message>"                     + "\n" +
"            "                                                                           + "\n" +
"<x:was...><x:description..."                                                            + "\n" +
"..."                                                                                    + "\n" +
"...<c:error href=\"ThrowJavaError.java\" line=\"17\">boom!</c:error>...</x:description>"+ "\n" +
"        </x:was>"                                                                       + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests in error:"                                                                        + "\n" +
"  test_non_existing"                                                                    + "\n" +
"    * ERROR: Error loading test description"                                            + "\n" +
"  non_existing_test"                                                                    + "\n" +
"    * ERROR: Test file does not exist"                                                  + "\n" +
"  test_throw_error_unexpected"                                                          + "\n" +
// FIXME: should not be a "compilationError"
"    Unexpected error"                                                                   + "\n" +
"      * ERROR: Error evaluating assertion"                                              + "\n" +
"  test_throw_java_error"                                                                + "\n" +
// FIXME: should not be a "compilationError"
"    Unexpected Java error"                                                              + "\n" +
"      * ERROR: Error evaluating assertion"                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 5, Failures: 0, Errors: 4, Skipped: 0"                                       + "\n"));
		assertTrue(new File(reportsDir, "test_non_existing.html").exists());
		assertTrue(new File(surefireReportsDir, "TEST-test_non_existing.xml").exists());
		assertFalse(new File(reportsDir, "non_existing_test.html").exists());
		assertFalse(new File(surefireReportsDir, "TEST-non_existing_test.xml").exists());
	}
	
	@Test
	public void testPending() {
		Map<String,File> tests = ImmutableMap.of("test_identity_pending", new File(testsDir, "test_identity_pending.xprocspec"));
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_identity_pending"                                                          + "\n" +
"Tests run: 4, Failures: 0, Errors: 0, Skipped: 2, Time elapsed: ... sec"                + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 4, Failures: 0, Errors: 0, Skipped: 2"                                       + "\n"));
		assertTrue(new File(reportsDir, "test_identity_pending.html").exists());
		assertTrue(new File(surefireReportsDir, "TEST-test_identity_pending.xml").exists());
	}
	
	@Test
	public void testMocking() {
		Map<String,File> tests = ImmutableMap.of("test_foo_catalog", new File(testsDir, "test_foo_catalog.xprocspec"));
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_foo_catalog"                                                               + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Error loading test description"                                                         + "\n" +
"line: 112"                                                                              + "\n" +
"column: 26"                                                                             + "\n" +
""                                                                                       + "\n" +
"                <message> * unable to load script: http://unexisting_domain/foo.xpl</message>\n" +
"            "                                                                           + "\n" +
"<x:was...><x:description..."                                                            + "\n" +
"..."                                                                                    + "\n" +
"</x:description></x:was>"                                                               + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests in error:"                                                                        + "\n" +
"  test_foo_catalog"                                                                     + "\n" +
"    * ERROR: Error loading test description"                                            + "\n" +
""                                                                                       + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0"                                       + "\n"));
		File catalog = new File(testsDir, "foo_catalog.xml");
		stream.reset();
		setup();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, catalog,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_foo_catalog"                                                               + "\n" +
"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... sec"                + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0"                                       + "\n"));
	}
	
	@Test
	public void testCustomJavaStep() {
		Map<String,File> tests = ImmutableMap.of(
			"test_foo_java", new File(testsDir, "test_foo_java.xprocspec"));
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_foo_java"                                                                  + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ... sec <<< ERROR!"     + "\n" +
"Error evaluating assertion"                                                             + "\n" +
"line: 112"                                                                              + "\n" +
"column: 26"                                                                             + "\n" +
""                                                                                       + "\n" +
"                <message>       * port not found: result</message>"                     + "\n" +
"            "                                                                           + "\n" +
"<x:was...><x:description..."                                                            + "\n" +
"..."                                                                                    + "\n" +
"...<c:error>Misconfigured. No 'class' in configuration for ex:foo</c:error>...</x:description>\n" +
"        </x:was>"                                                                       + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests in error:"                                                                        + "\n" +
"  test_foo_java"                                                                        + "\n" +
"    Foo"                                                                                + "\n" +
"      * ERROR: Error evaluating assertion"                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 1, Failures: 0, Errors: 1, Skipped: 0"                                       + "\n"));
		stream.reset();
		setup();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new File(testsDir, "foo_implementation_java.xml"),
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"Running test_foo_java"                                                                  + "\n" +
"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... sec"                + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0"                                       + "\n"));
	}
	
	@Test
	public void testCustomAssertion() {
		assertTrue(xprocspecRunner.run(ImmutableMap.of("test_custom_assertion",
		                                               new File(testsDir, "test_custom_assertion.xprocspec")),
		                               reportsDir, surefireReportsDir, tempDir, null,
		                               new XProcSpecRunner.Reporter.DefaultReporter()));
	}
	
	@Test
	public void testNothing() {
		Map<String,File> tests = ImmutableMap.of();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		xprocspecRunner.run(tests, reportsDir, surefireReportsDir, tempDir, null,
		                    new Reporter.DefaultReporter(new PrintStream(stream, true)));
		assertThat(stream.toString(), matchesPattern(
"-------------------------------------------------------"                                + "\n" +
" X P R O C S P E C   T E S T S"                                                         + "\n" +
"-------------------------------------------------------"                                + "\n" +
"There are no tests to run."                                                             + "\n" +
""                                                                                       + "\n" +
"Results :"                                                                              + "\n" +
""                                                                                       + "\n" +
"Tests run: 0, Failures: 0, Errors: 0, Skipped: 0"                                       + "\n"));
		assertTrue(new File(reportsDir, "index.html").exists());
	}
	
	public static class PatternMatcher extends BaseMatcher<String> {
		private final String pattern;
		private final String regex;
		public PatternMatcher(String pattern) {
			this.pattern = pattern;
			this.regex = "(?m)\\Q" + pattern
				.replaceAll("(?m)^...$", "\\\\E[\\\\S\\\\s]+?\\\\Q")
				.replace("...", "\\E.+?\\Q")
				+ "\\E";
		}
		public boolean matches(Object o){
			return ((String)o).matches(regex);
		}
		public void describeTo(Description description) {
			description.appendText("matches pattern\n");
			description.appendText(pattern);
		}
		@Override
		public void describeMismatch(Object item, Description description) {
			description.appendText("was\n");
			description.appendText(item.toString());
		}
	}
	
	public static PatternMatcher matchesPattern(String pattern) {
		return new PatternMatcher(pattern);
	}
}
