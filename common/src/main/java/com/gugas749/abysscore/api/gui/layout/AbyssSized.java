package com.gugas749.abysscore.api.gui.layout;

/**
 * For widgets whose visible size is not their widget size (AbyssTextField: the widget is the
 * inner text area, the frame is drawn around it). Layouts ask this instead of getWidth().
 */
public interface AbyssSized {
    int outerWidth();
    int outerHeight();
}
