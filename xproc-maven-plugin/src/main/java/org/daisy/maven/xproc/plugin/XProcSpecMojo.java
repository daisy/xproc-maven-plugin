package org.daisy.maven.xproc.plugin;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import org.daisy.maven.xproc.xprocspec.XProcSpecRunner;
import org.daisy.maven.xproc.xprocspec.XProcSpecRunner.TestLogger;
import org.daisy.maven.xproc.xprocspec.XProcSpecRunner.TestResult;

/**
 * Run an XProcSpec test.
 *
 * @goal xprocspec
 */
public class XProcSpecMojo extends AbstractMojo {
	
	/**
	 * Directory containing the XProcSpec tests.
	 *
	 * @parameter expression="${project.basedir}/src/test/xprocspec"
	 * @required
	 */
	private File xprocspecDirectory;
	
	/**
	 * Directory that will contain the generated reports.
	 *
	 * @parameter expression="${project.build.directory}/xprocspec-reports"
	 * @required
	 */
	private File reportsDirectory;
	
	/**
	 * Directory that will contain the generated Surefire reports.
	 *
	 * @parameter expression="${project.build.directory}/surefire-reports"
	 * @required
	 */
	private File surefireReportsDirectory;
	
	/**
	 * Temporary directory for storing XProcSpec related files.
	 *
	 * @parameter default-value="${project.build.directory}/xprocspec"
	 * @required
	 * @readonly
	 */
	private File tempDir;
	
	public void execute() throws MojoFailureException {
		
		final Log logger = getLog();
		
		TestLogger testLogger = new TestLogger() {
			public void info(String message) { logger.info(message); }
			public void warn(String message) { logger.warn(message); }
			public void error(String message) { logger.error(message); }
			public void debug(String message) { logger.debug(message); }
		};
		
		DirectoryScanner scanner = new DirectoryScanner();
		
		scanner.setBasedir(xprocspecDirectory);
		scanner.setIncludes(new String[]{"*.xprocspec"});
		scanner.scan();
		String[] tests = scanner.getIncludedFiles();
		
		logger.info("------------------------------------------------------------------------");
		logger.info("XPROCSPEC TESTS");
		logger.info("------------------------------------------------------------------------");
		
		XProcSpecRunner runner = new XProcSpecRunner();
		
		TestResult[] testResults = runner.run(xprocspecDirectory,
		                                      tests,
		                                      reportsDirectory,
		                                      surefireReportsDirectory,
		                                      tempDir,
		                                      testLogger);
		
		int run = testResults.length;
		ArrayList<String> failures = new ArrayList<String>();
		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> skipped = new ArrayList<String>();
		
		for (TestResult result : testResults) {
			switch (result.state) {
			case FAILURE:
				failures.add(result.name);
				break;
			case ERROR:
				errors.add(result.name);
				break;
			case SKIPPED:
				skipped.add(result.name);
				break;
			}
		}
		
		logger.info("");
		logger.info("Summary:");
		logger.info("--------");
		logger.info("");
		if (failures.size() > 0) {
			logger.info("Failed tests:");
			for (String s : failures)
				logger.info("\t" + s);
			logger.info(""); }
		if (errors.size() > 0) {
			logger.info("Tests in error:");
			for (String s : errors)
				logger.info("\t" + s);
			logger.info(""); }
		logger.info("Tests run: " + run + ", Failures: " + failures.size() + ", Errors: "+ errors.size());
		logger.info("");
		if (failures.size() + errors.size() > 0)
			throw new MojoFailureException("Some tests failed or had errors.");
		
	}
}
