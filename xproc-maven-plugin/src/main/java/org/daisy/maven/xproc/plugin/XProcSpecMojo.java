package org.daisy.maven.xproc.plugin;

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.daisy.maven.xproc.xprocspec.XProcSpecRunner;
import org.daisy.maven.xproc.xprocspec.XProcSpecRunner.Reporter;

import org.slf4j.bridge.SLF4JBridgeHandler;

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
	 * Set this to "true" to skip running tests, but still compile them. Its use
	 * is NOT RECOMMENDED, but quite convenient on occasion.
	 *
	 * @parameter property="skipTests" default-value="false"
	 */
	private boolean skipTests;

	/**
	 * Set this to "true" to bypass unit tests entirely. Its use is NOT
	 * RECOMMENDED, especially if you enable it using the "maven.test.skip"
	 * property, because maven.test.skip disables both running the tests and
	 * compiling the tests. Consider using the <code>skipTests</code> parameter
	 * instead.
	 *
	 * @parameter expression="${maven.test.skip}" default-value="false"
	 */
	private boolean skip;
	
	/**
	 * Temporary directory for storing XProcSpec related files.
	 *
	 * @parameter default-value="${project.build.directory}/xprocspec"
	 * @required
	 * @readonly
	 */
	private File tempDir;
	
	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;
	
	public void execute() throws MojoFailureException {
		
		File logbackXml = new File(new File(project.getBuild().getTestOutputDirectory()), "logback.xml");
		if (logbackXml.exists())
			System.setProperty("logback.configurationFile", logbackXml.toURI().toASCIIString());
		LogManager.getLogManager().reset();
		SLF4JBridgeHandler.install();
		Logger.getLogger("").setLevel(Level.FINEST);
		
		if (skip || skipTests) {
			getLog().info("Tests are skipped.");
			return; }
		
		XProcSpecRunner runner = new XProcSpecRunner();
		Reporter.DefaultReporter reporter = new Reporter.DefaultReporter(System.out);
		
		if (!runner.run(xprocspecDirectory,
		                reportsDirectory,
		                surefireReportsDirectory,
		                tempDir,
		                reporter))
			throw new MojoFailureException("There are test failures.");
		
	}
}
