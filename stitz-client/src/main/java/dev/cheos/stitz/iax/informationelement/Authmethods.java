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
 * Implements the AUTHMETHODS information element. From RFC 5456:
 * 
 * The purpose of the AUTHMETHODS information element is to indicate the
 * authentication methods a peer accepts.  It is sent as a bitmask two
 * octets long.  The table below lists the valid authentication methods.
 * 
 * The AUTHMETHODS information element MUST be sent with IAX AUTHREQ and
 * REGAUTH messages.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x0e     |      0x02     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Valid Authentication Methods |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * The following table lists valid values for authentication:
 * 
 *                 +--------+--------------------------+
 *                 | METHOD | DESCRIPTION              |
 *                 +--------+--------------------------+
 *                 | 0x0001 | Reserved (was Plaintext) |
 *                 |        |                          |
 *                 | 0x0002 | MD5                      |
 *                 |        |                          |
 *                 | 0x0004 | RSA                      |
 *                 +--------+--------------------------+
 */
public class Authmethods extends InformationElement {
	private final Method[] authmethods;
	private final short authmethodsRaw;
	
	protected Authmethods(Method... authmethods) {
		this.authmethods = authmethods;
		short tmp = 0;
		for (Method method : authmethods)
			tmp |= method.getId();
		this.authmethodsRaw = tmp;
	}
	
	protected Authmethods(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 2) throw new IllegalArgumentException("invalid size for Authmethods InformationElement");
		this.authmethodsRaw = buf.getShort();
		this.authmethods = Arrays.stream(Method.values())
				.filter(format -> (format.getId() & this.authmethodsRaw) != 0)
				.toArray(Method[]::new);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.AUTHMETHODS;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2;
	}
	
	public Method[] getAuthmethods() {
		return this.authmethods;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort(this.authmethodsRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("authmethods", this.authmethods)
				.toString();
	}
	
	
	public static enum Method {
		UNKNOWN(0),
		RESERVED(0x0001),
		MD5(0x0002),
		RSA(0x0004);
		
		private static final Map<Short, Method> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(Method::getId, Function.identity()));
		
		private final short id;
		
		private Method(int id) {
			this.id = (short) id;
		}
		
		public short getId() {
			return this.id;
		}
		
		public static Method byId(short id) {
			return TYPES.getOrDefault(id, UNKNOWN);
		}
	}
}
