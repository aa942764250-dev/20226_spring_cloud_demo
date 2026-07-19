package com.example.mario;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements KeyListener, Runnable {

    static final int PANEL_WIDTH = 800;
    static final int PANEL_HEIGHT = 600;
    static final int TILE_SIZE = 40;
    static final int GROUND_Y = PANEL_HEIGHT - TILE_SIZE;

    private Thread gameThread;
    private volatile boolean running = true;

    private Mario mario;
    private List<Platform> platforms;
    private List<Enemy> enemies;
    private List<Coin> coins;
    private List<Fireball> fireballs;

    private int cameraX = 0;
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;
    private boolean gameWin = false;

    private boolean[] keys = new boolean[256];

    public GamePanel() {
        setPreferredSize(new java.awt.Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        initLevel();
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void initLevel() {
        mario = new Mario(80, GROUND_Y - 48);
        platforms = new ArrayList<>();
        enemies = new ArrayList<>();
        coins = new ArrayList<>();
        fireballs = new ArrayList<>();
        score = 0;
        lives = 3;
        gameOver = false;
        gameWin = false;
        cameraX = 0;

        platforms.add(new Platform(0, GROUND_Y, 12 * TILE_SIZE, TILE_SIZE, PlatformType.GROUND));
        platforms.add(new Platform(14 * TILE_SIZE, GROUND_Y, 8 * TILE_SIZE, TILE_SIZE, PlatformType.GROUND));
        platforms.add(new Platform(24 * TILE_SIZE, GROUND_Y, 6 * TILE_SIZE, TILE_SIZE, PlatformType.GROUND));
        platforms.add(new Platform(32 * TILE_SIZE, GROUND_Y, 20 * TILE_SIZE, TILE_SIZE, PlatformType.GROUND));

        platforms.add(new Platform(5 * TILE_SIZE, GROUND_Y - 4 * TILE_SIZE, 3 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));
        platforms.add(new Platform(9 * TILE_SIZE, GROUND_Y - 6 * TILE_SIZE, 2 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));
        platforms.add(new Platform(16 * TILE_SIZE, GROUND_Y - 4 * TILE_SIZE, 4 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));
        platforms.add(new Platform(26 * TILE_SIZE, GROUND_Y - 5 * TILE_SIZE, 3 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));
        platforms.add(new Platform(34 * TILE_SIZE, GROUND_Y - 4 * TILE_SIZE, 2 * TILE_SIZE, TILE_SIZE, PlatformType.QUESTION));
        platforms.add(new Platform(38 * TILE_SIZE, GROUND_Y - 6 * TILE_SIZE, 3 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));
        platforms.add(new Platform(42 * TILE_SIZE, GROUND_Y - 4 * TILE_SIZE, 2 * TILE_SIZE, TILE_SIZE, PlatformType.QUESTION));
        platforms.add(new Platform(46 * TILE_SIZE, GROUND_Y - 5 * TILE_SIZE, 4 * TILE_SIZE, TILE_SIZE, PlatformType.BRICK));

        enemies.add(new Enemy(7 * TILE_SIZE, GROUND_Y - 36, EnemyType.GOOMBA));
        enemies.add(new Enemy(18 * TILE_SIZE, GROUND_Y - 36, EnemyType.GOOMBA));
        enemies.add(new Enemy(28 * TILE_SIZE, GROUND_Y - 36, EnemyType.GOOMBA));
        enemies.add(new Enemy(35 * TILE_SIZE, GROUND_Y - 36, EnemyType.KOOPA));
        enemies.add(new Enemy(40 * TILE_SIZE, GROUND_Y - 36, EnemyType.GOOMBA));
        enemies.add(new Enemy(45 * TILE_SIZE, GROUND_Y - 36, EnemyType.KOOPA));

        coins.add(new Coin(6 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(7 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(10 * TILE_SIZE + 10, GROUND_Y - 7 * TILE_SIZE));
        coins.add(new Coin(17 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(18 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(27 * TILE_SIZE + 10, GROUND_Y - 6 * TILE_SIZE));
        coins.add(new Coin(35 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(39 * TILE_SIZE + 10, GROUND_Y - 7 * TILE_SIZE));
        coins.add(new Coin(43 * TILE_SIZE + 10, GROUND_Y - 5 * TILE_SIZE));
        coins.add(new Coin(47 * TILE_SIZE + 10, GROUND_Y - 6 * TILE_SIZE));
        coins.add(new Coin(48 * TILE_SIZE + 10, GROUND_Y - 6 * TILE_SIZE));

        platforms.add(new Flag(49 * TILE_SIZE, GROUND_Y - 8 * TILE_SIZE));
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            repaint();
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void update() {
        if (gameOver || gameWin) return;

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
            mario.moveLeft();
        }
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
            mario.moveRight();
        }
        if (keys[KeyEvent.VK_SPACE] && mario.isOnGround()) {
            mario.jump();
        }

        mario.update();

        mario.setOnGround(false);
        for (Platform p : platforms) {
            if (p instanceof Flag) continue;
            checkPlatformCollision(mario, p);
        }

        for (Coin c : coins) {
            if (!c.isCollected() && mario.getBounds().intersects(c.getBounds())) {
                c.collect();
                score += 100;
            }
        }

        for (Enemy e : enemies) {
            if (!e.isDead()) {
                e.update(platforms);
                if (mario.getBounds().intersects(e.getBounds())) {
                    if (mario.getVelY() > 0 && mario.getY() + mario.getHeight() - 10 < e.getY() + e.getHeight() / 2) {
                        e.die();
                        mario.bounceOffEnemy();
                        score += 200;
                    } else {
                        lives--;
                        if (lives <= 0) {
                            gameOver = true;
                        } else {
                            mario.reset(80, GROUND_Y - 48);
                            cameraX = 0;
                        }
                    }
                }
            }
        }

        for (int i = fireballs.size() - 1; i >= 0; i--) {
            Fireball fb = fireballs.get(i);
            fb.update(platforms);
            if (!fb.isActive()) {
                fireballs.remove(i);
                continue;
            }
            for (Enemy e : enemies) {
                if (!e.isDead() && fb.getBounds().intersects(e.getBounds())) {
                    e.die();
                    fb.setActive(false);
                    score += 200;
                }
            }
        }

        for (Platform p : platforms) {
            if (p instanceof Flag) {
                Flag flag = (Flag) p;
                if (mario.getBounds().intersects(flag.getBounds())) {
                    gameWin = true;
                    score += 1000;
                }
            }
        }

        if (mario.getY() > PANEL_HEIGHT + 50) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
            } else {
                mario.reset(80, GROUND_Y - 48);
                cameraX = 0;
            }
        }

        int targetCameraX = mario.getX() - PANEL_WIDTH / 3;
        if (targetCameraX > cameraX) {
            cameraX = targetCameraX;
        }
        if (cameraX < 0) cameraX = 0;
    }

    private void checkPlatformCollision(Mario m, Platform p) {
        if (!m.getBounds().intersects(p.getBounds())) return;

        int overlapLeft = (m.getX() + m.getWidth()) - p.getX();
        int overlapRight = (p.getX() + p.getWidth()) - m.getX();
        int overlapTop = (m.getY() + m.getHeight()) - p.getY();
        int overlapBottom = (p.getY() + p.getHeight()) - m.getY();

        int minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapTop && m.getVelY() >= 0) {
            m.setY(p.getY() - m.getHeight());
            m.setVelY(0);
            m.setOnGround(true);
        } else if (minOverlap == overlapBottom && m.getVelY() < 0) {
            m.setY(p.getY() + p.getHeight());
            m.setVelY(0);
        } else if (minOverlap == overlapLeft) {
            m.setX(p.getX() - m.getWidth());
        } else if (minOverlap == overlapRight) {
            m.setX(p.getX() + p.getWidth());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawSky(g);
        drawClouds(g);
        drawMountains(g);

        for (Platform p : platforms) {
            p.draw(g, cameraX);
        }

        for (Coin c : coins) {
            if (!c.isCollected()) {
                c.draw(g, cameraX);
            }
        }

        for (Enemy e : enemies) {
            if (!e.isDead()) {
                e.draw(g, cameraX);
            }
        }

        for (Fireball fb : fireballs) {
            fb.draw(g, cameraX);
        }

        mario.draw(g, cameraX);

        drawHUD(g);

        if (gameOver) {
            drawOverlay(g, "GAME OVER", "按 R 重新开始");
        }
        if (gameWin) {
            drawOverlay(g, "YOU WIN!", "得分: " + score + "  按 R 重新开始");
        }
    }

    private void drawSky(Graphics g) {
        g.setColor(new Color(107, 170, 255));
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawClouds(Graphics g) {
        g.setColor(Color.WHITE);
        int[] cloudX = {100, 350, 600, 900, 1200, 1600};
        int[] cloudY = {60, 100, 40, 80, 50, 90};
        for (int i = 0; i < cloudX.length; i++) {
            int cx = cloudX[i] - cameraX / 2;
            int cy = cloudY[i];
            g.fillOval(cx, cy, 60, 30);
            g.fillOval(cx + 20, cy - 15, 50, 30);
            g.fillOval(cx + 40, cy, 60, 30);
        }
    }

    private void drawMountains(Graphics g) {
        g.setColor(new Color(80, 140, 80));
        int[] mx = {0, 300, 600, 1000, 1400};
        for (int m : mx) {
            int bx = m - cameraX / 3;
            int[] xPoints = {bx, bx + 120, bx + 240};
            int[] yPoints = {GROUND_Y, GROUND_Y - 150, GROUND_Y};
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("得分: " + score, 20, 30);
        g.drawString("生命: " + lives, 20, 55);
        g.drawString("金币: " + coins.stream().filter(Coin::isCollected).count() + "/" + coins.size(), PANEL_WIDTH - 150, 30);
    }

    private void drawOverlay(Graphics g, String title, String subtitle) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (PANEL_WIDTH - tw) / 2, PANEL_HEIGHT / 2 - 20);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        int sw = g.getFontMetrics().stringWidth(subtitle);
        g.drawString(subtitle, (PANEL_WIDTH - sw) / 2, PANEL_HEIGHT / 2 + 30);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (e.getKeyCode() == KeyEvent.VK_R && (gameOver || gameWin)) {
            initLevel();
        }
        if (e.getKeyCode() == KeyEvent.VK_F && !gameOver && !gameWin) {
            fireballs.add(new Fireball(mario.getX() + (mario.isFacingRight() ? mario.getWidth() : -12), mario.getY() + mario.getHeight() / 2, mario.isFacingRight()));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}