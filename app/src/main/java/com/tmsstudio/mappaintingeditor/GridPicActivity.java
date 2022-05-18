package com.tmsstudio.mappaintingeditor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;

import com.litl.leveldb.DB;
import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.PicFactory.PicFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class GridPicActivity extends AppCompatActivity {
    public static final int BEIBAO_COUNT = 36;      //玩家背包格子数量
    public boolean summon_ing = false;     //标记地图是否正在生成
    String folder;    //地图根文件夹路径
    String name;
    TextView text_state;
    ImageView show_image;
    CheckBox check_complete;
    EditText editText_row;
    EditText editText_col;
    GridView grid_view;
    ConstraintLayout grid_layout;
    Button summon_map;
    byte[] img;
    Bitmap bitmap;
    String picturePath;     //图片路径
    DB testDB;              //打开的数据库
    private int row = 0;        //行数
    private int col = 0;        //列数
    private ArrayList<Bitmap> bitmapArrayList = null;
    private ActivityResultLauncher<Intent> resultLauncher;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_pic);
        {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity) && intent.hasExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity)) {
                folder = intent.getStringExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity);
                name = intent.getStringExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity);
            }
        }
        this.setTitle(name);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeButtonEnabled(true);

        bitmap = null;

        show_image = findViewById(R.id.show_image_grid);
        check_complete = findViewById(R.id.check_complete);
        editText_row = findViewById(R.id.editText_row);
        editText_col = findViewById(R.id.editText_col);
        grid_view = findViewById(R.id.grid_view);
        grid_layout = findViewById(R.id.grid_layout);
        text_state = findViewById(R.id.text_state);
        summon_map = findViewById(R.id.summon_map_grid);
        grid_view.setVisibility(View.INVISIBLE);

        text_state.setText(GridPicActivity.this.getString(R.string.state) + "等待用户选择图片......");

        editText_row.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                loadGridPic();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        editText_col.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                loadGridPic();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        check_complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadGridPic();
            }
        });

        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //用户选择完要处理的图片
                        Uri selectedImage = result.getData().getData(); //获取系统返回的照片的Uri
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        picturePath = cursor.getString(columnIndex);  //获取照片路径
                        Log.i("TMS", picturePath);
                        //处理图片
                        img = PicFactory.convertPicTo(picturePath);
                        if (img == null || img.length <= 0) {
                            Toast.makeText(GridPicActivity.this, "发生了意外的错误，您可能需要更换图片？", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //转为图片并显示
                        bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                        show_image.setImageBitmap(bitmap);
                        show_image.setVisibility(View.VISIBLE);
                        grid_view.setVisibility(View.INVISIBLE);
                        bitmapArrayList = null;
                        text_state.setText(GridPicActivity.this.getString(R.string.state) + "已选择图片，等待切割图片。");
                    }
                }
        );
    }


    /**
     * 智能分析
     *
     * @param view
     */
    @SuppressLint("DefaultLocale")
    public void clickIntelligent(View view) {
        if (bitmap == null) {
            Toast.makeText(this, "请先选择图片！", Toast.LENGTH_SHORT).show();
            return;
        }
        int width_count = bitmap.getWidth();
        int height_count = bitmap.getHeight();
        int width = width_count;
        int height = height_count;
        if (width_count >= height_count) {
            double ss = width_count / (double) (width_count / 128);
            width_count = width_count / 128;
            height_count = Integer.valueOf(String.format("%1.0f", height_count / ss));
        } else {
            double ss = height_count / (double) (height_count / 128);
            height_count = height_count / 128;
            width_count = Integer.valueOf(String.format("%1.0f", width_count / ss));
        }
        while (width_count * height_count > BEIBAO_COUNT) {
            if (width_count >= height_count) {
                width_count--;
                double ss = width / (double) (width_count);
                height_count = Integer.valueOf(String.format("%1.0f", height / ss));
            } else {
                height_count--;
                double ss = height / (double) (height_count);
                width_count = Integer.valueOf(String.format("%1.0f", width / ss));
            }
        }
        if (width_count == 0) {
            width_count = 1;
        }
        if (height_count == 0) {
            height_count = 1;
        }
        row = height_count;
        col = width_count;
        editText_row.setText(String.valueOf(height_count));
        editText_col.setText(String.valueOf(width_count));
    }


    /**
     * 切分图片，并把切分结果显示在网格中
     */
    @SuppressLint("SetTextI18n")
    private void loadGridPic() {
        try {
            if (summon_ing) {
                Toast.makeText(GridPicActivity.this, "生成中，禁止操作", Toast.LENGTH_SHORT).show();
                return;
            }
            String rr = editText_row.getText().toString().trim();
            if (rr != null && !"".equals(rr)) {
                row = Integer.parseInt(rr);
            }
            String cc = editText_col.getText().toString().trim();
            if (cc != null && !"".equals(cc)) {
                col = Integer.parseInt(cc);
            }
            if (img == null || img.length == 0) {
                return;
            }
            if (row != 0 && col != 0 && row * col <= BEIBAO_COUNT) {
                bitmapArrayList = PicFactory.preparePicTo(img, row, col, 128, check_complete.isChecked());
                if (bitmapArrayList != null && bitmapArrayList.size() >= 1) {
                    grid_view.setNumColumns(col);
                    grid_view.setAdapter(new GridViewAdapter(GridPicActivity.this, bitmapArrayList, row, col));
                    show_image.setVisibility(View.INVISIBLE);
                    grid_view.setVisibility(View.VISIBLE);
                }
                text_state.setText(GridPicActivity.this.getString(R.string.state) + "切割完成，准备就绪！(/≧▽≦)/");
            } else {
                Toast.makeText(GridPicActivity.this, "行列相乘超过了36或是其他错误！", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(GridPicActivity.this, "行列相乘超过了36或是其他错误！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 选择图片
     *
     * @param view
     */
    public void selectPic(View view) {
        if (summon_ing) {
            Toast.makeText(GridPicActivity.this, "生成中，禁止操作", Toast.LENGTH_SHORT).show();
            return;
        }
        //让用户从图库中选择图片
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        resultLauncher.launch(intent);
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
     * 复制目录
     *
     * @param in
     * @param out
     */
    private void copyDir(DocumentFile in, File out) {
        if (in.isDirectory()) {
            out.mkdirs();
            DocumentFile[] documentFiles = in.listFiles();
            for (DocumentFile documentFile : documentFiles) {
                copyDir(documentFile, new File(out, documentFile.getName()));
            }
        } else {
            try {
                InputStream inputStream = getContentResolver().openInputStream(in.getUri());
                FileOutputStream fileOutputStream = new FileOutputStream(out);
                int n;
                byte[] bytes = new byte[1024];
                while ((n = inputStream.read(bytes)) != -1) {
                    fileOutputStream.write(bytes, 0, n);
                }
                inputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 复制目录
     *
     * @param in
     * @param out
     */
    private void copyDir(File in, DocumentFile out) {
        if (in.isDirectory()) {
            out.createDirectory(in.getName());
            File[] files = in.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                copyDir(file, out);
            }
        } else {
            try {
                DocumentFile documentFile = out.findFile(in.getName());
                if (documentFile == null) {
                    documentFile = out.createFile("*/*", in.getName());
                }
                OutputStream outputStream = getContentResolver().openOutputStream(documentFile.getUri());
                FileInputStream fileInputStream = new FileInputStream(in);
                int n;
                byte[] bytes = new byte[1024];
                while ((n = fileInputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, n);
                }
                fileInputStream.close();
                outputStream.close();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 遍历删除文件
     *
     * @param file
     */
    private void forEachDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    forEachDelete(f);
                }
            }
        }
        file.delete();
    }

    /**
     * 生成地图画按钮
     *
     * @param view
     */
    @SuppressLint("SetTextI18n")
    public void summonPicToMinecraft(View view) {
        if (bitmapArrayList == null || bitmapArrayList.size() == 0) {
            text_state.setText(GridPicActivity.this.getString(R.string.state) + "请选择图片并切割（切割只需要改变一下行列或者改变“完整切割”单选框即可完成）");
            return;
        }
        summon_map.setEnabled(false);
        summon_ing = true;
        text_state.setText(GridPicActivity.this.getString(R.string.state) + "正在生成，请勿退出");
        Toast.makeText(GridPicActivity.this, "生成中，请勿终止应用运行！！！", Toast.LENGTH_SHORT).show();


        Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        File file = new File(Environment.getExternalStorageDirectory(), "Android/data");
                        if (folder.startsWith(file.getAbsolutePath()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            DocumentFile documentFile = toDocumentFile(folder);
                            if (documentFile != null && documentFile.isDirectory()) {
                                File cacheDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), String.valueOf(System.currentTimeMillis()));
                                copyDir(documentFile, cacheDir);
                                testDB = Util.openDB(testDB, cacheDir.getAbsolutePath());

                                int slot = 0;
                                for (Bitmap t : bitmapArrayList) {
                                    byte[] mc_map = PicFactory.toMinecraftMap(t);
                                    if (mc_map.length <= 0) {
                                        return;
                                    }
                                    ImportPicActivity.summonMapItem(mc_map, slot, testDB);
                                    slot++;
                                    emitter.onNext(slot);
                                }
                                Util.closeDB(testDB);

                                copyDir(cacheDir, documentFile);
                                forEachDelete(cacheDir);
                            } else {
                                emitter.onError(new RuntimeException("地图路径无效"));
                            }
                        } else {
                            testDB = Util.openDB(testDB, folder);
                            int slot = 0;
                            for (Bitmap t : bitmapArrayList) {
                                byte[] mc_map = PicFactory.toMinecraftMap(t);
                                if (mc_map.length <= 0) {
                                    return;
                                }
                                ImportPicActivity.summonMapItem(mc_map, slot, testDB);
                                slot++;
                                emitter.onNext(slot);
                            }
                            Util.closeDB(testDB);
                        }
                        emitter.onComplete();
                    }

                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer integer) {
                        text_state.setText(GridPicActivity.this.getString(R.string.state) + "正在生成，请勿退出...\n当前已生成的地图个数:（" + integer + "）");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(GridPicActivity.this, "发生错误:" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {

                        text_state.setText(GridPicActivity.this.getString(R.string.state) + "生成成功o(〃＾▽＾〃)o");
                        Toast.makeText(GridPicActivity.this, "生成成功，请打开游戏查看。", Toast.LENGTH_SHORT).show();
                        summon_map.setEnabled(true);
                        summon_ing = false;
                    }
                });


    }

    @Override
    public void onBackPressed() {
        if (!summon_ing) {
            super.onBackPressed();
        } else {
            Toast.makeText(GridPicActivity.this, "生成中，禁止退出界面", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (summon_ing) {
                Toast.makeText(GridPicActivity.this, "生成中，禁止退出界面", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}