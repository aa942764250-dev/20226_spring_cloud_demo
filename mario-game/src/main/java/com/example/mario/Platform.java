package com.example.mario;

import java.awt.Color;
import java.awt.Graphics;

public class Platform {

    protected int x, y, width, height;
    protected PlatformType type;

    public Platform(int x, int y, int width, int height, PlatformType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public void draw(Graphics g, int cameraX) {
        int drawX = x - cameraX;
        if (drawX + width < -50 || drawX > 850) return;

        switch (type) {
            case GROUND:
                drawGround(g, drawX);
                break;
            case BRICK:
                drawBrick(g, drawX);
                break;
            case QUESTION:
                drawQuestion(g, drawX);
                break;
        }
    }

    private void drawGround(Graphics g, int drawX) {
        g.setColor(new Color(139, 90, 43));
        g.fillRect(drawX, y, width, height);
        g.setColor(new Color(34, 139, 34));
        g.fillRect(drawX, y, width, 8);
        g.setColor(new Color(100, 60, 20));
        for (int i = 0; i < width; i += 40) {
            g.drawRect(drawX + i, y + 8, 40, height - 8);
            g.drawLine(drawX + i + 20, y + 8, drawX + i + 20, y + height);
        }
    }

    private void drawBrick(Graphics g, int drawX) {
        g.setColor(new Color(180, 80, 30));
        g.fillRect(drawX, y, width, height);
        g.setColor(new Color(140, 60, 20));
        for (int i = 0; i < width; i += 40) {
            g.drawRect(drawX + i, y, 40, height);
            g.drawLine(drawX + i + 20, y, drawX + i + 20, y + height);
            g.drawLine(drawX + i, y + height / 2, drawX + i + 40, y + height / 2);
        }
        g.setColor(new Color(200, 100, 40));
        g.drawRect(drawX, y, width, height);
    }

    private void drawQuestion(Graphics g, int drawX) {
        g.setColor(new Color(255, 200, 50));
        g.fillRect(drawX, y, width, height);
        g.setColor(new Color(200, 150, 30));
        g.drawRect(drawX, y, width, height);
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
        int qw = g.getFontMetrics().stringWidth("?");
        g.drawString("?", drawX + (width - qw) / 2, y + height / 2 + 8);
    }

    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}