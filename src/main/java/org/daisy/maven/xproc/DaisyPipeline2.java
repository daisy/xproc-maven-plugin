package org.daisy.maven.xproc;

import java.lang.reflect.UndeclaredThrowableException;

import java.util.List;
import java.util.Map;

import com.rabbitmq.tools.jsonrpc.JsonRpcException;

import org.daisy.pipeline.xproc.connect.XProcClient;
import org.daisy.pipeline.xproc.connect.XProcService;

public class DaisyPipeline2 implements XProcEngine {
	
	private static XProcService service = new XProcClient().openConnection();
	
	public void run(String pipeline,
	                Map<String,List<String>> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters)
			throws XProcExecutionException {
		try {
			service.run(pipeline, inputs, outputs, options, parameters); }
		catch (UndeclaredThrowableException e) {
			Throwable cause = e.getCause();
			if (cause instanceof JsonRpcException)
				throw new XProcExecutionException("Pipeline 2 failed to execute XProc: "
				                                  + ((JsonRpcException)cause).message
				                                  + " (see logs for details)", e);
			else
				throw e; }
	}
}
