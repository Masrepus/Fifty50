package com.charliemouse.cambozola.profiles;

import com.charliemouse.cambozola.ViewerAttributeInterface;

/**
 * * com/charliemouse/cambozola/profiles.Profile_Panasonic_BL30.java
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
public class Profile_Panasonic_BLC30 extends Profile_LocalPTZ
{
    public Profile_Panasonic_BLC30(ViewerAttributeInterface vai)
    {
        super(vai);
    }

    public String getDescription()
    {
        return "Panasonic BLC30";
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
        return true; // Will do via client
    }

    public boolean supportsFocus()
    {
        return false;
    }

    public boolean supportsBrightness()
    {
        return false;
    }

    //=================================================================================

    public void panLeft()
    {
        if (supportsPan()) {
            execute("/nphControlCamera?Direction=PanLeft");
        }
    }

    public void panRight()
    {
        if (supportsPan()) {
            execute("/nphControlCamera?Direction=PanRight");
        }
    }

    public void tiltUp()
    {
        if (supportsTilt()) {
            execute("/nphControlCamera?Direction=TiltUp");
        }
    }

    public void tiltDown()
    {
        if (supportsTilt()) {
            execute("/nphControlCamera?Direction=TiltDown");
        }
    }

    public void moveToCenter(int w, int h, int x, int y)
    {
        if (supportsPan() || supportsTilt()) {
            execute("/nphControlCamera?Direction=Direct&Width=" + w + "&Height=" + h + "&NewPosition.x=" + x + "&NewPosition.y=" + y);
        }
    }

    public void homeView()
    {
        super.homeView();
        execute("/nphControlCamera?Direction=HomePosition");
    }

    public void focusNear()
    {
//        if (supportsFocus()) {
//            String path = "/nphControlCamera?Direction=FocusNear";
//            String res = execute(cameraURL, path);
//        }
    }

    public void focusFar()
    {
//        if (supportsFocus()) {
//            String path = "/nphControlCamera?Direction=FocusFar";
//            String res = execute(cameraURL, path);
//        }
    }

    public void focusAuto()
    {
//        if (supportsFocus()) {
//            String path = "/nphControlCamera?Direction=FocusAuto";
//            String res = execute(cameraURL, path);
//        }
    }

    public void zoomTele()
    {
        // "/nphControlCamera?Direction=ZoomTele";
        if (supportsZoom()) {
            super.zoomTele();
        }
    }

    public void zoomWide()
    {
        // "/nphControlCamera?Direction=ZoomWide";
        if (supportsZoom()) {
            super.zoomWide();
        }
    }

    public void darker()
    {
//        if (supportsBrightness()) {
//            String path = "/nphControlCamera?Direction=Darker";
//            String res = execute(cameraURL, path);
//        }
    }

    public void brighter()
    {
//        if (supportsBrightness()) {
//            String path = "/nphControlCamera?Direction=Brighter";
//            String res = execute(cameraURL, path);
//        }
    }

    public void standardBrightness()
    {
//        if (supportsBrightness()) {
//            String path = "/nphControlCamera?Direction=DefaultBrightness";
//            String res = execute(cameraURL, path);
//        }
    }

    public void mouseClicked(int w, int h, int x, int y, boolean doubleClick)
    {
        moveToCenter(w, h, x, y);
    }
}
