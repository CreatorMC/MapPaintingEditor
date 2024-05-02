package com.tmsstudio.mappaintingeditor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import com.tmsstudio.mappaintingeditor.Message.Message;

import java.util.ArrayList;

/**
 * 包名: com.tmsstudio.mappaintingeditor
 * 日期: 2022/5/18 17:58 星期三
 * 工程: Map Painting Editor
 */
class GridViewAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Bitmap> arrayList;
    private int row;
    private int col;

    private ActivityResultLauncher<Intent> resultLauncher;

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
