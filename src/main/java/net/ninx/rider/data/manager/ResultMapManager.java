package net.ninx.rider.data.manager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.util.ReflectUtil;
import net.ninx.rider.data.DB;
import net.ninx.rider.data.annotations.Column;
import net.ninx.rider.data.annotations.QueryResultAlias;
import net.ninx.rider.data.annotations.QueryResultAliases;
import net.ninx.rider.data.annotations.ResultEntity;
import net.ninx.rider.data.utils.CamelUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ResultMap
 */
@Slf4j
public class ResultMapManager {

    private static Map<String, Map<String, Map<String, Field>>> resultMapContainer = new HashMap<>();

    public static void scanResultMaps() {
        DB.use().getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(ResultEntity.class)) {

                Map<String, Map<String, Field>> resultMaps = new HashMap<>();
                resultMaps.put("", new HashMap<>());
                log.debug("Find resultMap : " + clazz.getName());

                // 扫描所有的resultMap
                for (Field field : ReflectUtil.getFields(clazz)) {
                    field.setAccessible(true);

                    /**
                     * 默认结果集
                     */
                    String columnName = "";
                    if (field.isAnnotationPresent(Column.class)) {
                        columnName = field.getAnnotation(Column.class).name();
                    }
                    columnName = columnName.length() == 0 ? field.getName() : columnName;
                    columnName = CamelUtil.camelToUnderScore(columnName);
                    resultMaps.get("").put(columnName, field);

                    /**
                     * 自定义结果集
                     */
                    if (field.isAnnotationPresent(QueryResultAliases.class)) {
                        for (QueryResultAlias alias : field.getAnnotation(QueryResultAliases.class).value()) {
                            if (!resultMaps.containsKey(alias.resultMap())) {
                                log.debug("Find resultMap: " + clazz.getName() + " -> resultMap alias: " + alias.resultMap());
                                resultMaps.put(alias.resultMap(), new HashMap<>());
                            }
                            resultMaps.get(alias.resultMap()).put(alias.alias(), field);
                        }
                    }
                }

                // 注入默认
                resultMaps.forEach((key, value) -> {
                    if (key.length() != 0) {
                        resultMaps.get("").forEach((dkey, dvalue) -> {
                            if (!value.containsKey(dkey)) {
                                value.put(dkey, dvalue);
                            }
                        });
                    }
                });

                resultMapContainer.put(clazz.getName(), resultMaps);
            }
        });
    }

    public static Map<String, Field> getResultMap(Class<?> clazz, String resultMap) {
        return resultMapContainer.get(clazz.getName()).get(resultMap);
    }
}