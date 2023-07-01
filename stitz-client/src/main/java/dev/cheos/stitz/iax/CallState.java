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
 * Original by Jan Jurgens, https://github.com/misternerd/djiax
 * Changes:
 * - refactor of individual components
 * - implement incoming calls
 */

package dev.cheos.stitz.iax;

import static dev.cheos.stitz.iax.informationelement.InformationElement.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.cheos.stitz.iax.frame.*;
import dev.cheos.stitz.iax.informationelement.*;

public abstract sealed class CallState implements FrameHandler<FullFrame> {
	protected static final Logger LOGGER = LoggerFactory.getLogger(CallState.class);
	protected final IaxCall call;
	
	protected CallState(IaxCall call) {
		this.call = call;
	}
	
	@Override
	public void handle(FullFrame frame) {
		if (frame instanceof IaxFrame iaxFrame)
			switch (iaxFrame.getIAXSubclass()) {
				case ACK -> { } // this.call.markResponded(iaxFrame.getISeqNo());
				case ACCEPT -> ack(iaxFrame);
				case HANGUP -> {
					this.call.remoteStop();
					ack(iaxFrame);
				}
				case LAGRP -> {
					LOGGER.info("current lag is {}", this.call.getTimestampRelative() - iaxFrame.getTimestamp());
					this.call.markResponded(iaxFrame.getISeqNo());
					ack(iaxFrame);
				}
				case LAGRQ -> this.call.send(this.call.frameBuilder.fork()
						.dstCallNumber(iaxFrame.getSrcCallNumber())
						.timestamp(iaxFrame.getTimestamp())
						.iaxSubclass(IaxFrame.Subclass.LAGRP)
						.iax(), true);
				case PING, POKE -> this.call.send(this.call.frameBuilder.fork()
						.dstCallNumber(iaxFrame.getSrcCallNumber())
						.timestamp(iaxFrame.getTimestamp())
						.iaxSubclass(IaxFrame.Subclass.PONG)
						.iax(), true);
				case PONG -> {
					this.call.markResponded(iaxFrame.getISeqNo());
					ack(iaxFrame);
				}
				case REGACK -> ack(iaxFrame);
				default -> {
					LOGGER.warn("state {} did not handle frame {}", getClass().getSimpleName(), iaxFrame);
					ack(iaxFrame);
				}
			}
		else {
			LOGGER.warn("state {} did not handle frame {}", getClass().getSimpleName(), frame);
			ack(frame);
		}
	}
	
	public abstract CallState next();
	public abstract CallState prev();
	
	protected void ack(FullFrame frame) {
		this.call.send(this.call.frameBuilder.fork()
				.oSeqNo(frame.getISeqNo())
				.iSeqNo((byte) (frame.getOSeqNo() + 1))
				.timestamp(frame.getTimestamp())
				.iaxSubclass(IaxFrame.Subclass.ACK)
				.iax());
	}
	
	protected void inval(FullFrame frame) {
		this.call.send(this.call.frameBuilder.fork()
				.oSeqNo(frame.getISeqNo())
				.iSeqNo((byte) (frame.getOSeqNo() + 1))
				.timestamp(frame.getTimestamp())
				.iaxSubclass(IaxFrame.Subclass.INVAL)
				.iax());
	}
	
	public static CallState getFor(IaxCall call) { return call.getState() != null ? call.getState() : new Initial(call); }
	
	
	final static class Pending extends CallState {
		Pending(IaxCall.Pending call) { super(call); }
		@Override public CallState next() { return this; }
		@Override public CallState prev() { return this; }
		
		@Override
		public void handle(FullFrame frame) {
			switch (((IaxCall.Pending) call).getPendingState()) {
				case REJECT_SENT -> {
					if (frame instanceof IaxFrame iaxFrame && iaxFrame.getIAXSubclass() == IaxFrame.Subclass.ACK) {
						this.call.markResponded(iaxFrame.getISeqNo());
						return;
					}
				}
				case HANGUP_SENT -> {
					if (frame instanceof IaxFrame iaxFrame && iaxFrame.getIAXSubclass() == IaxFrame.Subclass.ACK) {
						this.call.markResponded(iaxFrame.getISeqNo());
						return;
					}
				}
				case RINGING -> {
					if (frame instanceof IaxFrame iaxFrame)
						switch (iaxFrame.getIAXSubclass()) {
							case ACK -> {
								this.call.markResponded(iaxFrame.getISeqNo());
								return;
							}
							case HANGUP -> {
								((IaxCall.Pending) call).setDeclinedExt();
								ack(iaxFrame);
								return;
							}
							default -> { }
					}
				}
				default -> throw new IllegalStateException("unsupported pending call state");
			}
			super.handle(frame);
		}
	}
	
	final static class Initial extends CallState {
		Initial(IaxCall call) { super(call); }
		@Override public CallState next() { return new Waiting(this.call); }
		@Override public CallState prev() { return this; }
	}
	
	final static class Waiting extends CallState {
		private final AtomicInteger authTries = new AtomicInteger();
		Waiting(IaxCall call) { super(call); }
		@Override public CallState next() { return new Linked(this.call); }
		@Override public CallState prev() { return new Initial(this.call); }
		
		@Override
		public void handle(FullFrame frame) {
			if (frame instanceof IaxFrame iaxFrame)
				switch (iaxFrame.getIAXSubclass()) {
					case ACCEPT -> {
						this.call.setState(next());
						this.call.markResponded(iaxFrame.getISeqNo());
						ack(iaxFrame);
						return;
					}
					case AUTHREQ -> {
						this.call.markResponded(iaxFrame.getISeqNo());
						if (this.authTries.getAndIncrement() > 10) {
							LOGGER.warn("couldn't authenticate call, are the given credentials valid?");
							this.call.setState(prev());
							return;
						}
						Frame.Builder builder = this.call.frameBuilder.fork()
								.iaxSubclass(IaxFrame.Subclass.AUTHREP)
								.ie(username(this.call.getClient().getConfig().username()))
								.ie(refresh(this.call.getClient().getServerRefresh()));
						iaxFrame.<Challenge>getIEOpt(InformationElementType.CHALLENGE)
								.map(Challenge::getChallenge)
								.map(c -> md5Result(c, this.call.getClient().getConfig().password()))
								.ifPresentOrElse(builder::ie, () -> LOGGER.warn("did not receive a challenge from server, unable to send authentication response"));
						this.call.send(builder.iax(), true);
						return;
					}
					case REJECT -> {
						this.call.markResponded(iaxFrame.getISeqNo());
						this.call.setState(prev());
						this.call.remoteStop();
						LOGGER.warn("call {} rejected, cause being {}: {}",
								this.call.getName(),
								iaxFrame.<CauseCode>getIEOpt(InformationElementType.CAUSECODE).map(CauseCode::getCause).orElse(CauseCode.Cause.SERVICE_OR_OPTION_NOT_AVAILABLE),
								iaxFrame.<Cause>getIEOpt(InformationElementType.CAUSE).map(Cause::getCause).orElse("null"));
						ack(iaxFrame);
						return;
					}
					default -> { }
				}
			super.handle(frame);
		}
	}
	
	final static class Linked extends CallState {
		Linked(IaxCall call) { super(call); }
		@Override public CallState next() { return new Up(this.call); }
		@Override public CallState prev() { return new Initial(this.call); }
		
		@Override
		public void handle(FullFrame frame) {
			if (frame instanceof ControlFrame controlFrame)
				switch (controlFrame.getCFSubclass()) {
					case ANSWER -> {
						this.call.setState(next());
						this.call.listeners.forEach(l -> l.onAnswered(this.call));
						ack(controlFrame);
						return;
					}
					case BUSY -> {
						this.call.setState(prev());
						this.call.listeners.forEach(l -> l.onBusy(this.call));
						ack(controlFrame);
						return;
					}
					case CONGESTION -> {
						this.call.setState(prev());
						this.call.listeners.forEach(l -> l.onCongestion(this.call));
						ack(controlFrame);
						return;
					}
					case PROCEEDING -> {
						this.call.listeners.forEach(l -> l.onProceeding(this.call));
						ack(controlFrame);
						return;
					}
					case RINGING -> {
						this.call.listeners.forEach(l -> l.onRinging(this.call));
						ack(controlFrame);
						return;
					}
					default -> { }
				}
			else if (frame instanceof VoiceFrame voiceFrame) {
				this.call.setAudioActive(true);
				this.call.setSelectedCodec(voiceFrame.getFormat());
				byte[] data = voiceFrame.getData();
				if (data != null)
					this.call.audioListeners.forEach(l -> l.onAudioReceived(data, voiceFrame.getFormat()));
				ack(voiceFrame);
				return;
			}
			super.handle(frame);
		}
	}
	
	final static class Up extends CallState {
		Up(IaxCall call) { super(call); }
		@Override public CallState next() { return this; }
		@Override public CallState prev() { return new Initial(this.call); }
		
		@Override
		public void handle(FullFrame frame) {
			if (frame instanceof VoiceFrame voiceFrame) {
				this.call.setAudioActive(true);
				this.call.setSelectedCodec(voiceFrame.getFormat());
				byte[] data = voiceFrame.getData();
				if (data != null)
					this.call.audioListeners.forEach(l -> l.onAudioReceived(data, voiceFrame.getFormat()));
				ack(voiceFrame);
				return;
			}
			super.handle(frame);
		}
	}
}
