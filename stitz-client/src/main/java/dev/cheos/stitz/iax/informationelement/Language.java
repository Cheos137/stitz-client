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
 * Implements the LANGUAGE information element. From RFC 5456:
 * 
 * The purpose of the LANGUAGE information element is to indicate the
 * language in which the transmitting peer would like the remote peer to
 * send signaling information.  It carries UTF-8-encoded data and tags
 * should be selected per [RFC5646] and [RFC4647].
 * 
 * The LANGUAGE information element MAY be sent with an IAX NEW message.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0a     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :     UTF-8-encoded LANGUAGE    :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Language extends InformationElement {
	private final String language;
	private final byte[] languageRaw;
	
	protected Language(String language) {
		this.language = language;
		this.languageRaw = language.getBytes(StandardCharsets.UTF_8);
	}
	
	protected Language(ByteBuffer buf) {
		super(buf);
		this.languageRaw = new byte[buf.get()];
		buf.get(this.languageRaw);
		this.language = new String(this.languageRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.LANGUAGE;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.languageRaw.length;
	}
	
	public String getLanguage() {
		return this.language;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.languageRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("language", this.language)
				.toString();
	}
}
