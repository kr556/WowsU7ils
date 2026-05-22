package org.w7ls.vmeditor;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class VMAudioFile implements Serializable {
    @Serial
    private static final long serialVersionUID = -761719924860040752L;

    public File file;
    public String audioName;

    //===========<単位はすべてsecond>==========
    public double trimStart = 0.0;
    public double trimEnd = Double.MAX_VALUE;
    public double duration = 0.0;
    public transient double position = 0.0;
    //======================================

    private static double volume = 0.1;

    private VMAudioFile() {
        file = null;
        audioName = "?NONE";
    }

    public VMAudioFile(File file) throws IOException {
        init(file);
    }

    public VMAudioFile(File file, Consumer<IOException> catchAction) {
        try {
            init(file);
        } catch (IOException e) {
            catchAction.accept(e);
            e.printStackTrace();
        }
    }

    private void init(File file) throws IOException {
        if (file.getName().endsWith(".wem"))
            throw new RuntimeException(
                    new UnsupportedAudioFileException("unsupported wem. plz use 'VMAudioFile.CashedVMAudioFile'."));
        this.file = file;
        loadDuration();
        audioName = file.getName();
    }

    private void loadDuration() throws IOException {
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
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            duration = 0.0;
            e.printStackTrace();
        }
    }

    public Thread startTrimed() {
        Thread play = new Thread(() -> {
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

                long skipBytes = (long)(trimStart * pcmFormat.getFrameRate())
                                 * pcmFormat.getFrameSize();
                pcmAis.skip(skipBytes);

                SourceDataLine line = AudioSystem.getSourceDataLine(pcmFormat);
                line.open(pcmFormat);

                boolean needCPMScaling = false;
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                    float dB = (float) (20.0 * Math.log10(volume));
                    gain.setValue(dB);
                } else
                    needCPMScaling = true;
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
                        bytesRead = (bytesRead / pcmFormat.getFrameSize()) * pcmFormat.getFrameSize();
                        if (bytesRead > 0) {
                            line.write(buffer, 0, bytesRead);
                        }
                        break;
                    }
                    int alignedBytes = (bytesRead / pcmFormat.getFrameSize()) * pcmFormat.getFrameSize();
                    if (alignedBytes > 0) {
                        if (needCPMScaling) {
                            for (int i = 0; i < alignedBytes; i += 2) {
                                short sample = (short) ((buffer[i+1] << 8) | (buffer[i] & 0xff));
                                sample = (short) (sample * volume);
                                buffer[i] = (byte) (sample & 0xff);
                                buffer[i+1] = (byte) ((sample >> 8) & 0xff);
                            }
                        }

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
        });
        play.start();
        return play;
    }

    public double getEffectiveTrimEnd() {
        return trimEnd < 0 ? duration : trimEnd;
    }

    /**
     * 開始を0として，終了を1とする
     */
    public void trimAbs(double start, double end) {
        if (start >= end) {
            throw new RuntimeException("require 'end' more than 'start'.");
        }
        trimStart = start * duration;
        trimEnd = end * duration;
    }

    public void trim(double start, double end) {
        trimStart = start;
        trimEnd = end;
    }

    public static void setVolume(double volume) {
        if (0 <= volume && volume <= 1) // 0 ~ 1
            VMAudioFile.volume = volume;
    }

//    public void exportWem() {
//        try {
//            Process cmd = new ProcessBuilder(
//                    "libs/vgmstream-win64/vgmstream-cli.exe", "-o", wavStr, baseStr)
//                    .redirectErrorStream(true)
//                    .start();
//            cmd.getInputStream().readAllBytes(); // 消すな
//            cmd.waitFor();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static class CashedVMAudioFile extends VMAudioFile implements AutoCloseable {
        private static final String tmpFileDir = "tmp/audio/";
        private File base;
        private File mustDel;

        public CashedVMAudioFile(File base) throws IOException {
            super();
            this.base = base;
            String wavStr = Paths.get(tmpFileDir + base.getName() + ".wav").toFile().getAbsolutePath().replace("\\", "/");
            String baseStr = base.getAbsolutePath().replace("\\", "/");
            mustDel = new File(wavStr);
            try {
                Process cmd = new ProcessBuilder(
                        "libs/vgmstream-win64/vgmstream-cli.exe", "-o", wavStr, baseStr)
                        .redirectErrorStream(true)
                        .start();
                cmd.getInputStream().readAllBytes(); // 消すな
                cmd.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.file = new File(wavStr);
        }

        public File baseFile() {
            return base;
        }

        @Override
        public synchronized void close() throws Exception {
            Files.delete(mustDel.toPath());
            base = null;
            mustDel = null;
        }
    }

    @Override
    public String toString() {
        return audioName;
    }
}