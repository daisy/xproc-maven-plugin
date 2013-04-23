package org.daisy.pipeline.maven.xproc;

import java.io.FileOutputStream;
import java.net.URI;
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

import org.daisy.pipeline.maven.xproc.utils;

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
	                Map<String,String> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters) {
		try {
			XPipeline xpipeline = runtime.load(pipeline);
			if (inputs != null)
				for (String port : inputs.keySet())
					xpipeline.writeTo(port, runtime.parse(inputs.get(port), null));
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
						WritableDocument wdoc = new WritableDocument(
							runtime,
							outFile,
							xpipeline.getSerialization(port),
							new FileOutputStream(outFile));
						ReadablePipe rpipe = xpipeline.readFrom(port);
						while (rpipe.moreDocuments())
							wdoc.write(rpipe.read()); }}
		catch (Exception e) { throw new RuntimeException(e); }
	}
}
