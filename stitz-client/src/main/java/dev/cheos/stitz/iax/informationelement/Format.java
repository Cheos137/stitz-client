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

import dev.cheos.stitz.iax.frame.MediaFrame;

/**
 * Implements the FORMAT information element. From RFC 5456:
 * 
 * The purpose of the FORMAT information element is to indicate a single
 * preferred media CODEC.  When sent with a NEW message, the indicated
 * CODEC is the desired CODEC an IAX peer wishes to use for a call.
 * When sent with an ACCEPT message, it indicates the actual CODEC that
 * has been selected for the call.  Its data is represented in a 4-octet
 * bitmask according to Section 8.7.  Only one CODEC MUST be specified
 * in the FORMAT information element.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x09     |      0x04     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   FORMAT according to Media   |
 * | Format Subclass Values Table  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Format extends InformationElement {
	private final MediaFrame.Format format;
	
	protected Format(MediaFrame.Format format) {
		this.format = format;
	}
	
	protected Format(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 4) throw new IllegalArgumentException("invalid size for Format InformationElement");
		this.format = MediaFrame.Format.byId(buf.getInt());
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.FORMAT;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 4;
	}
	
	public MediaFrame.Format getFormat() {
		return this.format;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putInt(this.format.getId());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("format", this.format)
				.toString();
	}
}
