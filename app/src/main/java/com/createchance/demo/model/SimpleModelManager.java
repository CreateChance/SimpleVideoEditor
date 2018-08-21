package com.createchance.demo.model;

import com.createchance.demo.DemoApplication;
import com.createchance.simplevideoeditor.actions.AbstractAction;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class SimpleModelManager {

    private static SimpleModelManager sInstance;

    private List<AbstractAction> mActionList;

    private Template mCurrentTemplate;

    private List<Scene> mSceneList;

    private SimpleModelManager() {

    }

    public synchronized static SimpleModelManager getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleModelManager();
        }

        return sInstance;
    }

    public List<Work> getWorksList() {
        List<Work> workList = new ArrayList<>();

        return workList;
    }

    public List<Template> getTemplateList() {
        List<Template> templateList = new ArrayList<>();
        try {
            InputStream inputStream = DemoApplication.getContext().getAssets().open("template_list.json");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                Template template = Template.fromJson(jsonArray.getString(i));
                templateList.add(template);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return templateList;
    }

    public Template getCurrentTemplate() {
        return mCurrentTemplate;
    }

    public void setCurrentTemplate(Template mCurrentTemplate) {
        this.mCurrentTemplate = mCurrentTemplate;
    }

    public List<Scene> getSceneList() {
        return mSceneList;
    }

    public void setSceneList(List<Scene> mSceneList) {
        this.mSceneList = mSceneList;
    }

    public void setEditActionList(List<AbstractAction> actionList) {
        this.mActionList = actionList;
    }

    public List<AbstractAction> getEditActionList() {
        return mActionList;
    }
}
