package com.tmsstudio.mappaintingeditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.tmsstudio.mappaintingeditor.Message.Message;

public class TransitionActivity extends AppCompatActivity {

    String folder;    //地图根文件夹路径
//    ArrayList<String> arrayListName;
    String name;      //地图名称
//    int version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transition);

        final Intent intent = getIntent();
        folder = intent.getStringExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity);
        name = intent.getStringExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity);
        this.setTitle(name);
    }

    public void chooseNormal(View view) {
        //跳转到普通模式
        Intent intent = new Intent(view.getContext(), ImportPicActivity.class);
        intent.putExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity, folder);
        intent.putExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity, name);
        startActivity(intent);
    }

    public void chooseGrid(View view) {
        //跳转到网格模式
        Intent intent = new Intent(view.getContext(), GridPicActivity.class);
        intent.putExtra(Message.EXTRA_MESSAGE_TO_ImportPicActivity, folder);
        intent.putExtra(Message.EXTRA_MAPNAME_TO_ImportPicActivity, name);
        startActivity(intent);
    }
}
