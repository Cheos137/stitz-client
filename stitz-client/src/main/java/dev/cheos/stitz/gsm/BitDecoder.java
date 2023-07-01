/*
 * Copyright (c) 2023 Cheos
 *
 * This file is part of the GSM 6.10 audio decoder library for Java
 * Copyright (C) 1998 Steven Pickles (pix@test.at)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * This software is a port of the GSM Library provided by
 * Jutta Degener (jutta@cs.tu-berlin.de) and
 * Carsten Bormann (cabo@cs.tu-berlin.de),
 * Technische Universitaet Berlin
 *
 * (https://www.gnu.org/licenses/lgpl-3.0.html)
 */

package dev.cheos.stitz.gsm;

public class BitDecoder {
	public static enum AllocationMode {
		MSBitFirst, LSBitFirst;
	}
	
	private AllocationMode allocationMode;
	
	private byte[] m_codedFrame;
	private int m_codedFrameByteIndex;
	private int m_sr;
	private int m_currentBits;
	
	/**
	 * Constructor.
	 * 
	 * @param codedBytes
	 * @param allocationMode
	 */
	public BitDecoder(byte[] codedBytes, int bufferStartIndex, AllocationMode allocationMode) {
		super();
		this.allocationMode = allocationMode;
		m_codedFrame = codedBytes;
		m_codedFrameByteIndex = bufferStartIndex;
		m_sr = 0;
		m_currentBits = 0;
	}
	
	public void setCodedFrame(byte[] c, final int bufferStartIndex) {
		m_codedFrame = c;
		m_codedFrameByteIndex = bufferStartIndex;
	}
	
	private final void addNextCodedByteValue() {
		m_sr |= getNextCodedByteValue() << m_currentBits;
		m_currentBits += 8;
	}
	
	private final int getNextCodedByteValue() {
		int value = m_codedFrame[m_codedFrameByteIndex];
		m_codedFrameByteIndex++;
		return value & 0xFF;
	}
	
	public final int getNextBits(int bits) {
		switch (allocationMode) {
			case LSBitFirst:
				while (m_currentBits < bits) {
					addNextCodedByteValue();
				}
				int value = m_sr & GSMDef.BITMASKS[bits];
				m_sr >>>= bits;
				m_currentBits -= bits;
				return value;
			case MSBitFirst:
			default:
				throw new RuntimeException("not supported");
		}
	}
}
