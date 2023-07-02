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
 * Implements the REFRESH information element. From RFC 5456:
 * 
 * The purpose of the REFRESH information element is to indicate the
 * number of seconds before an event expires.  Its data field is 2
 * octets long.
 * 
 * The REFRESH information element is used with IAX REGREQ, REGACK, and
 * DPREP messages.  When sent with a REGREQ, it is a request that the
 * peer maintaining the registration set the timeout to REFRESH seconds.
 * When sent with a DPREP or REGACK, it is informational and tells a
 * remote peer when the local peer will no longer consider the event
 * valid.  The REFRESH sent with a DPREP tells a peer how long it SHOULD
 * store the received dialplan response.
 * 
 * If the REFRESH information element is not received with a DPREP, the
 * expiration of the cache data is assumed to be 10 minutes.  If the
 * REFRESH information element is not received with a REGACK,
 * registration expiration is assumed to occur after 60 seconds.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x13     |      0x02     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  2 octets specifying refresh  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Refresh extends InformationElement {
	private final short refresh;
	
	protected Refresh(short refresh) {
		this.refresh = refresh;
	}
	
	protected Refresh(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 2) throw new IllegalArgumentException("invalid size for Refresh InformationElement");
		this.refresh = buf.getShort();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.REFRESH;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2;
	}
	
	public short getRefresh() {
		return this.refresh;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort(this.refresh);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("refresh", this.refresh)
				.toString();
	}
}
