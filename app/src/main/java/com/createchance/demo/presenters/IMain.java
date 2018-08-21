package com.createchance.demo.presenters;

import com.createchance.demo.model.Work;

import java.util.List;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public interface IMain {
    interface View {
        void showWorksList(List<Work> workList);
    }

    interface Presenter {
        void getWorksList();
    }
}
