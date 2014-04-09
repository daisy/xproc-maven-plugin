package org.daisy.maven.xproc.calabash;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;

import net.sf.saxon.s9api.QName;

import org.daisy.maven.xproc.api.XProcEngine;
import org.daisy.maven.xproc.api.XProcExecutionException;

import org.xml.sax.InputSource;

public class Calabash implements XProcEngine {
	
	private XProcRuntime runtime;
	private URIResolver uriResolver;
	
	protected void setURIResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}
	
	protected void activate() {
		runtime = new XProcRuntime(new XProcConfiguration("he", false));
		runtime.setURIResolver(uriResolver == null ?
		                       JarURIResolver.newInstance() :
		                       JarURIResolver.wrap(uriResolver));
	}
	
	public void run(String pipeline,
	                Map<String,List<String>> inputs,
	                Map<String,String> outputs,
	                Map<String,String> options,
	                Map<String,Map<String,String>> parameters)
			throws XProcExecutionException {
		if (runtime == null)
			activate();
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
	
	private abstract static class JarURIResolver implements URIResolver {
		
		public abstract Source delegate(String href, String base) throws TransformerException;
		
		public Source resolve(String href, String base) throws TransformerException {
			try {
				if (base != null && base.startsWith("jar:file:"))
					return new SAXSource(new InputSource(new URL(new URL(base), href).toString())); }
			catch (MalformedURLException e) {}
			return delegate(href, base);
		}
		
		private static JarURIResolver newInstance() {
			return new JarURIResolver() {
				public Source delegate(String href, String base) throws TransformerException {
					try {
						URI uri;
						if (base != null)
							uri = new URI(base).resolve(new URI(href));
						else
							uri = new URI(href);
						return new SAXSource(new InputSource(uri.toASCIIString())); }
					catch (URISyntaxException e) {
						throw new TransformerException(e); }
				}
			};
		}
		
		private static JarURIResolver wrap(final URIResolver delegate) {
			return new JarURIResolver() {
				public Source delegate(String href, String base) throws TransformerException {
					return delegate.resolve(href, base);
				}
			};
		}
	}
}
