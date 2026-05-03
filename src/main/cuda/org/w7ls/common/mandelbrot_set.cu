#include <math.h>
#include <cuda_runtime.h>
#include "utils.cuh"

__device__ void mandelbrot_calc(int x, int y, int w, int h,
                                 float* r, float* g, float* b) {
    float cx = (x - w * 0.5f) / (w * 0.3f) - 0.5f;
    float cy = (y - h * 0.5f) / (h * 0.3f);

    float zx = 0, zy = 0;
    int max = 512;
    int c;
    for (c = 0; c < max; c++) {
        float tmp = zx * zx - zy * zy + cx;
        zy = 2.0f * zx * zy + cy;
        zx = tmp;
        if (zx * zx + zy * zy > 4.0f) break;
    }

    if (c >= max) {
        *r = 0; *g = 0; *b = 0;
    } else {
        float t = logf((float)c + 1.0f) / logf((float)max + 1.0f);
        *r = 0.5f - 0.5f * cosf(3.14159f * t);
        *g = 0.5f - 0.5f * cosf(3.14159f * t * 2.0f);
        *b = 0.5f - 0.5f * cosf(3.14159f * t * 3.0f);
    }
}

extern "C"
__global__ void mandelbrot_set(int w, int h, float *r, float *g, float *b) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= w || y >= h) return;
    int i = y * w + x;

    mandelbrot_calc(x, y, w, h, &r[i], &g[i], &b[i]);
}

extern "C"
__global__ void mandelbrot_set_irgb(int w, int h, int *rgb) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= w || y >= h) return;

    float r, g, b;
    mandelbrot_calc(x, y, w, h, &r, &g, &b);
    rgb[y * w + x] = to_argb(r, g, b);
}