package net.ninx.rider.data.utils;

import cn.hutool.core.util.ReUtil;

/**
 * 工具类
 */
public class CamelUtil {

    /**
     * 根据驼峰式命名，获取数据库表名或者字段名。驼峰处会添加下划线
     * 
     * @param camel
     * @return
     */
    public static String camelToUnderScore(String camel) {
        return ReUtil.replaceAll(camel, "([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    /**
     * 判断驼峰式命名与下划线命名是不是同一个命名
     * 
     * @param camel
     * @param underScore
     * @return
     */
    public static boolean isCamelForUnderScore(String camel, String underScore) {
        return camelToUnderScore(camel).equals(underScore.toUpperCase());
    }



}