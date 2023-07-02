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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import dev.cheos.stitz.iax.IaxConstants;

/*
 *  0               1               2               3
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |1|     Source Call Number      |R|   Destination Call Number   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            timestamp                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    OSeqno     |    ISeqno     |   Frame Type  |C|  Subclass   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * :                             Data                              :
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public abstract class FullFrame extends Frame {
	protected boolean retransmission;
	protected int retransmissionCount;
	protected long retransmissionTime;
	protected final long generationTime;
	protected final short dstCallNumber;
	protected final long timestamp;
	protected final byte oSeqNo, iSeqNo;
	protected final FullFrameType type;
	protected final boolean cBit;
	protected final int subclass;
	protected final byte[] data;
	
	protected FullFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			FullFrameType type,
			int subclass,
			byte[] data) {
		super(srcCallNumber);
		this.dstCallNumber = (short) (dstCallNumber & 0x7FFF);
		this.retransmission = retransmission;
		this.timestamp = timestamp;
		this.oSeqNo = oSeqNo;
		this.iSeqNo = iSeqNo;
		this.type = type;
		this.subclass = subclass;
		this.generationTime = System.currentTimeMillis();
		this.cBit = subclass > 127;
		this.data = data == null || data.length == 0 ? NO_DATA : data;
	}
	
	protected FullFrame(ByteBuffer buf) {
		super(buf);
		short tmp = buf.getShort();
		this.dstCallNumber = (short) (tmp & 0x7FFF);
		this.retransmission = (tmp & 0x8000) != 0;
		this.timestamp = buf.getInt() & 0xFFFFFFFF;
		this.oSeqNo = buf.get();
		this.iSeqNo = buf.get();
		this.type = FullFrameType.byId(buf.get());
		tmp = buf.get();
		this.cBit = (tmp & 0x80) != 0;
		int tmpSubclass = tmp & 0x7F;
		this.subclass = this.cBit ? 1 << (tmpSubclass & 0x1F) : tmpSubclass;
		this.generationTime = System.currentTimeMillis();
		if (buf.remaining() != 0) {
			this.data = new byte[buf.remaining()];
			buf.get(this.data);
		} else this.data = NO_DATA;
	}
	
	protected FullFrame(ByteBuffer buf, FullFrameType expectedType) {
		this(buf);
		Preconditions.checkState(getType() == expectedType, "Invalid frame type for " + getClass().getSimpleName() + ": " + getType());
	}
	
	/**
	 * Copy ctor, overridden by subclasses
	 * 
	 * @param frame
	 */
	protected FullFrame(FullFrame frame) {
		super(frame.srcCallNumber);
		this.retransmission = frame.retransmission;
		this.retransmissionCount = frame.retransmissionCount;
		this.dstCallNumber = frame.dstCallNumber;
		this.timestamp = frame.timestamp;
		this.oSeqNo = frame.oSeqNo;
		this.iSeqNo = frame.iSeqNo;
		this.type = frame.type;
		this.subclass = frame.subclass;
		this.cBit = frame.cBit;
		this.data = frame.data == NO_DATA ? NO_DATA : Arrays.copyOf(frame.data, frame.data.length);
		this.generationTime = frame.generationTime;
	}
	
	protected FullFrame(FullFrame frame, FullFrameType expectedType) {
		this(frame);
		Preconditions.checkState(getType() == expectedType, "Invalid frame type for " + getClass().getSimpleName() + ": " + getType());
	}
	
	public short getDstCallNumber() {
		return this.dstCallNumber;
	}
	
	public boolean isRetransmission() {
		return this.retransmission;
	}
	
	public int getRetransmissionCount() {
		return this.retransmissionCount;
	}
	
	public void incRetransmissionCount() {
		this.retransmission = true;
		this.retransmissionCount++;
	}
	
	public long getNextRetransmissionTime() {
		return this.retransmissionTime;
	}
	
	public void updateRetransmissionTime() {
		this.retransmissionTime = System.currentTimeMillis() + (this.retransmissionCount < 1
				?  2 * IaxConstants.FRAME_RETRANSMIT_INTERVAL
				: IaxConstants.FRAME_RETRANSMIT_INTERVAL);
	}
	
	public long getGenerationTime() {
		return this.generationTime;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public byte getOSeqNo() {
		return this.oSeqNo;
	}
	
	public byte getISeqNo() {
		return this.iSeqNo;
	}
	
	public FullFrameType getType() {
		return this.type;
	}
	
	public boolean isCBit() {
		return this.cBit;
	}
	
	public int getSubclass() {
		return this.subclass;
	}
	
	public byte[] getData() {
		return this.data;
	}
	
	@Override
	public final boolean isFullFrame() {
		return true;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 10 + this.data.length;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		buf.putShort((short) (this.dstCallNumber | (this.retransmission ? 0x8000 : 0)));
		buf.putInt((int) this.timestamp & 0xFFFFFFFF);
		buf.put(this.oSeqNo);
		buf.put(this.iSeqNo);
		buf.put(this.type.getId());
		long tmpSubclass = this.subclass;
		if (this.cBit)
			for (int i = 0; i < 31 && ((1 << i) & this.subclass) != 0; tmpSubclass = ++i);
		buf.put((byte) ((tmpSubclass & 0x7F) | (this.cBit ? 0x80 : 0)));
		buf.put(this.data);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", this.type)
				.add("srcCallNumber", this.srcCallNumber)
				.add("dstCallNumber", this.dstCallNumber)
				.add("oSeq", this.oSeqNo)
				.add("iSeq", this.iSeqNo)
				.add("subclass", this.subclass)
				.add("timestamp", this.timestamp)
				.add("size", getSize())
				.add("data", this.data)
				.toString();
	}
}
