package com.createchance.simplevideoeditor;

import java.util.HashMap;
import java.util.Map;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 30/04/2018
 */
public class EditParamsMap {
    public static final String KEY_VIDEO_INFO = "video_info";
    public static final String KEY_VIDEO_WATER_MARK_ADD_ACTION = "video_water_mark_add_action";

    private static Map<String, Object> mParamsMap = new HashMap<>();

    public synchronized static void saveParams(String key, Object value) {
        mParamsMap.put(key, value);
    }

    public static Object loadParams(String key) {
        return mParamsMap.get(key);
    }

    public synchronized static boolean removeParams(String key) {
        return mParamsMap.remove(key) != null;
    }

    public synchronized static boolean removeAllParams() {
        mParamsMap.clear();
        return mParamsMap.size() > 0;
    }

}
