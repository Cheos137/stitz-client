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

package dev.cheos.stitz.iax.informationelement;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import com.google.common.base.MoreObjects;

import dev.cheos.stitz.iax.frame.MediaFrame;

/*
 *  0               1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      IE       |  Data Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               |
 * :             DATA              :
 * |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public abstract class InformationElement {
	protected InformationElement() { }
	
	protected InformationElement(ByteBuffer buf) {
		byte type = buf.get();
		if (InformationElementType.byId(type) != getType())
			throw new IllegalArgumentException("Type mismatch in information element, expected " + getType() + ", got " + InformationElementType.byId(type));
	}
	
	public abstract InformationElementType getType();
	
	public int getSize() {
		return 2;
	}
	
	public void serialize(ByteBuffer buf) {
		buf.put((byte) (getType().getId() & 0xFF));
		buf.put((byte) ((getSize() - 2) & 0xFF));
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.toString();
	}
	
	// static IE factories
	public static InformationElement adsicpe(short adsicpe) {
		return new Adsicpe(adsicpe);
	}
	
	public static InformationElement apparentAddr(InetAddress apparentAddr, short port) {
		return new ApparentAddr(apparentAddr, port);
	}
	
	public static InformationElement apparentAddr(InetAddress apparentAddr, short port, int flow, int scope) {
		return new ApparentAddr(apparentAddr, port, flow, scope);
	}
	
	public static InformationElement authmethods(Authmethods.Method... methods) {
		return new Authmethods(methods);
	}
	
	public static InformationElement calledContext(String calledContext) {
		return new CalledContext(calledContext);
	}
	
	public static InformationElement calledNumber(String calledNumber) {
		return new CalledNumber(calledNumber);
	}
	
	public static InformationElement callingAni(String callingAni) {
		return new CallingAni(callingAni);
	}
	
	public static InformationElement callingName(String callingName) {
		return new CallingName(callingName);
	}
	
	public static InformationElement callingNumber(String callingNumber) {
		return new CallingNumber(callingNumber);
	}
	
	public static InformationElement callingPres(CallingPres.PresentationValue presentation) {
		return new CallingPres(presentation);
	}
	
	public static InformationElement callingTon(CallingTon.TonValue ton) {
		return new CallingTon(ton);
	}
	
	public static InformationElement capability(MediaFrame.Format... capabilities) {
		return new Capability(capabilities);
	}
	
	public static InformationElement cause(String cause) {
		return new Cause(cause);
	}
	
	public static InformationElement causeCode(CauseCode.Cause cause) {
		return new CauseCode(cause);
	}
	
	public static InformationElement challenge(String challenge) {
		return new Challenge(challenge);
	}
	
	public static InformationElement datetime(LocalDateTime datetime) {
		return new Datetime(datetime);
	}
	
	public static InformationElement dnid(String dnid) {
		return new DNID(dnid);
	}
	
	public static InformationElement format(MediaFrame.Format format) {
		return new Format(format);
	}
	
	public static InformationElement iaxUnknown(byte subclass) {
		return new IaxUnknown(subclass);
	}
	
	public static InformationElement language(String language) {
		return new Language(language);
	}
	
	public static InformationElement md5Result(String challenge, String password) {
		return new MD5Result(challenge, password);
	}
	
	public static InformationElement refresh(short refresh) {
		return new Refresh(refresh);
	}
	
	public static InformationElement rrDelay(short delay) {
		return new RrDelay(delay);
	}
	
	public static InformationElement rrDropped(int frames) {
		return new RrDropped(frames);
	}
	
	public static InformationElement rrJitter(int jitter) {
		return new RrJitter(jitter);
	}
	
	public static InformationElement rrLoss(byte percentage, int frames) {
		return new RrLoss(percentage, frames);
	}
	
	public static InformationElement rrOoo(int frames) {
		return new RrOoo(frames);
	}
	
	public static InformationElement rrPkts(int frames) {
		return new RrPkts(frames);
	}
	
	public static InformationElement samplingRate(short samplingRate) {
		return new SamplingRate(samplingRate);
	}
	
	public static InformationElement username(String username) {
		return new Username(username);
	}
	
	public static InformationElement version() {
		return new Version();
	}
}
