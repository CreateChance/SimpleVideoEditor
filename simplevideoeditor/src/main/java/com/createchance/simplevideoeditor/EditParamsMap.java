package com.createchance.simplevideoeditor;

import java.util.HashMap;
import java.util.Map;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 30/04/2018
 */
class EditParamsMap {
    static final String KEY_INPUT_VIDEO_INFO = "input_video_info";
    static final String KEY_VIDEO_WATER_MARK_ADD_ACTION = "video_water_mark_add_action";

    private static Map<String, Object> mParamsMap = new HashMap<>();

    synchronized static void saveParams(String key, Object value) {
        mParamsMap.put(key, value);
    }

    static Object loadParams(String key) {
        return mParamsMap.get(key);
    }

    synchronized static boolean removeParams(String key) {
        return mParamsMap.remove(key) != null;
    }

    synchronized static boolean removeAllParams() {
        mParamsMap.clear();
        return mParamsMap.size() > 0;
    }

}
