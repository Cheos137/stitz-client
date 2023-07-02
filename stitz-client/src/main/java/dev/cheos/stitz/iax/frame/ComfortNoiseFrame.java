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

import com.google.common.base.MoreObjects;

/**
 * The frame carries comfort noise. The subclass is the level of comfort noise
 * in -dBov.
 */
public class ComfortNoiseFrame extends FullFrame {
	protected ComfortNoiseFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			int level) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.COMFORTNOISE, level, NO_DATA);
	}
	
	protected ComfortNoiseFrame(ByteBuffer buf) {
		super(buf, FullFrameType.COMFORTNOISE);
	}
	
	protected ComfortNoiseFrame(FullFrame frame) {
		super(frame, FullFrameType.COMFORTNOISE);
	}
	
	public int getLevel() {
		return getSubclass();
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
				.add("level", this.subclass)
				.toString();
	}
}
