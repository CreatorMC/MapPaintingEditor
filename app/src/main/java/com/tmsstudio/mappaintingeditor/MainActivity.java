package com.tmsstudio.mappaintingeditor;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tmsstudio.mappaintingeditor.AndroidLevel11.MainSelectActivity;
import com.tmsstudio.mappaintingeditor.Message.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<map_item> list = new ArrayList<>();    //每一个地图存档
    private ArrayList<HashMap<String, Object>> app_information = null;
    protected static final int REQUEST_EXTERNAL_STORAGE = 1;
    protected static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    protected static InputStream inputStream = null;
    protected static Reader reader = null;
    protected static BufferedReader bufferedReader = null;
    public static final int INTERNATIONAL = 0;          //国际版
    public static final int NETEASE = 1;                //网易版
    public static final int NETEASETEST = 2;            //网易测试版
    public static final int DATAAPP = 3;                //此应用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();      //检查权限

    }

    private void init_map_item() {
        list.clear();
        Log.i("TMS", "初始化地图");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.i("TMS", "加载地图中");
                try {
                    ArrayList<HashMap<String, Object>> arrayList = Util.getItems(this);     //得到设备上的所有应用信息
                    app_information = arrayList;
                    File storage = Environment.getExternalStorageDirectory();                       //sd存储根目录
                    Log.i("TMS", "init_map_item: " + storage);
                    String file_path = "";                                                          //国际基岩版
                    String file_path_netease = "";                                                  //网易基岩版
                    String file_path_netease_test = "";                                             //网易测试基岩版
                    for(HashMap<String, Object> t: arrayList){
                        if("Minecraft".equals(t.get("appName").toString())){
                            file_path = storage.getPath() + "/Android/data/" + t.get("packageName") + "/files/games/com.mojang/minecraftWorlds";
                        } else if("我的世界".equals(t.get("appName").toString())){
                            file_path_netease = storage.getPath() + "/Android/data/" + t.get("packageName") + "/files/minecraftWorlds";
                        } else if("我的世界测试版".equals(t.get("appName").toString())) {
                            file_path_netease_test = storage.getPath() + "/Android/data/" + t.get("packageName") + "/files/minecraftWorlds";
                        }
                    }
                    //国际版
                    File dir = new File(file_path);
                    File[] dir_list = dir.listFiles();
                    if(!file_path.equals("")){
                        addListItem(dir_list, INTERNATIONAL);
                    }
                    //网易版
                    if(!file_path_netease.equals("")){
                        dir = new File(file_path_netease);
                        dir_list = dir.listFiles();
                        addListItem(dir_list, NETEASE);
                    }
                    //网易测试版
                    if(!file_path_netease_test.equals("")){
                        dir = new File(file_path_netease_test);
                        dir_list = dir.listFiles();
                        addListItem(dir_list, NETEASETEST);
                    }
                    //软件内部
                    dir = this.getExternalFilesDir("World");
                    dir_list = dir.listFiles();
                    addListItem(dir_list, DATAAPP);

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

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        MapItemAdapter adapter = new MapItemAdapter(list);
        recyclerView.setAdapter(adapter);
    }


    private void addListItem(File[] dir_list, int version) throws Exception {
        if(dir_list != null){
            String ver = "";
            if(version == INTERNATIONAL){
                ver = "[国际版]";
            } else if(version == NETEASE){
                ver = "[网易版]";
            } else if(version == NETEASETEST){
                ver = "[网易测试版]";
            } else if (version == DATAAPP){
                ver = "[此应用]";
            }
            for(File t: dir_list){
                File tempfile = new File(t.getPath());
                File file=new File(tempfile, "levelname.txt");
                inputStream = new FileInputStream(file);
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
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请允许相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);

        } else {
            init_map_item();    //初始化，获得地图列表
        }
    }

    /**
     * 显示关于对话框
     */
    private void showNormalDialog(){
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
        final View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.menu_about, null);
        normalDialog.setTitle("关于");
        normalDialog.setView(dialogView);
        TextView textView = dialogView.findViewById(R.id.about_message_view);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        // 显示
        normalDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //创建菜单
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_about:
                showNormalDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
                    init_map_item();    //初始化，获得地图列表
                } else {
                    Toast.makeText(this, "授权被拒绝！", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //防止因屏幕旋转而刷新界面
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //什么都不做
        } else {
            //什么都不做
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (Message.SELECT_MAP_FINISH):
            {
                if(resultCode == Activity.RESULT_OK){
                    //选择地图完成，返回主界面时
                    checkPermission();
                }
            }
        }
    }

    public void getEmpower(MenuItem item) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("警告")
                .setMessage("此功能专为安卓11及其以上的设备设计。\n\n此功能会让用户选择游戏内的地图，然后复制到本软件内的文件夹里。这样就能使用本软件的功能了。处理结束后，需要用户手动复制文件夹导入地图到游戏。\n\n软件内存储路径：\nAndroid/data/com.tmsstudio.mappaintingeditor/files/World\n\n是否要执行此操作？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "加载时间可能过长，请耐心等待，黑屏是正常现象。", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, MainSelectActivity.class);
                        intent.putExtra(Message.EXTRA_MESSAGE_TO_MAINSELECT, app_information);
                        startActivityForResult(intent, Message.SELECT_MAP_FINISH);
                    }
                })
                .create();
        dialog.show();
    }

    public void refreshList(MenuItem item) {
        checkPermission();
    }

    public void deleteMap(MenuItem item) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("警告")
                .setMessage("此操作会删除带有“[此应用]”字样的地图（也就是导入到本软件内的地图）\n\n确定要继续吗？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File tempDir = MainActivity.this.getExternalFilesDir("World");
                        Util.deleteFile(tempDir);
                        Toast.makeText(MainActivity.this, "删除完成", Toast.LENGTH_LONG).show();
                        checkPermission();
                    }
                })
                .create();
        dialog.show();
    }
}
