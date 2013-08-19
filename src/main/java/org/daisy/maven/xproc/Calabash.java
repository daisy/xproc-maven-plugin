package org.daisy.maven.xproc;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;

import net.sf.saxon.s9api.QName;

import org.daisy.maven.xproc.utils;

import org.xml.sax.InputSource;

public class Calabash implements XProcEngine {
	
	private final XProcRuntime runtime = new XProcRuntime(new XProcConfiguration("he", false));
	
	public Calabash() {
		runtime.setURIResolver(new URIResolver() {
			public Source resolve(String href, String base) {
				try { return new SAXSource(new InputSource(utils.resolve(href, base))); }
				catch (Exception e) { return null; }}});
	}
	
	public void run(String pipeline,
	                Map<String,List<String>> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters)
			throws XProcExecutionException {
		try {
			XPipeline xpipeline = runtime.load(pipeline);
			if (inputs != null)
				for (String port : inputs.keySet())
					for (String document : inputs.get(port))
						xpipeline.writeTo(port, runtime.parse(document, null));
			if (options != null)
				for (String name : options.keySet())
					xpipeline.passOption(new QName("", name), new RuntimeValue(options.get(name)));
			if (parameters != null)
				for (String port : parameters.keySet())
					for (String name : parameters.get(port).keySet())
						xpipeline.setParameter(port, new QName("", name), new RuntimeValue(parameters.get(port).get(name)));
			xpipeline.run();
			if (outputs != null)
				for (String port : xpipeline.getOutputs())
					if (outputs.containsKey(port)) {
						String outFile = new URI(outputs.get(port)).getPath();
						new File(outFile).getParentFile().mkdirs();
						WritableDocument wdoc = new WritableDocument(
							runtime,
							outFile,
							xpipeline.getSerialization(port),
							new FileOutputStream(outFile));
						ReadablePipe rpipe = xpipeline.readFrom(port);
						while (rpipe.moreDocuments())
							wdoc.write(rpipe.read()); }}
		catch (Exception e) {
			throw new XProcExecutionException("Calabash failed to execute XProc", e); }
	}
}
