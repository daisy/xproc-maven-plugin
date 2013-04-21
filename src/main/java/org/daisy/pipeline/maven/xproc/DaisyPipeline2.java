package org.daisy.pipeline.maven.xproc;

import java.util.Map;

import org.daisy.pipeline.xproc.connect.XProcClient;
import org.daisy.pipeline.xproc.connect.XProcService;

public class DaisyPipeline2 implements XProcEngine {
	
	private static XProcService service = new XProcClient().openConnection();
	
	public void run(String pipeline,
	                Map<String,String> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters) {
		service.run(pipeline, inputs, outputs, options, parameters);
	}
}
