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

package dev.cheos.stitz.iax.informationelement;

import java.net.*;
import java.nio.ByteBuffer;

import com.google.common.base.MoreObjects;

/**
 * Implements the APPARENT ADDR information element. From RFC 5456:
 * 
 * The purpose of the APPARENT ADDR information element is to indicate
 * the perceived network connection information used to reach a peer,
 * which may differ from the actual address when the peer is behind NAT.
 * The APPARENT ADDR IE is populated using the source address values of
 * the UDP and IP headers in the IAX message to which this response is
 * generated.  The data field of the APPARENT ADDR information element
 * is the same as the POSIX sockaddr struct for the address family in
 * use (i.e., sockaddr_in for IPv4, sockaddr_in6 for IPv6).  The data
 * length depends on the type of address being represented.
 * 
 * The APPARENT ADDR information element MUST be sent with IAX TXREQ and
 * REGACK messages.  When used with a TXREQ message, the APPARENT ADDR
 * MUST specify the address of the peer to which the local peer is
 * trying to transfer its end of the connection.  When used with a
 * REGACK message, the APPARENT ADDR MUST specify the address it uses to
 * reach the peer (which may be different than the address the peer
 * perceives itself as in the case of NAT or multi-homed peer machines).
 * 
 * The data field of the APPARENT ADDR information element is the same
 * as the Linux struct sockaddr_in: two octets for the address family,
 * two octets for the port number, four octets for the IPv4 address, and
 * 8 octets of padding consisting of all bits set to 0.  Thus, the total
 * length of the APPARENT ADDR information element is 18 octets.
 * 
 *  0               1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x12     |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        sockaddr struct        |
 * :   for address family in use   :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * The following diagram demonstrates the APPARENT ADDR format for an
 * IPv4 address:
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x12     |      0x10     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            0x0200             | <- Address family (INET)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            0x11d9             | <- Portno (default 4569)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      32-bit IP address        |
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * |      8 octets of all 0s       |
 * |   (padding in sockaddr_in)    |
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * The following diagram demonstrates the APPARENT ADDR format for an
 * IPv6 address:
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x12     |      0x1C     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            0x0A00             | <- Address family (INET6)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            0x11d9             | <- Portno (default 4569)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           32 bits             | <- Flow information
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      128-bit IP address       | <- Ip6 Address
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           32 bits             | <- Scope ID
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class ApparentAddr extends InformationElement {
	private final InetAddress apparentAddr;
	private final short port;
	private final int flow, scope;
	
	protected ApparentAddr(InetAddress apparentAddr, short port) {
		this(apparentAddr, port, 0, 0);
	}
	
	protected ApparentAddr(InetAddress apparentAddr, short port, int flow, int scope) {
		this.apparentAddr = apparentAddr;
		this.port = port;
		this.flow = flow;
		this.scope = scope;
	}
	
	protected ApparentAddr(ByteBuffer buf) {
		super(buf);
		byte size = buf.get();
		if (size != 16 && size != 28) throw new IllegalArgumentException("invalid size for ApparentAddr InformationElement: " + size);
		short addrFamily = buf.getShort();
		if ((size == 16 && addrFamily != 0x0200) || (size == 28 && addrFamily != 0x0A00))
			 throw new IllegalArgumentException("invalid size/address family combination for ApparentAddr InformationElement: " + size + ", " + addrFamily);
		this.port = buf.getShort();
		this.apparentAddr = switch (addrFamily) {
			case 0x0200 -> { // IPv4
				this.flow = this.scope = 0;
				byte[] addr = new byte[4];
				buf.get(addr);
				try {
					yield Inet4Address.getByAddress(addr);
				} catch (UnknownHostException e) {
					throw new IllegalArgumentException(e);
				}
			} case 0x0A00 -> { // IPv6
				this.flow = buf.getInt();
				byte[] addr = new byte[6];
				buf.get(addr);
				this.scope = buf.getInt();
				try {
					yield Inet4Address.getByAddress(addr);
				} catch (UnknownHostException e) {
					throw new IllegalArgumentException(e);
				}
			} default -> throw new IllegalArgumentException("invalid address family for ApparentAddr InformationElement: " + addrFamily);
		};
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.APPARENT_ADDR;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + (this.apparentAddr instanceof Inet4Address ? 16 : 28);
	}
	
	public InetAddress getApparentAddr() {
		return this.apparentAddr;
	}
	
	public short getPort() {
		return this.port;
	}
	
	public int getFlow() { // IPv6
		return flow;
	}
	
	public int getScope() { // IPv6
		return scope;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		if (this.apparentAddr instanceof Inet4Address) { // IPv4
			buf.putShort((short) 0x0200);
			buf.putShort(this.port);
			buf.put(this.apparentAddr.getAddress());
			buf.putLong(0); // 8 octets of all 0s padding
		} else { // IPv6
			buf.putShort((short) 0x0A00);
			buf.putShort(this.port);
			buf.putInt(this.flow);
			buf.put(this.apparentAddr.getAddress());
			buf.putInt(this.scope);
		}
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("address", this.apparentAddr)
				.add("port", this.port)
				.toString();
	}
}
