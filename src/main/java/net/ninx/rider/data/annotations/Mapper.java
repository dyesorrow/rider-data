package net.ninx.rider.data.annotations;

import java.lang.annotation.*;

/**
 * Mapper 必须是抽象类
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Mapper {
}