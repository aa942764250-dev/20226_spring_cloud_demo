package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;

public class Flag extends Platform {

    public Flag(int x, int y) {
        super(x, y, 10, 8 * 40, PlatformType.GROUND);
    }

    @Override
    public void draw(Graphics g, int cameraX) {
        int drawX = x - cameraX;
        g.setColor(new Color(139, 69, 19));
        g.fillRect(drawX, y, 6, height);

        g.setColor(Color.GREEN);
        int[] xPoints = {drawX + 6, drawX + 46, drawX + 6};
        int[] yPoints = {y, y + 20, y + 40};
        g.fillPolygon(xPoints, yPoints, 3);

        g.setColor(new Color(255, 215, 0));
        g.fillOval(drawX - 4, y - 8, 14, 14);
    }
}