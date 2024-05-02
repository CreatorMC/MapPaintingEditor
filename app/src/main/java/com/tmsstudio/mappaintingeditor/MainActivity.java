package com.tmsstudio.mappaintingeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ForwardScope;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final List<MapItem> mapItemList = new ArrayList<>();    //每一个地图存档
    private MapItemAdapter mapItemAdapter;
    private ActivityResultLauncher<Intent> externalStorageResultLauncher;
    private ActivityResultLauncher<Intent> resultLauncher;
    //充当信号量，防止多次异步刷新造成列表显示混乱
    private volatile boolean isInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mapItemAdapter = new MapItemAdapter(mapItemList);
        recyclerView.setAdapter(mapItemAdapter);

        if (checkPermission() && checkExternalStoragePermission() && checkASFPermission()) {
            initData();
        } else {
            externalStorageResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_LONG).show();
                            requestPermissions();
                        } else {
                            Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }
            });
            resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @SuppressLint("WrongConstant")
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                            Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_LONG).show();
                            initData();
                            return;
                        }
                    }
                    Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("需要存储权限才能进行操作，Android 11及以上需要额外授权Android/data目录访问权限")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //不要直接结束应用
                            //finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

    }

    /**
     * 初始化数据
     */
    private synchronized void initData() {
        //如果正在获取数据，直接返回
        if(isInit) {
            return;
        }
        isInit = true;
        Toast.makeText(this, "正在扫描游戏存档中", Toast.LENGTH_LONG).show();
        Log.i("tms", "初始化数据");
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
                                            if (Objects.requireNonNull(file.getName()).endsWith("com.mojang.minecraftpe")) {
                                                forEach(emitter, folder, WorldType.Bedrock);
                                            } else {
                                                throw new RuntimeException("not minecraft");
                                            }
                                        } catch (RuntimeException e) {
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
                                                if (f.toString().endsWith("com.mojang.minecraftpe")) {
                                                    forEach(emitter, folder, WorldType.Bedrock);
                                                } else {
                                                    throw new RuntimeException("not minecraft");
                                                }
                                            } catch (RuntimeException e) {
                                                forEach(emitter, folder, WorldType.Unknown);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        emitter.onComplete();
                    }

                    /**
                     * 根据路径查找对应文件
                     * @param file
                     * @param path
                     * @return
                     */
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
                                DocumentFile levelname = file.findFile("levelname.txt");
                                DocumentFile db = file.findFile("db");
                                DocumentFile icon = file.findFile("world_icon.jpeg");
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

                })
                .subscribeOn(Schedulers.io())
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
                        isInit = false;
                        Toast.makeText(MainActivity.this, "发生错误:" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {
                        isInit = false;
                        Toast.makeText(MainActivity.this, "扫描完成", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        mapItemAdapter.notifyItemRangeRemoved(0, mapItemList.size());
        mapItemList.clear();
        initData();
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
     * 请求全部文件访问权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            externalStorageResultLauncher.launch(intent);
        } else {
            requestPermissions();
        }
    }

    /**
     * 请求多个权限
     */
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
        PermissionX.init(this)
                .permissions(permissions)
                .setDialogTintColor(Color.parseColor("#1972e8"), Color.parseColor("#8ab6f5"))
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList, "打开设置授权", "确定", "取消");
                    }
                })
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
                        if (allGranted) {
                            Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_LONG).show();
                            if (checkASFPermission()) {
                                initData();
                            } else {
                                requestASFPermission();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    /**
     * 请求ASF文件访问框架权限
     */
    private void requestASFPermission() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, getDataDocumentFile().getUri());
        }
        resultLauncher.launch(intent);
    }

    /**
     * 检查存储权限
     *
     * @return
     */
    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查外部存储权限
     *
     * @return
     */
    private boolean checkExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * 检查ASF文件访问框架权限
     *
     * @return
     */
    private boolean checkASFPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return !getContentResolver().getPersistedUriPermissions().isEmpty();
        }
        return true;
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
    public void onConfigurationChanged(Configuration newConfig) {
        //防止因屏幕旋转而刷新界面
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //什么都不做
        } else {
            //什么都不做
        }
    }

    public void refreshList(MenuItem item) {
        refreshData();
        Toast.makeText(this, "刷新完成", Toast.LENGTH_SHORT).show();
    }

    public void getHelp(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, HelpActivity.class);
        startActivity(intent);
    }
}
