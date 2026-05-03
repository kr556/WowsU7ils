package org.w7ls.common.cuda;

interface CUReusable {
    boolean isReuse();

    boolean using();

    void used();
}
