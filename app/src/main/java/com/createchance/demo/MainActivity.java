package com.createchance.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_add_bgm).setOnClickListener(this);
        findViewById(R.id.btn_remove_bgm).setOnClickListener(this);
        findViewById(R.id.btn_cut).setOnClickListener(this);
        findViewById(R.id.btn_add_filter).setOnClickListener(this);
        findViewById(R.id.btn_merge).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add_bgm:
                AddBgmActivity.start(this);
                break;
            case R.id.btn_remove_bgm:

                break;
            case R.id.btn_cut:

                break;
            case R.id.btn_add_filter:

                break;
            case R.id.btn_merge:

                break;
            default:
                break;
        }
    }
}
