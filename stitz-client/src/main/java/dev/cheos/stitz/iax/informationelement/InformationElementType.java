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

import com.google.common.collect.ImmutableMap;

public enum InformationElementType implements Function<ByteBuffer, InformationElement> {
	UNKNOWN(-1),
	/**
	 * Number/extension being called
	 */
	CALLED_NUMBER(CalledNumber::new),
	/**
	 * Calling number
	 */
	CALLING_NUMBER(CallingNumber::new),
	/**
	 * Calling number ANI for billing
	 */
	CALLING_ANI(CallingAni::new),
	/**
	 * Name of caller
	 */
	CALLING_NAME(CallingName::new),
	/**
	 * Context for number
	 */
	CALLED_CONTEXT(CalledContext::new),
	/**
	 * Username (peer or user) for authentication
	 */
	USERNAME(Username::new),
	/**
	 * Password for authentication, unsupported
	 */
	PASSWORD,
	/**
	 * Actual CODEC capability
	 */
	CAPABILITY(Capability::new),
	/**
	 * Desired CODEC format
	 */
	FORMAT(Format::new),
	/**
	 * Desired language
	 */
	LANGUAGE(Language::new),
	/**
	 * Protocol version
	 */
	VERSION(Version::new),
	/**
	 * CPE ADSI capability
	 */
	ADSICPE(Adsicpe::new),
	/**
	 * Originally dialed DNID
	 */
	DNID(DNID::new),
	/**
	 * Authentication method(s)
	 */
	AUTHMETHODS(Authmethods::new),
	/**
	 * Challenge data for MD5/RSA
	 */
	CHALLENGE(Challenge::new),
	/**
	 * MD5 challenge result
	 */
	MD5_RESULT(MD5Result::new),
	/**
	 * RSA challenge result, unsupported
	 */
	RSA_RESULT,
	/**
	 * Apparent address of peer
	 */
	APPARENT_ADDR(ApparentAddr::new),
	/**
	 * When to refresh registration
	 */
	REFRESH(Refresh::new),
	/**
	 * Dialplan status, unsupported
	 */
	DPSTATUS,
	/**
	 * Call number of peer, unsupported
	 */
	CALLNO,
	/**
	 * Cause
	 */
	CAUSE(Cause::new),
	/**
	 * Unknown IAX command
	 */
	IAX_UNKNOWN(IaxUnknown::new),
	/**
	 * How many messages waiting, unsupported
	 */
	MSGCOUNT,
	/**
	 * Request auto-answering, unsupported
	 */
	AUTOANSWER,
	/**
	 * Request musiconhold with QUELCH, unsupported
	 */
	MUSICONHOLD,
	/**
	 * Transfer Request Identifier, unsupported
	 */
	TRANSFERID,
	/**
	 * Referring DNIS, unsupported
	 */
	RDNIS,
	/**
	 * Date/Time
	 */
	DATETIME(0x1f, Datetime::new),
	/**
	 * Calling presentation
	 */
	CALLINGPRES(0x26, CallingPres::new),
	/**
	 * Calling type of number
	 */
	CALLINGTON(0x27, CallingTon::new),
	/**
	 * Calling transit network select, unsupported
	 */
	CALLINGTNS(0x28),
	/**
	 * Supported sampling rates
	 */
	SAMPLINGRATE(0x29, SamplingRate::new),
	/**
	 * Hangup cause
	 */
	CAUSECODE(0x2a, CauseCode::new),
	/**
	 * Encryption format, unsupported
	 */
	ENCRYPTION(0x2b),
	/**
	 * Reserved for future Use, unsupported
	 */
	ENCKEY(0x2c),
	/**
	 * CODEC Negotiation, unsupported (use CAPABILITY & FORMAT instead)
	 */
	CODEC_PREFS(0x2d),
	/**
	 * Received jitter, as per RFC 3550
	 */
	RR_JITTER(0x2e, RrJitter::new),
	/**
	 * Received loss, as per RFC 3550
	 */
	RR_LOSS(0x2f, RrLoss::new),
	/**
	 * Received frames, as per RFC 3550
	 */
	RR_PKTS(0x30, RrPkts::new),
	/**
	 * Max playout delay for received frames in ms
	 */
	RR_DELAY(0x31, RrDelay::new),
	/**
	 * Dropped frames (presumably by jitter buffer)
	 */
	RR_DROPPED(0x32, RrDropped::new),
	/**
	 * Frames received Out of Order
	 */
	RR_OOO(0x33, RrOoo::new),
	/**
	 * OSP Token Block, unsupported
	 */
	OSPTOKEN(0x34);
	
	private static final Map<Byte, InformationElementType> TYPES = Arrays.stream(values())
			.collect(ImmutableMap.toImmutableMap(InformationElementType::getId, Function.identity()));
	
	private final byte id;
	private final Function<ByteBuffer, InformationElement> factory;
	
	InformationElementType() {
		this.id = (byte) ordinal();
		this.factory = null;
	}
	
	InformationElementType(int id) {
		this(id, null);
	}
	
	InformationElementType(Function<ByteBuffer, InformationElement> factory) {
		this.id = (byte) ordinal();
		this.factory = factory;
	}
	
	InformationElementType(int id, Function<ByteBuffer, InformationElement> factory) {
		this.id = (byte) id;
		this.factory = factory;
	}
	
	public byte getId() {
		return this.id;
	}
	
	@Override
	public InformationElement apply(ByteBuffer buf) {
		return this.factory == null ? null : this.factory.apply(buf);
	}
	
	public static InformationElementType byId(byte id) {
		return TYPES.getOrDefault(id, UNKNOWN);
	}
}
