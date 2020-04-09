package net.ninx.rider.data.base;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * IdMaker
 */
public class IdMaker {
    private final static Snowflake snowflake = IdUtil.createSnowflake(1, 1);
    public synchronized static long getOneId() {
        return snowflake.nextId();
    }
}