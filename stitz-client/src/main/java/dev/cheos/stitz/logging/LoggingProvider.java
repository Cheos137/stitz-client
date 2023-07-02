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

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class LoggingProvider implements SLF4JServiceProvider {
	public static String REQUESTED_API_VERSION = "2.0.99";
	
	private ILoggerFactory loggerFactory;
	private IMarkerFactory markerFactory;
	private MDCAdapter mdcAdapter;
	
	@Override
	public void initialize() {
		this.loggerFactory = new LoggerFactory();
		this.markerFactory = new BasicMarkerFactory();
		this.mdcAdapter = new NOPMDCAdapter();
	}
	
	@Override
	public ILoggerFactory getLoggerFactory() {
		return this.loggerFactory;
	}
	
	@Override
	public IMarkerFactory getMarkerFactory() {
		return this.markerFactory;
	}
	
	@Override
	public MDCAdapter getMDCAdapter() {
		return this.mdcAdapter;
	}
	
	@Override
	public String getRequestedApiVersion() {
		return REQUESTED_API_VERSION;
	}
}
