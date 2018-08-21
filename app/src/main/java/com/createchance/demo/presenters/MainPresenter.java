package com.createchance.demo.presenters;

import com.createchance.demo.model.SimpleModelManager;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class MainPresenter implements IMain.Presenter {

    private final IMain.View mView;

    public MainPresenter(IMain.View view) {
        this.mView = view;
    }

    @Override
    public void getWorksList() {
        mView.showWorksList(SimpleModelManager.getInstance().getWorksList());
    }
}
