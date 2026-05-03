package org.w7ls.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.w7ls.common.io.RequestQueries;
import org.w7ls.common.utils.ShipJsonEncyclopedia;

class ShipJsonEncyclopediaTest {
    @Test
    void testConstructor() {
        ShipJsonEncyclopedia sje = new ShipJsonEncyclopedia(RequestQueries.create()
                .add("language", "ja"));

        JsonNode ship = sje.getShip("白龍");
    }

    @Test
    void getShipProfile() {
    }

    @Test
    void testGetShipProfile() {
    }

    @Test
    void getShipById() {
    }

    @Test
    void getShip() {
    }
}