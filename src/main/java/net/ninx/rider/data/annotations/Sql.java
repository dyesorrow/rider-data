package net.ninx.rider.data.annotations;

import java.lang.annotation.*;

/**
 * sql标记
 * value 为必填项
 * type 默认为 SqlType.select
 * resultMap 默认为 ""
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Sql {
    public String value();

    public SqlType type() default SqlType.select;

    public String resultMap() default "";

    public static enum SqlType {
        insert, update, delete, select, execute
    }
}