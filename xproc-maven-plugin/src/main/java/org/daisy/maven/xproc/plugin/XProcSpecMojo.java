package org.daisy.maven.xproc.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

import org.daisy.maven.xproc.xprocspec.XProcSpecRunner;
import org.daisy.maven.xproc.xprocspec.XProcSpecRunner.Reporter;

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
