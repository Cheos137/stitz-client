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

import dev.cheos.stitz.iax.frame.Frame;
import dev.cheos.stitz.iax.frame.MediaFrame;

public interface IaxCallListener {
	default void onProceeding(IaxCall call) { }
	default void onRinging(IaxCall call) { }
	default void onAnswered(IaxCall call) { }
	default void onCongestion(IaxCall call) { }
	default void onHangup(IaxCall call) { }
	default void onBusy(IaxCall call) { }
	default void onRetransmitError(IaxCall call, Frame frame) { }
	
	interface AudioListener {
		default void onSetEnabled(boolean enabled) { }
		default void onAudioReceived(byte[] data, MediaFrame.Format format) { }
	}
}
