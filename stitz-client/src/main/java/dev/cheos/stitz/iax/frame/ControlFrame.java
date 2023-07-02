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

package dev.cheos.stitz.iax.frame;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * The frame carries session control data, i.e., it refers to control of a
 * device connected to an IAX endpoint.
 */
public class ControlFrame extends FullFrame {
	protected final Subclass cfSubclass;
	
	protected ControlFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			Subclass subclass) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.CONTROL, subclass.getId(), NO_DATA);
		this.cfSubclass = subclass;
	}
	
	protected ControlFrame(ByteBuffer buf) {
		super(buf, FullFrameType.CONTROL);
		this.cfSubclass = Subclass.byId(getSubclass());
	}
	
	protected ControlFrame(FullFrame frame) {
		super(frame, FullFrameType.CONTROL);
		this.cfSubclass = Subclass.byId(getSubclass());
	}
	
	public Subclass getCFSubclass() {
		return this.cfSubclass;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", this.type)
				.add("srcCallNumber", this.srcCallNumber)
				.add("dstCallNumber", this.dstCallNumber)
				.add("oSeq", this.oSeqNo)
				.add("iSeq", this.iSeqNo)
				.add("timestamp", this.timestamp)
				.add("size", getSize())
				.add("subclass", this.cfSubclass)
				.toString();
	}
	
	
	public enum Subclass {
		UNKNOWN(0x00),
		HANGUP(0x01),
		RINGING(0x03),
		ANSWER(0x04),
		BUSY(0x05),
		CONGESTION(0x08),
		FLASH_HOOK(0x09),
		OPTION(0x0b),
		KEY_RADIO(0x0c),
		UNKEY_RADIO(0x0d),
		PROGRESS(0x0e),
		PROCEEDING(0x0f),
		HOLD(0x10),
		UNHOLD(0x11);
		
		private static final Map<Integer, Subclass> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(Subclass::getId, Function.identity()));
		
		private final int id;
		
		Subclass(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Subclass byId(int id) {
			return TYPES.getOrDefault(id, UNKNOWN);
		}
	}
}
