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
 * Implements the USERNAME information element. From RFC 5456:
 * 
 * The purpose of the USERNAME information element is to specify the
 * identity of the user participating in an IAX message exchange.  It
 * carries UTF-8-encoded data.
 * 
 * The USERNAME information element MAY be sent with IAX NEW, AUTHREQ,
 * REGREQ, REGAUTH, or REGACK messages, or any time a peer needs to
 * identify a user.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x06     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :     UTF-8-encoded USERNAME    :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class CallingNumber extends InformationElement {
	private final String callingNumber;
	private final byte[] callingNumberRaw;
	
	protected CallingNumber(String callingNumber) {
		this.callingNumber = callingNumber;
		this.callingNumberRaw = callingNumber.getBytes(StandardCharsets.UTF_8);
	}
	
	protected CallingNumber(ByteBuffer buf) {
		super(buf);
		this.callingNumberRaw = new byte[buf.get()];
		buf.get(this.callingNumberRaw);
		this.callingNumber = new String(this.callingNumberRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLING_NUMBER;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.callingNumberRaw.length;
	}
	
	public String getCallingNumber() {
		return this.callingNumber;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.callingNumberRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("callingNumber", this.callingNumber)
				.toString();
	}
}
