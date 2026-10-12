package com.pixelpioneers.library.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/** Presentation Layer: a flat, rounded button with a hover colour. */
public class RoundedButton extends JButton {

    private final Color baseColor;
    private final Color hoverColor;
    private final int arc;

    public RoundedButton(String text, Color baseColor, Color hoverColor) {
        this(text, baseColor, hoverColor, 10);
    }

    public RoundedButton(String text, Color baseColor, Color hoverColor, int arc) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        this.arc = arc;

        setFont(UITheme.FONT_BUTTON);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 22, 10, 22));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { setBackground(hoverColor); repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { setBackground(baseColor); repaint(); }
        });
        setBackground(baseColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? getBackground() : baseColor.darker());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
