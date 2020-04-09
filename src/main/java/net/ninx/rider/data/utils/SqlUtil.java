package net.ninx.rider.data.utils;

import java.io.IOException;
import java.util.HashMap;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;

/**
 * SqlUtil
 */
public class SqlUtil {

    private static final GroupTemplate gt;
    static {
        try {
            StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();
            Configuration cfg = Configuration.defaultConfiguration();
            gt = new GroupTemplate(resourceLoader, cfg);
        } catch (IOException e) {
            throw new RuntimeException("初始化Beetl工具失败，请检查配置文件：" + e);
        }
    }

    public static class SqlMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1349505384282568715L;

        private SqlMap(){
        }

        public SqlMap<K, V> add(K key, V value) {
            super.put(key, value);
            return this;
        }
    }

    public static SqlMap<String, Object> NewSqlMap() {
        return new SqlMap<>();
    }

    public static String sql(String template) {
        return sql(template, NewSqlMap(), gt);
    }

    public static String sql(String template, SqlMap<String, ?> model) {
        return sql(template, model, gt);
    }

    public static String sql(String template, SqlMap<String, ?> model, GroupTemplate gt) {
        if (template == null) {
            return null;
        }
        if (gt == null) {
            throw new IllegalAccessError("Beetl GroupTemplate 不能为空 !");
        }

        Template t = gt.getTemplate(template);
        t.binding(model);
        return t.render();
    }
}