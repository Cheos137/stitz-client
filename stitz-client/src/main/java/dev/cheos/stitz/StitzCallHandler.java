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

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.cheos.stitz.gsm.GSMEncoder;
import dev.cheos.stitz.iax.*;
import dev.cheos.stitz.iax.frame.Frame;
import dev.cheos.stitz.iax.frame.MediaFrame;

public class StitzCallHandler implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(StitzCallHandler.class);
	private static final AtomicInteger CLIENT_CALL_NUMBER = new AtomicInteger(1);
	private final Object loginLock = new Object(), callIncomingLock = new Object();
	private IaxClient client;
	private IaxCall call;
	private IaxCall.Pending pending;
	private boolean loggedIn, loginSuccess, inCall;
	private Timer timer = new Timer();
	private TimerTask timestampUpdateTask;
	private ExecutorService voicePushExecutor;
	private Map<ExecutorService, Boolean> shutdownExecutors = new ConcurrentHashMap<>();
	
	public StitzCallHandler() {
		try {
			if (!login(Persistence.getUsername(), Persistence.getPassword(), false))
				StitzClient.postUIMessage("require-login");
		} catch (IOException e) {
			LOGGER.error("Exception logging in using persisted credentials", e);
			StitzClient.postUIMessage("require-login");
		}
	}
	
	public boolean login(String username, String password, boolean persistCredentials) throws IOException {
		if (username == null || password == null) return false;
		if (this.client != null) return false;
		
		this.client = new IaxClient(
				new IaxClient.Configuration(
						"StiTz client %d",
						getDisplayName(),
						username,
						password,
						InetAddress.getByName("stitz.stellwerksim.de"),
						4569,
						10,
						StitzClient.debug,
						StitzClient.verbose),
				(short) (CLIENT_CALL_NUMBER.getAndIncrement() % IaxConstants.CLIENT_MAX_SOURCE_CALL_NUMBER));
		this.client.addListener(new Listener());
		synchronized (this.loginLock) {
			this.client.connect();
			try { this.loginLock.wait(30_000); } catch (InterruptedException e) { }
		}
		if (!this.loginSuccess) {
			this.client.close();
			this.client = null;
			return false;
		}
		Persistence.setUsername(username);
		if (persistCredentials)
			Persistence.setPassword(password);
		this.loggedIn = true;
		this.timestampUpdateTask = new TimerTask() {
			@Override
			public void run() {
				if (StitzCallHandler.this.call != null)
					StitzClient.callUI("updateCallTimestamp", StitzCallHandler.this.call.getTimestampRelative());
			}
		};
		this.timer.scheduleAtFixedRate(this.timestampUpdateTask, 200, 200);
		return true;
	}
	
	public void logoutUI() {
		Persistence.setUsername(null);
		Persistence.setPassword(null);
		logout();
	}
	
	public void logout() {
		if (this.timestampUpdateTask != null)
			this.timestampUpdateTask.cancel();
		if (this.voicePushExecutor != null) {
			this.shutdownExecutors.put(this.voicePushExecutor, true);
			this.voicePushExecutor.shutdownNow();
			try { this.voicePushExecutor.awaitTermination(10, TimeUnit.SECONDS); }
			catch (InterruptedException e) { }
		}
		this.voicePushExecutor = null;
		this.loggedIn = false;
		this.inCall = false;
		if (this.client != null)
			try { this.client.close(); }
			catch (IOException e) { LOGGER.error("Exception closing iax client", e); }
		this.client = null;
		this.call = null;
		this.pending = null;
		synchronized (StitzCallHandler.this.loginLock) {
			StitzCallHandler.this.loginLock.notifyAll();
		}
		synchronized (StitzCallHandler.this.callIncomingLock) {
			StitzCallHandler.this.callIncomingLock.notifyAll();
		}
		StitzClient.callUI("require-login");
	}
	
	public void setDisplayName(String name) {
		Persistence.setDisplayName(name);
	}
	
	public String getDisplayName() {
		return Persistence.defaulted(Persistence::getDisplayName, () -> Persistence.defaulted(Persistence::getUsername, () -> ""));
	}
	
	public void call(String number) {
		if (this.inCall || this.client == null) return;
		this.inCall = true;
		this.call = this.client.call(number, MediaFrame.Format.GSM_FULL_RATE);
		this.call.addListener(new CallListener());
		this.call.addListener(StitzClient.getAudioHandler().getListener());
		this.call.start();
		StitzClient.getAudioHandler().flushMic();
		StitzCallHandler.this.voicePushExecutor = Executors.newSingleThreadExecutor();
		StitzCallHandler.this.voicePushExecutor.execute(() -> pushMic(this.voicePushExecutor));
		StitzClient.getAudioHandler().loopSound("call_outgoing");
		StitzClient.callUI("showCallOutgoing", this.call.getCalledNumber() /* TODO display name if in contacts */, this.call.getCalledNumber());
	}
	
	public void hangup() {
		this.call.stop(); // call listener takes care of the rest
	}
	
	public void accept() {
		if (this.pending == null || this.client == null) return;
		if (this.inCall) hangup();
		
		LOGGER.debug("accepting incoming call");
		
		this.pending.accept().thenAccept(call -> {
			LOGGER.debug("processing accepted call");
			if (call == null) {
				this.pending = null;
				StitzClient.postUIMessage("cancel-incoming-call");
				StitzClient.getAudioHandler().playSound("cancel_incoming");
				return;
			}
			this.inCall = true;
			this.call = call;
			call.addListener(new CallListener());
			call.addListener(StitzClient.getAudioHandler().getListener());
			StitzClient.getAudioHandler().flushMic();
			this.voicePushExecutor = Executors.newSingleThreadExecutor();
			this.voicePushExecutor.execute(() -> pushMic(this.voicePushExecutor));
			StitzClient.callUI("setInCall", true, this.pending.getCallingName(), this.pending.getUsername());
			this.pending = null;
			StitzClient.getAudioHandler().playSound("accept_incoming");
		});
		
		synchronized (StitzCallHandler.this.callIncomingLock) {
			LOGGER.debug("notifying incoming call lock (accept)");
			StitzCallHandler.this.callIncomingLock.notifyAll();
		}
	}
	
	public void decline() {
		if (this.pending == null) return;
		this.pending.decline();
		this.pending = null;
		StitzClient.getAudioHandler().cancelLoopSound("call_incoming");
		StitzClient.getAudioHandler().playSound("cancel_incoming");
		
		synchronized (StitzCallHandler.this.callIncomingLock) {
			LOGGER.debug("notifying incoming call lock (decline)");
			StitzCallHandler.this.callIncomingLock.notifyAll();
		}
	}
	
	public boolean isInCall() {
		return this.inCall;
	}
	
	@Override
	public void close() {
		logout();
		StitzClient.callUI("setInCall", false);
		StitzClient.postUIMessage("require-login");
	}
	
	private void pushMic(ExecutorService service) {
		short[] sbuf = new short[1600];
		short[] sbufGSM = new short[160];
		byte[] buf = new byte[3200];
		byte[] enc = new byte[33];
		GSMEncoder encoder = new GSMEncoder();
		for (;;) {
			if (this.shutdownExecutors.containsKey(service)) return; // task got cancelled
			int read = StitzClient.getAudioHandler().readMic(buf, sbuf);
			
			if (this.call != null && this.call.isAudioActive()) // discard data if call isn't ready yet
				for (int i = 0; i < 10; i++) {
					System.arraycopy(sbuf, i * sbufGSM.length, sbufGSM, 0, sbufGSM.length);
					encoder.encode(sbufGSM, enc);
					this.call.sendAudioData(enc);
				}
			
			if (read < buf.length) {
				LOGGER.debug("reached end of mic input stream, terminating mic push thread (read {} of {} bytes)", read, buf.length);
				return; // end of stream reached
			}
		}
	}
	
	
	private class Listener implements IaxClientListener {
		@Override
		public void onConnect(IaxClient client, boolean success) {
			synchronized (StitzCallHandler.this.loginLock) {
				StitzCallHandler.this.loginSuccess = success;
				StitzCallHandler.this.loginLock.notifyAll();
			}
		}
		
		@Override
		public void onDisconnect(IaxClient client) {
			if (!StitzCallHandler.this.loggedIn)
				return;
			
			try { client.close(); }
			catch (IOException e) { LOGGER.error("Exception closing iax client", e); }
			StitzCallHandler.this.client = null;
			try {
				if (!login(Persistence.getUsername(), Persistence.getPassword(), false))
					StitzClient.postUIMessage("require-login");
			} catch (IOException e) {
				LOGGER.error("Exception logging in using persisted credentials", e);
				StitzClient.postUIMessage("require-login");
			}
		}
		
		@Override
		public void onRetransmitError(IaxClient client, Frame frame) {
			LOGGER.warn("Error retransmitting frame {} for {}", frame, client);
		}
		
		@Override
		public void onCallIncoming(IaxClient client, IaxCall.Pending call) {
			StitzCallHandler.this.pending = call;
			StitzClient.callUI("callIncoming", call.getCallingName(), call.getUsername());
			StitzClient.getAudioHandler().loopSound("call_incoming");
			
			synchronized (StitzCallHandler.this.callIncomingLock) {
				LOGGER.debug("waiting on for incoming call to get accepted/declined");
				try { StitzCallHandler.this.callIncomingLock.wait(30_000); }
				catch (InterruptedException e) { }
			}
		}
		
		@Override
		public boolean onCheckCodecSupported(IaxClient client, MediaFrame.Format codec) {
			return codec == MediaFrame.Format.GSM_FULL_RATE;
		}
		
		@Override
		public MediaFrame.Format onQueryPreferredCodec(IaxClient client, Set<MediaFrame.Format> codecs) {
			return codecs.contains(MediaFrame.Format.GSM_FULL_RATE)
					? MediaFrame.Format.GSM_FULL_RATE
					: null;
		}
	}
	
	private class CallListener implements IaxCallListener {
		@Override public void onProceeding(IaxCall call) { }
		@Override public void onRinging(IaxCall call) { }
		@Override public void onCongestion(IaxCall call) { call.stop(); }
		@Override public void onBusy(IaxCall call) { call.stop(); }
		
		@Override
		public void onAnswered(IaxCall call) {
			StitzClient.callUI("setInCall", true, call.getCalledNumber() /* TODO display name if in contacts */, call.getCalledNumber());
			StitzClient.getAudioHandler().cancelLoopSound("call_outgoing");
		}
		
		@Override
		public void onHangup(IaxCall call) {
			StitzCallHandler.this.call = null;
			StitzCallHandler.this.inCall = false;
			if (StitzCallHandler.this.voicePushExecutor != null) {
				StitzCallHandler.this.shutdownExecutors.put(StitzCallHandler.this.voicePushExecutor, true);
				StitzCallHandler.this.voicePushExecutor.shutdown();
				try { StitzCallHandler.this.voicePushExecutor.awaitTermination(10, TimeUnit.SECONDS); }
				catch (InterruptedException e) { }
			}
			StitzCallHandler.this.voicePushExecutor = null;
			StitzClient.callUI("setInCall", false);
			StitzClient.getAudioHandler().cancelLoopSound("call_outgoing");
			StitzClient.getAudioHandler().playSound("hangup");
		}
		
		@Override
		public void onRetransmitError(IaxCall call, Frame frame) {
			LOGGER.warn("Error retransmitting frame {} for {}", frame, client);
		}
	}
}
