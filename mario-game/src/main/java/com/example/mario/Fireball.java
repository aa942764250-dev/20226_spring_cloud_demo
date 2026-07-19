package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

public class Fireball {

    private int x, y;
    private int velX;
    private int velY = 0;
    private boolean active = true;
    private int bounceCount = 0;

    private static final int GRAVITY = 1;
    private static final int SPEED = 8;
    private static final int MAX_BOUNCES = 3;

    public Fireball(int x, int y, boolean facingRight) {
        this.x = x;
        this.y = y;
        this.velX = facingRight ? SPEED : -SPEED;
    }

    public void update(List<Platform> platforms) {
        if (!active) return;

        velY += GRAVITY;
        if (velY > 10) velY = 10;
        x += velX;
        y += velY;

        for (Platform p : platforms) {
            if (p instanceof Flag) continue;
            if (getBounds().intersects(p.getBounds())) {
                int overlapTop = (y + 12) - p.getY();
                if (overlapTop > 0 && overlapTop < 20 && velY >= 0) {
                    y = p.getY() - 12;
                    velY = -6;
                    bounceCount++;
                    if (bounceCount >= MAX_BOUNCES) {
                        active = false;
                    }
                }
            }
        }

        if (y > 700 || x < -100 || x > 3000) {
            active = false;
        }
    }

    public void draw(Graphics g, int cameraX) {
        if (!active) return;
        int drawX = x - cameraX;
        g.setColor(new Color(255, 100, 0));
        g.fillOval(drawX, y, 12, 12);
        g.setColor(new Color(255, 200, 0));
        g.fillOval(drawX + 2, y + 2, 8, 8);
    }

    public java.awt.Rectangle getBounds() { return new java.awt.Rectangle(x, y, 12, 12); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}