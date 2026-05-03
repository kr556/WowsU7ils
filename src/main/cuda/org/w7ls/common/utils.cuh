#define BLOCK_X (blockIdx.x * blockDim.x + threadIdx.x)
#define BLOCK_Y (blockIdx.y * blockDim.y + threadIdx.y)
#define BLOCK_Z (blockIdx.z * blockDim.z + threadIdx.z)

__device__ __forceinline__ unsigned long deva_f_(float* d) {
    return (unsigned long)d;
}

__device__ __forceinline__ unsigned long deva_f_(int* d) {
    return (unsigned long)d;
}

// device address. pのアドレスを取得する
extern "C"
__global__ __forceinline__ void deva_f(unsigned long* p, float* d) {
    p[0] = deva_f_(d);
}

extern "C"
__global__ __forceinline__ void deva_i(unsigned long* p, int* d) {
    p[0] = deva_f_(d);
}

__device__ __forceinline__ int to_argb(float r, float g, float b, float a) {
    unsigned int R = (unsigned int)(__saturatef(r) * 255.0f);
    unsigned int G = (unsigned int)(__saturatef(g) * 255.0f);
    unsigned int B = (unsigned int)(__saturatef(b) * 255.0f);
    unsigned int A = (unsigned int)(__saturatef(a) * 255.0f);

    return (A << 24) | (R << 16) | (G << 8) | B;
}

__device__ __forceinline__ int to_argb(float r, float g, float b) {
    return to_argb(r, g, b, 1.0f);
}