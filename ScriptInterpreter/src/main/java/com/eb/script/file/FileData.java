package com.eb.script.file;

/**
 *
 * @author Earl Bosch
 */
public final class FileData {

    public final FileContext fileContext;
    public final byte[] byteData;
    public final String stringData;

    public FileData(FileContext openedFile, byte[] byteData) {
        this.fileContext = openedFile;
        this.byteData = byteData;
        this.stringData = null;
    }

    public FileData(FileContext openedFile, String stringData) {
        this.fileContext = openedFile;
        this.byteData = null;
        this.stringData = stringData;
    }

    public Object getData() {
        if (stringData == null) {
            return byteData;
        } else {
            return stringData;
        }
    }

    public FileContext getOpenedFile() {
        return fileContext;
    }

    public String getPathString() {
        if (fileContext.path != null) {
            return fileContext.path.toString();
        } else {
            return null;
        }
    }
}
