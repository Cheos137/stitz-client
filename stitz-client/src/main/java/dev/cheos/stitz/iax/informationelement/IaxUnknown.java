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
 * Implements the IAX UNKNOWN information element. From RFC 5456:
 * 
 * The purpose of the IAX UNKNOWN information element is to indicate
 * that a received IAX command was unknown or unrecognized.  The 1-octet
 * data field contains the subclass of the received frame that was
 * unrecognized.
 * 
 * The IAX UNKNOWN information element MUST be sent with IAX UNSUPPORT
 * messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x17     |      0x01     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Rec'd Subclass|
 * +-+-+-+-+-+-+-+-+
 */
public class IaxUnknown extends InformationElement {
	private final byte subclass;
	
	protected IaxUnknown(byte subclass) {
		this.subclass = subclass;
	}
	
	protected IaxUnknown(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 1) throw new IllegalArgumentException("invalid size for IaxUnknown InformationElement");
		this.subclass = buf.get();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.IAX_UNKNOWN;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 1;
	}
	
	public short getSubclass() {
		return this.subclass;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.subclass);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("subclass", this.subclass)
				.toString();
	}
}
