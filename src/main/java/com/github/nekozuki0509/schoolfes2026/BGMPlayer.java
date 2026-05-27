package com.github.nekozuki0509.schoolfes2026;

import java.io.File;
import javax.sound.sampled.*;

public class BGMPlayer {

    private Clip clip;
    private FloatControl volumeControl;

    /**
     * 指定したWAVファイルを読み込む
     * @param filePath WAVファイルのパス
     */
    public void load(String filePath) {
        try {
            stop(); // 既存の再生を停止

            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            clip = AudioSystem.getClip();
            clip.open(audioStream);
            audioStream.close();

            // 音量コントロールを取得
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * BGMをループ再生する
     */
    public void play() {
        if (clip != null && !clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /**
     * BGMを一時停止する
     */
    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /**
     * BGMを停止してリセットする
     */
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.close();
            clip = null;
        }
    }

    /**
     * 音量を設定する
     * @param volume 0.0f（無音）〜 1.0f（最大）
     */
    public void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum(); // 通常 -80.0 dB
            float max = volumeControl.getMaximum(); // 通常 6.0 dB
            // 線形の音量をデシベルに変換
            float dB = min + (max - min) * Math.max(0f, Math.min(1f, volume));
            volumeControl.setValue(dB);
        }
    }

    /**
     * 再生中かどうかを返す
     */
    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}