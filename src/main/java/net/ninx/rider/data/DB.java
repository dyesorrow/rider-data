package net.ninx.rider.data;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.hutool.core.convert.Convert;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.EntityHandler;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.ninx.rider.data.annotations.ResultEntity;
import net.ninx.rider.data.manager.MapperManager;
import net.ninx.rider.data.manager.ResultMapManager;
import net.ninx.rider.data.manager.TableManager;
import net.ninx.rider.data.utils.ClassScanner;

@Slf4j
public class DB {

    /**
     * 单例模式
     */
    private static volatile DB db;

    @Getter
    private final Set<Class<?>> classes;

    @Getter
    private DataSource dataSource;

    /**
     * 链接管理，一个线程一个链接
     */
    private final Map<Long, Connection> connectionMap = new HashMap<>();
    /**
     * mappers 管理
     */
    @Getter
    private final List<Object> mappers = new ArrayList<>();

    private DB(Class<?> clazz, DataSource dataSource) throws ClassNotFoundException {
        ClassScanner scanner = new ClassScanner();
        this.classes = scanner.findAllClass(clazz);
        this.dataSource = dataSource;
    }

    public static synchronized DB init(Class<?> clazz, DataSource dataSource) throws ClassNotFoundException {
        if (db == null) {
            db = new DB(clazz, dataSource);
            TableManager.scanAndInitTables();
            ResultMapManager.scanResultMaps();
            MapperManager.scanMappers();
        } else {
            throw new RuntimeException("RiderDataApp has been initialized");
        }
        return db;
    }

    public static synchronized DB init(Class<?> clazz, Set<Class<?>> classes, DataSource dataSource) throws ClassNotFoundException {
        if (db == null) {
            db = new DB(clazz, dataSource);
            db.classes.addAll(classes);

            TableManager.scanAndInitTables();
            ResultMapManager.scanResultMaps();
            MapperManager.scanMappers();
        } else {
            throw new RuntimeException("RiderDataApp has been initialized");
        }
        return db;
    }

    public static DB use() {
        if (db == null) {
            throw new RuntimeException("RiderDataApp not been initialized, please call `init()` first");
        }
        return db;
    }

    public static DB use(DataSource dataSource) {
        use();
        db.dataSource = dataSource;
        return db;
    }

    public void addMapper(Object object) {
        mappers.add(object);
    }

    @SuppressWarnings("unchecked")
    public <E> E mapper(Class<E> clazz) {
        for (Object mapper : mappers) {
            if (clazz.isInstance(mapper)) {
                return (E) mapper;
            }
        }
        return null;
    }

    public static class ExecuteException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ExecuteException(Throwable e) {
            super(e);
        }
    }

    /**
     * 获取当前线程唯一链接
     * 
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        long id = Thread.currentThread().getId();
        if (connectionMap.get(id) != null && !connectionMap.get(id).isClosed()) {
            return connectionMap.get(id);
        } else {
            connectionMap.entrySet().removeIf(entries -> {
                try {
                    return entries.getValue().isClosed();
                } catch (Exception e) {
                    return true;
                }
            });

            DataSource dataSource = DB.use().getDataSource();
            connectionMap.put(id, dataSource.getConnection());
            return connectionMap.get(id);
        }
    }

    public int execute(String sql) throws SQLException {
        return execute(sql, null);
    }

    public int execute(String sql, List<Object> paramList) throws SQLException {
        formatSqlParam(paramList);
        printSql(sql, Integer.class, "", paramList);
        return SqlExecutor.execute(getConnection(), sql, paramList == null ? new Object[] {} : paramList.toArray());
    }

    public <E> List<E> queryList(String sql, Class<E> resultType) throws SQLException {
        return queryList(sql, resultType, null);
    }

    public <E> List<E> queryList(String sql, Class<E> resultType, List<Object> paramList) throws SQLException {
        return queryList(sql, resultType, "", paramList);
    }

    public <E> List<E> queryList(String sql, Class<E> resultType, String resultMap, List<Object> paramList) throws SQLException {
        formatSqlParam(paramList);
        printSql(sql, resultType, resultMap, paramList);
        List<Entity> data = SqlExecutor.query(getConnection(), sql, new EntityListHandler(true), paramList == null ? new Object[] {} : paramList.toArray());
        return data == null ? null : data.stream().map(e -> caseTo(e, resultType, resultMap)).collect(Collectors.toList());
    }

    public <E> E query(String sql, Class<E> resultType) {
        return query(sql, resultType, null);
    }

    public <E> E query(String sql, Class<E> resultType, List<Object> paramList) {
        return query(sql, resultType, "", paramList);
    }

    public <E> E query(String sql, Class<E> resultType, String resultMap, List<Object> paramList) {
        formatSqlParam(paramList);
        printSql(sql, resultType, resultMap, paramList);
        try {
            Entity e = SqlExecutor.query(getConnection(), sql, new EntityHandler(), paramList == null ? new Object[] {} : paramList.toArray());
            return caseTo(e, resultType, resultMap);
        } catch (Exception e) {
            throw new ExecuteException(e);
        }
    }

    /**
     * 类型转换
     * 
     * @param <E>
     * @param e
     * @param resultType
     * @param resultMap
     * @return
     */
    public <E> E caseTo(Entity e, Class<E> resultType, String resultMap) {
        if (e == null) {
            return null;
        }
        try {
            if (resultType.isAnnotationPresent(ResultEntity.class)) {
                Map<String, Field> map = ResultMapManager.getResultMap(resultType, resultMap);
                E t = resultType.getDeclaredConstructor().newInstance();
                for (String k : e.keySet()) {
                    if (map.containsKey(k.toUpperCase())) {
                        Field field = map.get(k.toUpperCase());
                        // 类型转换，进行赋值
                        field.set(t, Convert.convert(field.getType(), e.get(k)));
                    }
                }
                return t;
            } else {
                if (isBaseTypeForSql(resultType)) {
                    for (Object v : e.values()) {
                        return Convert.convert(resultType, v);
                    }
                    return null;
                } else {
                    return e.toBean(resultType);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("类型转化失败：" + ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private static Class[] SQL_OBJECT_CLASS = { //
            String.class, //
            Boolean.class, //
            java.lang.Number.class, //
            java.util.Date.class, //
            java.sql.Ref.class, //
            java.sql.Blob.class, //
            java.sql.Clob.class, //
            java.sql.NClob.class, //
            java.sql.Struct.class, //
            java.net.URL.class, //
            java.sql.RowId.class, //
            java.sql.SQLXML.class, //
            java.sql.Array.class //
    };

    /**
     * 判断是否是基本类型
     * 
     * @param clazz
     * @return
     */
    private static boolean isBaseTypeForSql(Class<?> clazz) {
        for (Class<?> parent : SQL_OBJECT_CLASS) {
            if (parent.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理特殊类型处理成字符串进行存储
     * 
     * @return
     */
    private static List<Object> formatSqlParam(List<Object> params) {
        if (params == null) {
            return null;
        }
        params.replaceAll(e -> {
            if (e == null) {
                return null;
            }
            if (isBaseTypeForSql(e.getClass())) {
                return e;
            }
            return e.toString();
        });
        return params;
    }

    /**
     * 打印日志
     * 
     * @param sql
     * @param resultType
     * @param resultMap
     * @param paramList
     */
    public static void printSql(String sql, Class<?> resultType, String resultMap, List<Object> paramList) {
        log.debug("=================SQL=================");
        log.debug("SQL: \n" + sql);
        log.debug("@ Param: " + JSONObject.toJSONString(paramList, SerializerFeature.WriteMapNullValue));
        log.debug("Result: [class: " + resultType.getName() + "]" + " -> [resultMap: " + resultMap + "]");
        log.debug("=====================================");
    }

}