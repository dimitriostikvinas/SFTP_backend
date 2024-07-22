package com.example.demo.model;

public class FileInfo {
    private final String filename;
    private final String filetype;
    private final long size;
    private final long lastModified;

    public FileInfo(String filename, String filetype, long size, long lastModified) {
        this.filename = filename;
        this.filetype = filetype;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getFilename() {
        return filename;
    }

    public String getFiletype() {
        return filetype;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }
}
