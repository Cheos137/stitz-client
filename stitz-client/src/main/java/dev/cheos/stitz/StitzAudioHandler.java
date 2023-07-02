/*
 * Copyright (c) 2023 Cheos
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.cheos.stitz;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import dev.cheos.stitz.gsm.GSMDecoder;
import dev.cheos.stitz.iax.IaxCallListener;
import dev.cheos.stitz.iax.frame.MediaFrame.Format;

public class StitzAudioHandler implements AutoCloseable { // TODO allow for custom ringtones
	private static final Logger LOGGER = LoggerFactory.getLogger(StitzAudioHandler.class);
	private static final AudioFormat DEF_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 1, 2, 44100f, true);
	private static final AudioFormat IAX_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000f, 16, 1, 2, 8000f, true);
	private static final List<String> DEFAULT_SFX_IDS = ImmutableList.of("hangup", "mute", "unmute", "deafen", "undeafen", "accept_incoming", "cancel_incoming", "call_incoming", "call_incoming_rare", "call_outgoing");
	private static final Random RANDOM = new Random();
	private final Listener listener = new Listener();
	private List<Mixer.Info> audioIn, audioOut, clipOut;
	private Mixer.Info inMixerInfo, outMixerInfo, clipMixerInfo;
	private Mixer inMixer, outMixer, clipMixer;
	private TargetDataLine inLine;
	private SourceDataLine outLine;
	private AudioInputStream inStream;
	private Map<String, Clip> sfxClips = ImmutableMap.of();
	private boolean muted, deafened, wasMuted;
	private int inVolume, outVolume, clipVolume;
	private float /*inGain,*/ outGain, clipGain;
	
	public StitzAudioHandler() {
		refreshDeviceLists();
		
		this.inVolume = Persistence.getInputVolume();
		this.outVolume = Persistence.getOutputVolume();
		this.clipVolume = Persistence.getClipVolume();
//		this.inGain = linearToDB(this.inVolume * 0.0001f);
		this.outGain = linearToDB(this.outVolume * 0.0001f);
		this.clipGain = linearToDB(this.clipVolume * 0.0001f);
		onInDeviceChanged(this.audioIn.get(Persistence.getAudioInput(this)));
		onOutDeviceChanged(this.audioOut.get(Persistence.getAudioOutput(this)));
		onClipDeviceChanged(this.clipOut.get(Persistence.getClipOutput(this)));
	}
	
	public void setIn(int idx) {
		if (this.audioIn.get(idx).equals(this.inMixerInfo))
			return;
		Persistence.setAudioInput(idx, this);
		onInDeviceChanged(this.audioIn.get(idx));
	}
	
	public int getIn() {
		return this.audioIn.indexOf(this.inMixerInfo);
	}
	
	public List<Mixer.Info> listIn() {
		return this.audioIn;
	}
	
	public void setInVolume(int volume) {
		Persistence.setInputVolume(volume);
		this.inVolume = volume;
//		this.inGain = linearToDB(volume * 0.0001f);
	}
	
	public int getInVolume() {
		return this.inVolume;
	}
	
	public void setOut(int idx) {
		if (this.audioOut.get(idx).equals(this.outMixerInfo))
			return;
		Persistence.setAudioOutput(idx, this);
		onOutDeviceChanged(this.audioOut.get(idx));
	}
	
	public int getOut() {
		return this.audioOut.indexOf(this.outMixerInfo);
	}
	
	public List<Mixer.Info> listOut() {
		return this.audioOut;
	}
	
	public void setOutVolume(int volume) {
		Persistence.setOutputVolume(volume);
		this.outVolume = volume;
		this.outGain = linearToDB(volume * 0.0001f);
		if (this.outLine != null) {
			FloatControl gain = (FloatControl) this.outLine.getControl(FloatControl.Type.MASTER_GAIN);
			gain.setValue(this.outGain);
		}
	}
	
	public int getOutVolume() {
		return this.outVolume;
	}
	
	public void setClip(int idx) {
		if (this.clipOut.get(idx).equals(this.clipMixerInfo))
			return;
		Persistence.setClipOutput(idx, this);
		onClipDeviceChanged(this.clipOut.get(idx));
	}
	
	public int getClip() {
		return this.clipOut.indexOf(this.clipMixerInfo);
	}
	
	public List<Mixer.Info> listClip() {
		return this.clipOut;
	}
	
	public void setClipVolume(int volume) {
		Persistence.setClipVolume(volume);
		this.clipVolume = volume;
		this.clipGain = linearToDB(volume * 0.0001f);
		this.sfxClips.values().forEach(clip -> {
			FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gain.setValue(this.clipGain);
		});
	}
	
	public int getClipVolume() {
		return this.clipVolume;
	}
	
	public void setMute(boolean mute, boolean silent) {
		if (this.muted == mute) return;
		this.muted = mute;
		
		if (this.deafened && !mute) setDeafen(false, false);
		else if (!silent) playSound(mute ? "mute" : "unmute");
	}
	
	public boolean getMute() {
		return this.muted;
	}
	
	public void setDeafen(boolean deafen, boolean silent) {
		if (this.deafened == deafen) return;
		this.deafened = deafen;
		
		if (deafen) {
			if (!silent) playSound("deafen");
			this.wasMuted = this.muted;
			setMute(true, true);
		} else {
			if (!silent) playSound("undeafen");
			if (this.wasMuted)
				this.wasMuted = false;
			else setMute(false, true);
		}
		if (this.outLine != null)
			synchronized (this.outLine) {
				BooleanControl ctrl = (BooleanControl) this.outLine.getControl(BooleanControl.Type.MUTE);
				ctrl.setValue(deafen);
			}
	}
	
	public boolean getDeafen() {
		return this.deafened;
	}
	
	public void refreshDeviceLists() {
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		List<Mixer.Info> audioIn = new LinkedList<>();
		List<Mixer.Info> audioOut = new LinkedList<>();
		List<Mixer.Info> clipOut = new LinkedList<>();
		
		for (Mixer.Info mixerInfo : mixerInfos) {
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			for (Line.Info lineInfo : mixer.getTargetLineInfo())
				if (lineInfo.getLineClass() == TargetDataLine.class) {
					audioIn.add(mixerInfo);
					break;
				}
			for (Line.Info lineInfo : mixer.getSourceLineInfo())
				if (lineInfo.getLineClass() == SourceDataLine.class) {
					audioOut.add(mixerInfo);
					break;
				}
			for (Line.Info lineInfo : mixer.getSourceLineInfo())
				if (lineInfo.getLineClass() == Clip.class) {
					clipOut.add(mixerInfo);
					break;
				}
		}
		
		if (!audioIn.equals(this.audioIn))
			this.audioIn = ImmutableList.copyOf(audioIn);
		if (!audioOut.equals(this.audioOut))
			this.audioOut = ImmutableList.copyOf(audioOut);
		if (!clipOut.equals(this.clipOut))
			this.clipOut = ImmutableList.copyOf(clipOut);
	}
	
	public IaxCallListener.AudioListener getListener() {
		return this.listener;
	}
	
	private Clip getSound(String id) {
		Clip clip = this.sfxClips.get(id);
		if (clip == null) LOGGER.warn("Unknown sound effect {}", id);
		return clip;
	}
	
	public void playSound(String id) {
		Clip clip = getSound(id);
		if (clip == null) return;
		clip.stop();
		clip.setMicrosecondPosition(0);
		clip.start();
	}
	
	public void loopSound(String id) {
		if ("call_incoming".equals(id) && RANDOM.nextInt(10_000) == 0)
			id = "call_incoming_rare";
		Clip clip = getSound(id);
		if (clip == null) return;
		clip.setMicrosecondPosition(0);
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}
	
	public void cancelLoopSound(String id) {
		if ("call_incoming".equals(id))
			cancelLoopSound("call_incoming_rare");
		Clip clip = getSound(id);
		if (clip == null) return;
		clip.loop(0);
		clip.stop();
	}
	
	public int readMic(byte[] buf, short[] sbuf) {
		try {
			if (this.inStream == null || buf.length != 2 * sbuf.length) {
				Arrays.fill(buf, (byte) 0);
				Arrays.fill(sbuf, (short) 0);
				return buf.length;
			}
			
			int read = this.inStream.read(buf);
			
			if (this.muted) { // if muted, pretend like we read data and return
				Arrays.fill(buf, (byte) 0);
				Arrays.fill(sbuf, (short) 0);
				return buf.length;
			}
			
			scaleAudio(buf, sbuf, 0, read, this.inVolume);
			return read;
		} catch (IOException e) {
			LOGGER.error("Exception reading audio input", e);
			return -1;
		}
	}
	
	public void flushMic() {
		if (this.inLine != null)
			this.inLine.flush();
	}
	
	@Override
	public void close() {
		if (this.inLine != null) {
			this.inLine.stop();
			this.inLine.flush();
			this.inLine.close();
		}
		
		if (this.outLine != null) {
			this.outLine.stop();
			this.outLine.close();
		}
		
		for (Clip clip : this.sfxClips.values()) {
			clip.stop();
			clip.close();
		}
	}
	
	private void scaleAudio(byte[] buf, short[] sbuf, int start, int end, int volume) { // hardcoded for frameSize = 2 (bytes), mono, big endian, PCM_SIGNED
		if (volume == 10000) { // skip scaling if scale = 1, but fill short buffer
			for (int i = start; i < end; i += 2)
				sbuf[i>>1] = (short) (((buf[i] << 8) & 0xFF00) | (buf[i+1] & 0xFF));
			return;
		}
		for (int i = start; i < end; i += 2) {
			short value = (short) (((buf[i] << 8) & 0xFF00) | (buf[i+1] & 0xFF));
			float fvalue = value * 0.0001f * volume;
			value = (short) Math.round(fvalue);
			sbuf[i>>1] = (short) (value & 0xFFFF);
			buf[i] = (byte) ((value >> 8) & 0xFF);
			buf[i+1] = (byte) (value & 0xFF);
		}
	}
	
	private void onInDeviceChanged(Mixer.Info newInfo) {
		if (!listIn().contains(newInfo))
			newInfo = listIn().get(0);
		if (this.inMixerInfo == newInfo || newInfo.equals(this.inMixerInfo))
			return;
		
		this.inMixerInfo = newInfo;
		this.inMixer = AudioSystem.getMixer(newInfo);
		
		DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, DEF_FORMAT);
		try {
			TargetDataLine oldLine = this.inLine;
			TargetDataLine line = (TargetDataLine) this.inMixer.getLine(lineInfo);
			AudioInputStream oldStream = this.inStream;
			synchronized (line) {
				if (oldLine != null)
					oldLine.drain();
				this.inLine = line;
				line.open();
				line.start();
				this.inStream = AudioSystem.getAudioInputStream(IAX_FORMAT, new AudioInputStream(line));
			}
			
			if (oldLine != null) {
				oldLine.stop();
				oldLine.close();
			}
			if (oldStream != null)
				oldStream.close();
		} catch (LineUnavailableException e) {
			LOGGER.error("Exception opening input line", e);
			StitzClient.showError("Error configuring voice audio input device, please restart the application.");
		} catch (IOException e) {
			LOGGER.warn("Exception closing old audio input stream", e);
		}
	}
	
	private void onOutDeviceChanged(Mixer.Info newInfo) {
		if (!listOut().contains(newInfo))
			newInfo = listOut().get(0);
		if (this.outMixerInfo == newInfo || newInfo.equals(this.outMixerInfo))
			return;
		
		this.outMixerInfo = newInfo;
		this.outMixer = AudioSystem.getMixer(newInfo);
		
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, IAX_FORMAT);
		try {
			SourceDataLine oldLine = this.outLine;
			SourceDataLine line = (SourceDataLine) this.outMixer.getLine(lineInfo);
			synchronized (line) {
				this.outLine = line;
				line.open();
				FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				gain.setValue(this.outGain);
				if (this.listener.enabled)
					line.start();
			}
			
			if (oldLine != null) {
				oldLine.drain();
				oldLine.stop();
				oldLine.close();
			}
		} catch (LineUnavailableException e) {
			LOGGER.error("Exception opening output line", e);
			StitzClient.showError("Error configuring voice audio output device, please restart the application.");
		}
	}
	
	private void onClipDeviceChanged(Mixer.Info newInfo) {
		if (!listClip().contains(newInfo))
			newInfo = listClip().get(0);
		if (this.clipMixerInfo == newInfo || newInfo.equals(this.clipMixerInfo))
			return;
		
		this.clipMixerInfo = newInfo;
		this.clipMixer = AudioSystem.getMixer(newInfo);
		
		ImmutableMap.Builder<String, Clip> sfxClipBuilder = ImmutableMap.builder();
		DEFAULT_SFX_IDS.forEach(s -> sfxClipBuilder.put(s, loadClip(s)));
		/*
		 * +mute
		 * +unmute
		 * +deafen
		 * +undeafen
		 * +hangup
		 * +call_outgoing
		 * +call_incoming
		 * +cancel_incoming
		 * +accept_incoming
		 */
		
		Map<String, Clip> sfxClips = sfxClipBuilder.build();
		
		sfxClips.values().forEach(clip -> {
			clip.setLoopPoints(0, -1);
			FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gain.setValue(this.clipGain);
		});
		
		synchronized (sfxClips) {
			Map<String, Clip> oldSfxClips = this.sfxClips;
			for (Clip clip : oldSfxClips.values()) {
				clip.stop();
				clip.close();
			}
			this.sfxClips = sfxClips;
		}
	}
	
	private Clip loadClip(String name) {
		try {
			Clip clip = (Clip) this.clipMixer.getLine(new DataLine.Info(Clip.class, DEF_FORMAT));
			InputStream is = getClass().getResourceAsStream("/sfx/%s.wav".formatted(name));
			clip.open(AudioSystem.getAudioInputStream(is));
			return clip;
		} catch (Exception e) {
			LOGGER.error("Exception loading clip " + name, e);
			return null;
		}
	}
	
	public static int findDevice(List<Mixer.Info> available, int expectedIdx, String desc) {
		if (expectedIdx < 0) return 0; // default on invalid index
		if (available.size() > expectedIdx && available.get(expectedIdx).getName().equals(desc))
			return expectedIdx; // element is located where expected
		for (int i = 0; i < available.size(); i++) // or else search for element
			if (i == expectedIdx) continue;
			else if (available.get(i).getName().equals(desc))
				return i;
		return 0; // element not found => default to system default
	}
	
	private static float linearToDB(float linear) {
		return (float) (20 * Math.log10(linear == 0 ? 0.0001 : linear));
	}
	
	
	private final class Listener implements IaxCallListener.AudioListener {
		private final GSMDecoder decoder = new GSMDecoder();
		private final byte[] buf = new byte[320];
		private boolean enabled, inactive;
		
		@Override
		public void onSetEnabled(boolean enabled) {
			if (this.inactive) return;
			synchronized (StitzAudioHandler.this.outLine) {
				if (this.enabled = enabled)
					StitzAudioHandler.this.outLine.start();
				else {
					StitzAudioHandler.this.outLine.stop();
					StitzAudioHandler.this.outLine.flush(); // discard remaining buffer
				}
			}
		}
		
		@Override
		public void onAudioReceived(byte[] data, Format format) {
			if (!this.enabled || this.inactive) return;
			synchronized (StitzAudioHandler.this.outLine) {
				try {
					this.decoder.decode(data, 0, this.buf, 0, true);
					StitzAudioHandler.this.outLine.write(this.buf, 0, this.buf.length);
				} catch (Exception e) {
					LOGGER.error("Exception decoding audio data", e);
				}
			}
		}
	}
}
