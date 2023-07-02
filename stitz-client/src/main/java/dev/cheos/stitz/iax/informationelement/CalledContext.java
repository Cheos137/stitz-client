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
 * Implements the CALLED CONTEXT information element. From RFC 5456:
 * 
 * The purpose of the CALLED CONTEXT information element is to indicate
 * the context (or partition) of the remote peer's dialplan that the
 * CALLED NUMBER is interpreted.  It carries UTF-8-encoded data.
 * 
 * The CALLED CONTEXT information element MAY be sent with IAX NEW or
 * TRANSFER messages, though it is not required.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x05     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * : UTF-8-encoded CALLED CONTEXT  :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class CalledContext extends InformationElement {
	private final String calledContext;
	private final byte[] calledContextRaw;
	
	protected CalledContext(String calledContext) {
		this.calledContext = calledContext;
		this.calledContextRaw = calledContext.getBytes(StandardCharsets.UTF_8);
	}
	
	protected CalledContext(ByteBuffer buf) {
		super(buf);
		this.calledContextRaw = new byte[buf.get()];
		buf.get(this.calledContextRaw);
		this.calledContext = new String(this.calledContextRaw, StandardCharsets.UTF_8);
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.CALLED_CONTEXT;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.calledContextRaw.length;
	}
	
	public String getCalledContext() {
		return this.calledContext;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.put(this.calledContextRaw);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("calledContext", this.calledContext)
				.toString();
	}
}
