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
 * Implements the ADSICPE information element. From RFC 5456:
 * 
 * The purpose of the ADSICPE information element is to indicate the CPE
 * (Customer Premises Equipment) ADSI (Analog Display Services
 * Interface) capability. The data field of the ADSICPE information
 * element is 2 octets long.
 * 
 * The ADSICPE information element MAY be sent with an IAX NEW message.
 * 
 *  0               1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0c     |      0x02     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       ADSICPE Capability      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Adsicpe extends InformationElement {
	private final short adsicpe;
	
	protected Adsicpe(short adsicpe) {
		this.adsicpe = adsicpe;
	}
	
	protected Adsicpe(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 2) throw new IllegalArgumentException("invalid size for Adsicpe InformationElement");
		this.adsicpe = buf.getShort();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.ADSICPE;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2;
	}
	
	public short getAdsicpe() {
		return this.adsicpe;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort(this.adsicpe);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("adsicpe", this.adsicpe)
				.toString();
	}
}
