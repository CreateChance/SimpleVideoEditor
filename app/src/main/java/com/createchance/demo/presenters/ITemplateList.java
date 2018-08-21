package com.createchance.demo.presenters;

import com.createchance.demo.model.Template;

import java.util.List;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public interface ITemplateList {
    interface View {
        void showTemplateList(List<Template> templateList);
    }
    interface Presenter {
        void getTemplateList();
    }
}
