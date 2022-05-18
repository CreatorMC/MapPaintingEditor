package com.tmsstudio.mappaintingeditor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tmsstudio.mappaintingeditor.Message.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    public static final int INTERNATIONAL = 0;          //国际版
    public static final int NETEASE = 1;                //网易版
    public static final int NETEASETEST = 2;            //网易测试版
    public static final int OLDINTERNATIONAL = 4;       //存档迁移前的国际版
    public static final int DATAAPP = 3;                //此应用
    protected static final int REQUEST_EXTERNAL_STORAGE = 1;
    protected static final int REQUEST_11_EXTERNAL_STORAGE = 1024;
    protected static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final List<MapItem> mapItemList = new ArrayList<>();    //每一个地图存档
    protected MapItemAdapter mapItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mapItemAdapter = new MapItemAdapter(mapItemList);
        recyclerView.setAdapter(mapItemAdapter);

        checkPermission();      //检查权限

    }

    /**
     * 初始化数据
     */
    private void initData() {
        mapItemList.clear();
        mapItemAdapter.notifyDataSetChanged();
        Observable.create(new ObservableOnSubscribe<MapItem>() {
                    @Override
                    public void subscribe(ObservableEmitter<MapItem> emitter) throws Exception {
                        //单独扫描
                        forEach(emitter, new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftWorlds"), WorldType.OutdatedBedrock);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            //android 10 以上处理
                            DocumentFile documentFile = getDataDocumentFile();
                            for (DocumentFile file : documentFile.listFiles()) {
                                {
                                    DocumentFile folder = findDocumentFile(file, "files/minecraftWorlds");
                                    if (folder != null) {
                                        forEach(emitter, folder, WorldType.NetEaseBedrock);
                                        continue;
                                    }
                                }
                                {
                                    DocumentFile folder = findDocumentFile(file, "files/games/com.mojang/minecraftWorlds");
                                    if (folder != null) {
                                        try {
                                            PackageInfo packageInfo = getPackageManager().getPackageInfo(file.getName(), 0);
                                            File so = new File(packageInfo.applicationInfo.nativeLibraryDir, "libminecraftpe.so");
                                            if (so.exists()) {
                                                forEach(emitter, folder, WorldType.Bedrock);
                                            } else {
                                                throw new RuntimeException("not minecraft");
                                            }
                                        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                                            forEach(emitter, folder, WorldType.Unknown);
                                        }
                                    }
                                }
                            }

                        } else {
                            //android 11 以下处理
                            File file = new File(Environment.getExternalStorageDirectory(), "Android/data");
                            File[] files = file.listFiles();
                            if (files != null) {
                                for (File f : files) {
                                    Log.i("TAG", "subscribe: " + f.getAbsolutePath());
                                    {
                                        File folder = new File(f, "files/minecraftWorlds");
                                        if (folder.exists()) {
                                            forEach(emitter, folder, WorldType.NetEaseBedrock);
                                            continue;
                                        }
                                    }
                                    {
                                        File folder = new File(f, "files/games/com.mojang/minecraftWorlds");
                                        if (folder.exists()) {
                                            try {
                                                PackageInfo packageInfo = getPackageManager().getPackageInfo(file.getName(), 0);
                                                File so = new File(packageInfo.applicationInfo.nativeLibraryDir, "libminecraftpe.so");
                                                if (so.exists()) {
                                                    forEach(emitter, folder, WorldType.Bedrock);
                                                } else {
                                                    throw new RuntimeException("not minecraft");
                                                }
                                            } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                                                forEach(emitter, folder, WorldType.Unknown);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        emitter.onComplete();
                    }

                    private DocumentFile findDocumentFile(DocumentFile file, String path) {
                        if (file.isDirectory()) {
                            String[] names = path.split(File.separator);
                            for (String name : names) {
                                file = file.findFile(name);
                                if (file == null) {
                                    return null;
                                }
                            }
                            return file;
                        }
                        return null;
                    }

                    private void forEach(ObservableEmitter<MapItem> emitter, File dir, WorldType type) {
                        if (dir.isDirectory()) {
                            File[] files = dir.listFiles();
                            if (files == null) {
                                return;
                            }
                            for (File file : files) {
                                File levelname = new File(file, "levelname.txt");
                                File db = new File(file, "db");
                                File icon = new File(file, "world_icon.jpeg");
                                if (levelname.exists() && levelname.isFile() && db.exists() && db.isDirectory()) {
                                    try (FileInputStream inputStream = new FileInputStream(levelname)) {
                                        String name = readText(inputStream);
                                        if (icon.exists() && icon.isFile()) {
                                            Bitmap bitmap = BitmapFactory.decodeFile(icon.getAbsolutePath());
                                            emitter.onNext(new MapItem(bitmap, name, file.getAbsolutePath(), Util.getFileSizes(file), type));
                                        } else {
                                            emitter.onNext(new MapItem(name, file.getAbsolutePath(), Util.getFileSizes(file), type));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    private void forEach(ObservableEmitter<MapItem> emitter, DocumentFile dir, WorldType type) {
                        if (dir.isDirectory()) {
                            DocumentFile[] files = dir.listFiles();
                            for (DocumentFile file : files) {
                                DocumentFile levelname = dir.findFile("levelname.txt");
                                DocumentFile db = dir.findFile("db");
                                DocumentFile icon = dir.findFile("world_icon.jpeg");
                                if (levelname != null && levelname.isFile() && db != null && db.isDirectory()) {
                                    try (InputStream inputStream = getContentResolver().openInputStream(levelname.getUri())) {
                                        String name = readText(inputStream);
                                        if (icon != null && icon.isFile()) {
                                            InputStream in = getContentResolver().openInputStream(icon.getUri());
                                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                                            in.close();
                                            emitter.onNext(new MapItem(bitmap, name, toPath(file), Util.getFileSizes(file), type));
                                        } else {
                                            emitter.onNext(new MapItem(name, toPath(file), Util.getFileSizes(file), type));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    /**
                     * 读取文本
                     *
                     * @param inputStream
                     * @return
                     * @throws IOException
                     */
                    private String readText(InputStream inputStream) throws IOException {
                        StringBuilder builder = new StringBuilder();
                        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                            String temp;
                            while ((temp = bufferedReader.readLine()) != null) {
                                builder.append(temp);
                            }
                        }
                        return builder.toString();
                    }

                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MapItem>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MapItem mapItem) {
                        mapItemList.add(mapItem);
                        mapItemAdapter.notifyItemInserted(mapItemList.size());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "发生错误:" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(MainActivity.this, "初始化完成", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 获取data文件对象
     *
     * @return
     */
    private DocumentFile getDataDocumentFile() {
        return DocumentFile.fromTreeUri(this, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"));
    }

    /**
     * 构造文件对象
     *
     * @param path
     * @return
     */
    private DocumentFile toDocumentFile(String path) {
        DocumentFile documentFile = getDataDocumentFile();
        String[] names = path.replaceFirst(new File(Environment.getExternalStorageDirectory(), "Android/data").getAbsolutePath() + File.separator, "").split(File.separator);
        for (String name : names) {
            documentFile = documentFile.findFile(name);
            if (documentFile == null) {
                return null;
            }
        }
        return documentFile;
    }

    /**
     * 转为路径
     *
     * @param file
     * @return
     */
    public String toPath(DocumentFile file) {
        Uri uri = file.getUri();
        return Environment.getExternalStorageDirectory().getAbsolutePath() + uri.getPath().replaceFirst("tree/primary:Android/data/document/primary:", "");
    }

    /**
     * 权限
     */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "请允许相关权限，否则无法正常使用本应用！", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_11_EXTERNAL_STORAGE);
            } else {
                initData();
            }

        } else {
            //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "请允许相关权限，否则无法正常使用本应用！", Toast.LENGTH_LONG).show();
                }
                //申请权限
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } else {
                initData();    //初始化，获得地图列表
            }
        }

    }

    /**
     * 显示关于对话框
     */
    private void showNormalDialog() {
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
        switch (item.getItemId()) {
            case R.id.menu_about:
                showNormalDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
                    initData();    //初始化，获得地图列表
                } else {
                    Toast.makeText(this, "授权被拒绝！", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_11_EXTERNAL_STORAGE: {
                Log.i("TMS", "安卓11授权返回");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // 检查是否有权限
                    if (Environment.isExternalStorageManager()) {
                        initData();
                    } else {
                        Toast.makeText(this, "授权被拒绝！", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
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
            case (Message.SELECT_MAP_FINISH): {
                if (resultCode == Activity.RESULT_OK) {
                    //选择地图完成，返回主界面时
                    checkPermission();
                }
            }
        }
    }

    public void refreshList(MenuItem item) {
        checkPermission();
        Toast.makeText(this, "刷新完成", Toast.LENGTH_SHORT).show();
    }

    public void getHelp(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, HelpActivity.class);
        startActivity(intent);
    }
}
