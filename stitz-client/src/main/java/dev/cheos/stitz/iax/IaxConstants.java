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

package dev.cheos.stitz.iax;

// RFC ref: https://datatracker.ietf.org/doc/html/rfc5456
public class IaxConstants {
	public static final short CLIENT_MAX_SOURCE_CALL_NUMBER = 1000; // equals baseline call source number - 1
	public static final short CLIENT_REGISTRATION_REFRESH = 60; // s
	public static final long REGISTRATION_REJECTED_MAX_RETRY_COUNT = 1;
	public static final long REGISTRATION_REJECTED_RETRY_INTERVAL = 10; // s
	public static final long REGISTRATION_AUTH_MAX_RETRY_COUNT = 10;
	public static final long TRANSMISSION_RETRY_TIMEOUT = 10000; // ms
	public static final long TRANSMISSION_MAX_RETRY_COUNT = 4;
	public static final long FRAME_RETRANSMIT_INTERVAL = 1000; // ms
	public static final long CALL_RETRANSMIT_INTERVAL = 1000; // ms
	public static final long CALL_PING_INTERVAL = 20000; // ms
}
