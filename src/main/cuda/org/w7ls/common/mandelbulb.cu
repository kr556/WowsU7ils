// mandelbulb.cu
#define MAX_STEPS 100
#define MAX_DIST  100.0f
#define SURF_DIST 0.001f

extern "C"
__device__ float3 normalize(float3 v) {
    float len = sqrtf(v.x*v.x + v.y*v.y + v.z*v.z);
    return make_float3(v.x/len, v.y/len, v.z/len);
}

extern "C"
__device__ float mandelbulbDE(float3 pos, int maxIter, float power) {
    float3 z = pos;
    float dr = 1.0f;
    float r  = 0.0f;

    for (int i = 0; i < maxIter; i++) {
        r = sqrtf(z.x*z.x + z.y*z.y + z.z*z.z);
        if (r > 2.0f) break;

        // 球座標変換
        float theta = acosf(z.z / r);
        float phi   = atan2f(z.y, z.x);
        dr = powf(r, power - 1.0f) * power * dr + 1.0f;

        // n乗
        float rn = powf(r, power);
        z.x = rn * sinf(power * theta) * cosf(power * phi) + pos.x;
        z.y = rn * sinf(power * theta) * sinf(power * phi) + pos.y;
        z.z = rn * cosf(power * theta)                     + pos.z;
    }
    return 0.5f * logf(r) * r / dr; // Distance Estimator
}

extern "C"
__device__ float3 calcNormal(float3 p, int maxIter, float power) {
    float e = 0.001f;
    float3 n = make_float3(
            mandelbulbDE(make_float3(p.x+e, p.y,   p.z),   maxIter, power)
            - mandelbulbDE(make_float3(p.x-e, p.y,   p.z),   maxIter, power),
            mandelbulbDE(make_float3(p.x,   p.y+e, p.z),   maxIter, power)
            - mandelbulbDE(make_float3(p.x,   p.y-e, p.z),   maxIter, power),
            mandelbulbDE(make_float3(p.x,   p.y,   p.z+e), maxIter, power)
            - mandelbulbDE(make_float3(p.x,   p.y,   p.z-e), maxIter, power)
    );
    float len = sqrtf(n.x*n.x + n.y*n.y + n.z*n.z);
    return make_float3(n.x/len, n.y/len, n.z/len);
}

extern "C"
__global__ void mandelbulb(
        int w, int h,
        float* r, float* g, float* b,
        float c_x, float c_y, float c_z,
        float power, int maxIter) {
    int px = blockIdx.x * blockDim.x + threadIdx.x;
    int py = blockIdx.y * blockDim.y + threadIdx.y;
    if (px >= w || py >= h) return;

    int idx = py * w + px;

    // レイの方向
    float u = (2.0f * px - w) / h;
    float v = (2.0f * py - h) / h;

    float3 ro = make_float3(c_x, c_y, c_z); // カメラ位置
    float3 rd = normalize(make_float3(u, v, -1.5f)); // レイ方向

    // レイマーチング
    float t = 0.0f;
    float3 col = make_float3(0.0f, 0.0f, 0.0f);

    for (int s = 0; s < MAX_STEPS; s++) {
        float3 p = make_float3(
                ro.x + rd.x * t,
                ro.y + rd.y * t,
                ro.z + rd.z * t
        );
        float d = mandelbulbDE(p, maxIter, power);
        if (d < SURF_DIST) {
            // 法線計算 → ランバート拡散光
            float3 n   = calcNormal(p, maxIter, power);
            float3 lig = normalize(make_float3(1.0f, 1.0f, 1.0f));
            float  dif = fmaxf(n.x*lig.x + n.y*lig.y + n.z*lig.z, 0.0f);
            col = make_float3(dif * 0.8f + 0.1f, dif * 0.5f + 0.05f, dif * 0.3f);
            break;
        }
        if (t > MAX_DIST) break;
        t += d;
    }

    r[idx] = col.x;
    g[idx] = col.y;
    b[idx] = col.z;
}