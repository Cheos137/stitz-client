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
import java.util.*;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import dev.cheos.stitz.iax.informationelement.InformationElement;
import dev.cheos.stitz.iax.informationelement.InformationElementType;

public abstract class Frame {
	protected static final byte[] NO_DATA = new byte[0];
	protected final short srcCallNumber;
	
	protected Frame(short srcCallNumber) {
		this.srcCallNumber = (short) (srcCallNumber & 0x7FFF);
	}
	
	public Frame(ByteBuffer buf) {
		this(buf.getShort());
	}
	
	public final short getSrcCallNumber() {
		return this.srcCallNumber;
	}
	
	public abstract boolean isFullFrame();
	
	public int getSize() {
		return 2;
	}
	
	public void serialize(ByteBuffer buf) {
		buf.putShort((short) (this.srcCallNumber | (isFullFrame() ? 0x8000 : 0)));
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("srcCallNumber", this.srcCallNumber)
				.add("size", getSize())
				.toString();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder implements Cloneable {
		private short srcCallNumber, dstCallNumber;
		private boolean retransmission;
		private long timestamp;
		private Supplier<Long> timestampGenerator;
		private byte oSeqNo, iSeqNo;
		private Supplier<Byte> oSeqNoGenerator, iSeqNoGenerator;
		private boolean cBit;
		private int subclass;
		private byte[] data = NO_DATA;
		private String content;
		private FullFrame frame;
		private ByteBuffer buf;
		private ImmutableMap.Builder<InformationElementType, InformationElement> ies = ImmutableMap.builder();
		
		private Builder() { }

		private Builder(
				short srcCallNumber,
				short dstCallNumber,
				boolean retransmission,
				byte oSeqNo,
				byte iSeqNo,
				Supplier<Byte> oSeqNoGenerator,
				Supplier<Byte> iSeqNoGenerator,
				boolean cBit,
				int subclass,
				Supplier<Long> timestampGenerator) {
			this.srcCallNumber = srcCallNumber;
			this.dstCallNumber = dstCallNumber;
			this.retransmission = retransmission;
			this.oSeqNo = oSeqNo;
			this.iSeqNo = iSeqNo;
			this.oSeqNoGenerator = oSeqNoGenerator;
			this.iSeqNoGenerator = iSeqNoGenerator;
			this.cBit = cBit;
			this.subclass = subclass;
			this.timestampGenerator = timestampGenerator;
		}
		
		public Builder srcCallNumber(short srcCallNumber) { this.srcCallNumber = srcCallNumber; return this; }
		public short srcCallNumber() { return this.srcCallNumber; }
		
		public Builder dstCallNumber(short dstCallNumber) { this.dstCallNumber = dstCallNumber; return this; }
		public short dstCallNumber() { return this.dstCallNumber; }
		
		public Builder retransmission(boolean retransmission) { this.retransmission = retransmission; return this; }
		public boolean retransmission() { return this.retransmission; }
		
		public Builder timestamp(long timestamp) { this.timestamp = timestamp; this.timestampGenerator = null; return this; }
		public Builder timestamp(Supplier<Long> generator) { this.timestampGenerator = generator; return this; }
		public long timestamp() { return this.timestamp; }
		public long updateTimestamp() { return this.timestampGenerator != null ? this.timestamp = this.timestampGenerator.get() : this.timestamp; }
		
		public Builder oSeqNo(byte oSeqNo) { this.oSeqNo = oSeqNo; this.oSeqNoGenerator = null; return this; }
		public Builder oSeqNo(Supplier<Byte> generator) { this.oSeqNoGenerator = generator; return this; }
		public byte oSeqNo() { return this.oSeqNo; }
		public byte updateOSeqNo() { return this.oSeqNoGenerator != null ? this.oSeqNo = this.oSeqNoGenerator.get() : this.oSeqNo; }
		
		public Builder iSeqNo(byte iSeqNo) { this.iSeqNo = iSeqNo; this.iSeqNoGenerator = null; return this; }
		public Builder iSeqNo(Supplier<Byte> generator) { this.iSeqNoGenerator = generator; return this; }
		public byte iSeqNo() { return this.iSeqNo; }
		public byte updateISeqNo() { return this.iSeqNoGenerator != null ? this.iSeqNo = this.iSeqNoGenerator.get() : this.iSeqNo; }
		
		public Builder cBit(boolean cBit) { this.cBit = cBit; return this; }
		public boolean cBit() { return this.cBit; }
		
		public Builder subclass(int subclass) { this.subclass = subclass; return this; }
		public int subclass() { return this.subclass; }
		
		public Builder data(byte[] data) { this.data = data.length == 0 ? NO_DATA : data; return this; }
		public byte[] data() { return this.data; }
		
		public Builder content(String content) { this.content = content; return this; }
		public String content() { return this.content; }
		
		public Builder ie(Map<InformationElementType, ? extends InformationElement> ies) { this.ies.putAll(ies); return this; }
		public Builder ie(Collection<? extends InformationElement> ies) { ies.forEach(this::ie); return this; }
		public Builder ie(InformationElement... ies) { ie(Arrays.asList(ies)); return this; }
		public Builder ie(InformationElement ie) { this.ies.put(ie.getType(), ie); return this; }
		public Map<InformationElementType, InformationElement> ie() { return this.ies.buildKeepingLast(); }
		public Builder clearIEs() { this.ies = ImmutableMap.builder(); return this; }
		
		// for copy ctor invocation
		public Builder frame(FullFrame frame) { this.frame = frame; return this; }
		public FullFrame frame() { return this.frame; }
		
		// for deserialization
		public Builder buffer(ByteBuffer buffer) { this.buf = buffer; return this; }
		public ByteBuffer buffer() { return this.buf; }
		
		// frame-specific aliases
		public Builder noiseLevel(int noiseLevel) { this.subclass = noiseLevel; return this; }
		public int noiseLevel() { return this.subclass; }
		public Builder digit(int digit) { this.subclass = digit; return this; }
		public int digit() { return this.subclass; }
		public Builder cfSubclass(ControlFrame.Subclass subclass) { return subclass(subclass.getId()); }
		public ControlFrame.Subclass cfSubclass() { return ControlFrame.Subclass.byId(subclass()); }
		public Builder iaxSubclass(IaxFrame.Subclass subclass) { return subclass(subclass.getId()); }
		public IaxFrame.Subclass iaxSubclass() { return IaxFrame.Subclass.byId(subclass()); }
		public Builder mediaFormat(MediaFrame.Format format) { return subclass(format.getId()); }
		public MediaFrame.Format mediaFormat() { return MediaFrame.Format.byId(subclass()); }
		
		// duplication
		@Override
		public Builder clone() {
			try {
				return (Builder) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return fork()
						.data(data())
						.content(content())
						.ie(ie())
						.frame(frame())
						.buffer(buffer());
			}
		}
		
		public Builder fork() {
			return new Builder(
					this.srcCallNumber,
					this.dstCallNumber,
					this.retransmission,
					this.oSeqNo,
					this.iSeqNo,
					this.oSeqNoGenerator,
					this.iSeqNoGenerator,
					this.cBit,
					this.subclass,
					this.timestampGenerator);
		}
		
		// converters
		public MiniFrame mini() {
			if (this.frame != null) return new MiniFrame(this.frame.srcCallNumber, (int) this.frame.timestamp, this.frame.data);
			if (this.buf != null) return new MiniFrame(this.buf);
			return new MiniFrame(srcCallNumber(), (int) updateTimestamp(), data());
		}
		
		public ComfortNoiseFrame comfortNoise() {
			if (this.frame != null) return new ComfortNoiseFrame(this.frame);
			if (this.buf != null) return new ComfortNoiseFrame(this.buf);
			return new ComfortNoiseFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), subclass());
		}
		
		public ControlFrame control() {
			if (this.frame != null) return new ControlFrame(this.frame);
			if (this.buf != null) return new ControlFrame(this.buf);
			return new ControlFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), cfSubclass());
		}
		
		public DTMFFrame dtmf() {
			if (this.frame != null) return new DTMFFrame(this.frame);
			if (this.buf != null) return new DTMFFrame(this.buf);
			return new DTMFFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), subclass());
		}
		
		public HTMLFrame html() {
			if (this.frame != null) return new HTMLFrame(this.frame);
			if (this.buf != null) return new HTMLFrame(this.buf);
			return new HTMLFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), content());
		}
		
		public IaxFrame iax() {
			if (this.frame != null) return new IaxFrame(this.frame);
			if (this.buf != null) return new IaxFrame(this.buf);
			return new IaxFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), iaxSubclass(), ie());
		}
		
		public ImageFrame image() {
			if (this.frame != null) return new ImageFrame(this.frame);
			if (this.buf != null) return new ImageFrame(this.buf);
			return new ImageFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), mediaFormat(), data());
		}
		
		public VideoFrame video() {
			if (this.frame != null) return new VideoFrame(this.frame);
			if (this.buf != null) return new VideoFrame(this.buf);
			return new VideoFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), mediaFormat(), data());
		}
		
		public VoiceFrame voice() {
			if (this.frame != null) return new VoiceFrame(this.frame);
			if (this.buf != null) return new VoiceFrame(this.buf);
			return new VoiceFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), mediaFormat(), data());
		}
		
		public TextFrame text() {
			if (this.frame != null) return new TextFrame(this.frame);
			if (this.buf != null) return new TextFrame(this.buf);
			return new TextFrame(srcCallNumber(), dstCallNumber(), retransmission(), updateTimestamp(), updateOSeqNo(), updateISeqNo(), content());
		}
		
		public FullFrame of(FullFrameType type) {
			return switch(type) {
				case DTMF -> dtmf();
				case COMFORTNOISE -> comfortNoise();
				case CONTROL -> control();
				case HTML -> html();
				case IAXCONTROL -> iax();
				case IMAGE -> image();
				case TEXT -> text();
				case VIDEO -> video();
				case VOICE -> voice();
				default -> throw new IllegalArgumentException("invalid/unknown type: " + type);
			};
		}
	}
}
