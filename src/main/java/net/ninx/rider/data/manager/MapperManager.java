package net.ninx.rider.data.manager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.TypeUtil;
import net.ninx.rider.data.DB;
import net.ninx.rider.data.annotations.Mapper;
import net.ninx.rider.data.annotations.Param;
import net.ninx.rider.data.annotations.Sql;
import net.ninx.rider.data.utils.SqlUtil;
import net.ninx.rider.data.utils.SqlUtil.SqlMap;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


@Slf4j
public class MapperManager {

    private static interface MapperSqlExecutor {
        public Object execute(Object proxy, Method method, Object[] args) throws Throwable;
    }

    /**
     * cglib 动态代理抽象类 https://github.com/cglib/cglib/tree/master/cglib-sample
     * 
     * @param clazz
     * @param executor
     * @return
     */
    private static Object newInstance(Class<?> clazz, MapperSqlExecutor executor) {
        MethodInterceptor interceptor = new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (!Modifier.isAbstract(method.getModifiers())) {
                    return proxy.invokeSuper(obj, args); // with the correct exception handling
                } else {
                    return executor.execute(obj, method, args);
                }
            }
        };
        Enhancer e = new Enhancer();
        e.setSuperclass(clazz);
        e.setCallback(interceptor);
        return e.create();
    }

    public static void scanMappers() {
        DB.use().getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Mapper.class) && ClassUtil.isAbstract(clazz)) {
                log.debug("Find mapper: " + clazz);

                Object mapperObj = newInstance(clazz, (proxy, method, args) -> {
                    if (method.isAnnotationPresent(Sql.class)) {
                        Sql anno = method.getAnnotation(Sql.class);

                        // 获取模板参数
                        Parameter[] params = method.getParameters();
                        SqlMap<String, Object> sqlMap = SqlUtil.NewSqlMap();
                        for (int i = 0; i < params.length; i++) {
                            String paramName = "p" + i;
                            if (params[i].isAnnotationPresent(Param.class)) {
                                paramName = params[i].getAnnotation(Param.class).value();
                            }
                            sqlMap.put(paramName, args[i]);
                        }

                        // 根据模板生成sql
                        String sqlFtl = anno.value();
                        String sql = SqlUtil.sql(sqlFtl, sqlMap);
                        final List<Object> sqlParams = new ArrayList<>();

                        // 替换sql注入参数
                        sql = ReUtil.replaceAll(sql, "#\\{([A-Za-z_0-9]+)\\}", (matcher) -> {
                            String prop = matcher.group(1);
                            Object t = BeanUtil.getProperty(sqlMap, prop);
                            // if (t == null) {
                            //     log.info("Notice that: #{" + prop + "} is matched with null from params or not matched.");
                            // }
                            sqlParams.add(t);
                            return "?";
                        });

                        // 执行sql
                        if (method.getReturnType() == Void.class) {
                            DB.use().execute(sql, sqlParams);
                            return null;
                        } else {
                            switch (anno.type()) {
                                case insert:
                                case update:
                                case delete:
                                case execute:
                                    return DB.use().execute(sql, sqlParams);
                                case select:
                                    boolean returnList = List.class.isAssignableFrom(method.getReturnType());
                                    if (returnList) {
                                        Class<?> methodReturnBaseType = (Class<?>) TypeUtil.getTypeArgument(TypeUtil.getReturnType(method));
                                        return DB.use().queryList(sql, methodReturnBaseType, anno.resultMap(), sqlParams);
                                    } else {
                                        return DB.use().query(sql, method.getReturnType(), anno.resultMap(), sqlParams);
                                    }
                            }
                            return null;
                        }
                    } else {
                        log.warn("not sql for method " + method);
                        return null;
                    }
                });
                DB.use().addMapper(mapperObj);
            }
        });
    }
}