package org.w7ls.common.cuda;

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

// pointer
public class CUImagePtr extends CUWrapper<int[]> {
    private static final int BUFFERED_RGB_TYPE_NULL = -1;
    int[] bitmap;
    int w, h;
    int bufferedRgbType;

    public static CUImagePtr newImage(int w, int h, boolean reuse) {
        return new CUImagePtr(w, h, reuse);
    }

    public static CUImagePtr newImage(int[] bitmap, int width, boolean reuse) {
        return new CUImagePtr(bitmap, width, reuse);
    }

    protected CUImagePtr(int[] bitmap, int width, boolean reuse) {
        super(bitmap, reuse);
        this.h = bitmap.length / width;
        this.w = width;
        this.bufferedRgbType = BUFFERED_RGB_TYPE_NULL;
        this.bitmap = bitmap;
    }

    protected CUImagePtr(int width, int height, boolean reuse) {
        this(new int[width * height], width, reuse);
    }

    public BufferedImage toBufferedImage(int bufferedRgbType) {
        BufferedImage re =  new BufferedImage(w, h, bufferedRgbType);
        re.setRGB(0, 0, w, h, bitmap, 0, w);
        return re;
    }

    public BufferedImage copyTo(BufferedImage pointer) {
        return setRGBAtoARGB(pointer);
    }

    public BufferedImage toBufferedImage() {
        return setRGBAtoARGB(new BufferedImage(w, h, TYPE_INT_RGB));
    }

    public int[] intArr() {
        return bitmap;
    }

    private BufferedImage setRGBAtoARGB(BufferedImage bufferedImage) {
        final int[] _bitmap = bitmap;
        int bit;
        if (bufferedImage.getType() == TYPE_INT_RGB) {
            bufferedImage.setRGB(0, 0, w, h, bitmap, 0, w);
        } else if (bufferedImage.getType() == TYPE_INT_ARGB) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bit = _bitmap[x + y * w];
                    bufferedImage.setRGB(x, y, (bit << 24) | (bit >> 8));
                }
            }
        } else
            throw new UnsupportedOperationException("Unsupported RGB format.");
        return bufferedImage;
    }
}
