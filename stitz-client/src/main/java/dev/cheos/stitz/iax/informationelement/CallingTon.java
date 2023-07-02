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
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * Implements the CALLINGTON information element. From RFC 5456:
 * 
 * The purpose of the CALLINGTON information element is to indicate the
 * calling type of number of a caller, according to ITU-T Recommendation
 * Q.931 specifications.  The data field is 1 octet long and contains
 * data from the table below.
 * 
 * The CALLINGTON information element MUST be sent with IAX NEW
 * messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x27     |      0x01     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Calling TON  |
 * +-+-+-+-+-+-+-+-+
 *
 * The following table lists valid calling type of number values from
 * ITU-T Recommendation Q.931:
 * 
 * 	  +-------+-------------------------+
 * 	  | VALUE | DESCRIPTION             |
 * 	  +-------+-------------------------+
 * 	  | 0x00  | Unknown                 |
 * 	  |       |                         |
 * 	  | 0x10  | International Number    |
 * 	  |       |                         |
 * 	  | 0x20  | National Number         |
 * 	  |       |                         |
 * 	  | 0x30  | Network Specific Number |
 * 	  |       |                         |
 * 	  | 0x40  | Subscriber Number       |
 * 	  |       |                         |
 * 	  | 0x60  | Abbreviated Number      |
 * 	  |       |                         |
 * 	  | 0x70  | Reserved for extension  |
 * 	  +-------+-------------------------+
 */
public class CallingTon extends InformationElement {
	private final TonValue ton;
	
	protected CallingTon(TonValue ton) {
		this.ton = ton;
	}
	
	protected CallingTon(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 1) throw new IllegalArgumentException("invalid size for CallingTon InformationElement");
		this.ton = TonValue.byId(buf.get());
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLINGTON;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 1;
	}
	
	public TonValue getTon() {
		return this.ton;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.ton.getId());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("ton", this.ton)
				.toString();
	}
	
	
	public static enum TonValue {
		UNKNOWN(0x00), INTERNATIONAL_NUMBER(0x10),
		NATIONAL_NUMBER(0x20), NETWORK_SPECIFIC_NUMBER(0x30),
		SUBSCRIBER_NUMBER(0x40), ABBREVIATED_NUMBER(0x60),
		RESERVED_FOR_EXTENSION(0x70);
		
		private static final Map<Byte, TonValue> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(TonValue::getId, Function.identity()));
		
		private final byte id;
		
		private TonValue(int id) {
			this.id = (byte) id;
		}
		
		public byte getId() {
			return this.id;
		}
		
		public static TonValue byId(byte id) {
			return TYPES.getOrDefault(id, UNKNOWN);
		}
	}
}
