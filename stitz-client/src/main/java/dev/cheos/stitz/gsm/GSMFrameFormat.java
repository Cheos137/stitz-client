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

/**
 * Format of the coded GSM frame.
 * 
 * <p>
 * There are two formats incompatible with each other:
 * </p>
 * <ol>
 * 
 * <li>The format defined by Jutta Degner and Carsten Bormann at Technische
 * Universitaet Berlin ("toast" format). It has a leading 4 bit magic number, so
 * that the frame has 33 bytes. The frame rate is 50 frames per second. Bits
 * vectors for coefficients inside bytes are allocated MSB first.</li>
 * 
 * </li>The format defined by Microsoft as "ms-gsm". Universitaet Berlin. It
 * packs two native GSM frames into one frame, so that the frame has 65 bytes.
 * The frame rate is 25 frames per second. Bits vectors for coefficients inside
 * bytes are allocated LSB first.</li>
 * </ol>
 * 
 */
public enum GSMFrameFormat {
	/**
	 * 33 byte frames, 50 frames per second. 4 bits leading magic number. Bits
	 * vectors for coefficients inside bytes are allocated MSB first
	 */
	TOAST(GSMConstants.TOAST_ENCODED_GSM_FRAME_SIZE, GSMConstants.TOAST_SAMPLES_PER_FRAME),
	/**
	 * 65 byte frames, 25 frames per second. Bits vectors for coefficients inside
	 * bytes are allocated LSB first.
	 */
	MICROSOFT(GSMConstants.MICROSOFT_ENCODED_GSM_FRAME_SIZE, GSMConstants.MICROSOFT_SAMPLES_PER_FRAME);
	
	private final int frameSizeInBytes;
	private final int samplesPerFrame;
	
	private GSMFrameFormat(int frameSizeInBytes, int samplesPerFrame) {
		this.frameSizeInBytes = frameSizeInBytes;
		this.samplesPerFrame = samplesPerFrame;
	}
	
	/**
	 * Obtains the size of an encoded frame.
	 * 
	 * @return the size of the encoded frame in bytes
	 */
	public final int getFrameSize() {
		return frameSizeInBytes;
	}
	
	/**
	 * Obtains the number of decoded samples per GSM frame.
	 * 
	 * @return the number of decoded samples
	 */
	public final int getSamplesPerFrame() {
		return samplesPerFrame;
	}
}
