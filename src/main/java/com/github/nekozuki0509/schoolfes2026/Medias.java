package com.github.nekozuki0509.schoolfes2026;

import lombok.Getter;

@Getter
public enum Medias {
    Select("select", 181, true),
    Start("start", 230, false),
    GoOver("go_over", 61, true),
    Left("left", 83, false),
    Right("right", 81, false),
    Success("success", 125, false),
    Fail("fail", 51, false),
    Goal("goal", 191, false);

    private final String folderName;
    private final int frameCount;
    private final boolean loop;

    Medias(String folderName, int frameCount, boolean loop) {
        this.folderName = folderName;
        this.frameCount = frameCount;
        this.loop = loop;
    }
}