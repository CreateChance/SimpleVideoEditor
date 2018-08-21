package com.createchance.demo.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class Template {
    @SerializedName("scripts")
    public List<String> scriptList;

    public static Template fromJson(String json) {
        return new Gson().fromJson(json, Template.class);
    }
}
