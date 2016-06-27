package org.daisy.maven.xproc.pipeline.logging;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;
import ch.qos.logback.core.pattern.PostCompileProcessor;

import org.daisy.pipeline.client.models.JobMessages;
import org.daisy.pipeline.client.models.Message;

public class ProgressMessageEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
	
	private boolean includeProgress = false;
	private int maxDepth = -1;
	private String filePattern = null;
	private String linePattern = null;
	private String columnPattern = null;
	
	public void setIncludeProgress(boolean include) {
		includeProgress = include;
	}
	
	public void setMaxDepth(int max) {
		maxDepth = max;
	}
	
	public void setFilePattern(String value) {
		filePattern = value;
	}
	
	public void setLinePattern(String value) {
		linePattern = value;
	}
	
	public void setColumnPattern(String value) {
		columnPattern = value;
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
				map.put("F", FileConverter.class.getName());
				map.put("file", FileConverter.class.getName());
				map.put("L", LineConverter.class.getName());
				map.put("line", LineConverter.class.getName());
				map.put("C", ColumnConverter.class.getName());
				map.put("column", ColumnConverter.class.getName());
				return map;
			}
		};
		patternLayout.setContext(context);
		patternLayout.setPattern(getPattern());
		patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
		final Map<String,PatternLayout> filePatterns = compilePatterns(filePattern, context);
		final Map<String,PatternLayout> linePatterns = compilePatterns(linePattern, context);
		final Map<String,PatternLayout> columnPatterns = compilePatterns(columnPattern, context);
		patternLayout.setPostCompileProcessor(new PostCompileProcessor<ILoggingEvent>() {
			public void process(Converter<ILoggingEvent> head) {
				while (head != null) {
					if (head instanceof MessageConverter)
						((MessageConverter)head).setIncludeProgress(includeProgress);
					else if (head instanceof FileConverter)
						((FileConverter)head).setPatterns(filePatterns);
					else if (head instanceof LineConverter)
						((LineConverter)head).setPatterns(linePatterns);
					else if (head instanceof ColumnConverter)
						((ColumnConverter)head).setPatterns(columnPatterns);
					head = head.getNext(); }
			}
		});
		patternLayout.start();
		this.layout = patternLayout;
		super.start();
	}
	
	@Override
	public void doEncode(ILoggingEvent event) throws IOException {
		Message m = addMessageFromLogbackMessage(event);
		if (!includeProgress) {
			if (m.getText().isEmpty())
				return; }
		if (maxDepth >= 0) {
			if (m.depth > maxDepth)
				return; }
		super.doEncode(event);
	}
	
	public static class MessageConverter extends ClassicConverter {
		
		private boolean includeProgress = false;
		
		public void setIncludeProgress(boolean include) {
			includeProgress = include;
		}
		
		public String convert(ILoggingEvent event) {
			if (includeProgress)
				return currentMessage().text;
			else
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
			return indent.apply(depth);
		}
		
		private final Function<Integer,String> indent = memoize(
			depth -> {
				if (depth > 0) {
					StringBuilder indent = new StringBuilder();
					for (int i = 0; i < depth; i++)
						indent.append(childIndent);
					return indent.toString(); }
				else
					return "";
			}
		);
	}
	
	public static class FileConverter extends LoggerBasedForwardingConverter {
	}
	
	public static class LineConverter extends LoggerBasedForwardingConverter {
	}
	
	public static class ColumnConverter extends LoggerBasedForwardingConverter {
	}
	
	public static class LoggerBasedForwardingConverter extends ClassicConverter {
		
		private Map<String,PatternLayout> patterns;
		
		public void setPatterns(Map<String,PatternLayout> patterns) {
			this.patterns = patterns;
		}
		
		public String convert(ILoggingEvent event) {
			PatternLayout pattern = patternForLogger(event.getLoggerName());
			if (pattern != null)
				return pattern.doLayout(event);
			else
				return "";
		}
		
		private PatternLayout patternForLogger(String logger) {
			return patternForLogger.apply(logger);
		}
		
		private final Function<String,PatternLayout> patternForLogger = memoize(
			logger -> {
				if (patterns != null)
					for (String lo : patterns.keySet())
						if (logger.startsWith(lo))
							return patterns.get(lo);
				return null;
			}
		);
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
	
	private static Map<String,PatternLayout> compilePatterns(String patterns, Context context) {
		if (patterns != null)
			try {
				Properties p = new Properties();
				p.load(new StringReader(patterns));
				Map<String,PatternLayout> map = new TreeMap<String,PatternLayout>();
				for (String logger : p.stringPropertyNames())
					map.put(logger, compilePattern(p.getProperty(logger), context));
				return map; }
			catch (IOException e) {}
		return null;
	}
	
	private static PatternLayout compilePattern(String pattern, Context context) {
		PatternLayout layout = new PatternLayout();
		layout.setContext(context);
		layout.setPattern(pattern);
		layout.setPostCompileProcessor(null);
		layout.start();
		return layout;
	}
	
	private static <T,R> Function<T,R> memoize(Function<T,R> fn) {
		Map<T,R> map = new ConcurrentHashMap<T,R>();
		return (t) -> map.computeIfAbsent(t, fn);
	}
}
