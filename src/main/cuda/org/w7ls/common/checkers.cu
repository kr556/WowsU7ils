#include <stdint.h>
#include "utils.cuh"

#define EGNBST_DR_DF 20.0
#define EGNBST_RC_DF 40.0
#define KNOT_PER_MPS 2.6854
#define PI 3.14159265359

// 貫通判定
#define AR_NONE 0b00000000
#define AR_SHIP 0b00000010
#define AR_VP   0b00000100
// 貫通判定によるダメージ補正
#define damage_corr(f) (AR_NONE + \
                        ((AR_SHIP & (f)) >> 1) * 0.3333333333333 + \
                        ((AR_VP & (f)) >> 2))


// unsigned int型の装甲モデルを装甲厚に変換する. 2層分の装甲を保存できる. (上位16bit...VP装甲厚, 下位16bit...通常装甲厚) = 装甲厚mm
// vp
#define armor_tr_vp(i) (i >> 16)
// normal
#define armor_tr_nl(i) (i & 0xffff)

#define HE 0b001
#define SAP 0b010
#define AP 0b100

#define pow2(a) (a * a)
#define pow3(a) (a * a * a)
#define distance(x, y) sqrt(pow2(x) + pow2(y))

__device__ double arrival_time_(int c, int i_,
                              float egnbst_d,
                              float i_bst_d,
                              float i_bst_s,
                              float smn,
                              float smd,
                              float smx,
                              float egn_cooling_d,
                              float egn_cooling_c,
                              float dh) {
    if (i_ >= c) return 0.0;

    const float dist = i_ * dh;
    const float dt = 0.01;
    const float egnbst_ruse = 0.2 * egnbst_d;
    const float egnbst_rc = EGNBST_DR_DF;
    const float rcrate = egnbst_d / egnbst_ruse * EGNBST_RC_DF;

    float range_now = 0;
    float speed_now = 0;
    float bsting_time = egnbst_d;
    float time = 0;
    bool bst_sts = true;
    bool enable_egn_bst = true;
    for (; range_now < dist; time += dt) {
        if (bst_sts) {
            speed_now = smx;
            if ((bsting_time -= dt) <= 0) {
                if (enable_egn_bst) {
                    bsting_time += egn_cooling_d + egnbst_d;
                    enable_egn_bst = false;
                    goto check_bst;
                }
                bst_sts = false;
            }
            check_bst:;
        } else {
            speed_now = smd;
            if ((bsting_time += (dt * rcrate)) >= egnbst_ruse)
                bst_sts = true;
        }

        if ((i_bst_d -= dt) > 0)
            speed_now *= i_bst_s;
        range_now += speed_now * dt * KNOT_PER_MPS / 1000.0;
    }
    return time;
}

extern "C"
__global__ void arrival_time(int c, double *p,
                             float egnbst_d,
                             float i_bst_d,
                             float i_bst_s,
                             float smn,
                             float smd,
                             float smx,
                             float egn_cooling_d,
                             float egn_cooling_c,
                             float dh) {
    const int _i = BLOCK_X;
    p[_i] = arrival_time_(c, _i, egnbst_d, i_bst_d, i_bst_s, smn, smd, smx, egn_cooling_d, egn_cooling_c, dh);
}

extern "C"
__global__ void dpm_cv(int c, double *p,
                       float egnbst_d,
                       float i_bst_d,
                       float i_bst_s,
                       float smn,
                       float smd,
                       float smx,
                       float egn_cooling_d,
                       float egn_cooling_c,
                       float dh,

                       float fast_atk_time,
                       float dpw,
                       float rest_time) {
    const int _i = BLOCK_X;
    const float arr_time = arrival_time_(c, _i, egnbst_d, i_bst_d, i_bst_s, smn, smd, smx, egn_cooling_d, egn_cooling_c, dh);
    const float comp_time = arr_time + fast_atk_time;
    const float atk_rate = 60.0 / comp_time;
    p[_i] = atk_rate * dpw;
}

extern "C"
__global__ void dpm_cv_tac(int c, double *p,
                       float egnbst_d,
                       float i_bst_d,
                       float i_bst_s,
                       float smn,
                       float smd,
                       float smx,
                       float egn_cooling_d,
                       float egn_cooling_c,
                       float dh,

                       float fast_atk_time,
                       float dpw,
                       float rest_time) {
    const int _i = BLOCK_X;
    const float arr_time = arrival_time_(c, _i, egnbst_d, i_bst_d, i_bst_s, smn, smd, smx, egn_cooling_d, egn_cooling_c, dh);
    const float comp_time = arr_time + fast_atk_time;
    const float atk_rate = 60.0 / max(comp_time, rest_time + fast_atk_time);
    p[_i] = atk_rate * dpw;
}

__device__ __forceinline__ float h_c(float p, float ac, float rc) {
    return p / (ac * pow2(rc) * PI);
}

__device__ __forceinline__ float h_o(float p, float ao, float ac, float ro, float rc) {
    return (1 - p) / (PI * (ao * pow2(ro) - ac * pow2(rc)));
}

__device__ __forceinline__ bool ellipse_contains(
        float px, float py,
        float cx, float cy,
        float rx, float ry) {
    float dx = (px - cx) / rx;
    float dy = (py - cy) / ry;
    return dx * dx + dy * dy <= 1.0f;
}

__device__ float d_check_dmg(float x, float y,
                             float pen,
                             // 爆弾側のパラメータ
                             float p,
                             float wo, float ho,
                             float wc, float hc,
                             // 船体側のパラメータ
                             int* arm) {
    if (ellipse_contains(x, y, 0.0f, 0.0f, wo / 2, ho / 2)) {
        const float mo = max(wo, ho);
        const float aspo = mo == wo ?
                     wo / ho :
                     ho / wo;
        const float mc = max(wc, hc);
        const float aspc = mc == wc ?
                     wc / hc :
                     hc / wc;
        const float rc = min(wc, hc) / 2;
        if (ellipse_contains(x, y, 0.0f, 0.0f, wc / 2, hc / 2))
//            return h_c(p, aspo, min(wc, hc) / 2);
//        return h_o(p, aspo, aspc, min(wo, ho), rc);
    }
    return 0.0f;
}
