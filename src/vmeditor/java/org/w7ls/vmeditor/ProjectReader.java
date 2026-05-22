package org.w7ls.vmeditor;

import java.io.*;
import java.util.Objects;

import static org.w7ls.vmeditor.WinMain.pathDefaultXml;

public class ProjectReader implements AutoCloseable {
    private final FileInputStream prjStream;
    private Project project;

    public ProjectReader(File prjFile) throws FileNotFoundException {
        this.prjStream = new FileInputStream(prjFile);
        load();
    }

    public void load() {
        try {
            ObjectInputStream ois = new ObjectInputStream(prjStream);
            project = (Project) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new ProjectLoadFailedException(e);
        }
    }

    public Project getProject() {
        return project;
    }

    public AudioModification getAudioModification() {
        AudioModification mod = new AudioModification();
        try {
            mod.load(new File(pathDefaultXml));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var eventsMap = project.getMapByName();
        mod.getEvents().forEach(e -> {
            var event = eventsMap.get(e.shortName);
            if (Objects.nonNull(event)) {
                e.audios.clear();
                e.audios.addAll(eventsMap.get(e.shortName).audios);
            }
        });
        return mod;
    }

    public String getProjectName() {
        return project.projectName;
    }

    @Override
    public void close() throws Exception {
        prjStream.close();
    }

    public static class ProjectLoadFailedException extends RuntimeException {
        public ProjectLoadFailedException(String message) {
            super(message);
        }

        public ProjectLoadFailedException() {
            super();
        }

        public ProjectLoadFailedException(Exception e) {
            super(e);
        }
    }
}
