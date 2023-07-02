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

import com.google.common.base.MoreObjects;

/**
 * Implements the RR DELAY information element. From RFC 5456:
 * 
 * The purpose of the RR DELAY information element is to indicate the
 * maximum playout delay for a call, per [RFC3550].  The data field is 2
 * octets long and specifies the number of milliseconds a frame may be
 * delayed before it MUST be discarded.
 * 
 * The RR DELAY information element MAY be sent with IAX PONG messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x31     |      0x02     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Maximum Playout Delay      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class RrDelay extends InformationElement {
	private final short delay;
	
	protected RrDelay(short delay) {
		this.delay = delay;
	}
	
	protected RrDelay(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 2) throw new IllegalArgumentException("invalid size for RrDelay InformationElement");
		this.delay = buf.getShort();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.RR_DELAY;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2;
	}
	
	public int getDelay() {
		return this.delay;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putInt(this.delay);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("delay", this.delay)
				.toString();
	}
}
