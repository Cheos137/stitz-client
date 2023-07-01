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
 * Implements the CAUSECODE information element. From RFC 5456:
 * 
 * The purpose of the CAUSECODE information element is to indicate the
 * reason a call was REJECTed or HANGUPed.  It derives from ITU-T
 * Recommendation Q.931.  The data field is one octet long and contains
 * an entry from the table below.
 * 
 * The CAUSECODE information element SHOULD be sent with IAX HANGUP,
 * REJECT, REGREJ, and TXREJ messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x2a     |      0x01     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Cause Code  |
 * +-+-+-+-+-+-+-+-+
 */
public class CauseCode extends InformationElement {
	private final Cause cause;
	
	protected CauseCode(Cause cause) {
		this.cause = cause;
	}
	
	protected CauseCode(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 1) throw new IllegalArgumentException("invalid size for CauseCode InformationElement");
		this.cause = Cause.byId(buf.get());
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CAUSECODE;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 1;
	}
	
	public Cause getCause() {
		return this.cause;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.cause.getId());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("cause", this.cause)
				.toString();
	}
	
	
	public static enum Cause {
		UNKNOWN_CODE(0),
		UNASSIGNED_NUMBER(1), NO_ROUTE_TO_NETWORK(2), NO_ROUTE_TO_DESTINATION(3),
		CHANNEL_UNACCEPTABLE(6), CALL_AWARDED_AND_DELIVERED(7), NORMAL_CALL_CLEARING(16),
		USER_BUSY(17), NO_USER_RESPONSE(18), NO_ANSWER(19),
		UNASSIGNED_20(20), CALL_REJECTED(21), NUMBER_CHANGED(22),
		DESTINATION_OUT_OF_ORDER(27), INVALID_NUMBER_FORMAT(28),
		FACILITY_REJECTED(29), RESPONSE_TO_STATUS_ENQUIRY(30),
		NORMAL_UNSPECIFIED(31), NO_CHANNEL_AVAILABLE(34), NETWORK_OUT_OF_ORDER(38),
		TEMPORAY_FAILURE(41), SWITCH_CONGESTION(42), ACCESS_INFORMATION_DISCARDED(43),
		REQUESTED_CHANNEL_NOT_AVAILABLE(44), PREEMPTED(45), RESOURCE_UNAVAILABLE(47),
		FACILITY_NOT_SUBSCRIBED(50), OUTGOING_CALL_BARRED(52), INCOMING_CALL_BARRED(54),
		BEARER_CAPABILITY_NOT_AUTHORIZED(57), BEARER_CAPABILITY_NOT_AVAILABLE(58),
		SERVICE_OR_OPTION_NOT_AVAILABLE(63), BEARER_CAPABILITY_NOT_IMPLEMENTED(65),
		CHANNEL_TYPE_NOT_IMPLEMENTED(66), FACILITY_NOT_IMPLEMENTED(69),
		ONLY_RESTRICTED_BEARER_CAPABILITY_AVAILABLE(70), SERVICE_NOT_AVAILABLE(79),
		INVALID_CALL_REFERENCE(81), IDENTIFIED_CHANNEL_NOT_EXISTENT(82), SUSPENDED_CALL_EXISTS(83),
		CALL_IDENTITY_IN_USE(84), NO_CALL_SUSPENDED(85), CALL_CLEARED(86),
		INCOMPATIBLE_DESTINATION(88), INVALID_TRANSIT_NETWORK_SELECTION(91), INVALID_MESSAGE(95),
		MANDATORY_INFORMATION_ELEMENT_MISSING(96), MESSAGE_TYPE_NONEXISTENT(97),
		MESSAGE_NOT_COMPATIBLE(98), INFORMATION_ELEMENT_NONEXISTENT(99),
		INVALID_INFORMATION_ELEMENT_CONTENTS(100), MESSAGE_NOT_COMPATIBLE_WITH_CALLSTATE(101),
		RECOVERY_ON_TIMER_EXPIRATION(102), MANDATORY_INFORMATION_ELEMENT_LENGTH_ERROR(103),
		PROTOCOL_ERROR(111), INTERNETWORKING(127);
		
		private static final Map<Byte, Cause> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(Cause::getId, Function.identity()));
		
		private final byte id;
		
		private Cause(int id) {
			this.id = (byte) id;
		}
		
		public byte getId() {
			return this.id;
		}
		
		public static Cause byId(byte id) {
			return TYPES.getOrDefault(id, UNKNOWN_CODE);
		}
	}
}
