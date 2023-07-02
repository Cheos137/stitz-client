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

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Persistence {
	private static final Logger LOGGER = LoggerFactory.getLogger(Persistence.class);
	private static final Path CONFIG_FILE = StitzClient.DATA_DIR.resolve("config.properties");
	private static final Properties PROPS = new Properties();
	
	public static void init() throws IOException {
		if (Files.notExists(CONFIG_FILE))
			Files.createFile(CONFIG_FILE);
		load();
		save();
	}
	
	public static void setUsername(String username) {
		load();
		setProperty("username", username);
		save();
	}
	
	public static String getUsername() {
		load();
		return getProperty("username");
	}
	
	public static void setPassword(String password) {
		load();
		setProperty("password", password);
		save();
	}
	
	public static String getPassword() {
		load();
		return getProperty("password");
	}
	
	public static void setDisplayName(String displayName) {
		load();
		setProperty("displayName", displayName);
		save();
	}
	
	public static String getDisplayName() {
		load();
		return getProperty("displayName");
	}
	
	public static void setAudioInput(int idx, StitzAudioHandler handler) {
		load();
		setProperty("audio.in.idx", idx);
		setProperty("audio.in.desc", handler.listIn().get(idx).getName());
		save();
	}
	
	public static int getAudioInput(StitzAudioHandler handler) {
		load();
		int storedIdx = defaulted(() -> getProperty("audio.in.idx", Integer::parseInt), () -> -1);
		int idx = StitzAudioHandler.findDevice(handler.listIn(), storedIdx, getProperty("audio.in.desc"));
		if (storedIdx != idx) setAudioInput(idx, handler);
		return idx;
	}
	
	public static void setAudioOutput(int idx, StitzAudioHandler handler) {
		load();
		setProperty("audio.out.idx", idx);
		setProperty("audio.out.desc", handler.listOut().get(idx).getName());
		save();
	}
	
	public static int getAudioOutput(StitzAudioHandler handler) {
		load();
		int storedIdx = defaulted(() -> getProperty("audio.out.idx", Integer::parseInt), () -> -1);
		int idx = StitzAudioHandler.findDevice(handler.listOut(), storedIdx, getProperty("audio.out.desc"));
		if (storedIdx != idx) setAudioOutput(idx, handler);
		return idx;
	}
	
	public static void setClipOutput(int idx, StitzAudioHandler handler) {
		load();
		setProperty("audio.clip.idx", idx);
		setProperty("audio.clip.desc", handler.listClip().get(idx).getName());
		save();
	}
	
	public static int getClipOutput(StitzAudioHandler handler) {
		load();
		int storedIdx = defaulted(() -> getProperty("audio.clip.idx", Integer::parseInt), () -> -1);
		int idx = StitzAudioHandler.findDevice(handler.listOut(), storedIdx, getProperty("audio.clip.desc"));
		if (storedIdx != idx) setClipOutput(idx, handler);
		return idx;
	}
	
	public static void setInputVolume(int volume) {
		load();
		setProperty("audio.in.volume", volume);
		save();
	}
	
	public static int getInputVolume() {
		load();
		return defaulted(() -> getProperty("audio.in.volume", Integer::parseInt), () -> 10000);
	}
	
	public static void setOutputVolume(int volume) {
		load();
		setProperty("audio.out.volume", volume);
		save();
	}
	
	public static int getOutputVolume() {
		load();
		return defaulted(() -> getProperty("audio.out.volume", Integer::parseInt), () -> 10000);
	}
	
	public static void setClipVolume(int volume) {
		load();
		setProperty("audio.clip.volume", volume);
		save();
	}
	
	public static int getClipVolume() {
		load();
		return defaulted(() -> getProperty("audio.clip.volume", Integer::parseInt), () -> 10000);
	}
	
	public static void setWindowPos(int x, int y) {
		load();
		setProperty("window.x", x);
		setProperty("window.y", y);
		save();
	}
	
	public static Pair<Integer, Integer> getWindowPos() {
		load();
		return Pair.of(
				defaulted(() -> getProperty("window.x", Integer::parseInt), () -> null),
				defaulted(() -> getProperty("window.y", Integer::parseInt), () -> null));
	}
	
	
	public static void load() {
		try (InputStream is = Files.newInputStream(CONFIG_FILE, StandardOpenOption.READ)) {
			PROPS.load(is);
		} catch (IOException e) {
			LOGGER.error("error loading config", e);
		}
	}
	
	public static void save() {
		try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
			PROPS.store(os, "StiTz client configuration, not meant for manual editing!\n#This file is not guaranteed to automatically reload!");
		} catch (IOException e) {
			LOGGER.error("error saving config", e);
		}
	}
	
	private static <T> void setProperty(String key, T value) {
		if (value == null) PROPS.remove(key);
		else PROPS.setProperty(key, String.valueOf(value));
	}
	
	private static String getProperty(String key) {
		return PROPS.getProperty(key);
	}
	
	private static <T> T getProperty(String key, Function<String, T> mapper) {
		String value = getProperty(key);
		return value == null ? null : mapper.apply(value);
	}
	
	public static <T> T defaulted(Supplier<T> fn, Supplier<T> def) {
		T val = fn.get();
		return val == null ? def.get() : val;
	}
}
