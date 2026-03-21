#define EGNBST_DR_DF 20.0
#define EGNBST_RC_DF 40.0
#define KNOT_PER_MPS 2.6854

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
    const int _i = blockIdx.x * blockDim.x + threadIdx.x;
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
    const int _i = blockIdx.x * blockDim.x + threadIdx.x;
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
    const int _i = blockIdx.x * blockDim.x + threadIdx.x;
    const float arr_time = arrival_time_(c, _i, egnbst_d, i_bst_d, i_bst_s, smn, smd, smx, egn_cooling_d, egn_cooling_c, dh);
    const float comp_time = arr_time + fast_atk_time;
    const float atk_rate = 60.0 / max(comp_time, rest_time + fast_atk_time);
    p[_i] = atk_rate * dpw;
}


extern "C"
// pc個同時に呼び出す p = [cv1data, cv2data, ..]
// TODO
__global__ void dpm_cv_super16(int c, int pc, double *p,
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
    const int x = blockIdx.x * blockDim.x + threadIdx.x;
    const int y = blockIdx.y * blockDim.y + threadIdx.y;
    const int _i = blockIdx.x * blockDim.x + threadIdx.x;
    const float arr_time = arrival_time_(c, _i, egnbst_d, i_bst_d, i_bst_s, smn, smd, smx, egn_cooling_d, egn_cooling_c, dh);
    const float comp_time = arr_time + fast_atk_time;
    const float atk_rate = 60.0 / comp_time;
    p[_i] = atk_rate * dpw;
}
