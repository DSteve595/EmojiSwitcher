package com.stevenschoen.emojiswitcher;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;

import java.io.File;
import java.io.IOException;

public class EmojiSet extends EmojiSetListing {

    public File path;

    private String fileMd5;

    public EmojiSet(File path) {
        this.path = path;
    }

    public String getFileMd5() throws IOException {
        if (fileMd5 == null) {
            fileMd5 = Files.hash(path, Hashing.md5()).toString();
        }

        return fileMd5;
    }

    public boolean isIntact() throws IOException {
        return (getFileMd5().equals(md5));
    }
}