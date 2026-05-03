package org.w7ls.vme;

import javax.sound.sampled.*;
import java.io.File;

public class VMAudioFile {
    private final File file;
    String audioName;

    //===========<単位はすべてsecond>==========
    double trimStart = 0.0;
    double trimEnd = Double.MAX_VALUE;
    double duration = 0.0;
    double position = 0.0;
    //======================================

    public VMAudioFile(File file) {
        this.file = file;
        loadDuration();
        audioName = file.getName();
    }

    private void loadDuration() {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = ais.getFormat();

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            AudioInputStream pcmAis = AudioSystem.getAudioInputStream(pcmFormat, ais);

            // フレーム数が-1の場合は全部読み込んでカウントする
            long frames = pcmAis.getFrameLength();
            if (frames <= 0) {
                byte[] buffer = new byte[4096];
                long totalBytes = 0;
                int bytesRead;
                while ((bytesRead = pcmAis.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }
                frames = totalBytes / pcmFormat.getFrameSize();
            }

            duration = frames / pcmFormat.getFrameRate();
            trimEnd = duration;
            pcmAis.close();
            ais.close();
        } catch (Exception e) {
            duration = 0.0;
            e.printStackTrace();
        }
    }

    public void startTrimed() {
        new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                AudioFormat baseFormat = ais.getFormat();

                AudioFormat pcmFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                AudioInputStream pcmAis = AudioSystem.getAudioInputStream(pcmFormat, ais);

                // trimStartまでスキップ
                long skipBytes = (long)(trimStart * pcmFormat.getFrameRate())
                                 * pcmFormat.getFrameSize();
                pcmAis.skip(skipBytes);

                SourceDataLine line = AudioSystem.getSourceDataLine(pcmFormat);
                line.open(pcmFormat);
                line.start();

                int frameSize = pcmFormat.getFrameSize();
                byte[] buffer = new byte[(4096 / frameSize) * frameSize];
                int bytesRead;
                long totalBytesPlayed = 0;
                long maxBytes = (long)((trimEnd - trimStart) * pcmFormat.getFrameRate())
                                * pcmFormat.getFrameSize();

                while ((bytesRead = pcmAis.read(buffer)) != -1) {
                    if (totalBytesPlayed + bytesRead > maxBytes) {
                        bytesRead = (int)(maxBytes - totalBytesPlayed);
                        // frameSize の倍数に切り捨て
                        bytesRead = (bytesRead / pcmFormat.getFrameSize()) * pcmFormat.getFrameSize();
                        if (bytesRead > 0) {
                            line.write(buffer, 0, bytesRead);
                        }
                        break;
                    }
                    // frameSize の倍数に切り捨て
                    int alignedBytes = (bytesRead / pcmFormat.getFrameSize()) * pcmFormat.getFrameSize();
                    if (alignedBytes > 0) {
                        line.write(buffer, 0, alignedBytes);
                    }
                    totalBytesPlayed += alignedBytes;
                }

                line.drain();
                line.close();
                pcmAis.close();
                ais.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public double getEffectiveTrimEnd() {
        return trimEnd < 0 ? duration : trimEnd;
    }

    /**
     * 開始を0として，終了を1とする
     */
    public void trimAbs(double start, double end) {
        if (start < end) {
            throw new RuntimeException("require 'end' more than 'start'.");
        }
        trimStart = start * duration;
        trimEnd = end * duration;
    }

    public void trim(double start, double end) {
        trimStart = start;
        trimEnd = end;
    }

}