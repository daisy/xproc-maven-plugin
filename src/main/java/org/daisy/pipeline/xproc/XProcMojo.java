package org.daisy.maven.xproc;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

import static org.daisy.maven.xproc.utils.asURI;

/**
 * Run an XProc pipeline.
 *
 * @goal xproc
 */
public class XProcMojo extends AbstractMojo {
	
	/**
	 * Path to the pipeline
	 *
	 * @parameter
	 * @required
	 */
	private File pipeline;
	
	public void execute() throws MojoExecutionException {
		try {
			engine.run(asURI(pipeline), null, null, null, null);
			System.out.println("Running XProc ..."); }
		catch (Exception e) {
			throw new MojoExecutionException("Error running XProc", e); }
	}
}
