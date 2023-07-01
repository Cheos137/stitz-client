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

public abstract sealed class ClientState implements FrameHandler<FullFrame> {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ClientState.class);
	protected final IaxClient client;
	
	protected ClientState(IaxClient client) {
		this.client = client;
	}
	
	@Override
	public void handle(FullFrame frame) {
		if (frame instanceof IaxFrame iaxFrame)
			switch (iaxFrame.getIAXSubclass()) {
				case ACK -> { } // this.client.markResponded(iaxFrame.getISeqNo());
				case ACCEPT -> ack(iaxFrame);
				case HANGUP -> ack(iaxFrame); // hangup for client?
				case LAGRP -> {
					LOGGER.info("current lag is {}", this.client.getTimestampRelative() - iaxFrame.getTimestamp());
					this.client.markResponded(iaxFrame.getISeqNo());
					ack(iaxFrame);
				}
				case LAGRQ -> this.client.send(this.client.frameBuilder.fork()
						.dstCallNumber(iaxFrame.getSrcCallNumber())
						.timestamp(iaxFrame.getTimestamp())
						.iaxSubclass(IaxFrame.Subclass.LAGRP)
						.iax(), true);
				case PING, POKE -> this.client.send(this.client.frameBuilder.fork()
						.dstCallNumber(iaxFrame.getSrcCallNumber())
						.timestamp(iaxFrame.getTimestamp())
						.iSeqNo((byte) 1)
						.oSeqNo((byte) 0)
						.iaxSubclass(IaxFrame.Subclass.PONG)
						.iax(), true);
				case PONG -> {
					this.client.markResponded(iaxFrame.getISeqNo());
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
	
	public void retry() { }
	public long getRetryInterval() { return 0; }
	
	protected void ack(FullFrame frame) {
		this.client.send(this.client.frameBuilder.fork()
				.oSeqNo(frame.getISeqNo())
				.iSeqNo((byte) (frame.getOSeqNo() + 1))
				.timestamp(frame.getTimestamp())
				.iaxSubclass(IaxFrame.Subclass.ACK)
				.iax());
	}
	
	protected void inval(FullFrame frame) {
		this.client.send(this.client.frameBuilder.fork()
				.oSeqNo(frame.getISeqNo())
				.iSeqNo((byte) (frame.getOSeqNo() + 1))
				.timestamp(frame.getTimestamp())
				.iaxSubclass(IaxFrame.Subclass.INVAL)
				.iax());
	}
	
	
	static final class Unregistered extends ClientState {
		Unregistered(IaxClient client) { super(client); }
	}
	
	static final class RegSent extends ClientState {
		private final AtomicInteger authTries = new AtomicInteger();
		RegSent(IaxClient client) { super(client); }
		
		@Override
		public void handle(FullFrame frame) {
			if (this.client.getDstCallNumber() == -1)
				this.client.setDstCallNumber(frame.getSrcCallNumber());
			if (frame instanceof IaxFrame iaxFrame)
				switch (iaxFrame.getIAXSubclass()) {
					case REGACK -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						this.client.setState(new Registered(this.client));
						iaxFrame.<Datetime>getIEOpt(InformationElementType.DATETIME).map(Datetime::getDatetime).ifPresent(this.client::setServerDatetime);
						iaxFrame.<Refresh>getIEOpt(InformationElementType.REFRESH).map(Refresh::getRefresh).ifPresent(this.client::setServerRefresh);
						iaxFrame.<ApparentAddr>getIEOpt(InformationElementType.APPARENT_ADDR).map(ApparentAddr::getApparentAddr).ifPresent(this.client::setServerApparentAddr);
						ack(iaxFrame);
						return;
					}
					case REGAUTH -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						if (this.authTries.getAndIncrement() > IaxConstants.REGISTRATION_AUTH_MAX_RETRY_COUNT) {
							LOGGER.warn("couldn't register with server, are the given credentials valid?");
							this.client.setState(new NoAuth(this.client));
							return;
						}
						Frame.Builder builder = this.client.frameBuilder.fork()
								.iaxSubclass(IaxFrame.Subclass.REGREQ)
								.ie(username(this.client.getConfig().username()))
								.ie(refresh(this.client.getServerRefresh()));
						iaxFrame.<Challenge>getIEOpt(InformationElementType.CHALLENGE)
								.map(Challenge::getChallenge)
								.map(c -> md5Result(c, this.client.getConfig().password()))
								.ifPresentOrElse(builder::ie, () -> LOGGER.warn("did not receive a challenge from server, unable to send authentication response"));
						this.client.send(builder.iax(), true);
						return;
					}
					case REGREJ -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						this.client.setState(new Rejected(this.client));
						LOGGER.warn("registration rejected, cause being {}: {}",
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
	
	static final class NoAuth extends ClientState {
		NoAuth(IaxClient client) { super(client); }
	}
	
	static final class Registered extends ClientState {
		Registered(IaxClient client) { super(client); }
		
		@Override
		public void retry() {
			this.client.setState(new RegSent(this.client));
			this.client.resetISeqNo();
			this.client.resetOSeqNo();
			this.client.resetLastRegisteredTimestamp();
			this.client.send(this.client.frameBuilder.fork()
					.dstCallNumber((short) 0)
					.timestamp(0)
					.iaxSubclass(IaxFrame.Subclass.REGREQ)
					.ie(username(this.client.getConfig().username()))
					.ie(refresh(this.client.getServerRefresh()))
					.iax(), true);
		}
		
		@Override
		public long getRetryInterval() {
			return this.client.getServerRefresh() * 1000;
		}
	}
	
	static final class Rejected extends ClientState {
		Rejected(IaxClient client) { super(client); }
		
		@Override
		public void handle(FullFrame frame) {
			if (frame instanceof IaxFrame iaxFrame)
				switch (iaxFrame.getIAXSubclass()) {
					case REGACK -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						this.client.setState(new Registered(this.client));
						iaxFrame.<Datetime>getIEOpt(InformationElementType.DATETIME).map(Datetime::getDatetime).ifPresent(this.client::setServerDatetime);
						iaxFrame.<Refresh>getIEOpt(InformationElementType.REFRESH).map(Refresh::getRefresh).ifPresent(this.client::setServerRefresh);
						iaxFrame.<ApparentAddr>getIEOpt(InformationElementType.APPARENT_ADDR).map(ApparentAddr::getApparentAddr).ifPresent(this.client::setServerApparentAddr);
						ack(iaxFrame);
						this.client.timesRejected.set(0);
						return;
					}
					case REGAUTH -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						Frame.Builder builder = this.client.frameBuilder.fork()
								.iaxSubclass(IaxFrame.Subclass.REGREQ)
								.ie(username(this.client.getConfig().username()))
								.ie(refresh(this.client.getServerRefresh()));
						iaxFrame.<Challenge>getIEOpt(InformationElementType.CHALLENGE)
								.map(Challenge::getChallenge)
								.map(c -> md5Result(c, this.client.getConfig().password()))
								.ifPresentOrElse(builder::ie, () -> LOGGER.warn("did not receive a challenge from server, unable to send authentication response"));
						this.client.send(builder.iax(), true);
						return;
					}
					case REGREJ -> {
						this.client.setDstCallNumber(iaxFrame.getSrcCallNumber());
						this.client.markResponded(iaxFrame.getISeqNo());
						LOGGER.warn("registration rejected, cause being {}: {}",
								iaxFrame.<CauseCode>getIEOpt(InformationElementType.CAUSECODE).map(CauseCode::getCause).orElse(CauseCode.Cause.SERVICE_OR_OPTION_NOT_AVAILABLE),
								iaxFrame.<Cause>getIEOpt(InformationElementType.CAUSE).map(Cause::getCause).orElse("null"));
						ack(iaxFrame);
						return;
					}
					default -> { }
				}
			super.handle(frame);
		}
		
		@Override
		public void retry() {
			if (IaxConstants.REGISTRATION_REJECTED_MAX_RETRY_COUNT != -1 && this.client.timesRejected.getAndIncrement() >= IaxConstants.REGISTRATION_REJECTED_MAX_RETRY_COUNT) {
				LOGGER.warn("unable to register with server after {} tries, disconnecting...", this.client.timesRejected.get());
				this.client.disconnect();
				return;
			}
			this.client.setState(new RegSent(this.client));
			this.client.resetLastRegisteredTimestamp();
			this.client.resetISeqNo();
			this.client.resetOSeqNo();
			this.client.send(this.client.frameBuilder.fork()
					.dstCallNumber((short) 0)
					.timestamp(0)
					.iaxSubclass(IaxFrame.Subclass.REGREQ)
					.ie(username(this.client.getConfig().username()))
					.ie(refresh(this.client.getServerRefresh()))
					.iax(), true);
		}
		
		@Override
		@SuppressWarnings("unused")
		public long getRetryInterval() {
			return IaxConstants.REGISTRATION_REJECTED_MAX_RETRY_COUNT > 0 ? IaxConstants.REGISTRATION_REJECTED_RETRY_INTERVAL * 1000 : 0;
		}
	}
	
	static final class Releasing extends ClientState {
		Releasing(IaxClient client) { super(client); }
		
		@Override
		public void handle(FullFrame frame) {
			if (frame instanceof IaxFrame iaxFrame)
				switch (iaxFrame.getIAXSubclass()) {
					case ACK -> {
						if (iaxFrame.getTimestamp() == this.client.getRegRelTimestamp()) {
							LOGGER.info("client {} disconnected (ACK)", this.client);
							this.client.markResponded(iaxFrame.getISeqNo());
							this.client.setState(new Unregistered(this.client));
							this.client.resetISeqNo();
							this.client.resetOSeqNo();
						}
						return;
					}
					case REGACK -> {
						LOGGER.info("client {} disconnected (REGACK)", this.client);
						this.client.markResponded(iaxFrame.getISeqNo());
						this.client.setState(new Unregistered(this.client));
						this.client.send(this.client.frameBuilder.fork()
								.dstCallNumber(iaxFrame.getSrcCallNumber())
								.oSeqNo(frame.getISeqNo())
								.iSeqNo((byte) (frame.getOSeqNo() + 1))
								.timestamp(frame.getTimestamp())
								.iaxSubclass(IaxFrame.Subclass.ACK)
								.iax());
						this.client.resetISeqNo();
						this.client.resetOSeqNo();
						return;
					}
					case REGAUTH -> {
						this.client.markResponded(iaxFrame.getISeqNo());
						Frame.Builder builder = this.client.frameBuilder.fork()
								.dstCallNumber(iaxFrame.getSrcCallNumber())
								.timestamp(this.client.getRegRelTimestamp())
								.iaxSubclass(IaxFrame.Subclass.REGREL)
								.ie(username(this.client.getConfig().username()))
								.ie(refresh(IaxConstants.CLIENT_REGISTRATION_REFRESH));
						iaxFrame.<Challenge>getIEOpt(InformationElementType.CHALLENGE)
								.map(Challenge::getChallenge)
								.map(c -> md5Result(c, this.client.getConfig().password()))
								.ifPresentOrElse(builder::ie, () -> LOGGER.warn("did not receive a challenge from server, unable to send authentication response"));
						this.client.send(builder.iax(), true);
						return;
					}
					default -> { }
				}
			
			super.handle(frame);
		}
	}
}
