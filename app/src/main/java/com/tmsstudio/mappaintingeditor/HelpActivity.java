package com.tmsstudio.mappaintingeditor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.tmsstudio.mappaintingeditor.Widget.ExpandView;

public class HelpActivity extends AppCompatActivity {

    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        setTitle(R.string.help);
        linearLayout = findViewById(R.id.linear_layout);
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "软件读取不到世界存档",
                "1.确保自己安装了MC，并且有存档\n" +
                        "2.确保存档的存储位置在“外部”（设置->档案->文件存储->外部）\n"/* +
                "3.若安卓11及其以上，请点击软件右上角，选择导入游戏内地图\n" +
                "4.若安卓11及其以上，授权时不要选择其他路径，直接在data下授权（就是授权界面出现后直接点允许授权，不用选择文件夹）"*/);
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "显示生成成功，但是进入世界却没有地图画",
                "1.请完全退出MC后再进行操作\n" +
                        "2.查看生成地图画的存档在软件内是否有“此应用”的标识，如果有，请将存档手动复制回游戏存储位置。");
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "安卓11及其以上正确打开软件方式",
                "1.打开本软件的所有文件访问权限\n" + "2.点击刷新地图列表（或完全退出软件重新打开）");
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "安卓11及其以上任何方法都无法操作游戏内文件夹",
                "1.进行root（不推荐）\n" +
                        "2.使用VMOS等手机上的安卓模拟器（占用存储空间大）\n" +
                        "3.在主目录（\"/storage/emulated/0\"）中，按照\"games/com.mojang/minecraftWorlds\"路径创建文件夹，将世界存档复制进去。使用本软件操作完成后，再手动复制回原位置。"
        );
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "服务器里能用吗",
                "1.您必须是服主（或拥有服务器存档的人）\n" +
                        "2.只需将服务器存档放进手机基岩版，即可正常使用软件。生成完毕后放回服务器即可。");
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "旧国际版路径是什么意思",
                "1.这是指存档迁移前的国际版，正常操作即可。\n" +
                        "2.准确的说，是指位于主目录（\"/storage/emulated/0\"）里\"games/com.mojang/minecraftWorlds\"文件夹内的世界存档。"
        );
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "网易版生成闪退",
                "1.目前网易版（从客户端版本2.1开始）对玩家创建的世界也进行了加密，导致软件不能读取存档数据。\n" +
                        "2.解决方法：需要下载网易我的世界开发者工具并注册开发者账号，将网易版存档导入开发者工具内。然后点击测试按钮启动游戏。进入存档后即可退出游戏。再点击存档上的导出选项，导出存档。这样就可以得到解密后的存档了。需要注意的是，此方法的前提是存档是玩家自己创建的，从资源中心下载的存档并不适用。");
        linearLayout.addView(view);

        view = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_expand_view, null);
        createExpand(view, "我还有其他问题",
                "您可以加入“地图画编辑器反馈群”，反馈您遇到的问题。\n" +
                        "群号：869862626（如果没有问题，请勿打扰）");
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
