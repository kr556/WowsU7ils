package org.w7ls.main;

import java.util.HashMap;

public class Logger {
    private HashMap<String, String> log = new HashMap<>();

    public Logger() {
    }

    public void putLog(String name, String log) {
        this.log.put(name, log);
    }

    public String getLog(String name) {
        return this.log.get(name);
    }

    public String[] getLogNames() {
        return this.log.keySet().toArray(new String[0]);
    }
}
