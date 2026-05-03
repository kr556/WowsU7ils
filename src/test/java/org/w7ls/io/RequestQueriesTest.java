package org.w7ls.io;

import org.junit.jupiter.api.Test;
import org.w7ls.common.io.RequestQueries;

class RequestQueriesTest {
    @Test
    void merge() {
        RequestQueries r0 = RequestQueries.create()
                .add("a", "aa")
                .add("b", "bb");
        RequestQueries r1 = RequestQueries.create()
                .add("c", "cc")
                .add("d", "dd");


        r0.merge(r1);

        System.out.println(r0);
    }
}