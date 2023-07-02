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
import java.util.*;
import java.util.function.Function;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import dev.cheos.stitz.iax.informationelement.InformationElement;
import dev.cheos.stitz.iax.informationelement.InformationElementType;

/**
 * Frames of type 'IAX' are used to provide management of IAX endpoints. They
 * handle IAX signaling (e.g., call setup, maintenance, and tear- down). They
 * MAY also handle direct transmission of media data, but this is not optimal
 * for VoIP calls. They do not carry session- specific control (e.g., device
 * state), as this is the purpose of Control Frames.
 */
public class IaxFrame extends FullFrame {
	protected final Subclass iaxSubclass;
	protected final Map<InformationElementType, InformationElement> ies;
	protected final int ieSize;
	
	protected IaxFrame(
			short srcCallNumber,
			short dstCallNumber,
			boolean retransmission,
			long timestamp,
			byte oSeqNo,
			byte iSeqNo,
			Subclass subclass,
			Map<InformationElementType, InformationElement> ies) {
		super(srcCallNumber, dstCallNumber, retransmission, timestamp, oSeqNo, iSeqNo, FullFrameType.IAXCONTROL, subclass.getId(), NO_DATA);
		this.iaxSubclass = subclass;
		this.ies = ImmutableMap.copyOf(ies);
		this.ieSize = this.ies.values().stream().mapToInt(InformationElement::getSize).sum();
	}
	
	protected IaxFrame(ByteBuffer buf) {
		super(buf, FullFrameType.IAXCONTROL);
		this.iaxSubclass = Subclass.byId(getSubclass());
		this.ies = readIEs(ByteBuffer.wrap(getData())); // super reads all additional data into this array
		this.ieSize = this.ies.values().stream().mapToInt(InformationElement::getSize).sum();
	}
	
	protected IaxFrame(FullFrame frame) {
		super(frame, FullFrameType.IAXCONTROL);
		this.iaxSubclass = Subclass.byId(getSubclass());
		this.ies = readIEs(ByteBuffer.wrap(getData())); // super reads all additional data into this array
		this.ieSize = this.ies.values().stream().mapToInt(InformationElement::getSize).sum();
	}
	
	public Subclass getIAXSubclass() {
		return this.iaxSubclass;
	}
	
	public InformationElement getIERaw(InformationElementType type) {
		return this.ies.get(type);
	}
	
	public <T extends InformationElement> Optional<T> getIEOpt(InformationElementType type) {
		return Optional.ofNullable(getIE(type));
	}
	
	@SuppressWarnings("unchecked") // this can be trusted as each InformationElementType only maps to one class and vice-versa
	public <T extends InformationElement> T getIE(InformationElementType type) {
		return (T) getIERaw(type);
	}
	
	public <T extends InformationElement> T getIE(Class<T> typeOfT) {
		for (InformationElement ie : this.ies.values())
			if (typeOfT.isInstance(ie))
				return typeOfT.cast(ie);
		return null;
	}
	
	public <T extends InformationElement> T getIE(InformationElementType type, Class<T> typeOfT) {
		InformationElement ie = getIERaw(type);
		if (typeOfT.isInstance(ie))
			return typeOfT.cast(ie);
		return null;
	}
	
	public Map<InformationElementType, InformationElement> getIEs() {
		return this.ies;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + this.ieSize;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		this.ies.values().forEach(ie -> ie.serialize(buf));
	}
	
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
				.add("subclass", this.iaxSubclass)
				.add("ies", this.ies.values())
				.toString();
	}
	
	private static Map<InformationElementType, InformationElement> readIEs(ByteBuffer buf) {
		ImmutableMap.Builder<InformationElementType, InformationElement> builder = ImmutableMap.builder();
		InformationElement ie;
		while ((ie = readIE(buf)) != null) // if readIE returns null, we either received malformed data or reached the end of the buffer
			builder.put(ie.getType(), ie);
		return builder.build();
	}
	
	private static InformationElement readIE(ByteBuffer buf) {
		if (buf.remaining() < 3) return null; // need 2 bytes of header + 1 byte of body minimum
		Function<ByteBuffer, InformationElement> factory = InformationElementType.byId(buf.get(buf.position())); // read id without advancing
		if (factory == null) return null; // not implemented or invalid type
		return factory.apply(buf);
	}
	
	
	public enum Subclass {
		NEW(0x01), PING(0x02), PONG(0x03), ACK(0x04),
		HANGUP(0x05), REJECT(0x06), ACCEPT(0x07),
		AUTHREQ(0x08), AUTHREP(0x09), INVAL(0x0a),
		LAGRQ(0x0b), LAGRP(0x0c), REGREQ(0x0d),
		REGAUTH(0x0e), REGACK(0x0f), REGREJ(0x10),
		REGREL(0x11), VNAK(0x12), DPREQ(0x13),
		DPREP(0x14), DIAL(0x15), TXREQ(0x16), TXCNT(0x17),
		TXACC(0x18), TXREADY(0x19), TXREL(0x1a),
		TXREJ(0x1b), QUELCH(0x1c), UNQUELCH(0x1d),
		POKE(0x1e), MWI(0x20), UNSUPPORT(0x21), TRANSFER(0x22);
		
		private static final Map<Integer, Subclass> TYPES = Arrays.stream(values())
				.collect(ImmutableMap.toImmutableMap(Subclass::getId, Function.identity()));
		
		private final int id;
		
		Subclass(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Subclass byId(int id) {
			return TYPES.getOrDefault(id, INVAL);
		}
	}
}
