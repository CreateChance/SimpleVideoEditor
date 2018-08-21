package com.createchance.demo.presenters;

import com.createchance.demo.model.SimpleModelManager;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class TemplateListPresenter implements ITemplateList.Presenter {
    private final ITemplateList.View mView;

    public TemplateListPresenter(ITemplateList.View view) {
        this.mView = view;
    }

    @Override
    public void getTemplateList() {
        mView.showTemplateList(SimpleModelManager.getInstance().getTemplateList());
    }
}
