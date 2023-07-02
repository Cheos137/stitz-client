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
 * Implements the CALLING ANI information element. From RFC 5456:
 * 
 * The purpose of the CALLING ANI information element is to indicate the
 * calling number ANI (Automatic Number Identification) for billing.  It
 * carries UTF-8-encoded data.
 * 
 * The CALLING ANI information element MAY be sent with an IAX NEW
 * message, but it is not required.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x03     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * : UTF-8-encoded CALLING ANI     :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class CallingAni extends InformationElement {
	private final String callingAni;
	private final byte[] callingAniRaw;
	
	protected CallingAni(String callingAni) {
		this.callingAni = callingAni;
		this.callingAniRaw = callingAni.getBytes(StandardCharsets.UTF_8);
	}
	
	protected CallingAni(ByteBuffer buf) {
		super(buf);
		this.callingAniRaw = new byte[buf.get()];
		buf.get(this.callingAniRaw);
		this.callingAni = new String(this.callingAniRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLING_ANI;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.callingAniRaw.length;
	}
	
	public String getCallingAni() {
		return this.callingAni;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.callingAniRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("callingAni", this.callingAni)
				.toString();
	}
}
