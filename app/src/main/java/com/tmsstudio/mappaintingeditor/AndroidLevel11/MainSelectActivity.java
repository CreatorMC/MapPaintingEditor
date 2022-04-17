package com.tmsstudio.mappaintingeditor.AndroidLevel11;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tmsstudio.mappaintingeditor.MainActivity;
import com.tmsstudio.mappaintingeditor.MapItemAdapter;
import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.R;
import com.tmsstudio.mappaintingeditor.Util;
import com.tmsstudio.mappaintingeditor.map_item;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainSelectActivity extends MainActivity {

    private List<map_item> list = new ArrayList<>();    //每一个地图存档
    ArrayList<? extends HashMap<String, Object>> app_information = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity11_main_select);
        setTitle("请选择一个世界");

        app_information = getIntent().getParcelableArrayListExtra(Message.EXTRA_MESSAGE_TO_MAINSELECT);

        checkPermission();
    }

    private void init_map_item() {
        Log.i("TMS", "安卓11初始化地图");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.i("TMS", "安卓11加载地图中");
                try {
                    File storage = Environment.getExternalStorageDirectory();                       //sd存储根目录
                    Log.i("TMS", "init_map_item: " + storage);
                    String file_path = "";                                                          //国际基岩版
                    String file_path_netease = "";                                                  //网易基岩版
                    String file_path_netease_test = "";                                             //网易测试基岩版
                    for (HashMap<String, Object> t : app_information) {
                        if ("Minecraft".equals(t.get("appName").toString())) {
                            file_path = (String) t.get("packageName");
                        } else if ("我的世界".equals(t.get("appName").toString())) {
                            file_path_netease = (String) t.get("packageName");
                        } else if ("我的世界测试版".equals(t.get("appName").toString())) {
                            file_path_netease_test = (String) t.get("packageName");
                        }
                    }
                    //国际版
                    DocumentFile documentFile;
                    DocumentFile doc;
                    DocumentFile[] dir_list;
                    if (!file_path.equals("")) {
                        documentFile = DocumentFile.fromTreeUri(this, Uri.parse(Util.changeToUri("/storage/emulated/0/Android/data")));
                        doc = documentFile.findFile(file_path).findFile("files").findFile("games").findFile("com.mojang").findFile("minecraftWorlds");
                        dir_list = doc.listFiles();
                        addListItem(dir_list, INTERNATIONAL);
                    }
                    //网易版
                    if (!file_path_netease.equals("")) {
                        documentFile = DocumentFile.fromTreeUri(this, Uri.parse(Util.changeToUri("/storage/emulated/0/Android/data")));
                        doc = documentFile.findFile(file_path_netease).findFile("files").findFile("minecraftWorlds");
                        dir_list = doc.listFiles();
                        addListItem(dir_list, NETEASE);
                    }
                    //网易测试版
                    if (!file_path_netease_test.equals("")) {
                        documentFile = DocumentFile.fromTreeUri(this, Uri.parse(Util.changeToUri("/storage/emulated/0/Android/data")));
                        doc = documentFile.findFile(file_path_netease_test).findFile("files").findFile("minecraftWorlds");
                        dir_list = doc.listFiles();
                        addListItem(dir_list, NETEASETEST);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_11);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        MapItemAdapter11 adapter = new MapItemAdapter11(list);
        recyclerView.setAdapter(adapter);
    }


    private void addListItem(DocumentFile[] dir_list, int version) throws Exception {
        if(dir_list != null){
            String ver = "";
            if(version == INTERNATIONAL){
                ver = "  [国际版]";
            } else if(version == NETEASE){
                ver = "  [网易版]";
            } else if(version == NETEASETEST){
                ver = "  [网易测试版]";
            }
            for(DocumentFile t: dir_list){
                DocumentFile file = t.findFile("levelname.txt");
                inputStream = Util.getInputStream(this, file);
                reader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(reader);
                StringBuilder result = new StringBuilder();
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    result.append(temp);
                }
                map_item item = new map_item(result.toString(), Util.FormetFileSize(Util.getFileSizes(t)), t, ver);
                list.add(item);
            }
        }
    }

    private void checkPermission() {
        if(!Util.isGrant(this, "/storage/emulated/0/Android/data")){
            //未授权，开始授权
            Toast.makeText(this, "请同意在此目录下的授权，否则软件无法正常操作。", Toast.LENGTH_LONG).show();
            Util.startFor("/storage/emulated/0/Android/data", this, Message.REQUEST_EMPOWER);
        } else {
            init_map_item();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if (data == null) {
            return;
        }
        if (requestCode == Message.REQUEST_EMPOWER && (uri = data.getData()) != null) {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));//保存这个目录的访问权限
            Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
            init_map_item();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
