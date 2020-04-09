package net.ninx.rider.data.manager;

import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.ReflectUtil;
import net.ninx.rider.data.DB;
import net.ninx.rider.data.annotations.Column;
import net.ninx.rider.data.annotations.Table;
import net.ninx.rider.data.utils.DefaultDbAdapter;
import net.ninx.rider.data.utils.CamelUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * TableManager
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class TableManager {

    /**
     * 不同的数据库需要进行不同的适配
     */
    public static interface DbAdapter {
        public String getSqlType(Class<?> javaType, int length);

        public String getSqlType(String jdbcType, int length);
    }

    /**
     * 存储表信息
     */
    @Getter
    @Setter
    @RequiredArgsConstructor(staticName = "of")
    public static class TableElement {
        @NonNull
        private String tableName;
        @NonNull
        private List<TableColumnConfig> columns;
    }

    /**
     * 表sql配置信息
     */
    @Getter
    @Setter
    @EqualsAndHashCode
    @RequiredArgsConstructor(staticName = "of")
    @Accessors(fluent = true, chain = true)
    public static class TableColumnConfig {

        @EqualsAndHashCode.Exclude
        private Field field;

        private String COLUMN_NAME;

        private String TYPE_DEFINE;
    }

    /**
     * 找不到表异常
     */
    public static class NotTableException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    @Setter
    private static DbAdapter tableGenerator = new DefaultDbAdapter();


    private static final Map<Class, TableElement> tableContainer = new HashMap<>();

    public static void scanAndInitTables() {
        DB.use().getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Table.class)) {
                String tableName = clazz.getAnnotation(Table.class).name();
                tableName = tableName.length() == 0 ? clazz.getSimpleName() : tableName;
                tableName = CamelUtil.camelToUnderScore(tableName);
                log.debug("Find table: " + tableName);

                List<TableColumnConfig> columns = new ArrayList<>();
                for (Field field : ReflectUtil.getFields(clazz)) {
                    if (field.isAnnotationPresent(Column.class)) {


                        String columnName = field.getAnnotation(Column.class).name();
                        columnName = columnName.length() == 0 ? field.getName() : columnName;
                        columnName = CamelUtil.camelToUnderScore(columnName);

                        field.setAccessible(true);
                        columns.add(TableColumnConfig.of().field(field).COLUMN_NAME(columnName).TYPE_DEFINE(tableGenerator.getSqlType(field.getType(), field.getAnnotation(Column.class).length())));
                    }
                }

                tableContainer.put(clazz, TableElement.of(tableName, columns));

                // 生成或者更新表结构
                initTable(tableName, columns);
            }
        });
    }

    /**
     * 生成或者更新表结构
     * 
     * @param tableName
     * @param columns
     */
    private static void initTable(String tableName, List<TableColumnConfig> columns) {

        /**
         * 对比分析，初始化或者更新表结构
         */
        try {
            DatabaseMetaData md = DB.use().getConnection().getMetaData();
            /**
             * 获取表是否存在
             */
            ResultSet rs1 = md.getTables(null, "%", tableName, new String[] { "TABLE" });
            boolean tableExist = false;
            while (rs1.next()) {
                if (rs1.getString("TABLE_NAME").equals(tableName)) {
                    tableExist = true;
                }
            }
            if (!tableExist) {
                // 表不存在，创建表
                StringBuilder sb = new StringBuilder();
                sb.append("create table ");
                sb.append(tableName);
                sb.append(" (");
                sb.append("ID BIGINT PRIMARY KEY, ");

                for (TableColumnConfig tableColumnConfig : columns) {
                    if (tableColumnConfig.COLUMN_NAME.toUpperCase().equals("ID")) {
                        continue;
                    }
                    sb.append(tableColumnConfig.COLUMN_NAME);
                    sb.append(" ");
                    sb.append(tableColumnConfig.TYPE_DEFINE);
                    sb.append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());
                sb.append(")");

                DB.use().execute(sb.toString());
            } else {
                // 表存在，更新表结构信息

                /**
                 * 获取数据库中的表信息
                 */
                ResultSet rs = md.getColumns(null, "%", tableName, "%");
                Set<TableColumnConfig> tableInfo = new HashSet<>();
                while (rs.next()) {
                    TableColumnConfig column = new TableColumnConfig();
                    column.COLUMN_NAME = rs.getString("COLUMN_NAME").toUpperCase();
                    column.TYPE_DEFINE = tableGenerator.getSqlType(rs.getString("TYPE_NAME").toUpperCase(), rs.getInt("COLUMN_SIZE"));
                    tableInfo.add(column);
                }
                Map<String, Object> nowMap = tableInfo.stream().collect(Collectors.toMap(TableColumnConfig::COLUMN_NAME, TableColumnConfig -> TableColumnConfig));

                // 新增或者更新列
                for (TableColumnConfig e : columns) {
                    if (!nowMap.containsKey(e.COLUMN_NAME)) {
                        // 新增列
                        String sql = "alter table " + tableName + " add column " + e.COLUMN_NAME + " " + e.TYPE_DEFINE;
                        DB.use().execute(sql);
                    }
                    if (nowMap.containsKey(e.COLUMN_NAME) && !nowMap.get(e.COLUMN_NAME).equals(e)) {
                        // 修改列
                        String sql = "alter table " + tableName + " alter column " + e.COLUMN_NAME + " " + e.TYPE_DEFINE;
                        DB.use().execute(sql);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取表类信息
     * 
     * @param clazz
     * @return
     */
    public static TableElement getTableInfo(Class<?> clazz) {
        return tableContainer.get(clazz);
    }
}