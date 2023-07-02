/*
 * Copyright (c) 2023 Cheos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Copyright (c) by Jan Jurgens, https://github.com/misternerd/djiax
 * Changes:
 * - refactor of individual components
 * - implement incoming calls
 */

package dev.cheos.stitz.iax;

import static dev.cheos.stitz.iax.informationelement.InformationElement.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import dev.cheos.stitz.iax.frame.*;
import dev.cheos.stitz.iax.informationelement.*;

public class IaxCall implements FrameHandler<Frame> {
	private static final Logger LOGGER = LoggerFactory.getLogger(IaxCall.class);
	private final IaxClient client;
	private CallState state;
	private final @Nonnull String name;
	private final short srcCallNumber;
	private short dstCallNumber = -1;
	private final @Nonnull String calledNumber;
	private final @Nonnull MediaFrame.Format[] supportedCodecs;
	private final @Nullable MediaFrame.Format preferredCodec;
	private MediaFrame.Format selectedCodec;
	private short samplingRate;
	private long startTimestamp;
	private long lastPingTimestamp = System.currentTimeMillis();
	private long lastRetransmitTimestamp = System.currentTimeMillis();
	private final AtomicInteger iSeqNo = new AtomicInteger();
	private final AtomicInteger oSeqNo = new AtomicInteger();
	private boolean active, audioActive, canSendAudioMiniFrames;
	final List<IaxCallListener> listeners = new LinkedList<>();
	final List<IaxCallListener.AudioListener> audioListeners = new LinkedList<>();
	private final Map<Long, FullFrame> awaitingResponse = new ConcurrentHashMap<>();
	final Frame.Builder frameBuilder;
	
	IaxCall(IaxClient client, short srcCallNumber, String calledNumber, short samplingRate, MediaFrame.Format... supportedCodecs) {
		Preconditions.checkArgument(supportedCodecs.length != 0, "A minimum of one codec must be supported!");
		this.client = client;
		this.state = CallState.getFor(this);
		this.name = "%s/%d:%s".formatted(client.getName(), srcCallNumber, calledNumber);
		this.srcCallNumber = srcCallNumber;
		this.calledNumber = calledNumber;
		this.supportedCodecs = supportedCodecs.clone();
		this.preferredCodec = this.selectedCodec = this.supportedCodecs[0];
		this.samplingRate = samplingRate;
		this.startTimestamp = System.currentTimeMillis();
		this.listeners.add(client);
		this.frameBuilder = Frame.builder()
				.srcCallNumber(this.srcCallNumber)
				.dstCallNumber((short) 0)
				.retransmission(false)
				.timestamp(this::getTimestampRelative)
				.oSeqNo(this::getAndIncrementOSeqNo)
				.iSeqNo(this::getISeqNo);
	}
	
	public IaxCall start() {
		if (this.active) return this;
		this.active = true;
		this.state = this.state.next();
		send(this.frameBuilder
				.fork()
				.iaxSubclass(IaxFrame.Subclass.NEW)
				.ie(version())
				.ie(callingName(this.client.getConfig().displayName()))
				.ie(format(this.preferredCodec))
				.ie(capability(this.supportedCodecs))
				.ie(samplingRate(this.samplingRate))
				.ie(username(this.client.getConfig().username()))
				.ie(calledNumber(this.calledNumber))
				.iax(), true);
		return this;
	}
	
	public void stop() {
		if (!this.active) return;
		this.active = false;
		this.state = this.state.prev();
		setAudioActive(false);
		send(this.frameBuilder.fork().iaxSubclass(IaxFrame.Subclass.HANGUP).iax(), true);
		this.listeners.forEach(l -> l.onHangup(this));
	}
	
	void remoteStop() {
		if (!this.active) return;
		this.active = false;
		this.state = this.state.prev();
		setAudioActive(false);
		this.listeners.forEach(l -> l.onHangup(this));
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getCalledNumber() {
		return this.calledNumber;
	}
	
	public IaxClient getClient() {
		return this.client;
	}
	
	void setState(CallState state) {
		this.state = state;
	}
	
	public CallState getState() {
		return this.state;
	}
	
	public byte getISeqNo() {
		return (byte) this.iSeqNo.get();
	}
	
	private byte getAndIncrementISeqNo() {
		return (byte) this.iSeqNo.getAndIncrement();
	}
	
	public byte getOSeqNo() {
		return (byte) this.oSeqNo.get();
	}
	
	private byte getAndIncrementOSeqNo() {
		return (byte) this.oSeqNo.getAndIncrement();
	}
	
	public short getSrcCallNumber() {
		return this.srcCallNumber;
	}
	
	public short getDstCallNumber() {
		return this.dstCallNumber;
	}
	
	void setDstCallNumber(short dstCallNumber) {
		if (this.dstCallNumber != -1) {
			LOGGER.warn("tried to override an already set destination call number for call {}", this);
			return;
		}
		this.dstCallNumber = dstCallNumber;
		this.frameBuilder.dstCallNumber(dstCallNumber);
	}
	
	public long getTimestampRelative() {
		return System.currentTimeMillis() - this.startTimestamp;
	}
	
	void setSelectedCodec(MediaFrame.Format codec) {
		Preconditions.checkArgument(codec.getType() == MediaFrame.Format.Type.AUDIO, "Expected codec of type AUDIO, got %s", codec.getType());
		this.selectedCodec = codec;
	}
	
	public MediaFrame.Format getSelectedCodec() {
		return this.selectedCodec;
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public void addListener(IaxCallListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(IaxCallListener listener) {
		this.listeners.remove(listener);
	}
	
	public void addListener(IaxCallListener.AudioListener listener) {
		this.audioListeners.add(listener);
	}
	
	public void removeListener(IaxCallListener.AudioListener listener) {
		this.audioListeners.remove(listener);
	}
	
	public void setAudioActive(boolean active) {
		if (this.audioActive == active)
			return;
		this.audioActive = active;
		this.audioListeners.forEach(l -> l.onSetEnabled(active));
	}
	
	public boolean isAudioActive() {
		return this.audioActive;
	}
	
	public void sendAudioData(byte[] data) {
		Preconditions.checkState(this.active && this.audioActive, "Cannot send audio over an inactive call %s", this);
		if (!canSendAudioMiniFrames) {
			send(this.frameBuilder.fork().mediaFormat(this.selectedCodec).data(data).voice(), true);
			this.canSendAudioMiniFrames = true;
		} else send(this.frameBuilder.data(data).mini());
	}
	
	boolean send(Frame frame) {
		return send(frame, false);
	}
	
	boolean send(Frame frame, boolean requireResponse) {
		if (!this.client.send(frame)) // we want to handle ack/replies ourselves here, tell the client not to do it for us
			return false;
		if (frame instanceof FullFrame fullFrame) {
			if (requireResponse)
				this.awaitingResponse.put((long) fullFrame.getOSeqNo(), fullFrame);
		}
		return true;
	}
	
	void markResponded(long oSeqNo) {
		if (this.awaitingResponse.remove(oSeqNo - 1) == null)
			LOGGER.debug("Tried to mark frame index {} as acknowledged when no such frame is awaiting acknowledgement", oSeqNo);
	}
	
	@Override
	public void handle(Frame frame) {
		if (frame instanceof MiniFrame miniFrame)
			this.audioListeners.forEach(l -> l.onAudioReceived(miniFrame.getData(), this.selectedCodec));
		else if (frame instanceof FullFrame fullFrame) {
			orderCheck: {
				if (frame instanceof IaxFrame iaxFrame) {
					if (iaxFrame.getIAXSubclass() == IaxFrame.Subclass.ACK) {
						markResponded(iaxFrame.getISeqNo());
						return;
					} else if (iaxFrame.getIAXSubclass() == IaxFrame.Subclass.INVAL
							|| iaxFrame.getIAXSubclass() == IaxFrame.Subclass.TXACC
							|| iaxFrame.getIAXSubclass() == IaxFrame.Subclass.TXCNT
							|| iaxFrame.getIAXSubclass() == IaxFrame.Subclass.VNAK)
						break orderCheck;
				}
				
				byte iSeqNo = getISeqNo();
				if (fullFrame.getOSeqNo() == iSeqNo)
					break orderCheck;
				
				LOGGER.warn("dropping out of order frame {}, expected oSeqNo {}", fullFrame, getISeqNo());
				if (fullFrame.getOSeqNo() > iSeqNo) // silently drop frames which we already received but got resent
					send(this.frameBuilder.fork()
							.timestamp(fullFrame.getTimestamp())
							.iSeqNo((byte) (iSeqNo - 1)) // server is to resend all frames with a higher iSeqNo than VNAK
							.iaxSubclass(IaxFrame.Subclass.VNAK)
							.iax(), true);
				else send(this.frameBuilder.fork() // we received the frame already, peer seems to not be aware of this -> send iax/ACK
						.oSeqNo(fullFrame.getISeqNo())
						.iSeqNo((byte) (fullFrame.getOSeqNo() + 1))
						.timestamp(fullFrame.getTimestamp())
						.iaxSubclass(IaxFrame.Subclass.ACK)
						.iax());
				return;
			}
			
			getAndIncrementISeqNo();
			getState().handle(fullFrame);
		}
		// invalid/unrecognized frame, drop silently
	}
	
	public void update() {
		if (!this.active) return;
		long now = System.currentTimeMillis();
		if (this.lastRetransmitTimestamp + IaxConstants.CALL_RETRANSMIT_INTERVAL < now) {
			retransmit();
			this.lastRetransmitTimestamp = now;
		}
		if (this.lastPingTimestamp + IaxConstants.CALL_PING_INTERVAL < now) {
			send(this.frameBuilder.fork().iaxSubclass(IaxFrame.Subclass.PING).iax(), true);
			this.lastPingTimestamp = now;
		}
	}
	
	public void retransmit() {
		long now = System.currentTimeMillis();
		Iterator<Entry<Long, FullFrame>> it = this.awaitingResponse.entrySet().iterator();
		if (it.hasNext())
			for (FullFrame frame = it.next().getValue(); it.hasNext(); frame = it.next().getValue()) {
				if (frame.getNextRetransmissionTime() > now)
					continue;
				frame.incRetransmissionCount();
				if (frame.getRetransmissionCount() > IaxConstants.TRANSMISSION_MAX_RETRY_COUNT) {
					LOGGER.warn("Did not receive response for frame {} after {} tries, dropping frame...", frame, frame.getRetransmissionCount());
					for (IaxCallListener l : this.listeners) l.onRetransmitError(this, frame);
					it.remove();
				} else if (frame.getGenerationTime() + IaxConstants.TRANSMISSION_RETRY_TIMEOUT < now) {
					LOGGER.warn("Did not receive response for frame {} after {} ms, dropping frame...", frame, now - frame.getGenerationTime());
					for (IaxCallListener l : this.listeners) l.onRetransmitError(this, frame);
					it.remove();
				} else send(frame);
			}
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", this.name)
				.add("state", this.state.getClass().getSimpleName())
				.add("srcCallNumber", this.srcCallNumber)
				.add("dstCallNumber", this.dstCallNumber)
				.add("calledNumber", this.calledNumber)
				.add("supportedCodecs", this.supportedCodecs)
				.add("preferredCodec", this.preferredCodec)
				.add("selectedCodec", this.selectedCodec)
				.add("samplingRate", this.samplingRate)
				.add("startTimestamp", this.startTimestamp)
				.add("iSeqNo", this.iSeqNo.get())
				.add("oSeqNo", this.oSeqNo.get())
				.add("active", this.active)
				.add("audioActive", this.audioActive)
				.add("listeners", this.listeners.size())
				.add("audioListeners", this.audioListeners.size())
				.toString();
	}
	
	
	/**
	 * @apiNote only extends IaxCall for convenience reasons, no method implemented in IaxCall is supported from IaxCall.Pending
	 */
	public static class Pending extends IaxCall {
		private final IaxClient client;
		private final String callingName, username, calledNumber;
		private final MediaFrame.Format[] supportedCodecs;
		private final MediaFrame.Format preferredCodec;
		private final short srcCallNumber, dstCallNumber, samplingRate;
		private CompletableFuture<IaxCall> pendingCall = new CompletableFuture<>();
		private boolean declined, accepted;
		private State state;
		
		Pending(
				IaxClient client,
				short srcCallNumber,
				short dstCallNumber,
				String callingName,
				String username,
				String calledNumber,
				MediaFrame.Format[] supportedCodecs,
				MediaFrame.Format preferredCodec,
				short samplingRate) {
			super(client, srcCallNumber, "", (short) 8, new MediaFrame.Format[] { null });
			this.client = client;
			this.srcCallNumber = srcCallNumber;
			this.dstCallNumber = dstCallNumber;
			this.callingName = callingName;
			this.username = username;
			this.calledNumber = calledNumber;
			this.supportedCodecs = supportedCodecs;
			this.preferredCodec = preferredCodec;
			this.samplingRate = samplingRate;
			setDstCallNumber(dstCallNumber);
			setState(new CallState.Pending(this));
		}
		
		Pending(IaxClient client, short srcCallNumber, IaxFrame frame) {
			super(client, srcCallNumber, "", (short) 8, new MediaFrame.Format[] { null });
			this.client = client;
			this.srcCallNumber = srcCallNumber;
			this.dstCallNumber = frame.getSrcCallNumber();
			setDstCallNumber(this.dstCallNumber);
			setState(new CallState.Pending(this));
			
			Optional<CallingName> callingName = frame.getIEOpt(InformationElementType.CALLING_NAME);
			Optional<Username> username = frame.getIEOpt(InformationElementType.USERNAME);
			Optional<CalledNumber> calledNumber = frame.getIEOpt(InformationElementType.CALLED_NUMBER);
			Optional<Capability> capability = frame.getIEOpt(InformationElementType.CAPABILITY);
			Optional<Format> format = frame.getIEOpt(InformationElementType.FORMAT);
			Optional<SamplingRate> samplingRate = frame.getIEOpt(InformationElementType.SAMPLINGRATE);
			
			String callingNameTmp = null, usernameTmp = null;
			if (callingName.isPresent()) {
				callingNameTmp = callingName.get().getCallingName();
				if (username.isEmpty())
					usernameTmp = callingNameTmp;
			}
			if (username.isPresent()) {
				usernameTmp = username.get().getUsername();
				if (callingName.isEmpty())
					callingNameTmp = usernameTmp;
			}
			this.callingName = callingNameTmp;
			this.username = usernameTmp;
			
			this.calledNumber = calledNumber.isPresent() ? calledNumber.get().getCalledNumber() : null;
			
			if (capability.isPresent() && format.isPresent()) {
				this.supportedCodecs = capability.get().getCapabilities();
				this.preferredCodec = format.get().getFormat();
			} else if (capability.isPresent()) {
				this.supportedCodecs = capability.get().getCapabilities();
				this.preferredCodec = this.supportedCodecs[0];
			} else if (format.isPresent()) {
				this.preferredCodec = format.get().getFormat();
				this.supportedCodecs = new MediaFrame.Format[] { this.preferredCodec };
			} else { // just force a codec if we don't receive one...
				this.supportedCodecs = new MediaFrame.Format[] { MediaFrame.Format.GSM_FULL_RATE };
				this.preferredCodec = MediaFrame.Format.GSM_FULL_RATE;
			}
			
			this.samplingRate = samplingRate.isPresent() ? samplingRate.get().getSamplingRate() : 8;
		}
		
		@Override
		public IaxCall start() { return this; }
		
		@Override
		public void stop() {
			decline();
		}
		
		public String getCallingName() {
			return this.callingName;
		}
		
		public String getUsername() {
			return this.username;
		}
		
		@Override
		public String getCalledNumber() {
			return this.calledNumber;
		}
		
		public MediaFrame.Format[] getSupportedCodecs() {
			return this.supportedCodecs;
		}
		
		public MediaFrame.Format getPreferredCodec() {
			return this.preferredCodec;
		}
		
		@Override
		public short getSrcCallNumber() {
			return this.srcCallNumber;
		}
		
		@Override
		public short getDstCallNumber() {
			return this.dstCallNumber;
		}
		
		public short getSamplingRate() {
			return this.samplingRate;
		}
		
		public boolean isDeclined() {
			return this.declined;
		}
		
		public boolean isAccepted() {
			return this.accepted;
		}
		
		public State getPendingState() {
			return state;
		}
		
		public void setState(State state) {
			this.state = state;
		}
		
		IaxCall promote(MediaFrame.Format[] supportedCodecs, MediaFrame.Format selectedCodec) {
			if (this.isDeclined()) {
				discard();
				return null;
			}
			IaxCall call = new IaxCall(this.client, this.srcCallNumber, this.calledNumber, this.samplingRate, supportedCodecs);
			call.selectedCodec = selectedCodec;
			call.state = new CallState.Up(call);
			call.dstCallNumber = this.dstCallNumber;
			call.startTimestamp = super.startTimestamp;
			call.iSeqNo.set(super.iSeqNo.get());
			call.oSeqNo.set(super.oSeqNo.get());
			call.active = true;
			call.setAudioActive(true);
			call.frameBuilder.dstCallNumber(this.dstCallNumber);
			call.awaitingResponse.putAll(super.awaitingResponse);
			call.listeners.addAll(this.listeners);
			call.audioListeners.addAll(this.audioListeners);
			this.pendingCall.complete(call);
			return call;
		}
		
		void discard() {
			this.pendingCall.complete(null);
		}
		
		public CompletableFuture<IaxCall> accept() {
			Preconditions.checkState(!this.declined, "Cannot accept an already declined call");
			this.accepted = true;
			return this.pendingCall;
		}
		
		public void decline() {
			if (this.accepted) return;
			this.declined = true;
		}
		
		void setDeclinedExt() {
			this.declined = true;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("callingName", this.callingName)
					.add("username", this.username)
					.add("calledNumber", this.calledNumber)
					.add("supportedCodecs", this.supportedCodecs)
					.add("preferredCodec", this.preferredCodec)
					.add("srcCallNumber", this.srcCallNumber)
					.add("dstCallNumber", this.dstCallNumber)
					.add("samplingRate", this.samplingRate)
					.add("declined", this.declined)
					.add("accepted", this.accepted)
					.add("state", this.state)
					.toString();
		}
		
		static enum State {
			REJECT_SENT,
			RINGING,
			HANGUP_SENT;
		}
	}
}
