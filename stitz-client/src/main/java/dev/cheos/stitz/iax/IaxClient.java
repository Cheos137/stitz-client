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

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import dev.cheos.stitz.iax.frame.*;
import dev.cheos.stitz.iax.informationelement.CauseCode;

public class IaxClient implements IaxCallListener, FrameHandler<Frame>, FrameDistributor, AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(IaxClient.class);
	private final String name;
	private final short srcCallNumber;
	private short dstCallNumber = -1;
	private final AtomicInteger iSeqNo = new AtomicInteger();
	private final AtomicInteger oSeqNo = new AtomicInteger();
	private ClientState state;
	private long lastRegisteredTimestamp = -1;
	private long regRelTimestamp = -1;
	private final List<IaxClientListener> listeners = new LinkedList<>();
	private final Configuration config;
	private final Map<Short, IaxCall> calls = new ConcurrentHashMap<>();
	private final Map<Short, IaxCall> callsByDestination = new ConcurrentHashMap<>();
	private final AtomicInteger callCount = new AtomicInteger();
	private final IaxIOHandler ioHandler;
	private final ExecutorService packetHandlerService;
	private final Map<Long, FullFrame> awaitingResponse = new ConcurrentHashMap<>();
	private final Timer timer;
	private TimerTask updateTask, stateActionRetryTask;
	private boolean connected;
	private LocalDateTime serverDatetime;
	private InetAddress serverApparentAddr;
	private short serverRefresh = IaxConstants.CLIENT_REGISTRATION_REFRESH;
	final AtomicInteger timesRejected = new AtomicInteger();
	final Frame.Builder frameBuilder;
	
	public IaxClient(IaxClient.Configuration config, short srcCallNumber) throws IOException {
		this.config = config;
		this.srcCallNumber = srcCallNumber;
		this.name = config.clientName().formatted(srcCallNumber);
		this.ioHandler = new IaxIOHandler(this);
		this.packetHandlerService = Executors.newCachedThreadPool();
		this.state = new ClientState.Unregistered(this);
		this.timer = new Timer(this.name + "-timer");
		this.frameBuilder = Frame.builder()
				.srcCallNumber(this.srcCallNumber)
				.dstCallNumber((short) 0)
				.retransmission(false)
				.timestamp(this::getTimestampRelative)
				.oSeqNo(this::getAndIncrementOSeqNo)
				.iSeqNo(this::getISeqNo);
	}
	
	public void connect() {
		if (this.connected) return;
		this.connected = true;
		
		this.updateTask = new TimerTask() { @Override public void run() {
			retransmit();
			IaxClient.this.calls.values().forEach(IaxCall::update);
		}};
		this.timer.scheduleAtFixedRate(this.updateTask, 1000, 1000);
		
		resetLastRegisteredTimestamp();
		setState(new ClientState.RegSent(this));
		if (!send(this.frameBuilder.fork()
				.timestamp(0)
				.iaxSubclass(IaxFrame.Subclass.REGREQ)
				.ie(username(getConfig().username()))
				.ie(refresh(getServerRefresh()))
				.iax(), true))
			throw new IllegalStateException("failed to connect: unable to send REGREQ packet");
	}
	
	public void disconnect() {
		if (!this.connected) return;
		this.connected = false;
		
		this.updateTask.cancel();
		if (this.stateActionRetryTask != null)
			this.stateActionRetryTask.cancel();
		this.calls.forEach((v, call) -> call.stop());
		
		this.serverRefresh = IaxConstants.CLIENT_REGISTRATION_REFRESH;
		setState(new ClientState.Releasing(this));
		this.regRelTimestamp = getTimestampRelative();
		resetISeqNo();
		resetOSeqNo();
		setDstCallNumber((short) -1);
		this.frameBuilder.dstCallNumber((short) 0);
		if (!send(this.frameBuilder.fork()
				.timestamp(this.regRelTimestamp)
				.iaxSubclass(IaxFrame.Subclass.REGREL)
				.ie(username(getConfig().username()))
				.ie(causeCode(CauseCode.Cause.NORMAL_UNSPECIFIED))
				.ie(cause("user requested disconnect"))
				.iax(), true))
			throw new IllegalStateException("failed to disconnect: unable to send REGREL packet");
		this.listeners.forEach(l -> l.onDisconnect(this));
	}
	
	public IaxCall call(String number, MediaFrame.Format... codecs) {
		if (this.callCount.getAndIncrement() >= getConfig().callCountLimit()) { // no need to synchronize here as we immediately claim a call slot
			this.callCount.getAndDecrement(); // release the unavailable slot
			LOGGER.warn("tried to create more calls than the configured limit {}", getConfig().callCountLimit());
			return null;
		}
		
		IaxCall call;
		synchronized (this.calls) { // need to synchronize here as to not use a source call number twice at the same time
			short srcCallNumber = IaxConstants.CLIENT_MAX_SOURCE_CALL_NUMBER + 1;
			for (; this.calls.containsKey(srcCallNumber); srcCallNumber++); // use lowest available number
			
			call = new IaxCall(this, srcCallNumber, number, (short) 8 /* kHz */, codecs);
			this.calls.put(srcCallNumber, call);
		}
		return call;
	}
	
	public String getName() {
		return this.name;
	}
	
	public short getSrcCallNumber() {
		return this.srcCallNumber;
	}
	
	public short getDstCallNumber() {
		return this.dstCallNumber;
	}
	
	void setDstCallNumber(short dstCallNumber) {
		this.dstCallNumber = dstCallNumber;
		this.frameBuilder.dstCallNumber(dstCallNumber);
	}
	
	public Configuration getConfig() {
		return this.config;
	}
	
	public ClientState getState() {
		return this.state;
	}
	
	void setState(ClientState state) {
		this.listeners.forEach(l -> l.onStateChanged(this, this.state, state));
		this.state = state;
		if (this.stateActionRetryTask != null)
			this.stateActionRetryTask.cancel();
		this.stateActionRetryTask = null;
		
		if (this.state != null && this.state.getRetryInterval() > 0) {
			this.stateActionRetryTask = new TimerTask() { @Override public void run() { IaxClient.this.state.retry(); }};
			this.timer.scheduleAtFixedRate(this.stateActionRetryTask, this.state.getRetryInterval(), this.state.getRetryInterval());
		}
		if (state instanceof ClientState.Registered || state instanceof ClientState.Rejected || state instanceof ClientState.NoAuth)
			this.listeners.forEach(l -> l.onConnect(this, state instanceof ClientState.Registered));
	}
	
	public long getTimestampRelative() {
		return System.currentTimeMillis() - this.lastRegisteredTimestamp;
	}
	
	void resetLastRegisteredTimestamp() {
		this.lastRegisteredTimestamp = System.currentTimeMillis();
	}
	
	private byte getAndIncrementISeqNo() {
		return (byte) this.iSeqNo.getAndIncrement();
	}
	
	public byte getISeqNo() {
		return (byte) this.iSeqNo.get();
	}
	
	void resetISeqNo() {
		this.iSeqNo.set(0);
	}
	
	private byte getAndIncrementOSeqNo() {
		return (byte) this.oSeqNo.getAndIncrement();
	}
	
	public byte getOSeqNo() {
		return (byte) this.oSeqNo.get();
	}
	
	void resetOSeqNo() {
		this.oSeqNo.set(0);
	}
	
	public long getRegRelTimestamp() {
		return this.regRelTimestamp;
	}
	
	void setRegRelTimestamp(long timestamp) {
		this.regRelTimestamp = timestamp;
	}
	
	public LocalDateTime getServerDatetime() {
		return serverDatetime;
	}
	
	void setServerDatetime(LocalDateTime serverDatetime) {
		this.serverDatetime = serverDatetime;
	}
	
	public InetAddress getServerApparentAddr() {
		return serverApparentAddr;
	}
	
	void setServerApparentAddr(InetAddress serverApparentAddr) {
		this.serverApparentAddr = serverApparentAddr;
	}
	
	public short getServerRefresh() {
		return serverRefresh;
	}
	
	void setServerRefresh(short serverRefresh) {
		this.serverRefresh = serverRefresh;
	}
	
	public void addListener(IaxClientListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(IaxClientListener listener) {
		this.listeners.remove(listener);
	}
	
	boolean send(Frame frame) {
		return send(frame, false);
	}
	
	boolean send(Frame frame, boolean requireResponse) {
		try {
			LOGGER.debug("SEND {}", frame);
			this.ioHandler.send(frame);
			if (frame instanceof FullFrame fullFrame) {
				fullFrame.updateRetransmissionTime();
				if (requireResponse)
					this.awaitingResponse.put((long) fullFrame.getOSeqNo(), fullFrame);
			}
			return true;
		} catch (Exception e) {
			LOGGER.warn("Exception sending frame " + String.valueOf(frame), e);
			return false;
		}
	}
	
	void markResponded(long iSeqNo) { // iSeqNo is 1-based fsr when getting response from asterisk
		if (this.awaitingResponse.remove(iSeqNo - 1) == null)
			LOGGER.debug("Tried to mark frame index {} as acknowledged when no such frame is awaiting acknowledgement", iSeqNo - 1);
	}
	
	@Override
	public void onHangup(IaxCall call) {
		this.calls.remove(call.getSrcCallNumber());
		this.callsByDestination.remove(call.getDstCallNumber());
		this.callCount.getAndDecrement();
	}
	
	@Override
	public void submit(Frame frame) { // get work off the IO-thread as fast as possible
		// the executor service uses as many threads as needed at any given time
		// thus, it does not matter how long it takes to handle each individual frame
		this.packetHandlerService.submit(() -> handle(frame));
	}
	
	@Override
	public void handle(Frame frame) {
		Preconditions.checkNotNull(frame, "frame is null");
		LOGGER.debug("RECV {}", frame);
		
		if (frame instanceof MiniFrame) // miniFrames only transmit their source call number, need to map to the destination call number respectively or drop frame if call not found
			Optional.ofNullable(this.callsByDestination.get(frame.getSrcCallNumber())).ifPresent(call -> call.handle(frame));
		else if (frame instanceof FullFrame fullFrame) {
			if (fullFrame.getDstCallNumber() > IaxConstants.CLIENT_MAX_SOURCE_CALL_NUMBER) // frame needs to be handled by a call
				Optional.ofNullable(this.calls.get(fullFrame.getDstCallNumber())).ifPresentOrElse(
						call -> {
							this.callsByDestination.computeIfAbsent(fullFrame.getSrcCallNumber(), dst -> { call.setDstCallNumber(dst); return call; });
							call.handle(fullFrame);
						},
						() -> LOGGER.warn("dropping received frame {} for unknown call {}/{}:{}", frame, getName(), fullFrame.getDstCallNumber(), fullFrame.getSrcCallNumber()));
			else { // frame needs to be handled here
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
					
					if (fullFrame.getDstCallNumber() == 0) // call initiation, typically iax/NEW, iax/PING or iax/PONG, iSeqNo won't match as this is a separate call
						break orderCheck;
					
					byte iSeqNo = getISeqNo();
					if (fullFrame.getOSeqNo() == iSeqNo)
						break orderCheck;
					
					LOGGER.warn("dropping out of order frame {}, expected oSeqNo {}", fullFrame, getISeqNo());
					if (fullFrame.getOSeqNo() > iSeqNo) // silently drop frames which we already received but got resent
						send(this.frameBuilder.fork()
								.timestamp(fullFrame.getTimestamp())
								.iSeqNo((byte) (iSeqNo - 1)) // server is to resend all frames with a higher iSeqNo than the one of this iax/VNAK
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
				
				if (fullFrame.getDstCallNumber() != 0) // call initiation, typically iax/NEW, iax/PING or iax/PONG, don't increment as this is a separrate call
					getAndIncrementISeqNo();
				if (frame instanceof IaxFrame iaxFrame && iaxFrame.getIAXSubclass() == IaxFrame.Subclass.NEW) // iax/NEW must be sent to dstCallNo 0
					handleIncomingCall(iaxFrame);
				else getState().handle(fullFrame);
			}
		}
		// invalid/unrecognized frame, drop silently
	}
	
	private void handleIncomingCall(IaxFrame frame) {
		boolean reject = false;
		if (this.callCount.getAndIncrement() >= getConfig().callCountLimit()) { // no need to synchronize here as we immediately claim a call slot
			// don't release the unavailable slot
			reject = true;
		}
		
		IaxCall.Pending pendingCall;
		// don't care if already rejected, we have enough source call numbers to spare (also don't care about their order or consecutiveness)
		synchronized (this.calls) { // need to synchronize here as to not use a source call number twice at the same time
			short srcCallNumber = IaxConstants.CLIENT_MAX_SOURCE_CALL_NUMBER + 1;
			for (; this.calls.containsKey(srcCallNumber); srcCallNumber++); // use lowest available number
			pendingCall = new IaxCall.Pending(this, srcCallNumber, frame);
			this.calls.put(srcCallNumber, pendingCall);
			this.callsByDestination.put(frame.getSrcCallNumber(), pendingCall);
		}
		
		if (reject) {
			pendingCall.setState(IaxCall.Pending.State.REJECT_SENT);
			pendingCall.send(pendingCall.frameBuilder
					.fork()
					.iaxSubclass(IaxFrame.Subclass.REJECT)
					.ie(causeCode(CauseCode.Cause.NO_CHANNEL_AVAILABLE))
					.iax(), true);
			pendingCall.discard();
			return;
		}
		
		Set<MediaFrame.Format> supportedCodecs = new HashSet<>();
		List<MediaFrame.Format> codecs = Arrays.asList(pendingCall.getSupportedCodecs());
		this.listeners.forEach(l -> codecs.forEach(c -> {
			if (l.onCheckCodecSupported(this, c))
				supportedCodecs.add(c);
		}));
		
		if (supportedCodecs.isEmpty()) {
			pendingCall.setState(IaxCall.Pending.State.REJECT_SENT);
			pendingCall.send(pendingCall.frameBuilder
					.fork()
					.iaxSubclass(IaxFrame.Subclass.REJECT)
					.ie(causeCode(CauseCode.Cause.INCOMPATIBLE_DESTINATION))
					.iax(), true);
			pendingCall.discard();
			return;
		}
		
		MediaFrame.Format preferred = supportedCodecs.contains(pendingCall.getPreferredCodec()) ? pendingCall.getPreferredCodec() : null;
		for (IaxClientListener l : this.listeners) {
			MediaFrame.Format codec = l.onQueryPreferredCodec(this, supportedCodecs);
			if (codec != null)
				preferred = codec;
		}
		if (preferred == null)
			preferred = supportedCodecs.iterator().next();
		
		pendingCall.setState(IaxCall.Pending.State.RINGING);
		pendingCall.send(pendingCall.frameBuilder
				.fork()
				.iaxSubclass(IaxFrame.Subclass.ACCEPT)
				.ie(format(preferred))
				.iax(), true);
		pendingCall.send(pendingCall.frameBuilder
				.fork()
				.cfSubclass(ControlFrame.Subclass.RINGING)
				.iax(), true);
		this.listeners.forEach(l -> l.onCallIncoming(this, pendingCall));
		
		if (pendingCall.isDeclined()) {
			pendingCall.setState(IaxCall.Pending.State.HANGUP_SENT);
			pendingCall.send(pendingCall.frameBuilder
					.fork()
					.iaxSubclass(IaxFrame.Subclass.HANGUP)
					.ie(causeCode(CauseCode.Cause.CALL_REJECTED))
					.iax(), true);
			pendingCall.discard();
			return;
		} else if (!pendingCall.isAccepted()) {
			pendingCall.setState(IaxCall.Pending.State.HANGUP_SENT);
			pendingCall.send(pendingCall.frameBuilder
					.fork()
					.iaxSubclass(IaxFrame.Subclass.HANGUP)
					.ie(causeCode(CauseCode.Cause.NO_USER_RESPONSE))
					.iax(), true);
			pendingCall.discard();
			return;
		}
		pendingCall.send(pendingCall.frameBuilder
				.fork()
				.cfSubclass(ControlFrame.Subclass.ANSWER)
				.iax(), true);
		
		IaxCall call = pendingCall.promote(supportedCodecs.toArray(MediaFrame.Format[]::new), preferred);
		if (call == null) {
			this.calls.remove(pendingCall.getSrcCallNumber());
			this.callsByDestination.remove(pendingCall.getDstCallNumber());
		} else {
			this.calls.put(pendingCall.getSrcCallNumber(), call);
			this.callsByDestination.put(pendingCall.getDstCallNumber(), call);
			this.callCount.getAndDecrement();
		}
	}
	
	private void retransmit() {
		long now = System.currentTimeMillis();
		Iterator<Entry<Long, FullFrame>> it = this.awaitingResponse.entrySet().iterator();
		if (it.hasNext())
			for (FullFrame frame = it.next().getValue(); it.hasNext(); frame = it.next().getValue()) {
				if (frame.getNextRetransmissionTime() > now)
					continue;
				frame.incRetransmissionCount();
				if (frame.getRetransmissionCount() > IaxConstants.TRANSMISSION_MAX_RETRY_COUNT) {
					LOGGER.warn("Did not receive response for frame {} after {} tries, dropping frame...", frame, frame.getRetransmissionCount());
					for (IaxClientListener l : this.listeners) l.onRetransmitError(this, frame);
					it.remove();
				} else if (frame.getGenerationTime() + IaxConstants.TRANSMISSION_RETRY_TIMEOUT < now) {
					LOGGER.warn("Did not receive response for frame {} after {} ms, dropping frame...", frame, now - frame.getGenerationTime());
					for (IaxClientListener l : this.listeners) l.onRetransmitError(this, frame);
					it.remove();
				} else send(frame);
			}
	}
	
	@Override
	public void close() throws IOException {
		if (this.connected)
			disconnect();
		this.ioHandler.close();
		try {
			this.packetHandlerService.shutdown();
			if (!this.packetHandlerService.awaitTermination(10, TimeUnit.SECONDS))
				this.packetHandlerService.shutdownNow();
		} catch (InterruptedException e) {
			this.packetHandlerService.shutdownNow();
		}
		
		this.timer.cancel();
	}
	
	@Override
	public String toString() {
		if (!this.connected)
			return MoreObjects.toStringHelper(this)
					.addValue("disconnected")
					.add("name", this.name)
					.add("server", this.config.remoteAddress())
					.add("port", this.config.remotePort())
					.add("username", this.config.username())
					.toString();
		return MoreObjects.toStringHelper(this)
				.addValue("connected")
				.add("name", this.name)
				.add("server", this.config.remoteAddress())
				.add("port", this.config.remotePort())
				.add("username", this.config.username())
				.add("state", this.state.getClass().getSimpleName())
				.add("callCount", this.callCount)
				.add("srcCallNumber", this.srcCallNumber)
				.add("dstCallNumber", this.dstCallNumber)
				.add("iSeqNo", this.iSeqNo)
				.add("oSeqNo", this.oSeqNo)
				.toString();
	}
	
	
	public record Configuration(
			String clientName,
			String displayName,
			String username,
			String password,
			InetAddress remoteAddress,
			int remotePort,
			int callCountLimit) { }
}
