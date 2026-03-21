package org.w7ls.main.io;

import org.junit.jupiter.api.Test;

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