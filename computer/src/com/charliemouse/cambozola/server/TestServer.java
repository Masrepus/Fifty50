/**
 ** com/charliemouse/cambozola/TestServer.java
 **  Copyright (C) Andy Wilcock, 2001.
 **  Available from http://www.charliemouse.com
 **
 **  Cambozola is free software; you can redistribute it and/or modify
 **  it under the terms of the GNU General Public License as published by
 **  the Free Software Foundation; either version 2 of the License, or
 **  (at your option) any later version.
 **
 **  Cambozola is distributed in the hope that it will be useful,
 **  but WITHOUT ANY WARRANTY; without even the implied warranty of
 **  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **  GNU General Public License for more details.
 **
 **  You should have received a copy of the GNU General Public License
 **  along with Cambozola; if not, write to the Free Software
 **  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 **/
package com.charliemouse.cambozola.server;

import com.charliemouse.cambozola.shared.AppID;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {
	public final static int DEFAULT_PORT = 2020;
	protected int m_port;
	protected ServerSocket m_listen_socket;


	public static void main(String args[])
	{
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
        if (args.length > 1) {
            String rf = args[1];
        }
		new TestServer(port);
	}


	public TestServer(int port)
	{
		m_port = port;
		try {
			AppID m_props = AppID.getAppID();
			m_listen_socket = new ServerSocket(port);
            System.err.println("// " + m_props.getAppName() + "Test Server V" + m_props.getFullVersion() + " " + m_props.getCopyright());
            System.err.println("// Build date: " + m_props.getBuildDate());
            System.err.println("// Available from " + m_props.getLocURL());

			System.out.println("// listening on port " + port);
			while (true) {
				Socket client_socket = m_listen_socket.accept();
    			new StandardImageConnection(client_socket);
			}
		} catch (Exception e) {
			System.err.println("Exception occurred: " + e);
		}
	}
}

class StandardImageConnection extends Thread {
	private static final String BOUNDARY = "arflebarfle";

	private String[] imageset;
	private Socket client;
	private DataInputStream in;
	private DataOutputStream out;


	public StandardImageConnection(Socket client_socket)
	{
		client = client_socket;
		try {
			in = new DataInputStream(client.getInputStream());
			out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
		} catch (IOException ie) {
			System.err.println("Unable to start new connection: " + ie);
			try {
				client.close();
			} catch (IOException ie2) {
			}
			return;
		}
		//
		// Create the image list.
		//
		imageset = new String[]{"imagea", "imageb",
		                        "imagec", "imaged",
		                        "imagee", "imagef",
		                        "imageg", "imageh",
		                        "imagei", "imagej",
		                        "imagei", "imageh",
		                        "imageg", "imagef",
		                        "imagee", "imaged",
		                        "imagec", "imageb"};
		this.start();
	}


    public void run()
	{
		try {
			out.writeBytes("HTTP/1.0 200 OK\r\n");
			out.writeBytes("Server: Cambozola test server\r\n");
			out.writeBytes("Content-Type: multipart/x-mixed-replace;boundary=" + BOUNDARY + "\r\n");
			out.writeBytes("\r\n");
			out.writeBytes("--" + BOUNDARY + "\n");

			int bytesRead;
			byte[] barr = new byte[1024];
			while (true) {
				for (int i = 0; i < imageset.length; i++) {
					out.writeBytes("Content-type: image/gif\n\n");

					String fname = new String("testImages/" + imageset[i] + ".gif");
					DataInputStream fis = new DataInputStream(new BufferedInputStream(this.getClass().getResourceAsStream(fname)));
					while ((bytesRead = fis.read(barr)) != -1) {
						out.write(barr, 0, bytesRead);
					}
					fis.close();
//                    out.writeBytes("\n");
					out.writeBytes("--" + BOUNDARY + "\n");
					out.flush();
					Thread.sleep(500);
				}
			}
		} catch (Exception ie) {
			try {
				in.close();
				out.close();
				client.close();
			} catch (IOException ie2) {
			}
		}
	}
}
