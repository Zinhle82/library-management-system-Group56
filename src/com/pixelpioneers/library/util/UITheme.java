package com.pixelpioneers.library.util;

import java.awt.Color;
import java.awt.Font;

/** Central palette and fonts so every screen looks consistent. */
/** Presentation Layer: the colour palette and fonts used by every screen. */
public class UITheme {

    // Core palette
    public static final Color SIDEBAR_BG      = new Color(0x1E2A38);
    public static final Color SIDEBAR_HOVER   = new Color(0x2A3B4E);
    public static final Color PRIMARY         = new Color(0x4A6FE3);
    public static final Color PRIMARY_DARK    = new Color(0x3B5BC4);
    public static final Color SUCCESS         = new Color(0x2ECC71);
    public static final Color SUCCESS_DARK    = new Color(0x25A85C);
    public static final Color DANGER          = new Color(0xE74C3C);
    public static final Color DANGER_DARK     = new Color(0xC0392B);
    public static final Color WARNING         = new Color(0xF5A623);
    public static final Color BACKGROUND      = new Color(0xF2F5FA);
    public static final Color CARD_BG         = Color.WHITE;
    public static final Color BORDER          = new Color(0xDCE3ED);
    public static final Color TEXT_DARK       = new Color(0x1E2A38);
    public static final Color TEXT_MUTED      = new Color(0x8592A6);
    public static final Color TABLE_HEADER_BG = new Color(0x2A3B4E);
    public static final Color TABLE_ROW_ALT   = new Color(0xEEF2F8);

    // Extra accent colors - used for icons/cards so the app doesn't feel monochrome
    public static final Color ACCENT_BLUE   = new Color(0x4A6FE3);
    public static final Color ACCENT_TEAL   = new Color(0x17A2A0);
    public static final Color ACCENT_PURPLE = new Color(0x8E5AE2);
    public static final Color ACCENT_ORANGE = new Color(0xF5A623);
    public static final Color ACCENT_PINK   = new Color(0xE0559B);
    public static final Color ACCENT_GREEN  = new Color(0x2ECC71);
    public static final Color GOLD          = new Color(0xD4A017);

    // Fonts
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 19);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_LABEL_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_FIELD   = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TABLE   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_FINE    = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_ICON    = new Font("Segoe UI Emoji", Font.PLAIN, 26);

    private UITheme() { }
}
