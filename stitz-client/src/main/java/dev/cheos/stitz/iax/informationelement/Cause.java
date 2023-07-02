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

package dev.cheos.stitz.iax.informationelement;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.google.common.base.MoreObjects;

/**
 * Implements the CAUSE information element. From RFC 5456:
 * 
 * The purpose of the CAUSE information element is to indicate the
 * reason an event occurred.  It carries a description of the CAUSE of
 * the event as UTF-8-encoded data.  Notification of the event itself is
 * handled at the message level.
 * 
 * The CAUSE information element SHOULD be sent with IAX HANGUP, REJECT,
 * REGREJ, and TXREJ messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x16     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :  UTF-8-encoded CAUSE of event :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Cause extends InformationElement {
	private final String cause;
	private final byte[] causeRaw;
	
	protected Cause(String cause) {
		this.cause = cause;
		this.causeRaw = cause.getBytes(StandardCharsets.UTF_8);
	}
	
	protected Cause(ByteBuffer buf) {
		super(buf);
		this.causeRaw = new byte[buf.get()];
		buf.get(this.causeRaw);
		this.cause = new String(this.causeRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CAUSE;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.causeRaw.length;
	}
	
	public String getCause() {
		return this.cause;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.causeRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("cause", this.cause)
				.toString();
	}
}
