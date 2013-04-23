package org.daisy.pipeline.maven.xproc;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import com.google.common.base.Predicates;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import static org.daisy.pipeline.maven.xproc.utils.asURI;
import static org.daisy.pipeline.maven.xproc.utils.unpack;

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
	 * Temporary directory for storing XProcSpec source files.
	 *
	 * @parameter default-value="${project.build.directory}/xprocspec"
	 * @required
	 * @readonly
	 */
	private File tempDir;
	
	public void execute() throws MojoExecutionException {
		try {
			String pipeline;
			if (engine instanceof Calabash)
				pipeline = asURI(XProcSpecMojo.class.getResource("/xml/xproc/xprocspec.xpl"));
			else {
				for (String resource : new Reflections("xml.xproc", new ResourcesScanner()).getResources(Predicates.<String>alwaysTrue()))
					unpack(XProcSpecMojo.class.getResource("/" + resource), new File(tempDir, resource.substring("xml/xproc/".length())));
				pipeline = asURI(new File(tempDir, "xprocspec.xpl")); }
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
