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
 * Implements the CALLED NUMBER information element. From RFC 5456:
 * 
 * The purpose of the CALLED NUMBER information element is to indicate
 * the number or extension being called.  It carries UTF-8-encoded data.
 * The CALLED NUMBER information element MUST use UTF-8 encoding and not
 * numeric data because destinations are not limited to E.164 numbers
 * ([E164]), national numbers, or even digits.  It is possible for a
 * number or extension to include non-numeric characters.  The CALLED
 * NUMBER IE MAY contain a SIP URI, [RFC3261] or a URI in any other
 * format.  The ability to serve a CALLED NUMBER is server dependent.
 * 
 * The CALLED NUMBER information element is generally sent with IAX NEW,
 * DPREQ, DPREP, DIAL, and TRANSFER messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x01     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :  UTF-8-encoded CALLED NUMBER  :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class CalledNumber extends InformationElement {
	private final String calledNumber;
	private final byte[] calledNumberRaw;
	
	protected CalledNumber(String calledNumber) {
		this.calledNumber = calledNumber;
		this.calledNumberRaw = calledNumber.getBytes(StandardCharsets.UTF_8);
	}
	
	protected CalledNumber(ByteBuffer buf) {
		super(buf);
		this.calledNumberRaw = new byte[buf.get()];
		buf.get(this.calledNumberRaw);
		this.calledNumber = new String(this.calledNumberRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLED_NUMBER;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.calledNumberRaw.length;
	}
	
	public String getCalledNumber() {
		return this.calledNumber;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.calledNumberRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("calledNumber", this.calledNumber)
				.toString();
	}
}
