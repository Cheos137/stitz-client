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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Contains the "exploded" parameters of a GSM frame.
 * 
 * <p>
 * This are the parameters after bit-decoding or before bit-encoding.
 * 
 * <table border="1">
 * <tr>
 * <th>Parameter name</th>
 * <th>Variable name</th>
 * <th>number of parameters</th>
 * </tr>
 * <tr>
 * <td>LAR</td>
 * <td>m_LARc</td>
 * <td>8</td>
 * </tr>
 * <tr>
 * <td>N</td>
 * <td>m_Nc</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>m_bc</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>M</td>
 * <td>m_Mc</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>Xmax</td>
 * <td>m_xmaxc</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>m_xmc</td>
 * <td>52 (13 * 4)</td>
 * </tr>
 * </table>
 * 
 * @author Matthias Pfisterer
 * 
 */
public class GSMFrameParameters {
	public int[] m_LARc = new int[8];
	public int[] m_Nc = new int[4];
	public int[] m_Mc = new int[4];
	public int[] m_bc = new int[4];
	public int[] m_xmaxc = new int[4];
	public int[] m_xmc = new int[13 * 4];
	
	/**
	 * Dumps the parameter values to an output stream.
	 * 
	 * <p>
	 * Can be used in the following ways: <code>dump(System.out)</code> or
	 * <code>dump(System.err)</code>.
	 * </p>
	 * 
	 * @param printStream the stream to output the dump
	 */
	public void dump(PrintStream printStream) {
		PrintWriter pw = new PrintWriter(printStream);
		pw.println("GSM frame:");
		for (int i = 0; i < 8; i++) {
			pw.println("m_LARc[" + i + "]" + m_LARc[i]);
		}
		for (int i = 0; i < 4; i++) {
			pw.println("m_Nc[" + i + "]" + m_Nc[i]);
			pw.println("m_bc[" + i + "]" + m_bc[i]);
			pw.println("m_Mc[" + i + "]" + m_Mc[i]);
			pw.println("m_xmaxc[" + i + "]" + m_xmaxc[i]);
			for (int j = 0; j < 13; j++) {
				pw.println("m_xmc[" + i * 13 + j + "]" + m_xmc[i * 13 + j]);
			}
		}
	}
}
