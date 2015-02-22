package com.charliemouse.cambozola.profiles;

import com.charliemouse.cambozola.ViewerAttributeInterface;

/**
 * * com/charliemouse/cambozola/profiles.Profile_Axis_M1031.java
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
public class Profile_Axis_M1031 extends Profile_LocalPTZ
{
    public Profile_Axis_M1031(ViewerAttributeInterface vai)
    {
        super(vai);
    }

    public String getDescription()
    {
        return "Axis M1031";
    }

    public boolean supportsPan()
    {
        return true; // Will do via client
    }

    public boolean supportsTilt()
    {
        return true; // Will do via client
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
}
