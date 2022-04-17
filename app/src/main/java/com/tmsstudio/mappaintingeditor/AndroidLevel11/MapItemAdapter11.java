package com.tmsstudio.mappaintingeditor.AndroidLevel11;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.tmsstudio.mappaintingeditor.Message.Message;
import com.tmsstudio.mappaintingeditor.R;
import com.tmsstudio.mappaintingeditor.TransitionActivity;
import com.tmsstudio.mappaintingeditor.Util;
import com.tmsstudio.mappaintingeditor.map_item;

import java.io.File;
import java.util.List;

public class MapItemAdapter11 extends RecyclerView.Adapter<MapItemAdapter11.ViewHolder> {
    private List<map_item> list;

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView map_name;
        TextView map_size;
        TextView text_version;
        public ViewHolder (View view)
        {
            super(view);
            map_name = view.findViewById(R.id.map_name);
            map_size = view.findViewById(R.id.map_size);
            text_version = view.findViewById(R.id.text_version);
        }
    }

    /**
     * 构造函数
     * 初始化适配器的数据
     * @param dataSet
     */
    public MapItemAdapter11(List<map_item> dataSet) {
        list = dataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_map_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        //添加每个Itme的点击监听
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();//得到点击位置
                map_item item_map = list.get(position);//由位置得到实例

                Context context = v.getContext();
                File tempDir = context.getExternalFilesDir("World");
                if(!tempDir.exists()){
                    tempDir.mkdir();
                }
                tempDir = new File(tempDir.getPath()+ "/" + item_map.getDocumentFile().getName());
                if(!tempDir.exists()){
                    //建立地图本身的文件夹
                    tempDir.mkdir();
                }
                Util.listDocumentFile(item_map.getDocumentFile(), tempDir, context);
                Toast.makeText(context, "复制完成", Toast.LENGTH_SHORT).show();
                ((Activity) context).setResult(Activity.RESULT_OK);
                ((Activity) context).finish();
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        map_item mapItem = list.get(position);
        holder.map_name.setText(mapItem.getMap_name());
        holder.map_size.setText(mapItem.getMap_size());
        holder.text_version.setText(mapItem.getVersion());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
