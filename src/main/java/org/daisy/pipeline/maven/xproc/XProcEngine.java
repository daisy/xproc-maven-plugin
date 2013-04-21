package org.daisy.pipeline.maven.xproc;

import java.util.Map;

public interface XProcEngine {
	public void run(String pipeline,
	                Map<String,String> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters);
}
