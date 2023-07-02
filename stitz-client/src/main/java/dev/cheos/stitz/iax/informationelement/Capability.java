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
import java.util.Arrays;

import com.google.common.base.MoreObjects;

import dev.cheos.stitz.iax.frame.MediaFrame;

/**
 * Implements the CAPABILITY information element. From RFC 5456:
 * 
 * The purpose of the CAPABILITY information element is to indicate the
 * media CODEC capabilities of an IAX peer.  Its data is represented in
 * a 4-octet bitmask according to Section 8.7.  Multiple CODECs MAY be
 * specified by logically OR'ing them into the CAPABILITY information
 * element.
 * 
 * The CAPABILITY information element is sent with IAX NEW messages if
 * appropriate for the CODEC negotiation method the peer is using.
 * 
 *                      1
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x08     |      0x04     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | CAPABILITY according to Media |
 * | Format Subclass Values Table  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Capability extends InformationElement {
	private final MediaFrame.Format[] capabilities;
	private final int capabilitiesRaw;
	
	protected Capability(MediaFrame.Format... capabilities) {
		this.capabilities = capabilities;
		int tmp = 0;
		for (MediaFrame.Format format : capabilities)
			tmp |= format.getId();
		this.capabilitiesRaw = tmp;
	}
	
	protected Capability(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 4) throw new IllegalArgumentException("invalid size for Capability InformationElement");
		this.capabilitiesRaw = buf.getInt();
		this.capabilities = Arrays.stream(MediaFrame.Format.values())
				.filter(format -> (format.getId() & this.capabilitiesRaw) != 0)
				.toArray(MediaFrame.Format[]::new);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CAPABILITY;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 4;
	}
	
	public MediaFrame.Format[] getCapabilities() {
		return this.capabilities;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putInt(this.capabilitiesRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("capabilities", this.capabilities)
				.toString();
	}
}
