package com.charliemouse.cambozola.profiles;

/**
 * * com/charliemouse/cambozola/profiles.ICameraProfile.java
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
public interface ICameraProfile
{
    String getDescription();

    boolean supportsPan();

    boolean supportsTilt();

    boolean supportsFocus();

    boolean supportsZoom();

    boolean supportsBrightness();

    void panLeft();

    void panRight();

    void tiltUp();

    void tiltDown();

    void homeView();

    void moveToCenter(int w, int h, int x, int y);

    void focusNear();

    void focusFar();

    void focusAuto();

    void zoomTele();

    void zoomWide();

    void darker();

    void brighter();

    void standardBrightness();

    void mouseClicked(int w, int h, int x, int y, boolean doubleClick);
}
