package com.tmsstudio.mappaintingeditor;

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
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.litl.leveldb.DB;
import com.litl.leveldb.Iterator;
import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.PicFactory.PicFactory;
import com.tmsstudio.mappaintingeditor.nbt.Keys;
import com.tmsstudio.mappaintingeditor.nbt.convert.DataConverter;
import com.tmsstudio.mappaintingeditor.nbt.tags.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

@SuppressWarnings("rawtypes")
public class ImportPicActivity extends AppCompatActivity {
    String folder;    //地图根文件夹路径
    String name;
    ImageView show_image;
    CheckBox check_scall;
    Spinner spinner;
    byte[] img;
    String picturePath;     //图片路径
    Button summon_map;
    DB testDB;              //打开的数据库

    private ActivityResultLauncher<Intent> resultLauncher;

    /**
     * 在存档里生成地图物品
     *
     * @param mc_map 图片数组
     * @param slot   物品栏槽位
     * @param testDB 数据库
     */
    public static void summonMapItem(byte[] mc_map, int slot, DB testDB) {
        try {
            Log.e("TMS", "生成地图");
            byte[] keys = testDB.get(Keys.LOCAL_PLAYER.getBytes()); //读取玩家根目录
            ArrayList<Tag> list = DataConverter.read(keys);
            CompoundTag tag = (CompoundTag) list.get(0);
            ListTag tag_inventory = (ListTag) tag.getChildTagByKey(Keys.INVENTORY);

            /*
                TAG_COMPOUND(): 5 entries
                {
                   TAG_BYTE(Count): 0
                   TAG_SHORT(Damage): 0
                   TAG_STRING(Name):
                   TAG_BYTE(Slot): 0
                   TAG_BYTE(WasPickedUp): 0
                }
            */
            ArrayList<Tag> tag_list_inventory = tag_inventory.getValue();
            CompoundTag tag_item = (CompoundTag) tag_list_inventory.get(slot);   //得到玩家背包第slot个格子
            ArrayList<Tag> new_list = tag_item.getValue();                          //得到所有tag，准备修改
            new_list.clear();                                                       //清空所有tag


            /*-------------------------------------生成地图----------------------------------------*/
            ArrayList<Tag> map_tag_compound_list = new ArrayList<>();
            map_tag_compound_list.add(new ByteTag("map_is_init", (byte) 1));
            map_tag_compound_list.add(new IntTag("map_name_index", slot + 1));

            long map_uuid;                                  //保持正数，貌似锁定的地图的uuid都是正数
            boolean has_uuid = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            do {
                Random random = new Random();
                map_uuid = Math.abs(random.nextLong() % (Long.MAX_VALUE) + 1);  //避免超范围
                has_uuid = isFindEqualKey("map_" + map_uuid, testDB);
            } while (has_uuid);
            map_tag_compound_list.add(new LongTag("map_uuid", map_uuid));

            CompoundTag map_tag_compound = new CompoundTag("tag", map_tag_compound_list);

            new_list.add(new ByteTag("Count", (byte) 1));
            new_list.add(new ShortTag("Damage", (short) 6));
            new_list.add(new StringTag("Name", "minecraft:filled_map"));
            new_list.add(new ByteTag("Slot", (byte) slot));
            new_list.add(new ByteTag("WasPickedUp", (byte) 0));
            new_list.add(map_tag_compound);

            tag_item.setValue(new_list);
            tag_list_inventory.remove(slot);
            tag_list_inventory.add(slot, tag_item);
            tag_inventory.setValue(tag_list_inventory);

            ArrayList<Tag> temp_list = tag.getValue();
            for (int i = 0; i < temp_list.size(); i++) {
                if (Keys.INVENTORY.equals(temp_list.get(i).getName())) {
                    temp_list.remove(i);
                    temp_list.add(i, tag_inventory);
                    break;
                }
            }
            tag.setValue(temp_list);
            list.remove(0);
            list.add(0, tag);
            byte[] data = DataConverter.write(list);
            testDB.put(Keys.LOCAL_PLAYER.getBytes(), data);

            //生成地图数据
            ArrayList<Tag> map_data_list = new ArrayList<>();
            ArrayList<Tag> child_list = new ArrayList<>();
            child_list.add(new ByteArrayTag("colors", mc_map));
            child_list.add(new ListTag("decorations", new ArrayList<>()));
            child_list.add(new ByteTag("dimension", (byte) -1));
            child_list.add(new ByteTag("fullyExplored", (byte) 0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_uuid));
            child_list.add(new ByteTag("mapLocked", (byte) 1));
            child_list.add(new LongTag("parentMapId", -1L));
            child_list.add(new ByteTag("scale", (byte) 4));
            child_list.add(new ByteTag("unlimitedTracking", (byte) 0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            CompoundTag root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            byte[] data_map = DataConverter.write(map_data_list);
            String map_name = "map_" + map_uuid;
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查是否有重复key
     *
     * @param key_name 键名
     * @param testDB   数据库
     * @return false:没有重复
     */
    public static boolean isFindEqualKey(String key_name, DB testDB) {
        if (testDB == null || testDB.isClosed()) {
            return false;
        }
        try(Iterator iterator = testDB.iterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                if (key_name.equals(new String(iterator.getKey()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_pic);
        img = null;
        picturePath = null;
        testDB = null;

        {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity) && intent.hasExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity)) {
                folder = intent.getStringExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity);
                name = intent.getStringExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity);
            }
        }
        this.setTitle(name);
        Log.i("TMS", folder);

        show_image = findViewById(R.id.show_image_grid);
        check_scall = findViewById(R.id.check_complete);
        summon_map = findViewById(R.id.summon_map_grid);
        spinner = findViewById(R.id.spinner);

        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //用户选择完要处理的图片
                Intent intent = result.getData();
                if (intent == null){
                    return;
                }
                Uri selectedImage = intent.getData();
                Log.i("TMS", selectedImage.toString());
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);  //获取照片路径
                Log.i("TMS", picturePath);
                boolean forceSize = !check_scall.isChecked();
                //处理图片
                img = PicFactory.convertPicToMinecraft(picturePath, forceSize);
                if (img == null || img.length <= 0) {
                    Toast.makeText(ImportPicActivity.this, "发生了意外的错误，您可能需要更换图片？", Toast.LENGTH_SHORT).show();
                }
                //转为图片并显示
                Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                Log.i("TMS", "w:" + bitmap.getWidth() + ", h:" + bitmap.getHeight());
                show_image.setImageBitmap(bitmap);
            }
        });

        check_scall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean forceSize = !check_scall.isChecked();
                if (img != null && img.length > 0 && picturePath != null) {
                    //处理图片
                    img = PicFactory.convertPicToMinecraft(picturePath, forceSize);
                    //转为图片并显示
                    Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    Log.i("TMS", "w:" + bitmap.getWidth() + ", h:" + bitmap.getHeight());
                    show_image.setImageBitmap(bitmap);
                }
            }
        });

        show_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //让用户从图库中选择图片
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                resultLauncher.launch(intent);
            }
        });

        summon_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (img != null && img.length > 0 && picturePath != null) {
                        Toast.makeText(ImportPicActivity.this, "正在生成，请勿退出", Toast.LENGTH_SHORT).show();
                        summon_map.setEnabled(false);
                        byte[] mc_map = PicFactory.toMinecraftMap(img);
                        summon_map.setEnabled(true);

                        Log.i("TMS", "本地文件路径: " + folder);
                        int slot = startEditWorld(mc_map, folder);

                        Toast.makeText(ImportPicActivity.this, "生成成功，请打开游戏查看。", Toast.LENGTH_SHORT).show();
                        spinner.setSelection((slot + 1) % 36);                              //生成完后编号自增1
                    } else {
                        Toast.makeText(ImportPicActivity.this, "请选择图片再生成！", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable e) {
                    Toast.makeText(ImportPicActivity.this, "发生了意外的错误:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
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
     * 启动编辑世界
     *
     * @param bytes
     * @param folder
     * @return
     */
    private int startEditWorld(byte[] bytes, String folder) {
        File file = new File(Environment.getExternalStorageDirectory(), "Android/data");
        if (folder.startsWith(file.getAbsolutePath()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DocumentFile documentFile = toDocumentFile(folder);
            if (documentFile != null && documentFile.isDirectory()) {
                File cacheDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), documentFile.getName());
                copyDir(documentFile, cacheDir);
                testDB = Util.openDB(testDB, cacheDir.getAbsolutePath());
                int slot = Integer.parseInt(spinner.getSelectedItem().toString());  //根据用户选择的背包格子进行生成
                summonMapItem(bytes, slot, testDB);
                testDB = Util.closeDB(testDB);
                for (DocumentFile f : documentFile.listFiles()){
                    forEachDelete(f);
                }
                for (File f : cacheDir.listFiles()){
                    copyDir(f, documentFile);
                }
                forEachDelete(cacheDir);
                return slot;
            } else {
                throw new RuntimeException("地图路径无效");
            }
        } else {
            testDB = Util.openDB(testDB, folder);
            int slot = Integer.parseInt(spinner.getSelectedItem().toString());  //根据用户选择的背包格子进行生成
            summonMapItem(bytes, slot, testDB);
            testDB = Util.closeDB(testDB);
            return slot;
        }
    }

    /**
     * 复制目录
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
     * @param in
     * @param out
     */
    private void copyDir(File in, DocumentFile out) {
        DocumentFile documentFile = out.findFile(in.getName());
        if (in.isDirectory()) {
            if (documentFile == null) {
                documentFile = out.createDirectory(in.getName());
            }
            File[] files = in.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                copyDir(file, documentFile);
            }
        } else {
            try {
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
     * 遍历删除文件
     *
     * @param file
     */
    private void forEachDelete(DocumentFile file) {
        if (file.isDirectory()) {
            DocumentFile[] documentFiles = file.listFiles();
            for (DocumentFile documentFile : documentFiles) {
                forEachDelete(documentFile);
            }
        }
        file.delete();
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
}
