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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import javax.sound.sampled.Mixer;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIMessageRouter extends CefMessageRouterHandlerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIMessageRouter.class);
	private final boolean debug;
	
	public UIMessageRouter(boolean debug) {
		this.debug = true;
	}
	
	/*
	 * failure codes:
	 *  0 - not in debug mode
	 *  1 - arg expectation failed
	 *  2 - invalid credentials
	 *  3 - internal error
	 */
	@Override
	public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
		String[] args = request.split(" ");
		String command = args[0];
		args = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
		
		switch (command) {
			/*
			 * debug
			 */
			case "debug" -> {
				if (!this.debug)
					callback.failure(0, "not in debug mode");
				else callback.success("");
				return true;
			}
			/*
			 * login
			 */
			case "login" -> {
				if (args.length < 2) {
					callback.failure(1, "login requires 2 argument");
					return true;
				}
				boolean persistCredentials = false;
				if (args.length > 2)
					persistCredentials = Boolean.parseBoolean(args[2]);
				try {
					if (!StitzClient.getCallHandler().login(args[0], args[1], persistCredentials)) {
						callback.failure(2, "invalid credentials or stitz server is unavailable");
						return true;
					}
				} catch (Exception e) {
					LOGGER.error("Exception while logging in", e);
					callback.failure(3, e.getMessage());
				}
			}
			case "logout" -> StitzClient.getCallHandler().logoutUI();
			case "setDisplayName" -> {
				if (args.length < 1) {
					callback.failure(1, "setDisplayName requires 1 argument");
					return true;
				}
				StitzClient.getCallHandler().setDisplayName(args[0]);
			}
			case "getUsername" -> { return success(callback, Persistence.defaulted(Persistence::getUsername, () -> "")); }
			case "getDisplayName" -> { return success(callback, StitzClient.getCallHandler().getDisplayName()); }
			/*
			 * call management
			 */
			case "callCall" -> {
				if (args.length < 1) {
					callback.failure(1, "callCall requires 1 argument");
					return true;
				}
				StitzClient.getCallHandler().call(args[0]);
			}
			case "callHangup" -> StitzClient.getCallHandler().hangup();
			case "callAccept" -> StitzClient.getCallHandler().accept();
			case "callDecline" -> StitzClient.getCallHandler().decline();
			case "callInCall" -> { return success(callback, StitzClient.getCallHandler().isInCall()); }
			/*
			 * audio device management
			 */
			case "audioSetIn" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetIn requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setIn(Integer.parseInt(args[0]));
			}
			case "audioGetIn" -> { return success(callback, StitzClient.getAudioHandler().getIn()); }
			case "audioListIn" -> { return success(callback, Mixer.Info::getName, StitzClient.getAudioHandler().listIn()); }
			case "audioSetInVolume" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetInVolume requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setInVolume(Integer.parseInt(args[0]));
			}
			case "audioGetInVolume" -> { return success(callback, StitzClient.getAudioHandler().getInVolume()); }
			case "audioSetOut" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetOut requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setOut(Integer.parseInt(args[0]));
			}
			case "audioGetOut" -> { return success(callback, StitzClient.getAudioHandler().getOut()); }
			case "audioListOut" -> { return success(callback, Mixer.Info::getName, StitzClient.getAudioHandler().listOut()); }
			case "audioSetOutVolume" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetOutVolume requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setOutVolume(Integer.parseInt(args[0]));
			}
			case "audioGetOutVolume" -> { return success(callback, StitzClient.getAudioHandler().getOutVolume()); }
			case "audioSetClip" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetClip requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setClip(Integer.parseInt(args[0]));
			}
			case "audioGetClip" -> { return success(callback, StitzClient.getAudioHandler().getClip()); }
			case "audioListClip" -> { return success(callback, Mixer.Info::getName, StitzClient.getAudioHandler().listClip()); }
			case "audioSetClipVolume" -> {
				if (args.length < 1) {
					callback.failure(1, "audioSetClipVolume requires 1 argument");
					return true;
				}
				StitzClient.getAudioHandler().setClipVolume(Integer.parseInt(args[0]));
			}
			case "audioGetClipVolume" -> { return success(callback, StitzClient.getAudioHandler().getClipVolume()); }
			case "audioSetMute" -> StitzClient.getAudioHandler().setMute(args.length > 0 ? Boolean.parseBoolean(args[0]) : !StitzClient.getAudioHandler().getMute(), false);
			case "audioGetMute" -> { return success(callback, StitzClient.getAudioHandler().getMute()); }
			case "audioSetDeafen" -> StitzClient.getAudioHandler().setDeafen(args.length > 0 ? Boolean.parseBoolean(args[0]) : !StitzClient.getAudioHandler().getDeafen(), false);
			case "audioGetDeafen" -> { return success(callback, StitzClient.getAudioHandler().getDeafen()); }
			case "audioRefreshDevices" -> StitzClient.getAudioHandler().refreshDeviceLists();
			default -> { return false; } // didn't handle command
		}
		callback.success("");
		return true;
	}
	
	public void post(CefBrowser browser, String message) {
		callRaw(browser, "handle('%s');".formatted(message.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'")));
	}
	
	public void call(CefBrowser browser, String function, Object... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(function);
		sb.append('(');
		String sep = "";
		for (Object param : params) {
			sb.append(sep);
			sep = ", ";
			if (param instanceof String str)
				sb.append("'").append(str.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'")).append("'");
			else if (param != null && param.getClass().isArray()) {
				String str = Arrays.deepToString(new Object[] { param });
				sb.append(str, 1, str.length() - 1);
			} else sb.append(param);
		}
		sb.append(");");
		callRaw(browser, sb.toString());
	}
	
	public void callRaw(CefBrowser browser, String js) {
		browser.executeJavaScript(js, null, 0);
	}
	
	private static boolean success(CefQueryCallback callback, Object param) {
		if (param instanceof Collection<?> coll) return success(callback, coll.toArray());
		callback.success(String.valueOf(param));
		return true;
	}
	
	private static boolean success(CefQueryCallback callback, Object... params) {
		String param = Arrays.deepToString(params);
		return success(callback, param.substring(1, param.length() - 1));
	}
	
	private static <T> boolean success(CefQueryCallback callback, Function<T, String> stringifier, Iterable<T> params) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (T param : params) {
			sb.append(sep).append(stringifier.apply(param));
			sep = ", ";
		}
		return success(callback, sb.toString());
	}
}
