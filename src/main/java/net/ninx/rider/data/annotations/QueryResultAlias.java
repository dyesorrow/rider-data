package net.ninx.rider.data.annotations;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(QueryResultAliases.class)
public @interface QueryResultAlias {

    /**
     * 指定resultMap, 如果未指定，则表示适用于所有resultMap
     * @return
     */
    String resultMap();

    /**
     * 别名
     * @return
     */
    String alias();

}