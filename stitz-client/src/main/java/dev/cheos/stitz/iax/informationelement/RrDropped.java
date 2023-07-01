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

package dev.cheos.stitz.iax.informationelement;

import java.nio.ByteBuffer;

import com.google.common.base.MoreObjects;

/**
 * Implements the RR DROPPED information element. From RFC 5456:
 * 
 * The purpose of the RR DROPPED information element is to indicate the
 * total number of dropped frames for a call, per [RFC3550].  The data
 * field is 4 octets long and carries the number of frames dropped.
 * 
 * The RR DROPPED information element MAY be sent with IAX PONG
 * messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x32     |      0x04     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Total Frames Dropped     |
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class RrDropped extends InformationElement {
	private final int frames;
	
	protected RrDropped(int frames) {
		this.frames = frames;
	}
	
	protected RrDropped(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 4) throw new IllegalArgumentException("invalid size for RrDropped InformationElement");
		this.frames = buf.getInt();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.RR_DROPPED;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 4;
	}
	
	public int getDropped() {
		return this.frames;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putInt(this.frames);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("frames", this.frames)
				.toString();
	}
}
