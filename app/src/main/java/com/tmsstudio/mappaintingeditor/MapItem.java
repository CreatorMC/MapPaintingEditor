package com.tmsstudio.mappaintingeditor;

import android.graphics.Bitmap;

public class MapItem {
    private Bitmap icon;
    private String name;
    private String path;
    private long size;
    private WorldType type;

    public MapItem(String name, String path, long size, WorldType type) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
    }

    public MapItem(Bitmap icon, String name, String path, long size, WorldType type) {
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public WorldType getType() {
        return type;
    }
}
