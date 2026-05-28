package com.github.nekozuki0509.schoolfes2026;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class AutoShrinkLabel extends Label {

    private static final double MAX_FONT_SIZE = 240.0;
    private static final double MIN_FONT_SIZE = 1.0;
    private static final double STEP = 0.5;

    public AutoShrinkLabel() {
        init();
    }

    public AutoShrinkLabel(String text) {
        super(text);
        init();
    }

    private void init() {
        setMinSize(0, 0);
        setEllipsisString("");

        layoutBoundsProperty().addListener((obs, old, val) -> scheduleAdjust());
        textProperty().addListener((obs, old, val) -> scheduleAdjust());
        visibleProperty().addListener((obs, old, val) -> {
            if (val) scheduleAdjust();
        });
    }

    private void scheduleAdjust() {
        Platform.runLater(() -> Platform.runLater(this::adjustFontSize));
    }

    private void adjustFontSize() {
        if (!isVisible()) return;

        String text = getText();
        if (text == null || text.isEmpty()) return;

        Insets insets = getInsets();
        double availW = getWidth() - insets.getLeft() - insets.getRight();
        double availH = getHeight() - insets.getTop() - insets.getBottom();

        if (availW <= 0 || availH <= 0) return;

        String fontFamily = getFont().getFamily();
        double fontSize = MAX_FONT_SIZE;

        while (fontSize > MIN_FONT_SIZE) {
            Text helper = new Text(text);
            helper.setFont(Font.font(fontFamily, fontSize));
            helper.setWrappingWidth(isWrapText() ? availW : 0);
            helper.applyCss();

            double tw = helper.getBoundsInLocal().getWidth();
            double th = helper.getBoundsInLocal().getHeight();

            if (tw <= availW && th <= availH) break;
            fontSize -= STEP;
        }

        setFont(Font.font(fontFamily, fontSize));
    }
}