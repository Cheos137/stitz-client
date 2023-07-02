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
 * Implements the RR LOSS information element. From RFC 5456:
 * 
 * The purpose of the RR LOSS information element is to indicate the
 * number of lost frames on a call, per [RFC3550].  The data field is 4
 * octets long and carries the percentage of frames lost in the first
 * octet, and the count of lost frames in the next 3 octets.
 * 
 * The RR LOSS information element MAY be sent with IAX PONG messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x2f     |      0x04     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Loss Percent |               |
 * +-+-+-+-+-+-+-+-+  Loss Count   |
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class RrLoss extends InformationElement {
	private final byte percentage;
	private final int frames;
	
	protected RrLoss(byte percentage, int frames) {
		this.percentage = percentage;
		this.frames = frames & 0xFFFFFF;
	}
	
	protected RrLoss(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 4) throw new IllegalArgumentException("invalid size for RrLoss InformationElement");
		int tmp = buf.getInt();
		this.percentage = (byte) ((tmp >> 24) & 0xFF);
		this.frames = tmp & 0xFFFFFF;
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.RR_LOSS;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 4;
	}
	
	public byte getPercentage() {
		return this.percentage;
	}
	
	public int getFrames() {
		return this.frames;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putInt((this.percentage << 24) | this.frames);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("percentage", this.percentage)
				.add("frames", this.frames)
				.toString();
	}
}
