package com.createchance.demo.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.createchance.demo.R;
import com.createchance.demo.model.Template;
import com.createchance.demo.presenters.ITemplateList;
import com.createchance.demo.presenters.TemplateListPresenter;

import java.util.List;

public class TemplateListActivity extends Activity implements View.OnClickListener,
        ITemplateList.View {
    private static final String TAG = "TemplateListActivity";

    private RecyclerView mTemplateListView;
    private TemplateListAdapter mTemplateListAdapter;

    private ITemplateList.Presenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_list);

        // init views
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.tv_custom_scene).setOnClickListener(this);
        mTemplateListView = findViewById(R.id.rcv_template_list);

        mPresenter = new TemplateListPresenter(this);
        mPresenter.getTemplateList();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TemplateListActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.tv_custom_scene:
                ScenesActivity.start(this, null, true);
                break;
            default:
                break;
        }
    }

    @Override
    public void showTemplateList(List<Template> templateList) {
        mTemplateListAdapter = new TemplateListAdapter(this, templateList);
        mTemplateListView.setLayoutManager(new LinearLayoutManager(this));
        mTemplateListView.setAdapter(mTemplateListAdapter);
    }
}
