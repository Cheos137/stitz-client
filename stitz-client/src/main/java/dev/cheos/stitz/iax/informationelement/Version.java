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

/**
 * Implements the VERSION information element. From RFC 5456:
 * 
 * The purpose of the VERSION information element is to indicate the
 * protocol version the peer is using.  Peers at each end of a call MUST
 * use the same protocol version.  Currently, the only supported version
 * is 2.  The data field of the VERSION information element is 2 octets
 * long.
 * 
 * 
 * The CAPABILITY information element is sent with IAX NEW messages if
 * appropriate for the CODEC negotiation method the peer is using.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0b     |      0x02     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            0x0002             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Version extends InformationElement { // no need to store version information as only IAX2 is supported anyways
	protected Version() { }
	
	protected Version(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 2) throw new IllegalArgumentException("invalid size for Version InformationElement");
		if (buf.getShort() != 2)  throw new IllegalArgumentException("unsupported IAX version");
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.VERSION;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort((short) 2);
	}
}
