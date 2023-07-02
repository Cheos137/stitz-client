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
 * Implements the CALLING NAME information element. From RFC 5456:
 * 
 * The purpose of the CALLING NAME information element is to indicate
 * the calling name of the transmitting peer.  It carries UTF-8-encoded
 * data.
 * 
 * The CALLING NAME information element is usually sent with IAX NEW
 * messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x04     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :  UTF-8-encoded CALLING NAME   :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class CallingName extends InformationElement {
	private final String callingName;
	private final byte[] callingNameRaw;
	
	protected CallingName(String callingName) {
		this.callingName = callingName;
		this.callingNameRaw = callingName.getBytes(StandardCharsets.UTF_8);
	}
	
	protected CallingName(ByteBuffer buf) {
		super(buf);
		this.callingNameRaw = new byte[buf.get()];
		buf.get(this.callingNameRaw);
		this.callingName = new String(this.callingNameRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLING_NAME;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.callingNameRaw.length;
	}
	
	public String getCallingName() {
		return this.callingName;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.callingNameRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("callingName", this.callingName)
				.toString();
	}
}
