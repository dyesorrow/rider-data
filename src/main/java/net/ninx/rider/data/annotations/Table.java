package net.ninx.rider.data.annotations;

import java.lang.annotation.*;

/**
 * Table
 */
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    String name() default "";
}