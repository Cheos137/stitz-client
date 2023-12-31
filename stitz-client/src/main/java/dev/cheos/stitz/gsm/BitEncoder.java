/*
 * Copyright (c) 2023 Cheos
 *
 * This file is part of the GSM 6.10 audio decoder library for Java
 * Copyright (c) 1998 Steven Pickles (pix@test.at)
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

import java.util.Arrays;

public class BitEncoder {
	public static enum AllocationMode {
		MSBitFirst, LSBitFirst;
	}
	
	private int byteIndex;
	private int remainingBitsInCurrentByte;
	private byte[] codedBytes;
	private AllocationMode allocationMode;
	
	/**
	 * Constructor.
	 * 
	 * <p>
	 * Note: this constructor writes 0 values into the passed byte array.
	 * </p>
	 * 
	 * @param codedBytes
	 * @param allocationMode
	 */
	public BitEncoder(byte[] codedBytes, AllocationMode allocationMode) {
		super();
		byteIndex = 0;
		remainingBitsInCurrentByte = 8;
		this.codedBytes = codedBytes;
		this.allocationMode = allocationMode;
		Arrays.fill(codedBytes, (byte) 0);
	}
	
	/**
	 * Resets the array pointers and clears the array.
	 */
	public void reset() {
		byteIndex = 0;
		remainingBitsInCurrentByte = 8;
		Arrays.fill(codedBytes, (byte) 0);
	}
	
	public final void addBits(int value, int numBits) {
		while (numBits > 0) {
			if (byteIndex >= codedBytes.length) {
				throw new RuntimeException("No more bytes in coded bytes array");
			}
			int bits = Math.min(numBits, remainingBitsInCurrentByte);
			int nextRemainingBits = remainingBitsInCurrentByte - bits;
			int nextNumBits = numBits - bits;
			int x;
			switch (allocationMode) {
				case LSBitFirst:
					x = (((value) & GSMDef.BITMASKS[bits]) << (8 - remainingBitsInCurrentByte));
					codedBytes[byteIndex] |= x;
					value >>>= bits;
					break;
				case MSBitFirst:
					x = (((value >>> nextNumBits) & GSMDef.BITMASKS[bits]) << nextRemainingBits);
					codedBytes[byteIndex] |= x;
					break;
			}
			remainingBitsInCurrentByte = nextRemainingBits;
			if (remainingBitsInCurrentByte == 0) {
				byteIndex++;
				remainingBitsInCurrentByte = 8;
			}
			numBits = nextNumBits;
		}
	}
}
