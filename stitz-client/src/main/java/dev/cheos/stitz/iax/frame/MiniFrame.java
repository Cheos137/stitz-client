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

package dev.cheos.stitz.iax.frame;

import java.nio.ByteBuffer;

import com.google.common.base.MoreObjects;

public class MiniFrame extends Frame {
	private final short timestamp;
	private final byte[] data;
	
	public MiniFrame(short srcCallNumber, int timestamp, byte[] data) {
		super(srcCallNumber);
		this.timestamp = (short) (timestamp & 0xFFFF);
		this.data = data;
	}
	
	public MiniFrame(ByteBuffer buf) {
		super(buf);
		this.timestamp = buf.getShort();
		this.data = new byte[buf.remaining()];
		buf.get(this.data);
	}
	
	public short getTimestamp() {
		return this.timestamp;
	}
	
	public byte[] getData() {
		return this.data;
	}

	@Override
	public final boolean isFullFrame() {
		return false;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 2 + this.data.length;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort(this.timestamp);
		buf.put(this.data);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("srcCallNumber", this.srcCallNumber)
				.add("timestamp", this.timestamp)
				.add("size", getSize())
				.add("data", this.data)
				.toString();
	}
}
