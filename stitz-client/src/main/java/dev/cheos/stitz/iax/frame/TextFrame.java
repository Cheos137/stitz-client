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
import java.nio.charset.StandardCharsets;

import com.google.common.base.MoreObjects;

/**
 * The frame carries a non-control text message in UTF-8 [RFC3629] format. All
 * text frames have a subclass of 0.
 */
public class TextFrame extends FullFrame {
	protected final String content;
	
	protected TextFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			String content) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.TEXT, 0, content.getBytes(StandardCharsets.UTF_8));
		this.content = content;
	}
	
	protected TextFrame(ByteBuffer buf) {
		super(buf, FullFrameType.TEXT);
		this.content = new String(getData(), StandardCharsets.UTF_8);
	}
	
	protected TextFrame(FullFrame frame) {
		super(frame, FullFrameType.TEXT);
		this.content = new String(getData(), StandardCharsets.UTF_8);
	}
	
	public String getContent() {
		return this.content;
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
				.add("content", this.content)
				.toString();
	}
}
