/*
 * Copyright (c) 2023 Cheos
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.cheos.stitz.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

public class LoggerImpl implements Logger {
	private static final SimpleDateFormat FILE_DF = new SimpleDateFormat("YYYY-MM-dd_HH-mm-ss");
	private static final SimpleDateFormat LOG_DF = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	private static final HashMap<String, LoggerImpl> loggers = new HashMap<>();
	private static final List<Pair<PrintStream, Boolean>> printStreams = new ArrayList<>(8);
	private static Queue<Pair<String, LogLevel>> logQueue = new ConcurrentLinkedQueue<>();
	private static Thread writeThread;
	private static boolean shutdown;
	protected static Path logFile;
	private final String name;
	private final Set<PrintStream> localPrintStreams = new HashSet<>(8);
	
	static {
		writeThread = new Thread(LoggerImpl::writeLoop, "LoggerThread@write");
		writeThread.setDaemon(true);
		writeThread.start();
	}
	
	public static LoggerImpl getLogger(String name) {
		if (name == null)
			return LoggerImpl.getLogger(5);
		if (loggers.containsKey(name))
			return loggers.get(name);
		
		LoggerImpl logger = new LoggerImpl(name);
		loggers.put(name, logger);
		return logger;
	}
	
	public static LoggerImpl getLogger() {
		return getLogger(5);
	}
	
	private static LoggerImpl getLogger(int tracepoint) {
		return getLogger(Thread.currentThread().getStackTrace()[tracepoint].getClassName());
	}
	
	public static Path nextLogFile(Path directory) throws IOException {
		if (Files.notExists(directory)) Files.createDirectories(directory);
		if (!Files.isDirectory(directory))
			directory = directory.getParent();
		
		Path file = directory.resolve(FILE_DF.format(new Date()) + ".log");
		int i = 0;
		
		while (Files.exists(file))
			file = directory.resolve(FILE_DF.format(new Date()) + "_" + ++i + ".log");
		return file;
	}
	
	public static void addGlobalPrintStream(PrintStream ps) {
		addGlobalPrintStream(ps, false);
	}
	
	public static void addGlobalPrintStream(PrintStream ps, boolean enableDebug) {
		printStreams.add(Pair.of(ps, enableDebug));
	}
	
	public static void logTo(Path file) throws IOException {
		logTo(file, false);
	}
	
	public static void logTo(Path file, boolean enableDebug) throws IOException {
		if (Files.isDirectory(file))
			file = nextLogFile(file);
		
		if (Files.notExists(file.getParent()))
			Files.createDirectories(file.getParent());
		if (Files.notExists(file))
			Files.createFile(file);
		addGlobalPrintStream(new PrintStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND), true), enableDebug);
		logFile = file;
	}
	
	public static synchronized void shutdown() {
		globalLog(LogLevel.INFO, "Logger system shutting down...");
		shutdown = true;
		
		try { writeThread.join(1000L); } catch (InterruptedException e) { }
		if (writeThread.isAlive())
			writeThread.interrupt();
		
		loggers.values().forEach(LoggerImpl::close);
		
		for (Pair<String, LogLevel> log : logQueue)
			printStreams.forEach(p -> {
				if (p != null && (p.getRight() || log.getRight() != LogLevel.DEBUG))
					p.getLeft().println(log.getLeft());
			});
		
		logQueue.clear();
		logQueue = null;
		writeThread = null;
		printStreams.forEach(p -> p.getLeft().close());
		printStreams.clear();
	}
	
	private static void writeLoop() {
		try {
			while (!shutdown) {
				if (printStreams != null) {
					Pair<String, LogLevel> log;
					while ((log = logQueue.poll()) != null) {
						final Pair<String, LogLevel> eflog = log;
						printStreams.forEach(p -> {
							if (p != null && (p.getRight() || eflog.getRight().ordinal() > LogLevel.DEBUG.ordinal()))
								p.getLeft().println(eflog.getLeft());
						});
					}
				}
				Thread.sleep(20L);
			}
		} catch (InterruptedException e) { }
	}
	
	private static void globalLog0(LogLevel level, String message) {
		if (shutdown) return;
		logQueue.add(Pair.of(message, level));
	}
	public static void globalLog(LogLevel level, String name, String message) {
		if (name != null)
			globalLog0(level, String.format("[%s] [%s] [%s | %s]: %s",
					LOG_DF.format(new Date()),
					Thread.currentThread().getName(),
					name,
					level == null ? LogLevel.WARN : level,
							message == null ? "null" : message));
		else globalLog(level, message);
	}
	public static void globalLog(LogLevel level, String message) {
		globalLog(level, new Exception().getStackTrace()[1].getClassName(), message);
	}
	public static void globalLogTrace(LogLevel level, String name, Throwable t) {
		StackTraceElement[] steArray;
		if (t != null && (steArray = t.getStackTrace()) != null) {
			for (int i = 0; i < 15 && i < steArray.length; ++i)
				globalLog(level, name, "    " + steArray[i]);
			if (steArray.length > 15)
				globalLog(level, name, String.format("... and %d more.", steArray.length - 15));
		}
	}
	
	private LoggerImpl(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	public void addPrintStream(PrintStream ps) {
		this.localPrintStreams.add(ps);
	}
	
	private void log(String message, LogLevel level) {
		for (PrintStream ps : this.localPrintStreams)
			if (ps != null)
				ps.println(message);
		globalLog0(level, message);
	}
	
	public void log(LogLevel level, String msg) {
		log(String.format("[%s | %s] [%s | %s]: %s",
				LOG_DF.format(new Date()),
				Thread.currentThread().getName(),
				this.name,
				level == null ? LogLevel.WARN : level,
						msg == null ? "null" : msg),
				level);
	}
	public void log(LogLevel level, Object obj) {
		log(level, obj == null ? "null" : obj.toString());
	}
	public void log(LogLevel level, String msg, Throwable t) {
		if (msg != null) log(level, msg);
		if (t == null) return;
		log(level, t.getClass().getName() + ": " + String.valueOf(t.getMessage()));
		logTrace(level, t);
		while ((t = t.getCause()) != null) {
			log(level, "Caused by " + t.getClass().getName() + ": " + String.valueOf(t.getMessage()));
			logTrace(level, t);
		}
	}
	public void log(LogLevel level, Throwable t) {
		log(level, (t == null ? "Exception" : t.getClass().getSimpleName()) + " in '" + Thread.currentThread().getName() + "'", t);
	}
	public void log(LogLevel level, Object obj, Throwable t) {
		log(level, obj == null ? "null" : obj.toString(), t);
	}
	private void logTrace(LogLevel level, Throwable t) {
		StackTraceElement[] steArray;
		if (t != null && (steArray = t.getStackTrace()) != null) {
			for (int i = 0; i < 15 && i < steArray.length; ++i)
				log(level, "    " + steArray[i]);
			if (steArray.length > 15)
				log(level, String.format("... and %d more.", steArray.length - 15));
		}
	}
	
	public void trace(Object obj)                                        { log(LogLevel.TRACE, obj); }
	public void trace(Throwable t)                                       { log(LogLevel.TRACE, t); }
	public void trace(Object obj, Throwable t)                           { log(LogLevel.TRACE, obj); }
	@Override public void trace(String msg)                              { log(LogLevel.TRACE, msg); }
	@Override public void trace(String format, Object arg)               { log(LogLevel.TRACE, format(format, arg)); }
	@Override public void trace(String format, Object arg1, Object arg2) { log(LogLevel.TRACE, format(format, arg1, arg2)); }
	@Override public void trace(String format, Object... args)           { log(LogLevel.TRACE, format(format, args)); }
	@Override public void trace(String msg, Throwable t)                 { log(LogLevel.TRACE, msg, t); }
	@Override public void trace(Marker marker, String msg)                              { trace(msg); }
	@Override public void trace(Marker marker, String format, Object arg)               { trace(format, arg); }
	@Override public void trace(Marker marker, String format, Object arg1, Object arg2) { trace(format, arg1, arg2); }
	@Override public void trace(Marker marker, String format, Object... args)           { trace(format, args); }
	@Override public void trace(Marker marker, String msg, Throwable t)                 { trace(msg, t); }
	
	public void debug(Object obj)                                        { log(LogLevel.DEBUG, obj); }
	public void debug(Throwable t)                                       { log(LogLevel.DEBUG, t); }
	public void debug(Object obj, Throwable t)                           { log(LogLevel.DEBUG, obj, t); }
	@Override public void debug(String msg)                              { log(LogLevel.DEBUG, msg); }
	@Override public void debug(String format, Object arg)               { log(LogLevel.DEBUG, format(format, arg)); }
	@Override public void debug(String format, Object arg1, Object arg2) { log(LogLevel.DEBUG, format(format, arg1, arg2)); }
	@Override public void debug(String format, Object... args)           { log(LogLevel.DEBUG, format(format, args)); }
	@Override public void debug(String msg, Throwable t)                 { log(LogLevel.DEBUG, msg, t); }
	@Override public void debug(Marker marker, String msg)                              { debug(msg); }
	@Override public void debug(Marker marker, String format, Object arg)               { debug(format, arg); }
	@Override public void debug(Marker marker, String format, Object arg1, Object arg2) { debug(format, arg1, arg2); }
	@Override public void debug(Marker marker, String format, Object... args)           { debug(format, args); }
	@Override public void debug(Marker marker, String msg, Throwable t)                 { debug(msg, t); }
	
	public void info (Object obj)                                        { log(LogLevel.INFO , obj); }
	public void info (Throwable t)                                       { log(LogLevel.INFO , t); }
	public void info (Object obj, Throwable t)                           { log(LogLevel.INFO , obj, t); }
	@Override public void info (String msg)                              { log(LogLevel.INFO , msg); }
	@Override public void info (String format, Object arg)               { log(LogLevel.INFO , format(format, arg)); }
	@Override public void info (String format, Object arg1, Object arg2) { log(LogLevel.INFO , format(format, arg1, arg2)); }
	@Override public void info (String format, Object... args)           { log(LogLevel.INFO , format(format, args)); }
	@Override public void info (String msg, Throwable t)                 { log(LogLevel.INFO , msg, t); }
	@Override public void info (Marker marker, String msg)                              { info (msg); }
	@Override public void info (Marker marker, String format, Object arg)               { info (format, arg); }
	@Override public void info (Marker marker, String format, Object arg1, Object arg2) { info (format, arg1, arg2); }
	@Override public void info (Marker marker, String format, Object... args)           { info (format, args); }
	@Override public void info (Marker marker, String msg, Throwable t)                 { info (msg, t); }
	
	public void warn (Object obj)                                        { log(LogLevel.WARN , obj); }
	public void warn (Throwable t)                                       { log(LogLevel.WARN , t); }
	public void warn (Object obj, Throwable t)                           { log(LogLevel.WARN , obj, t); }
	@Override public void warn (String msg)                              { log(LogLevel.WARN , msg); }
	@Override public void warn (String format, Object arg)               { log(LogLevel.WARN , format(format, arg)); }
	@Override public void warn (String format, Object arg1, Object arg2) { log(LogLevel.WARN , format(format, arg1, arg2)); }
	@Override public void warn (String format, Object... args)           { log(LogLevel.WARN , format(format, args)); }
	@Override public void warn (String msg, Throwable t)                 { log(LogLevel.WARN , msg, t); }
	@Override public void warn (Marker marker, String msg)                              { warn (msg); }
	@Override public void warn (Marker marker, String format, Object arg)               { warn (format, arg); }
	@Override public void warn (Marker marker, String format, Object arg1, Object arg2) { warn (format, arg1, arg2); }
	@Override public void warn (Marker marker, String format, Object... args)           { warn (format, args); }
	@Override public void warn (Marker marker, String msg, Throwable t)                 { warn (msg, t); }
	
	public void error(Object obj)                                        { log(LogLevel.ERROR, obj); }
	public void error(Throwable t)                                       { log(LogLevel.ERROR, t); }
	public void error(Object obj, Throwable t)                           { log(LogLevel.ERROR, obj, t); }
	@Override public void error(String msg)                              { log(LogLevel.ERROR, msg); }
	@Override public void error(String format, Object arg)               { log(LogLevel.ERROR, format(format, arg)); }
	@Override public void error(String format, Object arg1, Object arg2) { log(LogLevel.ERROR, format(format, arg1, arg2)); }
	@Override public void error(String format, Object... args)           { log(LogLevel.ERROR, format(format, args)); }
	@Override public void error(String msg, Throwable t)                 { log(LogLevel.ERROR, msg, t); }
	@Override public void error(Marker marker, String msg)                              { error(msg); }
	@Override public void error(Marker marker, String format, Object arg)               { error(format, arg); }
	@Override public void error(Marker marker, String format, Object arg1, Object arg2) { error(format, arg1, arg2); }
	@Override public void error(Marker marker, String format, Object... args)           { error(format, args); }
	@Override public void error(Marker marker, String msg, Throwable t)                 { error(msg, t); }
	
	@Deprecated public final void fatal(String msg)              { log(LogLevel.FATAL, msg); }
	@Deprecated public final void fatal(Object obj)              { log(LogLevel.FATAL, obj); }
	@Deprecated public final void fatal(String msg, Throwable t) { log(LogLevel.FATAL, msg, t); }
	@Deprecated public final void fatal(Throwable t)             { log(LogLevel.FATAL, t); }
	@Deprecated public final void fatal(Object obj, Throwable t) { log(LogLevel.FATAL, obj, t); }
	
	@Override public boolean isTraceEnabled() { return true; }
	@Override public boolean isTraceEnabled(Marker marker) { return true; }
	@Override public boolean isDebugEnabled() { return true; }
	@Override public boolean isDebugEnabled(Marker marker) { return true; }
	@Override public boolean isInfoEnabled() { return true; }
	@Override public boolean isInfoEnabled(Marker marker) { return true; }
	@Override public boolean isWarnEnabled() { return true; }
	@Override public boolean isWarnEnabled(Marker marker) { return true; }
	@Override public boolean isErrorEnabled() { return true; }
	@Override public boolean isErrorEnabled(Marker marker) { return true; }
	
	public void close() {
		this.localPrintStreams.forEach(PrintStream::close);
		this.localPrintStreams.clear();
	}
	
	
	private static String format(String format, Object... args) {
		return MessageFormatter.basicArrayFormat(format, args);
	}
	
	public static enum LogLevel {
		TRACE,
		DEBUG,
		INFO ("INFO "),
		WARN ("WARN "),
		ERROR,
		@Deprecated FATAL;
		
		private final String name;
		
		LogLevel() { this.name = null; }
		LogLevel(String name) { this.name = name; }
		
		@Override public String toString() { return name == null ? name() : name; }
	}
}
