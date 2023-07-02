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
 * Implements the DNID information element. From RFC 5456:
 * 
 * The purpose of the DNID information element is to indicate the Dialed
 * Number ID, which may differ from the 'called number'.  It carries
 * UTF-8-encoded data.
 * 
 * The DNID information element MAY be sent with an IAX NEW message.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0d     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :    UTF-8-encoded DNID Data    :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class DNID extends InformationElement {
	private final String dnid;
	private final byte[] dnidRaw;
	
	protected DNID(String dnid) {
		this.dnid = dnid;
		this.dnidRaw = dnid.getBytes(StandardCharsets.UTF_8);
	}
	
	protected DNID(ByteBuffer buf) {
		super(buf);
		this.dnidRaw = new byte[buf.get()];
		buf.get(this.dnidRaw);
		this.dnid = new String(this.dnidRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.DNID;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.dnidRaw.length;
	}
	
	public String getDNID() {
		return this.dnid;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.dnidRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("DNID", this.dnid)
				.toString();
	}
}
