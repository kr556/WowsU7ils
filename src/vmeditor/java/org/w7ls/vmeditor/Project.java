package org.w7ls.vmeditor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Project implements Serializable {
    @Serial
    private static final long serialVersionUID = -7845341663023746894L;

    public AudioModification.ExternalEvent[] events;
    public String projectName;

    public Map<String, AudioModification.ExternalEvent> getMapByName() {
        HashMap<String, AudioModification.ExternalEvent> re = new HashMap<>();
        for (var e : events)
            re.put(e.shortName, e);
        return Collections.unmodifiableMap(re);
    }
}
