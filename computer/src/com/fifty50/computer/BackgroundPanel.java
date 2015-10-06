package com.fifty50.computer;

import javax.swing.*;
import java.awt.*;

/**
 * Created by samuel on 06.07.15.
 */
public class BackgroundPanel extends JPanel {

    Image background;
    private int width, height;

    public BackgroundPanel(Image background, int width, int height) {
        this.background = background;
        this.width = width;
        this.height = height;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, width, height, null);
    }
}
