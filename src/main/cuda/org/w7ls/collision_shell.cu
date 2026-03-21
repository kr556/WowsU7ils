
// 爆弾の散布を disw * dishの楕円として考え, それにダメージがマッピングされた高さ分の楕円柱となる．
// 楕円柱の堆積は dpa と同じであり，微小面積当たりのダメージをポインタとして返す
extern "C"
__global__ void dmg_dispersion(int w, int h, float *p,
                               int s_w, // 標的となる船の幅
                               int s_h, // 標的となる船の長さ
                               int disw, // 散布界
                               int dish, // 散布界
                               int disw_c, // 中心散布
                               int dish_c, // 中心散布
                               float drop_cntr_per, // 内部楕円への着弾率
                               float dpa // 攻撃ごとの理論ダメージ
                               ) {
    const int x = blockIdx.x * blockDim.x + threadIdx.x;
    const int y = blockIdx.y * blockDim.y + threadIdx.y;
}