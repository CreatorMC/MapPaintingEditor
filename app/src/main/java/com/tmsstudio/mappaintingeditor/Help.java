package com.tmsstudio.mappaintingeditor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tmsstudio.mappaintingeditor.Widget.ExpandView;

public class Help extends AppCompatActivity {

    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        setTitle(R.string.help);
        linearLayout = findViewById(R.id.linear_layout);
        LinearLayout view = (LinearLayout)getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "软件读取不到世界存档",
                "1.确保自己安装了MC，并且有存档\n" +
                "2.确保存档的存储位置在“外部”（设置->档案->文件存储->外部）\n" +
                "3.若安卓11及其以上，请点击软件右上角，选择导入游戏内地图\n" +
                "4.若安卓11及其以上，授权时不要选择其他路径，直接在data下授权（就是授权界面出现后直接点允许授权，不用选择文件夹）");
        linearLayout.addView(view);

        view = (LinearLayout)getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "显示生成成功，但是进入世界却没有地图画",
                "1.请完全退出MC后再进行操作\n" +
                "2.查看生成地图画的存档在软件内是否有“此应用”的标识，如果有，请将存档手动复制回游戏存储位置。");
        linearLayout.addView(view);

        view = (LinearLayout)getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "网易版生成闪退",
                "1.目前最新网易版客户端（客户端版本2.1）对玩家创建的世界也进行了加密，导致软件不能读取存档数据。尚无很好的办法。");
        linearLayout.addView(view);
    }

    /**
     * 为一个view初始化折叠内容
     * @param view
     * @param question
     * @param ans
     */
    private void createExpand(LinearLayout view, String question, String ans){
        LinearLayout linearLayout = (LinearLayout) view.getChildAt(0);
        final ExpandView expandView = (ExpandView) view.getChildAt(1);

        final ImageView imageView = (ImageView) linearLayout.getChildAt(0);
        TextView textView = (TextView) linearLayout.getChildAt(1);
        textView.setText(question);

        TextView ans_view = new TextView(this);
        ans_view.setPadding(100,0,0,0);
        ans_view.setText(ans);

        expandView.setLayout(R.layout.activity_help);
        expandView.setContentView(ans_view);
        linearLayout.setClickable(true);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandView.isExpand()) {
                    expandView.collapse();
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_up_black_24dp, null));
                } else {
                    expandView.expand();
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_24dp, null));
                }
            }
        });
    }
}