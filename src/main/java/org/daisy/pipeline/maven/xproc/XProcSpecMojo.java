package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

import static org.daisy.pipeline.maven.xproc.utils.asURI;

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
	
	private static final String pipeline = asURI(XProcSpecMojo.class.getResource("/xml/xproc/xprocspec.xpl"));
	
	public void execute() throws MojoExecutionException {
		try {
			reportsDirectory.mkdirs();
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setBasedir(xprocspecDirectory);
			scanner.setIncludes(new String[]{"*.xprocspec"});
			scanner.scan();
			for (String f : scanner.getIncludedFiles()) {
				Map<String,String> input = new HashMap<String,String>();
				Map<String,String> output = new HashMap<String,String>();
				input.put("source", asURI(new File(xprocspecDirectory, f)));
				output.put("junit", asURI(new File(reportsDirectory, f.replaceAll("^(.*)\\.xprocspec$", "TEST-$1.xml"))));
				getLog().info("Running XProcSpec test '" + f + "' ...");
				engine.run(pipeline, input, output, null, null); }}
		catch (Exception e) {
			throw new MojoExecutionException("Error running XProcSpec test", e); }
	}
}
