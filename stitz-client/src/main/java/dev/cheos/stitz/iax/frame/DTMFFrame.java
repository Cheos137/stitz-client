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
 * The frame carries a single digit of DTMF (Dual Tone Multi-Frequency). For
 * DTMF frames, the subclass is the actual DTMF digit carried by the frame.
 */
public class DTMFFrame extends FullFrame {
	protected DTMFFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			int digit) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.DTMF, digit, NO_DATA);
	}
	
	protected DTMFFrame(ByteBuffer buf) {
		super(buf, FullFrameType.DTMF);
	}
	
	protected DTMFFrame(FullFrame frame) {
		super(frame, FullFrameType.DTMF);
	}
	
	public int getDigit() {
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
				.add("digit", this.subclass)
				.toString();
	}
}
