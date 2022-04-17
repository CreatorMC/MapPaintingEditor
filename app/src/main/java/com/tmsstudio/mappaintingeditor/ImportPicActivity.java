package com.tmsstudio.mappaintingeditor;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.litl.leveldb.DB;
import com.litl.leveldb.Iterator;
import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.PicFactory.PicFactory;
import com.tmsstudio.mappaintingeditor.nbt.Keys;
import com.tmsstudio.mappaintingeditor.nbt.convert.DataConverter;
import com.tmsstudio.mappaintingeditor.nbt.tags.ByteArrayTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.ByteTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.CompoundTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.IntTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.ListTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.LongTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.ShortTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.StringTag;
import com.tmsstudio.mappaintingeditor.nbt.tags.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ImportPicActivity extends AppCompatActivity{
    String folder;    //地图根文件夹路径
//    ArrayList<String> arrayListName;
//    int version;
    String name;
    ImageView show_image;
    CheckBox check_scall;
    byte[] img;
    String picturePath;     //图片路径
    Button summon_map;
    DB testDB;              //打开的数据库

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_pic);
        img = null;
        picturePath = null;
        testDB = null;

        final Intent intent = getIntent();
        folder = intent.getStringExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity);
        name = intent.getStringExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity);
        this.setTitle(name);
        Log.i("TMS", folder);

        show_image = findViewById(R.id.show_image_grid);
        check_scall = findViewById(R.id.check_complete);
        summon_map = findViewById(R.id.summon_map_grid);

        check_scall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean forceSize = !check_scall.isChecked();
                if(img != null && img.length > 0 && picturePath != null){
                    //处理图片
                    img = PicFactory.convertPicToMinecraft(picturePath,forceSize);
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
                Intent intent1 = new Intent();
                intent1.setAction(Intent.ACTION_PICK);
                intent1.setType("image/*");
                startActivityForResult(intent1, Message.EXTRA_MESSAGE_SELECT_PIC);
            }
        });

        summon_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(img != null && img.length > 0 && picturePath != null) {
                    Toast.makeText(ImportPicActivity.this, "正在生成，请勿退出", Toast.LENGTH_SHORT).show();
                    summon_map.setEnabled(false);
                    byte[] mc_map = PicFactory.toMinecraftMap(img);
                    summon_map.setEnabled(true);
                    if(mc_map == null || mc_map.length <= 0){
                        Toast.makeText(ImportPicActivity.this, "发生了意外的错误，您可能需要更换图片？", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.i("TMS", "本地文件路径: " + folder);
                    testDB = Util.openDB(testDB, folder);
                    summonMapItem(mc_map, 0, testDB);
                    testDB = Util.closeDB(testDB);

                    Toast.makeText(ImportPicActivity.this, "生成成功，请打开游戏查看。", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ImportPicActivity.this, "请选择图片再生成！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * 在存档里生成地图物品
     * @param mc_map 图片数组
     * @param slot 物品栏槽位
     * @param testDB 数据库
     */
    public static void summonMapItem(byte[] mc_map, int slot, DB testDB) {
        try {
            Log.e("TMS", "生成地图" );
            byte[] keys = testDB.get(Keys.LOCAL_PLAYER.getBytes()); //读取玩家根目录
            ArrayList<Tag> list = DataConverter.read(keys);
            CompoundTag tag = (CompoundTag) list.get(0);
            ListTag tag_inventory = (ListTag)tag.getChildTagByKey(Keys.INVENTORY);

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


            /*-------------------------------------第一地图----------------------------------------*/
            ArrayList<Tag> map_tag_compound_list = new ArrayList<>();
            map_tag_compound_list.add(new ByteTag("map_is_init", (byte) 1));
            map_tag_compound_list.add(new IntTag("map_name_index", slot+1));

            long map_uuid;                                  //保持正数，貌似锁定的地图的uuid都是正数
            boolean has_uuid = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            do{
                Random random = new Random();
                map_uuid = Math.abs(random.nextLong() % (Long.MAX_VALUE) + 1);  //避免超范围
                has_uuid = isFindEqualKey("map_" + map_uuid, testDB);
            }while (has_uuid);
            map_tag_compound_list.add(new LongTag("map_uuid", map_uuid));

            CompoundTag map_tag_compound = new CompoundTag("tag", map_tag_compound_list);

            new_list.add(new ByteTag("Count",(byte)1));
            new_list.add(new ShortTag("Damage",(short) 6));
            new_list.add(new StringTag("Name","minecraft:filled_map"));
            new_list.add(new ByteTag("Slot",(byte)slot));
            new_list.add(new ByteTag("WasPickedUp",(byte)0));
            new_list.add(map_tag_compound);

            tag_item.setValue(new_list);
            tag_list_inventory.remove(slot);
            tag_list_inventory.add(slot,tag_item);
            tag_inventory.setValue(tag_list_inventory);

            ArrayList<Tag> temp_list = tag.getValue();
            for(int i = 0; i < temp_list.size(); i++){
                if(Keys.INVENTORY.equals(temp_list.get(i).getName())){
                    temp_list.remove(i);
                    temp_list.add(i, tag_inventory);
                    break;
                }
            }
            tag.setValue(temp_list);
            list.remove(0);
            list.add(0,tag);
            byte[] data = DataConverter.write(list);
            testDB.put(Keys.LOCAL_PLAYER.getBytes(), data);


            //生成地图数据
            long map_parient_uuid;                                  //保持负数，貌似未锁定的地图的uuid都是负数
            boolean has_uuid_0 = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            boolean has_uuid_1 = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            boolean has_uuid_2 = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            boolean has_uuid_3 = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            boolean has_uuid_4 = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            do{
                Random random = new Random();
                map_parient_uuid = -Math.abs(random.nextLong() % (Long.MAX_VALUE ) + 1);     //地图的父uuid
                has_uuid_0 = isFindEqualKey("map_" + (map_parient_uuid), testDB);
                has_uuid_1 = isFindEqualKey("map_" + (map_parient_uuid+1), testDB);
                has_uuid_2 = isFindEqualKey("map_" + (map_parient_uuid+2), testDB);
                has_uuid_3 = isFindEqualKey("map_" + (map_parient_uuid+3), testDB);
                has_uuid_4 = isFindEqualKey("map_" + (map_parient_uuid+4), testDB);
            }while (has_uuid_0 || has_uuid_1 || has_uuid_2 || has_uuid_3 || has_uuid_4);

            ArrayList<Tag> map_data_list = new ArrayList<>();
            ArrayList<Tag> child_list = new ArrayList<>();
            child_list.add(new ByteArrayTag("colors", mc_map));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_uuid));
            child_list.add(new ByteTag("mapLocked", (byte)1));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 1));
            child_list.add(new ByteTag("scale", (byte)0));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
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








            /*-----------------------------------第二地图------------------------------------------*/
            has_uuid = false;                       //uuid是否重复，重复的话重新生成，不重复退出循环
            do{
                Random random = new Random();
                map_uuid = Math.abs(random.nextLong() % (Long.MAX_VALUE) + 1);  //避免超范围
                has_uuid = isFindEqualKey("map_" + map_uuid, testDB);
            }while (has_uuid);

            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", mc_map));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_uuid));
            child_list.add(new ByteTag("mapLocked", (byte)1));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 1));
            child_list.add(new ByteTag("scale", (byte)0));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + map_uuid;
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/









            /*-----------------------------------第三地图------------------------------------------*/
            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", mc_map));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_parient_uuid));
            child_list.add(new ByteTag("mapLocked", (byte)0));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 1));
            child_list.add(new ByteTag("scale", (byte)0));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + map_parient_uuid;
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/







            /*-----------------------------------第四地图------------------------------------------*/
            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", new byte[65536]));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_parient_uuid + 1));
            child_list.add(new ByteTag("mapLocked", (byte)0));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 2));
            child_list.add(new ByteTag("scale", (byte)1));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + (map_parient_uuid+1);
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/








            /*-----------------------------------第五地图------------------------------------------*/
            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", new byte[65536]));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_parient_uuid + 2));
            child_list.add(new ByteTag("mapLocked", (byte)0));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 3));
            child_list.add(new ByteTag("scale", (byte)2));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + (map_parient_uuid+2);
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/






            /*-----------------------------------第六地图------------------------------------------*/
            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", new byte[65536]));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_parient_uuid + 3));
            child_list.add(new ByteTag("mapLocked", (byte)0));
            child_list.add(new LongTag("parentMapId", map_parient_uuid + 4));
            child_list.add(new ByteTag("scale", (byte)3));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + (map_parient_uuid+3);
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/






            /*-----------------------------------第七地图------------------------------------------*/
            map_data_list.clear();
            child_list.clear();
            child_list.add(new ByteArrayTag("colors", new byte[65536]));
            child_list.add(new ListTag("decorations",new ArrayList<Tag>()));
            child_list.add(new ByteTag("dimension", (byte)0));
            child_list.add(new ByteTag("fullyExplored", (byte)0));
            child_list.add(new ShortTag("height", (short) 128));
            child_list.add(new LongTag("mapId", map_parient_uuid + 4));
            child_list.add(new ByteTag("mapLocked", (byte)0));
            child_list.add(new LongTag("parentMapId", -1));
            child_list.add(new ByteTag("scale", (byte)4));
            child_list.add(new ByteTag("unlimitedTracking", (byte)0));
            child_list.add(new ShortTag("width", (short) 128));
            child_list.add(new IntTag("xCenter", 0));
            child_list.add(new IntTag("zCenter", 0));

            root_comp = new CompoundTag(tag_item.getName(), child_list);
            map_data_list.add(root_comp);
            data_map = DataConverter.write(map_data_list);
            map_name = "map_" + (map_parient_uuid+4);
            Log.i("TMS", map_name);
            testDB.put(map_name.getBytes(), data_map);
            /*-------------------------------------------------------------------------------------*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 检查是否有重复key
     * @param key_name 键名
     * @param testDB 数据库
     * @return false:没有重复
     */
    public static boolean isFindEqualKey(String key_name, DB testDB){
//        Log.i("TMS", "isFindEqualKey: " + testDB.isClosed());
        if(testDB == null || testDB.isClosed()){
            return false;
        }
        Iterator iterator = testDB.iterator();
        for(iterator.seekToFirst();iterator.isValid();iterator.next()) {
//            Log.i("TMS", new String(iterator.getKey()));
            if(key_name.equals(new String(iterator.getKey()))){
                return true;
            }
        }
        iterator.close();
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case Message.EXTRA_MESSAGE_SELECT_PIC:
                if(resultCode == RESULT_OK){
                    //用户选择完要处理的图片
                    Uri selectedImage = data.getData(); //获取系统返回的照片的Uri
                    Log.i("TMS", selectedImage.toString());
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    Cursor cursor =getContentResolver().query(selectedImage, filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);  //获取照片路径
                    Log.i("TMS", picturePath);
                    boolean forceSize = !check_scall.isChecked();
                    //处理图片
                    img = PicFactory.convertPicToMinecraft(picturePath,forceSize);
                    if(img == null || img.length <= 0){
                        Toast.makeText(ImportPicActivity.this, "发生了意外的错误，您可能需要更换图片？", Toast.LENGTH_SHORT).show();
                    }
                    //转为图片并显示
                    Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    Log.i("TMS", "w:" + bitmap.getWidth() + ", h:" + bitmap.getHeight());
                    show_image.setImageBitmap(bitmap);
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
}
