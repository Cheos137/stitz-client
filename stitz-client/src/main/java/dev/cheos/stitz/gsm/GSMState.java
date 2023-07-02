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

public class GSMState {
	private short[] m_dp0;
	private short z1; /* preprocessing, Offset_com. */
	private int L_z2; /* Offset_com. */
	private int mp; /* Preemphasis */
	
	private short[] u; /* short_term.java */
	private short[][] m_LARpp; /*                                  */
	private short m_j; /*                                  */
	
	private short nrp; /* long_term.java, synthesis */
	private short[] v; /* short_term.java, synthesis */
	private short msr; /* Gsm_Decoder.java, Postprocessing */
	
	public GSMState() {
		short Dp0[] = new short[280];
		short U[] = new short[8];
		short LARpp[][] = new short[2][8];
		short V[] = new short[9];
		
		this.setDp0(Dp0);
		this.setZ1((short) 0);
		this.setL_z2(0);
		this.setMp(0);
		this.setU(U);
		this.setLARpp(LARpp);
		this.setJ((short) 0);
		this.setNrp((short) 40);
		this.setV(V);
		this.setMsr((short) 0);
	}
	
	public void dump_Gsm_State() {
		int i, col;
		
		System.out.println("\ndp0[]: ");
		/*
		 * for(i = 0; i < dp0.length; ++i) { System.out.print("["+i+"] "+dp0[i]); if (i
		 * < dp0.length - 1) System.out.print(", "); }
		 */
		System.out.println("\nz1: " + z1);
		System.out.println("\nL_z2: " + L_z2);
		System.out.println("\nmp: " + mp);
		System.out.println("\nu[]: ");
		for (i = 0; i < u.length; ++i) {
			System.out.print("[" + i + "] " + u[i]);
			if (i < u.length - 1)
				System.out.print(", ");
		}
		System.out.print("\n");
		System.out.println("\nLARpp[]: ");
		for (i = 0; i < 2; ++i) {
			for (col = 0; col < 8; ++col) {
				System.out.print("[" + i + "][" + col + "] " + m_LARpp[i][col]);
				System.out.print(", ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
		System.out.println("\nj: " + m_j);
		System.out.println("\nnrp: " + nrp);
		System.out.println("\nv[]: ");
		for (i = 0; i < v.length; ++i) {
			System.out.print("[" + i + "] " + v[i]);
			if (i < v.length - 1)
				System.out.print(", ");
		}
		System.out.print("\n");
		System.out.println("\nmsr: " + msr);
		// System.out.println("\nverbose: " + verbose);
		// System.out.println("\nfast: " + fast);
	}
	
	@Override
	public String toString() {
		return String.valueOf(nrp);
	}
	
	public void setDp0(short[] lcl_arg0) {
		m_dp0 = lcl_arg0;
	}
	
	public void setDp0Indexed(int ix, short lcl_arg0) {
		m_dp0[ix] = lcl_arg0;
	}
	
	public short[] getDp0() {
		return m_dp0;
	}
	
	public short getDp0Indexed(int ix) {
		return m_dp0[ix];
	}
	
	public void setZ1(short lcl_arg0) {
		z1 = lcl_arg0;
	}
	
	public short getZ1() {
		return z1;
	}
	
	public void setL_z2(int lcl_arg0) {
		L_z2 = lcl_arg0;
	}
	
	public int getL_z2() {
		return L_z2;
	}
	
	public void setMp(int lcl_arg0) {
		mp = lcl_arg0;
	}
	
	public int getMp() {
		return mp;
	}
	
	public void setU(short[] lcl_arg0) {
		u = lcl_arg0;
	}
	
	public void setUIndexed(int ix, short lcl_arg0) {
		u[ix] = lcl_arg0;
	}
	
	public short[] getU() {
		return u;
	}
	
	public short getUIndexed(int ix) {
		return u[ix];
	}
	
	public void setLARpp(short[][] lcl_arg0) {
		m_LARpp = lcl_arg0;
	}
	
	public void setLARppIndexed(int ix, short[] lcl_arg0) {
		m_LARpp[ix] = lcl_arg0;
	}
	
	public short[][] getLARpp() {
		return m_LARpp;
	}
	
	public short[] getLARppIndexed(int ix) {
		return m_LARpp[ix];
	}
	
	public void setJ(short lcl_arg0) {
		m_j = lcl_arg0;
	}
	
	public short getJ() {
		return m_j;
	}
	
	public void setNrp(short lcl_arg0) {
		nrp = lcl_arg0;
	}
	
	public short getNrp() {
		return nrp;
	}
	
	public void setV(short[] lcl_arg0) {
		v = lcl_arg0;
	}
	
	public void setVIndexed(int ix, short lcl_arg0) {
		v[ix] = lcl_arg0;
	}
	
	public short[] getV() {
		return v;
	}
	
	public short getVIndexed(int ix) {
		return v[ix];
	}
	
	public void setMsr(short lcl_arg0) {
		msr = lcl_arg0;
	}
	
	public short getMsr() {
		return msr;
	}
}
