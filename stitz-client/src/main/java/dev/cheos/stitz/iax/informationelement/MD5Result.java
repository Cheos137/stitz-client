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
import java.security.MessageDigest;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;

/**
 * Implements the MD5 RESULT information element. From RFC 5456:
 * 
 * The purpose of the MD5 RESULT information element is to offer an MD5
 * response to an authentication CHALLENGE.  It carries the UTF-8-
 * encoded challenge result.  The MD5 Result value is computed by taking
 * the MD5 [RFC1321] digest of the challenge string and the password
 * string.
 * 
 * The MD5 RESULT information element MAY be sent with IAX AUTHREP and
 * REGREQ messages if an AUTHREQ or REGAUTH and appropriate CHALLENGE
 * has been received.  This information element MUST NOT be sent except
 * in response to a CHALLENGE.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     0x10      |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :    UTF-8-encoded MD5 Result   :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class MD5Result extends InformationElement {
	private static final BaseEncoding BASE16 = BaseEncoding.base16().lowerCase();
	private static final MessageDigest MD5;
	static {
		try {
			MD5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			throw new RuntimeException("failed to get MD5 instance");
		}
	}
	
	private final String md5Result;
	private final byte[] md5ResultRaw;
	
	protected MD5Result(String challenge, String password) {
		this.md5Result = md5(challenge, password);
		this.md5ResultRaw = this.md5Result.getBytes(StandardCharsets.UTF_8);
	}
	
	protected MD5Result(ByteBuffer buf) {
		super(buf);
		this.md5ResultRaw = new byte[buf.get()];
		buf.get(this.md5ResultRaw);
		this.md5Result = new String(this.md5ResultRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.MD5_RESULT;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.md5ResultRaw.length;
	}
	
	public String getMD5Result() {
		return this.md5Result;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.md5ResultRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("MD5Result", this.md5Result)
				.toString();
	}
	
	private static String md5(String challenge, String password) {
		byte[] cb = challenge.getBytes(StandardCharsets.UTF_8);
		byte[] pb = password.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buf = ByteBuffer.allocate(cb.length + pb.length);
		buf.put(cb);
		buf.put(pb);
		buf.flip();
		MD5.update(buf);
		return BASE16.encode(MD5.digest());
	}
}
