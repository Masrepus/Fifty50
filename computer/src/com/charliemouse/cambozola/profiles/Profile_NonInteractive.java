package com.charliemouse.cambozola.profiles;

import com.charliemouse.cambozola.ViewerAttributeInterface;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * * com/charliemouse/cambozola/profiles.Profile_NonInteractive.java
 * *  Copyright (C) Andy Wilcock, 2001.
 * *  Available from http://www.charliemouse.com
 * *
 * *  Cambozola is free software; you can redistribute it and/or modify
 * *  it under the terms of the GNU General Public License as published by
 * *  the Free Software Foundation; either version 2 of the License, or
 * *  (at your option) any later version.
 * *
 * *  Cambozola is distributed in the hope that it will be useful,
 * *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * *  GNU General Public License for more details.
 * *
 * *  You should have received a copy of the GNU General Public License
 * *  along with Cambozola; if not, write to the Free Software
 * *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
public class Profile_NonInteractive implements ICameraProfile
{
    public ViewerAttributeInterface m_viewerAttribs = null;

    public Profile_NonInteractive(ViewerAttributeInterface vai)
    {
        m_viewerAttribs = vai;
    }

    public String getDescription()
    {
        return "Non-interactive view";
    }

    public void setViewerAttributes(ViewerAttributeInterface v)
    {
        m_viewerAttribs = v;
    }

    public ViewerAttributeInterface getViewerAttributes()
    {
        return m_viewerAttribs;
    }

    public boolean supportsPan()
    {
        return false;
    }

    public boolean supportsTilt()
    {
        return false;
    }

    public boolean supportsZoom()
    {
        return false;
    }

    public boolean supportsFocus()
    {
        return false;
    }

    public boolean supportsBrightness()
    {
        return false;
    }

    public void panLeft()
    {
    }

    public void panRight()
    {
    }

    public void tiltUp()
    {
    }

    public void tiltDown()
    {
    }

    public void homeView()
    {
    }

    public void moveToCenter(int w, int h, int x, int y)
    {
    }

    public void focusNear()
    {
    }

    public void focusFar()
    {
    }

    public void focusAuto()
    {
    }

    public void zoomTele()
    {
    }

    public void zoomWide()
    {
    }

    public void darker()
    {
    }

    public void brighter()
    {
    }

    public void standardBrightness()
    {
    }

    public void mouseClicked(int w, int h, int x, int y, boolean doubleClick)
    {
    }

    protected void execute(String path)
    {
        try {
            URL camStream = getViewerAttributes().getStream().getStreamURL();
            URL u2 = new URL(camStream.getProtocol(), camStream.getHost(), camStream.getPort(), path, null);
            URLConnection uconn = u2.openConnection();
            int unused = uconn.getContentLength();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}