import org.w7ls.annotation.cuda.*;
import org.w7ls.common.cuda.BindableImage;
import org.w7ls.common.cuda.CUDAKernels;
import org.w7ls.common.cuda.CUImagePtr;
import org.w7ls.common.cuda.CUWrapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.w7ls.annotation.cuda.CUHeader.HeaderFileType.PTX_FILE;
import static org.w7ls.annotation.cuda.CULaunchKernelOption.USER_SETTING;
import static org.w7ls.annotation.cuda.CUTypes.*;

public class JCudaTest {
    public static void main(String[] args) throws Exception {
        int w = 1024, h = 1024;
        CUImagePtr imagePtr = CUImagePtr.newImage(new int[w * h], w, true);
        imagePtr.setSendToHostWhenClose(true);

        try (TestMemFree imageKernel = CUDAKernels.bindInstanceImage(TestMemFree.class, imagePtr)) {
            imageKernel.mandelbrotSet(w, h, imagePtr);
        }
        System.out.println(Arrays.toString(imagePtr.getPointer()));

        BufferedImage image = imagePtr.toBufferedImage(TYPE_INT_RGB);

        ImageIO.write(image, "PNG", new File("results/man.png"));
    }

    @CUHeader(codeType = PTX_FILE, cudaCode = "mandelbrot_set")
    public static abstract class TestMemFree implements BindableImage {
        @CUMethod(functionName = "mandelbrot_set_irgb", args = {_int, _int, p_int})
        @CULaunchKernelOption(w = USER_SETTING, h = USER_SETTING)
        public abstract void mandelbrotSet(int w, int h, CUImagePtr imagePtr);
    }
}
