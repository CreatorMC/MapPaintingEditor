package com.tmsstudio.mappaintingeditor;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tmsstudio.mappaintingeditor.Message.Message;

import java.util.List;

public class MapItemAdapter extends RecyclerView.Adapter<MapItemAdapter.ViewHolder> {
    private final List<MapItem> list;

    /**
     * 构造函数
     * 初始化适配器的数据
     *
     * @param dataSet
     */
    public MapItemAdapter(List<MapItem> dataSet) {
        list = dataSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_map_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MapItem item = list.get(position);
        if (item.getIcon() != null) {
            holder.icon.setImageBitmap(item.getIcon());
        }
        holder.name.setText(item.getName());
        holder.size.setText(Util.FormetFileSize(item.getSize()));
        holder.type.setText(worldTypeToName(item.getType()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), TransitionActivity.class);
                intent.putExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity, item.getName());
                intent.putExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity, item.getPath());
                v.getContext().startActivity(intent);
            }
        });
    }

    private String worldTypeToName(WorldType type) {
        switch (type) {
            case OutdatedBedrock:
                return "旧国际版路径";
            case Bedrock:
                return "国际版路径";
            case NetEaseBedrock:
                return "中国版";
            default:
                return "第三方软件";
        }
    }

    /**
     * 解决滑动之后显示数据错乱
     *
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView size;
        TextView type;

        public ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.map_icon);
            name = view.findViewById(R.id.map_name);
            size = view.findViewById(R.id.map_size);
            type = view.findViewById(R.id.map_type);
        }
    }
}
