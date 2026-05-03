package org.w7ls.common.io;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RequestQueries {
    List<String> _key;
    List<String> _value;

    public RequestQueries() {
        _key = new ArrayList<>();
        _value = new ArrayList<>();
    }

    public RequestQueries(List<String> keys, List<String> values) {
        _key = keys;
        _value = values;
    }

    public static RequestQueries create() {
        return new RequestQueries();
    }

    public @Nullable String getValue(String key) {
        int i;
        if ((i = _key.indexOf(key)) != -1)
            return _value.get(i);
        return null;
    }

    public RequestQueries setValue(String key, String value) {
        int i = _key.indexOf(key);
        if (i != -1)
            _value.set(i, value);
        return this;
    }

    public RequestQueries setValue(String key, long value) {
        int i = _key.indexOf(key);
        if (i != -1)
            _value.set(i, String.valueOf(value));
        return this;
    }

    public void merge(RequestQueries req) {
        for (String k : req.getKeys()) {
            add(k, req.getValue(k));
        }
    }

    public RequestQueries add(String key, String value) {
        if (getValue(key) != null)
            return this;

        _key.add(key);
        _value.add(value);
        return this;
    }

    public RequestQueries add(String key, double value) {
        if (getValue(key) != null)
            return this;

        _key.add(key);
        _value.add(String.valueOf(value));
        return this;
    }

    public RequestQueries add(String key, long value) {
        if (getValue(key) != null)
            return this;

        _key.add(key);
        _value.add(String.valueOf(value));
        return this;
    }

    List<String> getKeys() {
        return _key;
    }

    List<String> getValues() {
        return _value;
    }
}
