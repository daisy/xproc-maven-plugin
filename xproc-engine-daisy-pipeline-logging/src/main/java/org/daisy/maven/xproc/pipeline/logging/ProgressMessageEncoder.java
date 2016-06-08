package org.daisy.maven.xproc.pipeline.logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

import org.daisy.pipeline.client.models.JobMessages;
import org.daisy.pipeline.client.models.Message;

public class ProgressMessageEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
	
	private boolean skipIfOnlyProgress = true;
	private int maxDepth = -1;
	
	public void setSkipIfOnlyProgress(boolean skip) {
		skipIfOnlyProgress = skip;
	}
	
	public void setMaxDepth(int max) {
		maxDepth = max;
	}
	
	@Override
	public void start() {
		PatternLayout patternLayout = new PatternLayout() {
			@Override
			public Map<String,String> getEffectiveConverterMap() {
				Map<String,String> map = new HashMap<String,String>(super.getEffectiveConverterMap());
				map.put("progress", ProgressConverter.class.getName());
				map.put("indent", IndentConverter.class.getName());
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
		Message m = addMessageFromLogbackMessage(event);
		if (skipIfOnlyProgress) {
			if (m.getText().isEmpty())
				return; }
		if (maxDepth >= 0) {
			if (m.depth > maxDepth)
				return; }
		super.doEncode(event);
	}
	
	public static class MessageConverter extends ClassicConverter {
		public String convert(ILoggingEvent event) {
			return currentMessage().getText();
		}
	}
	
	public static class ProgressConverter extends ClassicConverter {
		public String convert(ILoggingEvent event) {
			return currentMessage().getProgressInfo();
		}
	}
	
	public static class IndentConverter extends ClassicConverter {
		
		private String childIndent;
		
		@Override
		public void start() {
			childIndent = getFirstOption();
			super.start();
		}
		
		public String convert(ILoggingEvent event) {
			return indent(currentMessage().depth);
		}
		
		private String indent(int depth) {
			if (depth > 0) {
				StringBuilder indent = new StringBuilder();
				for (int i = 0; i < depth; i++)
					indent.append(childIndent);
				return indent.toString(); }
			else
				return "";
		}
	}
	
	private static Message currentMessage() {
		return messages.get(sequence - 1);
	}
	
	// threat everything as one single job
	private static final JobMessages messages = new JobMessages();
	
	private static int sequence = 0;
	
	private static Message addMessageFromLogbackMessage(ILoggingEvent event) {
		Message m = new Message();
		m.text = event.getFormattedMessage();
		m.sequence = sequence++;
		messages.add(m);
		// use get() in order to force computation of depth
		return messages.get(sequence - 1);
	}
}
