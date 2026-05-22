package org.w7ls.vmeditor;

import java.io.*;

public class ProjectWriter implements AutoCloseable {
    private final ObjectOutputStream prjStream;

    public ProjectWriter(File file) {
        try {
            prjStream = new ObjectOutputStream(new FileOutputStream(file));
        } catch (IOException e) {
            throw new ProjectWriteFailedException(e);
        }
    }

    public void write(Project project) throws IOException {
        prjStream.writeObject(project);
    }

    @Override
    public void close() throws Exception {
        prjStream.close();
    }

    public static class ProjectWriteFailedException extends RuntimeException {
        public ProjectWriteFailedException(String message) {
            super(message);
        }

        public ProjectWriteFailedException() {
            super();
        }

        public ProjectWriteFailedException(IOException e) {
            super(e);
        }
    }
}
