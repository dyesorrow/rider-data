package net.ninx.rider.data.utils;


import net.ninx.rider.data.manager.TableManager.DbAdapter;


public class DefaultDbAdapter implements DbAdapter {

    public String getSqlType(Class<?> javaType, int length) {
        if (javaType == Long.class) {
            return "BIGINT";
        }
        if (javaType == Integer.class) {
            return "INTEGER";
        }
        if (javaType == Short.class) {
            return "INTEGER";
        }
        if (javaType == Double.class) {
            return "DOUBLE";
        }
        if (javaType == Float.class) {
            return "FLOAT";
        }
        if (javaType == String.class) {
            return "VARCHAR(" + length + ")";
        }
        if (javaType == java.util.Date.class || javaType == java.sql.Timestamp.class) {
            return "TIMESTAMP";
        }
        if (javaType == java.sql.Date.class) {
            return "DATE";
        }
        if (javaType == Boolean.class) {
            return "BOOLEAN";
        }
        if (javaType == java.sql.Clob.class) {
            return "CLOB";
        }
        return "VARCHAR(" + length + ")";
    }

    public String getSqlType(String jdbcType, int length) {
        switch (jdbcType.toUpperCase()) {
            case "VARCHAR":
                return "VARCHAR(" + length + ")";
            default:
                return jdbcType;
        }
    }

}