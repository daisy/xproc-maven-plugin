package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.google.common.base.Predicates;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import static org.daisy.pipeline.maven.xproc.utils.asURI;
import static org.daisy.pipeline.maven.xproc.utils.unpack;
import static org.daisy.pipeline.maven.xproc.utils.evaluateXPath;

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
	
	private static Map<String,String> ns = new HashMap<String,String>();
	static {
		ns.put("x", "http://www.daisy.org/ns/pipeline/xproc/test");
		ns.put("c", "http://www.w3.org/ns/xproc-step");
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		getLog().info("------------------------------------------------------------------------");
		getLog().info("XPROCSPEC TESTS");
		getLog().info("------------------------------------------------------------------------");
		
		try {
			String pipeline;
			if (engine instanceof Calabash)
				pipeline = asURI(XProcSpecMojo.class.getResource("/xml/xproc/xprocspec.xpl"));
			else {
				File srcDir = new File(tempDir, "src");
				for (String resource : new Reflections("xml.xproc", new ResourcesScanner()).getResources(Predicates.<String>alwaysTrue()))
					unpack(XProcSpecMojo.class.getResource("/" + resource), new File(srcDir, resource.substring("xml/xproc/".length())));
				pipeline = asURI(new File(srcDir, "xprocspec.xpl")); }
			reportsDirectory.mkdirs();
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setBasedir(xprocspecDirectory);
			scanner.setIncludes(new String[]{"*.xprocspec"});
			scanner.scan();
			String[] tests = scanner.getIncludedFiles();
			List<String> failures = new ArrayList<String>();
			List<String> errors = new ArrayList<String>();
			for (String test : tests) {
				Map<String,String> input = new HashMap<String,String>();
				Map<String,String> output = new HashMap<String,String>();
				Map<String,String> options = new HashMap<String,String>();
				input.put("source", asURI(new File(xprocspecDirectory, test)));
				String testName = test.replaceAll("^(.*)\\.xprocspec$", "$1");
				File report = new File(reportsDirectory, testName + ".xml");
				output.put("result", asURI(report));
				output.put("junit", asURI(new File(surefireReportsDirectory, "TEST-" + testName + ".xml")));
				options.put("temp-dir", asURI(tempDir) + "/tmp/");
				getLog().info("Running: " + testName);
				engine.run(pipeline, input, output, options, null);
				if (!(Boolean)evaluateXPath(report, "//x:scenario-results[not(x:test-result/@result='true')]", ns, Boolean.class))
					getLog().info("...SUCCESS");
				else if ((Boolean)evaluateXPath(report, "//x:test-result/c:was/c:errors", ns, Boolean.class)) {
					errors.add(testName);
					getLog().info("...ERROR"); }
				else {
					failures.add(testName);
					getLog().info("...FAILED"); }}
			getLog().info("");
			getLog().info("Summary:");
			getLog().info("--------");
			getLog().info("");
			if (failures.size() > 0) {
				getLog().info("Failed tests:");
				for (String s : failures)
					getLog().info("\t" + s);
				getLog().info(""); }
			if (errors.size() > 0) {
				getLog().info("Tests in error:");
				for (String s : errors)
					getLog().info("\t" + s);
				getLog().info(""); }
			getLog().info("Tests run: " + tests.length + ", Failures: " + failures.size() + ", Errors: "+ errors.size());
			getLog().info("");
			if (failures.size() + errors.size() > 0)
				throw new MojoFailureException("Some tests failed or had errors."); }
		catch (MojoFailureException e) {
			throw e; }
		catch (Exception e) {
			throw new MojoExecutionException("Error running XProcSpec test suite", e); }
		
	}
}
