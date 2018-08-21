package com.createchance.demo.views;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.createchance.demo.R;
import com.createchance.demo.model.Work;
import com.createchance.demo.presenters.IMain;
import com.createchance.demo.presenters.MainPresenter;

import java.util.List;


/**
 * Main ui
 *
 * @author createchance
 * @date 2018-03-13
 */
public class MainActivity extends Activity implements IMain.View, View.OnClickListener {
    private static final String TAG = "MainActivity";

    private View mNoWorksTipView;
    private RecyclerView mWorksListView;
    private WorksListAdapter mWorksListAdapter;

    private IMain.Presenter mMainPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNoWorksTipView = findViewById(R.id.tv_no_works_tip);
        mWorksListView = findViewById(R.id.rcv_works_list);
        findViewById(R.id.iv_take_one_work).setOnClickListener(this);

        mMainPresenter = new MainPresenter(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMainPresenter.getWorksList();
    }

    @Override
    public void showWorksList(List<Work> workList) {
        if (workList.size() == 0) {
            mWorksListView.setVisibility(View.GONE);
            mNoWorksTipView.setVisibility(View.VISIBLE);
        } else {
            mWorksListView.setVisibility(View.VISIBLE);
            mNoWorksTipView.setVisibility(View.GONE);
            mWorksListAdapter = new WorksListAdapter(this, workList);
            mWorksListView.setLayoutManager(new LinearLayoutManager(this));
            mWorksListView.setAdapter(mWorksListAdapter);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_take_one_work:
                TemplateListActivity.start(this);
                break;
            default:
                break;
        }
    }
}
