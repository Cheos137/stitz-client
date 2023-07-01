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
import java.nio.charset.StandardCharsets;

import com.google.common.base.MoreObjects;

/**
 * Implements the CHALLENGE information element. From RFC 5456:
 * 
 * The purpose of the CHALLENGE information element is to offer the MD5
 * or RSA challenge to be used for authentication.  It carries the
 * actual UTF-8-encoded challenge data.
 * 
 * The CHALLENGE information element MUST be sent with IAX AUTHREQ and
 * REGAUTH messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0f     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :  UTF-8-encoded Challenge Data :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Challenge extends InformationElement {
	private final String challenge;
	private final byte[] challengeRaw;
	
	protected Challenge(String challenge) {
		this.challenge = challenge;
		this.challengeRaw = challenge.getBytes(StandardCharsets.UTF_8);
	}
	
	protected Challenge(ByteBuffer buf) {
		super(buf);
		this.challengeRaw = new byte[buf.get()];
		buf.get(this.challengeRaw);
		this.challenge = new String(this.challengeRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CHALLENGE;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.challengeRaw.length;
	}
	
	public String getChallenge() {
		return this.challenge;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.challengeRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("challenge", this.challenge)
				.toString();
	}
}
