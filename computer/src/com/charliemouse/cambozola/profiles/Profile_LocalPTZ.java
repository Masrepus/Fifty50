package com.charliemouse.cambozola.profiles;

import com.charliemouse.cambozola.PercentArea;
import com.charliemouse.cambozola.ViewerAttributeInterface;

/**
 * * com/charliemouse/cambozola/profiles.Profile_LocalPTZ.java
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
public class Profile_LocalPTZ extends Profile_NonInteractive
{
    public Profile_LocalPTZ(ViewerAttributeInterface vai)
    {
        super(vai);
    }

    public String getDescription()
    {
        return "Client-side PanTiltZoom";
    }

    public boolean supportsPan()
    {
        return true;
    }

    public boolean supportsTilt()
    {
        return true;
    }

    public boolean supportsZoom()
    {
        return true;
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
        if (supportsPan()) {
            getViewerAttributes().getViewArea().panHorizontal(-1);
        }
    }

    public void panRight()
    {
        if (supportsPan()) {
            getViewerAttributes().getViewArea().panHorizontal(1);
        }
    }

    public void tiltUp()
    {
        if (supportsTilt()) {
            getViewerAttributes().getViewArea().panVertical(-1);
        }
    }

    public void tiltDown()
    {
        if (supportsTilt()) {
            getViewerAttributes().getViewArea().panVertical(1);
        }
    }

    public void homeView()
    {
        ViewerAttributeInterface vfi = getViewerAttributes();
        vfi.getViewArea().reset();
        vfi.repaint();
    }

    public void zoomTele()
    {
        if (supportsZoom()) {
            ViewerAttributeInterface vfi = getViewerAttributes();
            vfi.getViewArea().zoomIn();
            vfi.repaint();
        }
    }

    public void zoomWide()
    {
        if (supportsZoom()) {
            ViewerAttributeInterface vfi = getViewerAttributes();
            vfi.getViewArea().zoomOut();
            vfi.repaint();
        }
    }

    public void mouseClicked(int w, int h, int x, int y, boolean doubleClick)
    {
        moveToCenter(w, h, x, y);
    }

    public void moveToCenter(int w, int h, int x, int y)
    {
        if (supportsPan() || supportsTilt()) {
            ViewerAttributeInterface vfi = getViewerAttributes();
            PercentArea pa = vfi.getViewArea();
            //
            double newCentX = ((double) x / w) * 100.0;
            double newCentY = ((double) y / h) * 100.0;
            //
            pa.setBoundsAspect(newCentX, newCentY, pa.getWidth(), pa.getHeight());
        }
    }
}
