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

public interface GSMConstants {
	/**
	 * Samples per frame in toast frame format.
	 */
	public static final int TOAST_SAMPLES_PER_FRAME = 160;
	
	/**
	 * Samples per frame in Microsoft frame format.
	 */
	public static final int MICROSOFT_SAMPLES_PER_FRAME = 320;
	
	/**
	 * Length of a coded frame in bytes in toast frame format.
	 */
	public static final int TOAST_ENCODED_GSM_FRAME_SIZE = 33;
	
	/**
	 * Length of a coded frame in bytes in Microsoft frame format.
	 */
	public static final int MICROSOFT_ENCODED_GSM_FRAME_SIZE = 65;
}
