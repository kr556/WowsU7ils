#include<math.h>

extern "C"
__global__ void Test(int c, float *p ,float *arg) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i < c)
        p[i] = arg[i] * i;

}
