package com.stevenschoen.emojiswitcher;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmojiSet {
    private String friendlyName;
    private File path;
    private String hash;

    public EmojiSet(File path) {
        this.path = path;

        friendlyName = FilenameUtils.removeExtension(path.getName());
        if (filenamesToFriendlyNames.containsKey(friendlyName)) {
            friendlyName = filenamesToFriendlyNames.get(friendlyName);
        }
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public File getPath() {
        return path;
    }

    public String getHash() throws IOException {
        if (hash == null || hash.length() ==  0) {
            hash = Files.hash(getPath(), Hashing.md5()).toString();
        }

        return hash;
    }

    @Override
    public String toString() {
        return getFriendlyName();
    }

    private static Map<String, String> filenamesToFriendlyNames = new HashMap<>();
    static {
        filenamesToFriendlyNames.put("GoogleLollipop", "Google (Lollipop)");
        filenamesToFriendlyNames.put("GoogleKitkat", "Google (KitKat)");
        filenamesToFriendlyNames.put("HTCM8", "HTC M8");
        filenamesToFriendlyNames.put("LGG3", "LG G3");
        filenamesToFriendlyNames.put("SamsungS4", "Samsung S4");
    }
}