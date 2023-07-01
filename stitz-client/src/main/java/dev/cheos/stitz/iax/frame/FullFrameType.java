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

package dev.cheos.stitz.iax.frame;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

public enum FullFrameType {
	UNKNOWN(-1),
	DTMF,
	VOICE,
	VIDEO,
	CONTROL,
	NULL,
	IAXCONTROL,
	TEXT,
	IMAGE,
	HTML,
	COMFORTNOISE;
	
	private static final Map<Byte, FullFrameType> TYPES = Arrays.stream(values())
			.collect(ImmutableMap.toImmutableMap(FullFrameType::getId, Function.identity()));
	
	private final byte id;
	
	FullFrameType() {
		this.id = (byte) ordinal();
	}
	
	FullFrameType(int id) {
		this.id = (byte) id;
	}
	
	public byte getId() {
		return this.id;
	}
	
	public boolean canTransmit() {
		return this != UNKNOWN && this != NULL;
	}
	
	public static FullFrameType byId(byte id) {
		return TYPES.getOrDefault(id, UNKNOWN);
	}
}
