package org.daisy.maven.xproc.pipeline.logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

import org.daisy.pipeline.client.models.Message;

public class ProgressMessageEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
	
	private boolean skipIfOnlyProgress = true;
	
	public void setSkipIfOnlyProgress(boolean skip) {
		skipIfOnlyProgress = skip;
	}
	
	@Override
	public void start() {
		PatternLayout patternLayout = new PatternLayout() {
			@Override
			public Map<String,String> getEffectiveConverterMap() {
				Map<String,String> map = new HashMap<String,String>(super.getEffectiveConverterMap());
				map.put("progress", ProgressConverter.class.getName());
				map.put("m", MessageConverter.class.getName());
				map.put("msg", MessageConverter.class.getName());
				map.put("message", MessageConverter.class.getName());
				return map;
			}
		};
		patternLayout.setContext(context);
		patternLayout.setPattern(getPattern());
		patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
		patternLayout.start();
		this.layout = patternLayout;
		super.start();
	}
	
	@Override
	public void doEncode(ILoggingEvent event) throws IOException {
		if (skipIfOnlyProgress) {
			Message m = new Message();
			m.text = event.getFormattedMessage();
			if (m.getText().isEmpty())
				return; }
		super.doEncode(event);
	}
	
	public static class MessageConverter extends ClassicConverter {
		public String convert(ILoggingEvent event) {
			Message m = new Message();
			m.text = event.getFormattedMessage();
			return m.getText();
		}
	}
	
	public static class ProgressConverter extends ClassicConverter {
		public String convert(ILoggingEvent event) {
			Message m = new Message();
			m.text = event.getFormattedMessage();
			return m.getProgressInfo();
		}
	}
	
}
