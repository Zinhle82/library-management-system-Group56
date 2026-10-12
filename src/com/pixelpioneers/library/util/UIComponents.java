package com.pixelpioneers.library.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/** Presentation Layer helpers: styled fields, tables, and hand-drawn vector icons shared by every screen. */
public class UIComponents {

    public static JTextField styledField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(UITheme.FONT_FIELD);
        Border line = BorderFactory.createLineBorder(UITheme.BORDER, 1, true);
        Border padding = BorderFactory.createEmptyBorder(8, 10, 8, 10);
        field.setBorder(BorderFactory.createCompoundBorder(line, padding));
        return field;
    }

    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_LABEL_BOLD);
        label.setForeground(UITheme.TEXT_DARK);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_HEADING);
        label.setForeground(UITheme.TEXT_DARK);
        return label;
    }

    /**
     * A small colored circle with a hand-drawn vector icon centered in it.
     * Deliberately does NOT use emoji characters: Java's Swing text renderer
     * cannot draw Windows' color emoji font (it shows as empty "tofu" boxes),
     * so every icon here is drawn with plain shapes instead - this always
     * renders correctly regardless of what fonts are installed.
     *
     * @param iconKey one of: "home", "book", "people", "plus", "back", "chart", "person", "warning", "coin"
     */
    public static JPanel iconBadge(String iconKey, Color color, int size) {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());
                drawIcon(g2, iconKey, getWidth(), getHeight());
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(size, size));
        badge.setMaximumSize(new Dimension(size, size));
        return badge;
    }

    /** Draws a simple white vector glyph centered in a size x size area. */
    private static void drawIcon(Graphics2D g2, String key, int w, int h) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(1.6f, w * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float cx = w / 2f, cy = h / 2f;
        float s = w * 0.30f; // half-extent for most glyphs

        switch (key) {
            case "home": {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - s, cy + s * 0.6);
                p.lineTo(cx - s, cy - s * 0.1);
                p.lineTo(cx, cy - s * 1.1);
                p.lineTo(cx + s, cy - s * 0.1);
                p.lineTo(cx + s, cy + s * 0.6);
                p.closePath();
                g2.draw(p);
                break;
            }
            case "book": {
                Rectangle2D r = new Rectangle2D.Float(cx - s, cy - s * 0.75f, s * 2, s * 1.5f);
                g2.draw(r);
                g2.draw(new Line2D.Float(cx, cy - s * 0.75f, cx, cy + s * 0.75f));
                break;
            }
            case "people": {
                float r = s * 0.42f;
                g2.draw(new Ellipse2D.Float(cx - s * 0.75f - r, cy - s * 0.55f, r * 2, r * 2));
                g2.draw(new Ellipse2D.Float(cx + s * 0.05f - r, cy - s * 0.55f, r * 2, r * 2));
                Path2D body1 = new Path2D.Float();
                body1.moveTo(cx - s * 0.95f, cy + s * 0.85f);
                body1.curveTo(cx - s * 0.95f, cy + s * 0.15f, cx - s * 0.35f, cy + s * 0.15f, cx - s * 0.35f, cy + s * 0.85f);
                g2.draw(body1);
                Path2D body2 = new Path2D.Float();
                body2.moveTo(cx - s * 0.15f, cy + s * 0.85f);
                body2.curveTo(cx - s * 0.15f, cy + s * 0.15f, cx + s * 0.45f, cy + s * 0.15f, cx + s * 0.45f, cy + s * 0.85f);
                g2.draw(body2);
                break;
            }
            case "coin": {
                float r = s * 1.05f;
                g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, Math.round(w * 0.38f)));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString("R", cx - fm.stringWidth("R") / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
                break;
            }
            case "plus": {
                g2.draw(new Line2D.Float(cx - s, cy, cx + s, cy));
                g2.draw(new Line2D.Float(cx, cy - s, cx, cy + s));
                break;
            }
            case "back": {
                g2.draw(new Line2D.Float(cx + s, cy, cx - s, cy));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(cx - s * 0.35f, cy - s * 0.65f);
                arrow.lineTo(cx - s, cy);
                arrow.lineTo(cx - s * 0.35f, cy + s * 0.65f);
                g2.draw(arrow);
                break;
            }
            case "chart": {
                g2.draw(new Line2D.Float(cx - s, cy + s, cx + s, cy + s));
                g2.fill(new Rectangle2D.Float(cx - s * 0.9f, cy + s * 0.1f, s * 0.45f, s * 0.9f));
                g2.fill(new Rectangle2D.Float(cx - s * 0.2f, cy - s * 0.5f, s * 0.45f, s * 1.5f));
                g2.fill(new Rectangle2D.Float(cx + s * 0.5f, cy - s * 0.9f, s * 0.45f, s * 1.9f));
                break;
            }
            case "warning": {
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx, cy - s);
                tri.lineTo(cx + s, cy + s * 0.75f);
                tri.lineTo(cx - s, cy + s * 0.75f);
                tri.closePath();
                g2.draw(tri);
                g2.fill(new Ellipse2D.Float(cx - w * 0.035f, cy + s * 0.32f, w * 0.07f, w * 0.07f));
                g2.draw(new Line2D.Float(cx, cy - s * 0.45f, cx, cy + s * 0.12f));
                break;
            }
            case "person":
            default: {
                float r = s * 0.5f;
                g2.draw(new Ellipse2D.Float(cx - r, cy - s * 0.95f, r * 2, r * 2));
                Path2D body = new Path2D.Float();
                body.moveTo(cx - s, cy + s);
                body.curveTo(cx - s, cy + s * 0.05f, cx + s, cy + s * 0.05f, cx + s, cy + s);
                g2.draw(body);
                break;
            }
        }
    }

    public static void styleTable(JTable table) {
        table.setFont(UITheme.FONT_TABLE);
        table.setForeground(UITheme.TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UITheme.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setOpaque(false);

        // IMPORTANT: header.setBackground()/setForeground() alone are silently
        // ignored by the Windows Look & Feel, which paints its own native header
        // style regardless. A custom renderer is the only reliable way to force
        // the header's colors, so we set one explicitly instead.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(UITheme.TABLE_HEADER_BG);
                label.setForeground(Color.WHITE);
                label.setFont(UITheme.FONT_TABLE_HEADER);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.SIDEBAR_HOVER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                return label;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                // Set colors explicitly rather than trusting inherited Look & Feel
                // defaults, which on some Windows themes render as near-invisible
                // until the row is focused or selected.
                if (isSelected) {
                    c.setForeground(Color.WHITE);
                } else {
                    c.setForeground(UITheme.TEXT_DARK);
                    c.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ROW_ALT);
                }
                return c;
            }
        });
    }

    private UIComponents() { }
}
