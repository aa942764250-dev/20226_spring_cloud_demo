package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;

public class Mario {

    private int x, y;
    private int width = 32;
    private int height = 48;
    private int velX = 0;
    private int velY = 0;
    private boolean onGround = false;
    private boolean facingRight = true;
    private int animFrame = 0;
    private int animTimer = 0;

    private static final int SPEED = 4;
    private static final int JUMP_FORCE = -14;
    private static final int GRAVITY = 1;

    public Mario(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveLeft() {
        velX = -SPEED;
        facingRight = false;
    }

    public void moveRight() {
        velX = SPEED;
        facingRight = true;
    }

    public void jump() {
        velY = JUMP_FORCE;
        onGround = false;
    }

    public void bounceOffEnemy() {
        velY = JUMP_FORCE / 2;
        onGround = false;
    }

    public void update() {
        velY += GRAVITY;
        if (velY > 15) velY = 15;

        x += velX;
        y += velY;

        if (velX == 0) {
            animFrame = 0;
        } else {
            animTimer++;
            if (animTimer > 6) {
                animTimer = 0;
                animFrame = (animFrame + 1) % 3;
            }
        }
        velX = 0;

        if (x < 0) x = 0;
    }

    public void reset(int x, int y) {
        this.x = x;
        this.y = y;
        this.velX = 0;
        this.velY = 0;
        this.onGround = false;
    }

    public void draw(Graphics g, int cameraX) {
        int drawX = x - cameraX;

        g.setColor(new Color(220, 50, 30));
        g.fillRect(drawX + 4, y, width - 8, 14);

        g.setColor(new Color(255, 200, 150));
        g.fillRect(drawX + 6, y + 14, width - 12, 14);

        g.setColor(new Color(220, 50, 30));
        g.fillRect(drawX + 4, y + 28, width - 8, 4);

        g.setColor(new Color(50, 50, 200));
        int legOffset = animFrame == 1 ? 3 : (animFrame == 2 ? -3 : 0);
        g.fillRect(drawX + 6, y + 32, 8, 16);
        g.fillRect(drawX + width - 14, y + 32 + legOffset, 8, 16);

        g.setColor(Color.WHITE);
        if (facingRight) {
            g.fillRect(drawX + 18, y + 4, 6, 6);
            g.setColor(Color.BLACK);
            g.fillRect(drawX + 22, y + 5, 3, 3);
        } else {
            g.fillRect(drawX + 8, y + 4, 6, 6);
            g.setColor(Color.BLACK);
            g.fillRect(drawX + 8, y + 5, 3, 3);
        }

        g.setColor(new Color(139, 69, 19));
        g.fillRect(drawX + 4, y + 10, width - 8, 4);
    }

    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getVelY() { return velY; }
    public boolean isOnGround() { return onGround; }
    public boolean isFacingRight() { return facingRight; }
    public void setY(int y) { this.y = y; }
    public void setX(int x) { this.x = x; }
    public void setVelY(int velY) { this.velY = velY; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
}