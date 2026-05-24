package com.github.nekozuki0509.schoolfes2026;

import lombok.Getter;

@Getter
public enum Medias {
    Select("textures/media/select.mp4"),
    Start("textures/media/start.mp4"),
    GoOver("textures/media/go_over.mp4"),
    Left("textures/media/left.mp4"),
    Right("textures/media/right.mp4"),
    Success("textures/media/success.mp4"),
    Fail("textures/media/fail.mp4"),
    Goal("textures/media/goal.mp4");

    private final String path;

    Medias(final String path) {
        this.path = path;
    }

}