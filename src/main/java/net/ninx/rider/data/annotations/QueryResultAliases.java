package net.ninx.rider.data.annotations;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface QueryResultAliases {
    QueryResultAlias[] value();
}