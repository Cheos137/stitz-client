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

package dev.cheos.stitz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.cheos.stitz.logging.LoggerImpl;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;

public class StitzClient { // TODO fix: lock ui interaction until login panel appeared / login was successful TODO fix audio resampling still being stupid (in jar only?) TODO fix incoming calls (looks like improper locking or sth.)
	private static final Logger LOGGER = LoggerFactory.getLogger(StitzClient.class);
	public static final Path DATA_DIR = Path.of(System.getProperty("user.home")).resolve(".sts").resolve("stitz-client");
	public static final Path LOG_DIR = DATA_DIR.resolve("logs");
	private static final List<Runnable> SHUTDOWN_HOOKS = new LinkedList<>();
	private static final Object CEF_TERMINATION_LOCK = new Object();
	private static StitzCallHandler callHandler;
	private static StitzAudioHandler audioHandler;
	private static CefApp cefApp;
	private static CefClient client;
	private static UIMessageRouter router;
	private static CefBrowser browser;
	public static boolean debug, verbose;
	
	public static void main(String[] args) {
		for (String arg : args)
			switch (arg) {
				case "--debug" -> debug = true;
				case "--verbose", "-v" -> verbose = true;
				default -> { }
			}
		
		try {
			if (Files.notExists(LOG_DIR))
				Files.createDirectories(LOG_DIR);
			LoggerImpl.logTo(LOG_DIR, debug);
			LoggerImpl.addGlobalPrintStream(System.out, debug);
			Persistence.init();
			
			CefAppBuilder builder = new CefAppBuilder();
			builder.getCefSettings().windowless_rendering_enabled = false;
			builder.setAppHandler(new MavenCefAppHandlerAdapter() {
				@Override public boolean onBeforeTerminate() { return false; }
				@Override public void stateHasChanged(CefAppState state) {
					if (state == CefAppState.TERMINATED)
						synchronized (CEF_TERMINATION_LOCK) {
							CEF_TERMINATION_LOCK.notifyAll();
						}
				}
			});
			builder.setProgressHandler((state, percentage) ->
				LOGGER.info("{} > {}", state, percentage == -1 ? "in progress" : percentage)
			);
			
			cefApp = builder.build();
			addShutdownHook(cefApp::dispose);
			client = cefApp.createClient();
			
			CefMessageRouter cefRouter = CefMessageRouter.create(new CefMessageRouterConfig("javaCall", "cancelJavaCall"));
			cefRouter.addHandler(router = new UIMessageRouter(debug), true);
			client.addMessageRouter(cefRouter);
			
			browser = client.createBrowser(Path.of("").resolve("static").resolve("index.html").toAbsolutePath().toString(), false, false);
			StitzUI.init(browser.getUIComponent());
			audioHandler = new StitzAudioHandler();
			callHandler = new StitzCallHandler();
			addShutdownHook(callHandler::close);
			addShutdownHook(audioHandler::close);
			addShutdownHook(StitzClient::awaitCefTermination); // await termination last, all other tasks should complete first so cef can shutdown in the meantime
		} catch (Exception e) {
			LOGGER.error("caught exception during startup", e);
			exit(-1);
		}
	}
	
	public static void postUIMessage(String message) {
		router.post(browser, message);
	}
	
	public static void callUI(String function, Object... params) {
		router.call(browser, function, params);
	}
	
	public static void callUIRaw(String js) {
		router.callRaw(browser, js);
	}
	
	public static void showError(String message) {
		callUI("showError", message);
	}
	
	public static StitzCallHandler getCallHandler() {
		return callHandler;
	}
	
	public static StitzAudioHandler getAudioHandler() {
		return audioHandler;
	}
	
	public static void addShutdownHook(Runnable callback) {
		SHUTDOWN_HOOKS.add(callback);
	}
	
	public static void exit(int code) {
		new Thread(() -> {
			SHUTDOWN_HOOKS.forEach(Runnable::run);
			System.exit(code);
		}, "Shutdown").start();
	}
	
	private static void awaitCefTermination() {
		synchronized (CEF_TERMINATION_LOCK) {
			try {
				if (CefApp.getState() == CefAppState.TERMINATED)
					return;
				CEF_TERMINATION_LOCK.wait();
			} catch (InterruptedException e) {
				LOGGER.warn("", e);
			}
		}
	}
}
