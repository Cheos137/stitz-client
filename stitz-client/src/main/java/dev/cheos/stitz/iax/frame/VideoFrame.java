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

/**
 * The frame carries video data. The subclass specifies the video format of the
 * data.
 */
public class VideoFrame extends MediaFrame {
	protected VideoFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			Format format,
			byte[] data) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.VIDEO, format, data);
	}
	
	protected VideoFrame(ByteBuffer buf) {
		super(buf, FullFrameType.VIDEO);
	}
	
	protected VideoFrame(FullFrame frame) {
		super(frame, FullFrameType.VIDEO);
	}
	
	@Override
	protected boolean isValidFormat(Format format) {
		return format.getType() == Format.Type.VIDEO;
	}
}
