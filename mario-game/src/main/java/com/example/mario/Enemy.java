package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

public class Enemy {

    private int x, y;
    private int width, height;
    private int velX;
    private int velY = 0;
    private boolean dead = false;
    private EnemyType type;
    private int animFrame = 0;
    private int animTimer = 0;

    private static final int GRAVITY = 1;

    public Enemy(int x, int y, EnemyType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        if (type == EnemyType.GOOMBA) {
            this.width = 32;
            this.height = 32;
            this.velX = -2;
        } else {
            this.width = 32;
            this.height = 48;
            this.velX = -3;
        }
    }

    public void update(List<Platform> platforms) {
        if (dead) return;

        velY += GRAVITY;
        if (velY > 12) velY = 12;
        x += velX;
        y += velY;

        for (Platform p : platforms) {
            if (p instanceof Flag) continue;
            if (getBounds().intersects(p.getBounds())) {
                int overlapTop = (y + height) - p.getY();
                if (overlapTop > 0 && overlapTop < 20 && velY >= 0) {
                    y = p.getY() - height;
                    velY = 0;
                }
            }
        }

        boolean onPlatform = false;
        java.awt.Rectangle footArea = new java.awt.Rectangle(x + 4, y + height, width - 8, 6);
        for (Platform p : platforms) {
            if (p instanceof Flag) continue;
            if (footArea.intersects(p.getBounds())) {
                onPlatform = true;
                break;
            }
        }

        if (!onPlatform && velY == 0) {
            velX = -velX;
        }

        animTimer++;
        if (animTimer > 10) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 2;
        }
    }

    public void die() {
        dead = true;
    }

    public void draw(Graphics g, int cameraX) {
        if (dead) return;
        int drawX = x - cameraX;
        if (drawX + width < -50 || drawX > 850) return;

        if (type == EnemyType.GOOMBA) {
            g.setColor(new Color(139, 69, 19));
            g.fillOval(drawX, y + 4, width, height - 4);
            g.setColor(Color.WHITE);
            g.fillOval(drawX + 6, y + 8, 8, 8);
            g.fillOval(drawX + 18, y + 8, 8, 8);
            g.setColor(Color.BLACK);
            int eyeOff = animFrame * 2;
            g.fillOval(drawX + 8 + eyeOff, y + 10, 4, 4);
            g.fillOval(drawX + 20 + eyeOff, y + 10, 4, 4);
            g.setColor(new Color(100, 50, 10));
            g.fillRect(drawX + 2, y + height - 8, 10, 8);
            g.fillRect(drawX + width - 12, y + height - 8, 10, 8);
        } else {
            g.setColor(new Color(50, 180, 50));
            g.fillOval(drawX + 2, y, width - 4, height / 2);
            g.setColor(new Color(255, 255, 100));
            g.fillOval(drawX + 6, y + 6, 8, 8);
            g.fillOval(drawX + 18, y + 6, 8, 8);
            g.setColor(Color.BLACK);
            g.fillOval(drawX + 10, y + 8, 4, 4);
            g.fillOval(drawX + 22, y + 8, 4, 4);
            g.setColor(new Color(50, 180, 50));
            g.fillRect(drawX + 4, y + height / 2, width - 8, height / 2);
            g.setColor(new Color(255, 200, 50));
            g.fillRect(drawX + 2, y + height - 8, 10, 8);
            g.fillRect(drawX + width - 12, y + height - 8, 10, 8);
        }
    }

    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x, y, width, height);
    }

    public boolean isDead() { return dead; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHeight() { return height; }
}