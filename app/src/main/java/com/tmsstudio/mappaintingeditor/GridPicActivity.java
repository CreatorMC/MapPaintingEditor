package com.tmsstudio.mappaintingeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.litl.leveldb.DB;
import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.PicFactory.PicFactory;

import java.util.ArrayList;


class GridViewAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Bitmap> arrayList;
    private int row;
    private int col;

    public GridViewAdapter(Context context, ArrayList<Bitmap> arrayList, int row, int col) {
        this.arrayList = arrayList;
        this.context = context;
        this.row = row;
        this.col = col;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return arrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(arrayList.get(i));
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(((GridPicActivity) context).summon_ing){
                    Toast.makeText(context, "生成中，禁止操作", Toast.LENGTH_SHORT).show();
                    return;
                }
                //让用户从图库中选择图片
                Intent intent1 = new Intent();
                intent1.setAction(Intent.ACTION_PICK);
                intent1.setType("image/*");
                ((GridPicActivity) context).startActivityForResult(intent1, Message.EXTRA_MESSAGE_SELECT_PIC);
            }
        });
        float dp = Util.pxToDp(128, context);
        float pic_size = 128;
        if(dp * row > 300.0){   //行数超过了控件大小
            pic_size = Util.dpToPx((float)(300.0 / row), context);
        }
        if(dp * col > 300.0){
            pic_size = Util.dpToPx((float)(300.0 / col), context);
        }
        int size = (int)pic_size;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size,size);
        imageView.setLayoutParams(params);

        return imageView;
    }
}


public class GridPicActivity extends AppCompatActivity {

    public static final int Edit_OK = 1;
    public static final int SUMMON_OK = 78515;
    public static final int SUMMON_ING = 465486;
    public static final int BEIBAO_COUNT = 36;      //玩家背包格子数量
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
    public boolean summon_ing = false;     //标记地图是否正在生成

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            if (Edit_OK == msg.what) {
                Log.i("TMS", "输入完成" );
                loadGridPic();
            }
            if(SUMMON_OK == msg.arg1){
                Util.closeDB(testDB);
                text_state.setText(GridPicActivity.this.getString(R.string.state) + "生成成功o(〃＾▽＾〃)o");
                Toast.makeText(GridPicActivity.this, "生成成功，请打开游戏查看。", Toast.LENGTH_SHORT).show();
                summon_map.setEnabled(true);
                summon_ing = false;
            }
            if(SUMMON_ING == msg.arg1){
                text_state.setText(GridPicActivity.this.getString(R.string.state) + "正在生成，请勿退出...\n当前已生成的地图个数:（" + msg.arg2 + "）");
            }
        }
    };
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(Edit_OK);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_pic);
        final Intent intent = getIntent();
        folder = intent.getStringExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity);
        name = intent.getStringExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity);
        this.setTitle(name);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeButtonEnabled(true);

        bitmap = null;

        show_image = findViewById(R.id.show_image_grid);
        check_complete =findViewById(R.id.check_complete);
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
                mHandler.removeCallbacks(mRunnable);
                mHandler.postDelayed(mRunnable, 800);
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
                mHandler.removeCallbacks(mRunnable);
                mHandler.postDelayed(mRunnable, 800);
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
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case Message.EXTRA_MESSAGE_SELECT_PIC:
                if(resultCode == RESULT_OK){
                    //用户选择完要处理的图片
                    Uri selectedImage = data.getData(); //获取系统返回的照片的Uri
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    Cursor cursor =getContentResolver().query(selectedImage, filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);  //获取照片路径
                    Log.i("TMS", picturePath);
                    //处理图片
                    img = PicFactory.convertPicTo(picturePath);
                    if(img == null || img.length <= 0){
                        Toast.makeText(this, "发生了意外的错误，您可能需要更换图片？", Toast.LENGTH_SHORT).show();
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
    }

    /**
     * 智能分析
     * @param view
     */
    @SuppressLint("DefaultLocale")
    public void clickIntelligent(View view) {
        if(bitmap == null){
            Toast.makeText(this, "请先选择图片！", Toast.LENGTH_SHORT).show();
            return;
        }
        int width_count = bitmap.getWidth();
        int height_count = bitmap.getHeight();
        int width = width_count;
        int height = height_count;
        if(width_count >= height_count){
            double ss = width_count / (double) (width_count / 128);
            width_count = width_count / 128;
            height_count = Integer.valueOf(String.format("%1.0f", height_count / ss));
        } else {
            double ss = height_count / (double) (height_count / 128);
            height_count = height_count / 128;
            width_count = Integer.valueOf(String.format("%1.0f", width_count / ss));
        }
        while (width_count * height_count > BEIBAO_COUNT){
            if(width_count >= height_count){
                width_count--;
                double ss = width / (double) (width_count);
                height_count = Integer.valueOf(String.format("%1.0f", height / ss));
            } else {
                height_count--;
                double ss = height / (double) (height_count);
                width_count = Integer.valueOf(String.format("%1.0f", width / ss));
            }
        }
        if(width_count == 0){
            width_count = 1;
        }
        if(height_count == 0){
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
            if(summon_ing){
                Toast.makeText(GridPicActivity.this, "生成中，禁止操作", Toast.LENGTH_SHORT).show();
                return;
            }
            String rr = editText_row.getText().toString().trim();
            if(rr != null && !"".equals(rr)){
                row = Integer.parseInt(rr);
            }
            String cc = editText_col.getText().toString().trim();
            if(cc != null && !"".equals(cc)){
                col = Integer.parseInt(cc);
            }
            if(img == null || img.length == 0){
                return;
            }
            if(row != 0 && col != 0 && row * col <= BEIBAO_COUNT){
                bitmapArrayList = PicFactory.preparePicTo(img, row, col, 128, check_complete.isChecked());
                if(bitmapArrayList != null && bitmapArrayList.size() >= 1){
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
     * @param view
     */
    public void selectPic(View view) {
        if(summon_ing){
            Toast.makeText(GridPicActivity.this, "生成中，禁止操作", Toast.LENGTH_SHORT).show();
            return;
        }
        //让用户从图库中选择图片
        Intent intent1 = new Intent();
        intent1.setAction(Intent.ACTION_PICK);
        intent1.setType("image/*");
        startActivityForResult(intent1, Message.EXTRA_MESSAGE_SELECT_PIC);
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
     * 生成地图画按钮
     * @param view
     */
    @SuppressLint("SetTextI18n")
    public void summonPicToMinecraft(View view) {
        if(bitmapArrayList == null || bitmapArrayList.size() == 0){
            text_state.setText(GridPicActivity.this.getString(R.string.state) + "请选择图片并切割（切割只需要改变一下行列或者改变“完整切割”单选框即可完成）");
            return;
        }
        summon_map.setEnabled(false);
        summon_ing = true;
        text_state.setText(GridPicActivity.this.getString(R.string.state) + "正在生成，请勿退出");
        Toast.makeText(GridPicActivity.this, "生成中，请勿终止应用运行！！！", Toast.LENGTH_SHORT).show();
        testDB = Util.openDB(testDB,folder);

        new Thread("TMS_GRID_MAP") {
            @Override
            public void run() {
                int slot = 0;

                for(Bitmap t: bitmapArrayList){
                    byte[] mc_map = PicFactory.toMinecraftMap(t);
                    if(mc_map == null || mc_map.length <= 0){
                        return;
                    }
                    ImportPicActivity.summonMapItem(mc_map,slot, testDB);
                    slot++;
                    android.os.Message ms = android.os.Message.obtain();
                    ms.arg1 = SUMMON_ING;
                    ms.arg2 = slot;
                    mHandler.sendMessage(ms);
                }
                android.os.Message message = android.os.Message.obtain();
                message.arg1 = SUMMON_OK;
                mHandler.sendMessage(message);
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        if(!summon_ing){
            super.onBackPressed();
        } else {
            Toast.makeText(GridPicActivity.this, "生成中，禁止退出界面", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            if(summon_ing){
                Toast.makeText(GridPicActivity.this, "生成中，禁止退出界面", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}