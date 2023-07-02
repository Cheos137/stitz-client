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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.cheos.stitz.iax.frame.Frame;
import dev.cheos.stitz.iax.frame.FullFrameType;

public class IaxIOHandler implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(IaxIOHandler.class);
	private final IaxClient client;
	private final DatagramChannel channel;
	private boolean active = true;
	private final Thread thread;
	
	public IaxIOHandler(IaxClient client) throws IOException {
		this.client = client;
		this.channel = DatagramChannel.open();
		this.channel.connect(new InetSocketAddress(client.getConfig().remoteAddress(), client.getConfig().remotePort()));
		this.thread = new Thread(this::receive, "%d-receiver".formatted(client.getSrcCallNumber()));
		this.thread.setDaemon(true);
		this.thread.start();
	}
	
	public void send(Frame frame) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(frame.getSize());
		frame.serialize(buf);
		buf.flip();
		this.channel.write(buf);
	}
	
	@Override
	public void close() throws IOException {
		this.active = false;
		this.channel.close();
		this.thread.interrupt();
	}
	
	private void receive() {
		ByteBuffer buf = ByteBuffer.allocate(10240 /* 10KiB is way bigger than any typical IAX frame */);
		while (this.active) {
			try {
				buf.clear();
				if (this.channel.receive(buf) == null) return; // can't happen, we're in blocking mode
				buf.flip();
				short srcCallNumber = buf.getShort(0);
				Frame.Builder builder = Frame.builder().buffer(buf);
				
				if (srcCallNumber == 0) {
					LOGGER.warn("received meta frame -- not supported, discarding!");
				} else if ((srcCallNumber & 0x8000) != 0) {
					byte subclass = (byte) (buf.get(10) & 0x7F);
					FullFrameType type = FullFrameType.byId(subclass);
					if (type.canTransmit())
						this.client.handle(builder.of(type));
					else LOGGER.warn("received frame of invalid/unknown type: " + subclass + " / " + type);
				} else this.client.handle(builder.mini());
			} catch (ClosedChannelException e) { return; }
			catch (Exception e) { LOGGER.warn("Exception listening for incoming frames", e); }
		}
	}
}
