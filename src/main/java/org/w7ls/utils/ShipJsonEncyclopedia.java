package org.w7ls.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;
import org.w7ls.main.APIQueryNames;
import org.w7ls.main.io.JsonMappers;
import org.w7ls.main.io.RequestQueries;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class ShipJsonEncyclopedia {
    private final JsonNode[] encyclopedia;
    private final APIQueryNames.Language language;
    private final String urlShipProfile;
    private final String applicationId;

    public interface ShipSearchType{
        Object getValue();
    }


    public enum ShipTier implements ShipSearchType  {
        I(1),
        II(2),
        III(3),
        IV(4),
        V(5),
        VI(6),
        VII(7),
        VIII(8),
        IX(9),
        X(10),
        XI(11);

        private final int value;

        ShipTier(int value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    public enum Country implements ShipSearchType {
        JAPAN('J'),
        USA('A'),
        USSR('R'),
        UK('K'),
        FRANCE('F'),
        GERMANY('G'),
        ITALY('I'),
        EUROPE('W'),
        SPAIN('S'),
        NETHERLANDS('H'),
        PAN_ASIA('Z'),
        PAN_AMERICA('V'),
        COMMONWEALTH('U');

        private final char value;

        Country(char oneCharName) {
            this.value = oneCharName;
        }

        @Override
        public Object getValue() {
            return this.value;
        }
    }

    public enum ShipType implements ShipSearchType {
        SS("Submarine"),
        DD("Destroyer"),
        CA("Cruiser"),
        BB("Battleship"),
        CV("Aircraft Carrier");

        private final String value;

        ShipType(String value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    public ShipJsonEncyclopedia(@Nullable RequestQueries queries) {
        JsonNode w7lsMeta;
        String baseUrlShip;

        try {
            w7lsMeta = JsonMappers.getJsonByResource("meta.json");
            baseUrlShip = w7lsMeta.get("base_url_ship").asText();
            applicationId = w7lsMeta.get("application_id").asText();
            urlShipProfile = w7lsMeta.get("base_url_ship_prof").asText();

            if (queries == null)
                queries = RequestQueries.create();

            String _lang = queries.getValue(APIQueryNames.Language.keyName());
            if (_lang == null) {
                queries.add(APIQueryNames.Language.keyName(), APIQueryNames.Language.EN.toValue());
                _lang = APIQueryNames.Language.EN.toValue();
            }
            language = APIQueryNames.Language.valueOf(_lang.toUpperCase());

            queries.add("application_id", applicationId)
                    .add("page_no", 1);

            JsonNode page;
            int page_total = Integer.MAX_VALUE;
            int encyclopediaLen = 0;

            JsonNode[] encyclopediaPages = null;
            for (int pn = 1; page_total >= pn; pn++) {
                page = JsonMappers.getJsonRes(baseUrlShip, queries.setValue("page_no", pn));
                if (page == null)
                    throw new RuntimeException(": page is null.");
                if (page_total == Integer.MAX_VALUE) {
                    JsonNode meta = page.get("meta");
                    if (!Objects.equals(page.get("status").asText(), "ok"))
                        throw new RuntimeException(StringTmp.str(" : Error response. [", meta.get("code"), "]"));
                    page_total = meta.get("page_total").asInt();
                    encyclopediaPages = new JsonNode[page_total];
                    encyclopediaLen = meta.get("total").asInt();
                }
                encyclopediaPages[pn - 1] = page.get("data");
            }

            JsonNode tmp;
            encyclopedia = new JsonNode[encyclopediaLen];
            for (int i = 0; i < encyclopediaPages.length; i++) {
                Iterator<JsonNode> enit = encyclopediaPages[i].iterator();
                for (int j = 0; j < 100; j++) {
                    if (!enit.hasNext()) break;
                    tmp = enit.next();
                    encyclopedia[j + 100 * i] = tmp;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable JsonNode getShipProfile(long id) {
        return JsonMappers.getJsonRes(urlShipProfile, RequestQueries.create()
                        .add("application_id", applicationId)
                        .add("language", language.toValue())
                        .add("ship_id", String.valueOf(id))
                , e -> null);
    }

    public JsonNode getShipProfile(String name) {
        return null; // TODO
    }


    public @Nullable JsonNode getShipById(String shipId) {
        for (JsonNode page : encyclopedia) {
            JsonNode ship;
            if ((ship = page.get(shipId)) == null)
                continue;
            return ship;
        }
        return null;
    }

//    public @Nullable List<JsonNode> getShips(@Nullable ShipSearchType...shipSearchTypes) {
//        List<JsonNode>  = Arrays.stream(encyclopediaPages).toList();
//        List<JsonNode> foundShips_ = new ArrayList<>(100 * encyclopediaPages.length);
//
//
//        for (ShipSearchType shipSearchType : shipSearchTypes) {
//            if (shipSearchType instanceof ShipTier tier)
//                foundShips = getShips(foundShips, s -> s.get("tier").asInt() == tier.value);
//            if (shipSearchType instanceof ShipType type)
//                foundShips = getShips(foundShips, s -> Objects.equals(s.get("type").asText(), type.value));
//            if (shipSearchType instanceof Country country)
//                foundShips = getShips(foundShips, s -> Objects.equals(s.get("ship_id_str").asText().toCharArray()[1], country.value));
//        }
//        return foundShips;
//    }
//
//    private static List<JsonNode> getShips(List<JsonNode> ships, Function<JsonNode, Boolean> shipCondition) {
//        List<JsonNode> matchShip = new ArrayList<>();
//        for (JsonNode page : ships) {
//            for (JsonNode ship : page) {
//                if (shipCondition.apply(ship))
//                    matchShip.add(ship);
//            }
//        }
//        return matchShip;
//    }
//
    private JsonNode getShip(Function<JsonNode, Boolean> shipCondition) {
        for (JsonNode ship : encyclopedia) {
            if (shipCondition.apply(ship))
                return ship;
        }
        return null;
    }

    public @Nullable JsonNode getShip(String name) {
        return getShip(s -> s.get("name").asText().equals(name));
    }
}
