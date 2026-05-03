#include<math.h>
#include<stdio.h>
#include "utils.cuh"

extern "C"
__global__ void ad1(int c, float *p) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i < c)
        p[i] += 1;

}

extern "C"
__global__ void set_2d(int w, int h, float *p) {
    int x = BLOCK_X;
    int y = BLOCK_Y;

    p[y * w + x] += (y * w + x);
}

extern "C"
__global__ void ad1_2d(int w, int h, int *p) {
    int x = BLOCK_X;
    int y = BLOCK_Y;

    p[y * w + x] += 1;
}

extern "C"
__global__ void ad1_even(int w, int h, int *p) {
    int x = BLOCK_X;
    int y = BLOCK_Y;

    int f = p[y * w + x];
    if (f % 2 == 0)
        p[y * w + x] += 1;
}