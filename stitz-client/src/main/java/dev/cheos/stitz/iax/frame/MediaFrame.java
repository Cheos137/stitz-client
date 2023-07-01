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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public abstract class MediaFrame extends FullFrame {
	protected final Format format;
	
	protected MediaFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			FullFrameType type,
			Format format,
			byte[] data) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, type, format.getId(), data);
		this.format = format;
		checkFormat();
	}
	
	protected MediaFrame(ByteBuffer buf, FullFrameType type) {
		super(buf, type);
		this.format = Format.byId(getSubclass());
		checkFormat();
	}
	
	protected MediaFrame(FullFrame frame, FullFrameType type) {
		super(frame, type);
		this.format = Format.byId(getSubclass());
		checkFormat();
	}
	
	public Format getFormat() {
		return this.format;
	}
	
	private void checkFormat() {
		if (!isValidFormat(this.format))
			throw new IllegalArgumentException("Invalid format " + this.format + " of type " + this.format.getType() + " for " + getClass().getSimpleName());
	}
	
	protected abstract boolean isValidFormat(Format format);
	
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
				.add("format", this.format)
				.add("data", this.data)
				.toString();
	}
	
	public enum Format {
		UNKNOWN(0, null),
		// AUDIO
		G7231(0x00000001, Type.AUDIO),
		GSM_FULL_RATE(0x00000002, Type.AUDIO),
		G711_MULAW(0x00000004, Type.AUDIO),
		G711_ALAW(0x00000008, Type.AUDIO),
		G726(0x00000010, Type.AUDIO),
		ADPCM(0x00000020, Type.AUDIO),
		LE_16_BIT_LINEAR(0x00000040, Type.AUDIO),
		LPC10(0x00000080, Type.AUDIO),
		G729(0x00000100, Type.AUDIO),
		SPEEX(0x00000200, Type.AUDIO),
		ILBC(0x00000400, Type.AUDIO),
		G7262(0x00000800, Type.AUDIO),
		G722(0x00001000, Type.AUDIO),
		AMR(0x00002000, Type.AUDIO),
		
		// VIDEO
		H261(0x00040000, Type.VIDEO),
		H263(0x00080000, Type.VIDEO),
		H263P(0x00100000, Type.VIDEO),
		H264(0x00200000, Type.VIDEO),
		
		// IMAGE
		JPEG(0x00010000, Type.IMAGE),
		PNG(0x00020000, Type.IMAGE);
		
		public enum Type { AUDIO, VIDEO, IMAGE }
		
		private static final Map<Integer, Format> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(Format::getId, Function.identity()));
		
		private final int id;
		private final Type type;
		
		Format(int id, Type type) {
			this.id = id;
			this.type = type;
		}
		
		public int getId() {
			return id;
		}
		
		public Type getType() {
			return type;
		}
		
		public static Format byId(int id) {
			return TYPES.getOrDefault(id, UNKNOWN);
		}
	}
}
