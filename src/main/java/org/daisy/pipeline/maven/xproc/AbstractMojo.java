package org.daisy.pipeline.maven.xproc;

public abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
	
	protected static XProcEngine engine;
	
	static {
		ClassLoader cl = AbstractMojo.class.getClassLoader();
		try {
			Class.forName("com.xmlcalabash.drivers.Main", false, cl);
			engine = new Calabash(); }
		catch(ClassNotFoundException e) {
			try {
				Class.forName("org.daisy.pipeline.xproc.connect.XProcClient", false, cl);
				engine = new DaisyPipeline2(); }
			catch (ClassNotFoundException ee) {
				throw new RuntimeException("Could not find any XProc engines on the classpath."); }}
	}
}
