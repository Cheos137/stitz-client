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

import java.nio.ByteBuffer;
import java.time.*;

import com.google.common.base.MoreObjects;

/**
 * Implements the DATETIME information element. From RFC 5456:
 * 
 * The DATETIME information element indicates the time a message is
 * sent.  This differs from the header time-stamp because that time-
 * stamp begins at 0 for each call, while the DATETIME is a call-
 * independent value representing the actual real-world time.  The data
 * field of a DATETIME information element is four octets long and
 * stores the time as follows: the 5 least significant bits are seconds,
 * the next 6 least significant bits are minutes, the next least
 * significant 5 bits are hours, the next least significant 5 bits are
 * the day of the month, the next least significant 4 bits are the
 * month, and the most significant 7 bits are the year.  The year is
 * offset from 2000, and the month is a 1-based index (i.e., January ==
 * 1, February == 2, etc.).  The timezone of the clock MUST be UTC to
 * avoid confusion between the peers.
 * 
 * The DATETIME information element SHOULD be sent with IAX NEW and
 * REGACK messages.  However, it is strictly informational.
 * 
 *                  1
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      0x1f     |      0x04     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     year    | month |   day   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  hours  |  minutes  | seconds |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Datetime extends InformationElement {
	private final ZonedDateTime datetime;
	private final LocalDateTime localDatetime;
	
	protected Datetime(LocalDateTime datetime) {
		this.datetime = datetime.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
		this.localDatetime = datetime;
	}
	
	protected Datetime(ByteBuffer buf) {
		super(buf);
		if (buf.get() != 4) throw new IllegalArgumentException("invalid size for Datetime InformationElement");
		
		int tmp = buf.getInt();
		this.datetime = ZonedDateTime.of(
				((tmp >> 25) & 0x7F) + 2000,
				((tmp >> 21) & 0x0F),
				(tmp >> 16) & 0x1F,
				(tmp >> 11) & 0x1F,
				(tmp >> 5) & 0x3F,
				tmp & 0x1F,
				0, ZoneOffset.UTC);
		this.localDatetime = this.datetime.withZoneSameInstant(ZoneOffset.systemDefault()).toLocalDateTime();
	}
	
	@Override
	public InformationElementType getType() {
		return InformationElementType.DATETIME;
	}
	
	@Override
	public int getSize() {
		return super.getSize() + 4;
	}
	
	public LocalDateTime getDatetime() {
		return this.localDatetime;
	}
	
	@Override
	public void serialize(ByteBuffer buf) {
		super.serialize(buf);
		int tmp = 0;
		tmp |= (this.datetime.getYear() - 2000) << 25;
		tmp |= (this.datetime.getMonthValue() & 0x0F) << 21;
		tmp |= (this.datetime.getDayOfMonth() & 0x1F) << 16;
		tmp |= (this.datetime.getHour() & 0x1F) << 11;
		tmp |= (this.datetime.getMinute() & 0x3F) << 5;
		tmp |= this.datetime.getSecond() & 0x1F;
		buf.putInt(tmp);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("type", getType())
				.add("size", getSize())
				.add("datetime", this.datetime)
				.toString();
	}
}
