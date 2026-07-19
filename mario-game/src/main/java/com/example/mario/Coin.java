package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;

public class Coin {

    private int x, y;
    private boolean collected = false;
    private int animTimer = 0;

    public Coin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void collect() {
        collected = true;
    }

    public void draw(Graphics g, int cameraX) {
        if (collected) return;
        int drawX = x - cameraX;
        if (drawX < -50 || drawX > 850) return;

        animTimer++;
        int w = 16;
        if (animTimer % 30 < 10) {
            w = 6;
        } else if (animTimer % 30 < 20) {
            w = 14;
        }

        g.setColor(new Color(255, 215, 0));
        g.fillOval(drawX + (16 - w) / 2, y, w, 20);
        g.setColor(new Color(255, 240, 100));
        g.fillOval(drawX + (16 - w) / 2 + 2, y + 2, Math.max(w - 4, 1), 16);
    }

    public boolean isCollected() { return collected; }
    public java.awt.Rectangle getBounds() { return new java.awt.Rectangle(x, y, 16, 20); }
}