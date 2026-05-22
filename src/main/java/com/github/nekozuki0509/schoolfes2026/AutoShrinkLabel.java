package com.github.nekozuki0509.schoolfes2026;

import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class AutoShrinkLabel extends Label {

    private static final double MAX_FONT_SIZE = 240.0;
    private static final double STEP = 0.5;

    public AutoShrinkLabel() {
        textProperty().addListener((obs, old, val) -> adjustFontSize());
        widthProperty().addListener((obs, old, val) -> adjustFontSize());
        heightProperty().addListener((obs, old, val) -> adjustFontSize());
    }

    public AutoShrinkLabel(String text) {
        super(text);
        textProperty().addListener((obs, old, val) -> adjustFontSize());
        widthProperty().addListener((obs, old, val) -> adjustFontSize());
        heightProperty().addListener((obs, old, val) -> adjustFontSize());
    }

    private void adjustFontSize() {
        double fontSize = MAX_FONT_SIZE;
        String fontFamily = getFont().getFamily();

        while (true) {
            Font font = Font.font(fontFamily, fontSize);

            // Textノードで実寸を計測
            Text helper = new Text(getText());
            helper.setFont(font);
            helper.setWrappingWidth(isWrapText() ? getWidth() : 0);

            double textWidth  = helper.getBoundsInLocal().getWidth();
            double textHeight = helper.getBoundsInLocal().getHeight();

            if (textWidth <= getWidth() && textHeight <= getHeight()) {
                break;
            }
            fontSize -= STEP;
        }

        setFont(Font.font(fontFamily, fontSize));
    }
}