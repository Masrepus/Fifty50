/**
 ** com/charliemouse/cambozola/shared/CamStream.java
 **  Copyright (C) Andy Wilcock, 2001.
 **  Available from http://www.charliemouse.com
 **
 ** This file is part of the Cambozola package (c) Andy Wilcock, 2001.
 ** Available from http://www.charliemouse.com
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
package com.charliemouse.cambozola.shared;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class CamStream extends Thread {
    public static final int CONNECT_STYLE_SOCKET = 1;
    public static final int CONNECT_STYLE_HTTP   = 2;


	private static final int IMG_FLUFF_FACTOR = 1;

    private ExceptionReporter m_reporter = null;
	private Vector m_listeners;
	private Image m_imain = null;
	private Toolkit m_tk;
	private URL m_stream;
    private String m_userpassEncoded;
	private URL m_docBase;
	private DataInputStream m_inputStream = null;
	private boolean m_isDefunct = false;
	private boolean m_collecting = false;
	private byte[] m_rawImage;
	private String m_imageType = "image/jpeg";
	private long m_startTime = 0;
	private int m_imgidx = 0;
	private int m_retryCount = 1;
	private int m_retryDelay = 1000;
	private String m_appName = "";
    private boolean m_debug = true;

    public CamStream(URL strm, String app, URL docBase, int retryCount, int retryDelay, ExceptionReporter reporter, boolean debug)
	{
		m_tk = Toolkit.getDefaultToolkit();
		m_listeners = new Vector();
        //
        // Pull open stream - look for user/password.
        //
        m_stream = strm;
        String userPass = strm.getUserInfo();
        //
        // Encode if needed...
        //
        m_userpassEncoded = null;
        if (userPass != null && userPass.length() > 0)
        {
            m_userpassEncoded = Base64.encode(userPass.getBytes());
        }
        m_appName = app;
        m_reporter = reporter;
		m_isDefunct = false;
		m_docBase = docBase;
		m_retryCount = retryCount;
		m_retryDelay = retryDelay;
        m_debug = debug;
	}


	public synchronized Image getCurrent()
	{
		return m_imain;
	}


	public synchronized final byte[] getRawImage()
	{
		return m_rawImage;
	}


	public synchronized int getIndex()
	{
		return m_imgidx;
	}


	public synchronized String getType()
	{
		return m_imageType;
	}


	public URL getStreamURL()
	{
		return m_stream;
	}


	public double getFPS()
	{
		if (m_startTime == 0) {
			return 0.0;
		}
		long currTime = System.currentTimeMillis();
		return (1000.0 * (m_imgidx - IMG_FLUFF_FACTOR)) / (currTime - m_startTime);
	}


	public void addImageChangeListener(ImageChangeListener cl)
	{
		m_listeners.addElement(cl);
	}


	public void removeImageChangeListener(ImageChangeListener cl)
	{
		m_listeners.removeElement(cl);
	}


	private void fireImageChange()
	{
		ImageChangeEvent ce = new ImageChangeEvent(this);
		for (Enumeration e = m_listeners.elements(); e.hasMoreElements();) {
			((ImageChangeListener) e.nextElement()).imageChanged(ce);
		}
	}


	public void run()
	{
		StreamSplit ssplit;
		try {
            //
			// Loop for a while until we either give up (hit m_retryCount), or
			// get a connection.... Sleep inbetween.
			//
			String connectionError;
			String ctype;
			Hashtable headers;
			int tryIndex = 0;
			int retryCount = m_retryCount;
			int retryDelay = m_retryDelay;
			//
			do {
				//
				// Keep track of how many times we tried.
				//
				tryIndex++;

                if (m_debug)
                {
                    System.err.println("// Connection URL = " + m_stream);
                }
                //
                // Better method - access via URL Connection
                //
                URLConnection conn = m_stream.openConnection();
                if (m_docBase != null) {
                    conn.setRequestProperty("Referer", m_docBase.toString());
                }
                conn.setRequestProperty("User-Agent", m_appName);
                conn.setRequestProperty("Host", m_stream.getHost());
                if (m_userpassEncoded != null) {
                    conn.setRequestProperty("Authorization", "Basic " + m_userpassEncoded);
                }
                m_inputStream = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
                //
                // Read Headers for the main thing...
                //
                headers = StreamSplit.readHeaders(conn);
                ssplit = new StreamSplit(m_inputStream);
                //
                if (m_debug)
                {
                    System.err.println("// Request sent; Main Response headers:");
                    for (Enumeration enm = headers.keys();enm.hasMoreElements();)
                    {
                        String hkey = (String)enm.nextElement();
                        System.err.println("//   " + hkey + " = " + headers.get(hkey));
                    }
                }
                //
				m_collecting = true;
				//
				// Work out the content type/boundary.
				//
				connectionError = null;
				ctype = (String) headers.get("content-type");
				if (ctype == null) {
					connectionError = "No main content type";
				} else if (ctype.indexOf("text") != -1) {
					String response;
                    //noinspection deprecation
                    while ((response = m_inputStream.readLine()) != null) {
						System.out.println(response);
					}
					connectionError = "Failed to connect to server (denied?)";
				}
				if (connectionError == null)
				{
					break; // Yay!! got one.
				} else if (m_isDefunct) {
					//
					// Not wanted any more...
					//
					return;
				} else {
					//
					// Wait a while before retrying...
					//
                    if (m_debug)
                    {
                        System.err.println("// Waiting for " + retryDelay + " ms");
                    }
					m_reporter.reportFailure(connectionError);
					sleep(retryDelay);
				}
			} while (tryIndex < retryCount);
			//
			if (connectionError != null)
			{
				return;
			}
			//
			// Boundary will always be something - '--' or '--foobar'
            //
			int bidx = ctype.indexOf("boundary=");
			String boundary = StreamSplit.BOUNDARY_MARKER_PREFIX;
			if (bidx != -1) {
				boundary = ctype.substring(bidx + 9);
				ctype = ctype.substring(0, bidx);
                //
                // Remove quotes around boundary string [if present]
                //
                if (boundary.startsWith("\"") && boundary.endsWith("\""))
                {
                    boundary = boundary.substring(1, boundary.length()-1);
                }
                if (!boundary.startsWith(StreamSplit.BOUNDARY_MARKER_PREFIX)) {
					boundary = StreamSplit.BOUNDARY_MARKER_PREFIX + boundary;
				}
			}
			//
			// Now if we have a boundary, read up to that.
			//
			if (ctype.startsWith("multipart/x-mixed-replace")) {
                if (m_debug)
                {
                    System.err.println("// Reading up to boundary");
                }
				ssplit.skipToBoundary(boundary);
			}

			do {
				if (m_collecting) {
					//
					// Now we have the real type...
					//  More headers (for the part), then the object...
					//
					if (boundary != null) {
						headers = ssplit.readHeaders();
                        if (m_debug) {
                            System.err.println("// Chunk Headers recieved:");
                            for (Enumeration enm = headers.keys();enm.hasMoreElements();)
                            {
                                String hkey = (String)enm.nextElement();
                                System.err.println("//   " + hkey + " = " + headers.get(hkey));
                            }
                        }
						//
						// Are we at the end of the m_stream?
						//
						if (ssplit.isAtStreamEnd()) {
							break;
						}
						ctype = (String) headers.get("content-type");
						if (ctype == null) {
							throw new Exception("No part content type");
						}
					}
					//
					// Mixed Type -> just skip...
					//
					if (ctype.startsWith("multipart/x-mixed-replace")) {
						//
						// Skip
						//
						bidx = ctype.indexOf("boundary=");
						boundary = ctype.substring(bidx + 9);
						//
                        if (m_debug) {
                            System.err.println("// Skipping to boundary");
                        }
						ssplit.skipToBoundary(boundary);
					} else {
						//
						// Something we want to keep...
						//
                        if (m_debug) {
                            System.err.println("// Reading to boundary");
                        }
						byte[] img = ssplit.readToBoundary(boundary);
						if (img.length == 0) {
							break;
						}
						//
						// FPS counter.
						//
						if (m_imgidx > IMG_FLUFF_FACTOR && m_startTime == 0) {
							m_startTime = System.currentTimeMillis();
						}
						//
						// Update the image [fores events off]
						//
						updateImage(ctype, img);
					}
				}
				try {
					Thread.sleep(20);
				} catch (InterruptedException ignored) {
				}
			} while (!m_isDefunct);
		} catch (Exception e) {
            if (!m_collecting)
            {
                m_reporter.reportFailure(e.toString());
            } else if (!m_isDefunct) {
				m_reporter.reportError(e);
			}
		} finally {
			unhook();
		}
		//
		// At this point, the m_stream m_inputStream done
		// [could dispplay a that's all folks - leaving it as it m_inputStream
		//  will leave the last frame up]
		//
	}

	private synchronized void updateImage(String ctype, byte[] img)
	{
		//
		// Update our image...
		//
        m_imageType = ctype;
        m_imain = m_tk.createImage(img);
        m_rawImage = img;
        m_imgidx++;

        m_imain.getWidth(new ImageObserver()
        {
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
            {
                boolean fully = ((infoflags & (ImageObserver.ALLBITS | ImageObserver.PROPERTIES)) != 0);
                if (fully) {
                    fireImageChange();
                }
                return !fully;
            }
        });
	}

	public void finalize() throws Throwable
	{
		unhook();
        super.finalize();
	}


	public void unhook()
	{
		m_collecting = false;
		m_isDefunct = true;
		try {
			if (m_inputStream != null)
			{
				m_inputStream.close();
			}
			m_inputStream = null;
		} catch (Exception ignored) {
		}
	}
}