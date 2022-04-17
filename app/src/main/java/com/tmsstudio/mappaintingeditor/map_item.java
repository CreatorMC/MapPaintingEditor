package com.tmsstudio.mappaintingeditor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class map_item extends AppCompatActivity {
    String map_name;     //地图名称
    String map_size;
    String version;
    File folder;
    DocumentFile documentFile;
//    int version;

    public map_item(){
        this.map_name = null;
        this.map_size = null;
        this.folder = null;
        this.documentFile = null;
        this.version = null;
    }

    public map_item(String map_name, String map_size, File folder, String version) {
        this.map_name = map_name;
        this.map_size = "大小:" + map_size;
        this.folder = folder;
        this.version = version;
    }

    public map_item(String map_name, String map_size, DocumentFile folder, String version) {
        this.map_name = map_name;
        this.map_size = "大小:" + map_size;
        this.documentFile = folder;
        this.version = version;
    }

    public String getMap_name() {
        return map_name;
    }

    public String getMap_size() {
        return map_size;
    }

    public File getFolder() {
        return folder;
    }

    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    public String getVersion() {
        return version;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_item);
    }
}
