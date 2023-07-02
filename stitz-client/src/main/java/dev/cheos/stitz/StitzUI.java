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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.lang3.tuple.Pair;

public class StitzUI {
	private static final Color BG_COLOR = new Color(0x313136);
	private static final JFrame frame = new JFrame();
	public static final int WIDTH = 300, HEIGHT = 350;
	
	public static void init(Component browserComponent) {
		JPanel contentPane;
		if (browserComponent instanceof JPanel panel) contentPane = panel;
		else {
			contentPane = new JPanel();
			contentPane.add(browserComponent);
		}
		frame.setContentPane(contentPane);
		frame.setTitle("StiTz client");
		frame.setResizable(false);
		frame.getContentPane().setBackground(BG_COLOR);
		frame.pack();
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationByPlatform(true);
		Pair<Integer, Integer> pos = Persistence.getWindowPos();
		if (pos.getLeft() != null && pos.getRight() != null)
			frame.setLocation(pos.getLeft(), pos.getRight());
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				StitzClient.exit(0);
			}
		});
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				Persistence.setWindowPos(frame.getX(), frame.getY());
			}
		});
		StitzClient.addShutdownHook(frame::dispose);
		frame.setVisible(true);
		frame.toFront();
	}
}
