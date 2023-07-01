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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * Implements the CALLINGPRES information element. From RFC 5456:
 * 
 * The purpose of the CALLINGPRES information element is to indicate the
 * calling presentation of a caller.  The data field is 1 octet long and
 * contains a value from the table below.
 * 
 * The CALLINGPRES information element MUST be sent with IAX NEW
 * messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x26     |      0x01     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Calling Pres. |
 * +-+-+-+-+-+-+-+-+
 *
 * The following table lists valid calling presentation values:
 * 
 * 	  +------+--------------------------------------+
 * 	  | FLAG | PRESENTATION                         |
 * 	  +------+--------------------------------------+
 * 	  | 0x00 | Allowed user/number not screened     |
 * 	  |      |                                      |
 * 	  | 0x01 | Allowed user/number passed screen    |
 * 	  |      |                                      |
 * 	  | 0x02 | Allowed user/number failed screen    |
 * 	  |      |                                      |
 * 	  | 0x03 | Allowed network number               |
 * 	  |      |                                      |
 * 	  | 0x20 | Prohibited user/number not screened  |
 * 	  |      |                                      |
 * 	  | 0x21 | Prohibited user/number passed screen |
 * 	  |      |                                      |
 * 	  | 0x22 | Prohibited user/number failed screen |
 * 	  |      |                                      |
 * 	  | 0x23 | Prohibited network number            |
 * 	  |      |                                      |
 * 	  | 0x43 | Number not available                 |
 * 	  +------+--------------------------------------+
 */
public class CallingPres extends InformationElement {
	private final PresentationValue presentation;
	
	protected CallingPres(PresentationValue presentation) {
		this.presentation = presentation;
	}
	
	protected CallingPres(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 1) throw new IllegalArgumentException("invalid size for CallingPres InformationElement");
		this.presentation = PresentationValue.byId(buf.get());
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLINGPRES;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 1;
	}
	
	public PresentationValue getPresentation() {
		return this.presentation;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.presentation.getId());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("presentation", this.presentation)
				.toString();
	}
	
	
	public static enum PresentationValue {
		ALLOWED_USER_NUMBER_NOT_SCREENED(0x00), ALLOWED_USER_NUMBER_PASSED_SCREEN(0x01),
		ALLOWED_USER_NUMBER_FAILED_SCREEN(0x02), ALLOWED_NETWORK_NUMBER(0x03),
		PROHIBITED_USER_NUMBER_NOT_SCREENED(0x20), PROHIBITED_USER_NUMBER_PASSED_SCREEN(0x21),
		PROHIBITED_USER_NUMBER_FAILED_SCREEN(0x22), PROHIBITED_NETWORK_NUMBER(0x23),
		NUMBER_NOT_AVAILABLE(0x43);
		
		private static final Map<Byte, PresentationValue> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(PresentationValue::getId, Function.identity()));
		
		private final byte id;
		
		private PresentationValue(int id) {
			this.id = (byte) id;
		}
		
		public byte getId() {
			return this.id;
		}
		
		public static PresentationValue byId(byte id) {
			return TYPES.getOrDefault(id, NUMBER_NOT_AVAILABLE);
		}
	}
}
