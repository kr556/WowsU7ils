#include<math.h>

extern "C"
__global__ void mandelbrot_set(int w, int h, float *r, float *g, float *b) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    int i = y * w + x;
    float px = (x - w * 0.8f) / (w * 0.25f);
    float py = (y - h * 0.5f) / (h * 0.25f);
    float tmp = 0;
    float zx = 0;
    float zy = 0;
    float c;
    float max = 32768;
    for (c = 0; c < max && (zx * zx + zy * zy < 4.0); c++) {
        tmp = zx * zx - zy * zy + px;
        zy = 2.0 * zx * zy + py;
        zx = tmp;
    }
    float res = c / max;
    r[i] = res;
    g[i] = res;
    b[i] = res;
}