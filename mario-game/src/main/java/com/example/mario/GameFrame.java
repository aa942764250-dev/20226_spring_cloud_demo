package com.example.mario;

import javax.swing.JFrame;

public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("超级马里奥 - Java版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new GameFrame();
    }
}