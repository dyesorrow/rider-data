package net.ninx.rider.data.base;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.TypeUtil;
import net.ninx.rider.data.DB;
import net.ninx.rider.data.manager.TableManager;
import net.ninx.rider.data.manager.TableManager.TableElement;

/**
 * 基础的Mapper, 赋予 Po 基础的增删改查。非必须
 */
public abstract class BaseMapper<T extends BasePo> {

    @SuppressWarnings("unchecked")
    protected Class<T> getTClass() {
        return (Class<T>) TypeUtil.getTypeArgument(this.getClass());
    }

    /**
     * 添加一条记录，返回添加后的结果，主要是 id
     * 
     * @param data
     * @return
     */
    public T create(T data) {
        data.setCreateTime(new Date());
        data.setUpdateTime(new Date());
        data.setDeleted(false);
        data.setId(IdMaker.getOneId());

        TableElement tableInfo = TableManager.getTableInfo(getTClass());

        StringBuilder sb = new StringBuilder();
        sb.append("insert into ");
        sb.append(tableInfo.getTableName());
        sb.append(" (");
        sb.append(CollUtil.join(tableInfo.getColumns().stream().map(e -> e.COLUMN_NAME()).collect(Collectors.toList()), ", "));
        sb.append(") values (");
        sb.append(CollUtil.join(tableInfo.getColumns().stream().map(e -> "?").collect(Collectors.toList()), ", "));
        sb.append(")");

        try {
            DB.use().execute(sb.toString(), tableInfo.getColumns().stream().map(e -> {
                try {
                    return e.field().get(data);
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.toList()));
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
        return data;
    }

    /**
     * 更新数据，空值会进行替换
     * 
     * @param data
     */
    public int put(T data) {

        data.setUpdateTime(new Date());

        TableElement tableInfo = TableManager.getTableInfo(getTClass());

        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(tableInfo.getTableName());
        sb.append(" set ");
        sb.append(CollUtil.join(tableInfo.getColumns().stream().filter(e -> !e.COLUMN_NAME().equals("ID")).map(e -> e.COLUMN_NAME() + "=?").collect(Collectors.toList()), ", "));
        sb.append(" where ID = ?");

        List<Object> params = tableInfo.getColumns().stream().filter(e -> !e.COLUMN_NAME().equals("ID")).map(e -> {
            try {
                return e.field().get(data);
            } catch (IllegalArgumentException | IllegalAccessException e1) {
                throw new RuntimeException(e1);
            }
        }).collect(Collectors.toList());
        params.add(data.getId());

        try {
            return DB.use().execute(sb.toString(), params);
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * 更新数据，空值不会进行set操作
     * 
     * @param data
     */
    public int update(T data) {

        data.setUpdateTime(new Date());

        TableElement tableInfo = TableManager.getTableInfo(getTClass());

        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(tableInfo.getTableName());
        sb.append(" set ");
        sb.append(CollUtil.join(tableInfo.getColumns().stream().filter(e -> !e.COLUMN_NAME().equals("ID")).filter(e -> {
            try {
                return e.field().get(data) != null;
            } catch (IllegalArgumentException | IllegalAccessException e2) {
                throw new RuntimeException(e2);
            }
        }).map(e -> e.COLUMN_NAME() + "=?").collect(Collectors.toList()), ", "));
        sb.append(" where ID = ?");

        List<Object> params = tableInfo.getColumns().stream().filter(e -> !e.COLUMN_NAME().equals("ID")).map(e -> {
            try {
                return e.field().get(data);
            } catch (IllegalArgumentException | IllegalAccessException e1) {
                throw new RuntimeException(e1);
            }
        }).filter(e -> e != null).collect(Collectors.toList());
        params.add(data.getId());

        try {
            return DB.use().execute(sb.toString(), params);
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * 根据id删除结果
     * 
     * @param id
     */
    public int delete(Long id) {
        try {
            T data = this.getTClass().getDeclaredConstructor().newInstance();
            data.setDeleted(true);
            data.setId(id);
            return update(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Id获取一个, 没有返回 null
     * 
     * @param id
     * @return
     */
    public T get(Long id) {
        try {
            T example = getTClass().getDeclaredConstructor().newInstance();
            example.setId(id);
            return getOne(example);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据示例获取一个结果, 没有返回 null. 示例中空值不做等于判断
     * 
     * @param example
     * @return
     */
    public T getOne(T example) {
        List<T> result = list(example);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * 根据示例获取所有结果。示例中空值不做等于判断
     * 
     * @param example
     * @return
     */
    public List<T> list(T example) {
        return list(example, 1L, Long.MAX_VALUE);
    }

    /**
     * 根据示例获取分页结果。示例中空值不做等于判断. 默认取 pageAt = 1，pageSize = 10。 分页的起始页为 1， 分页方式为 limit a, b
     * 
     * 如果设置 pageSize = Long.MAX_VALUE， 则表示不进行分页
     * 
     * @param example
     * @param pageAt
     * @param pageSize
     * @return
     */
    public List<T> list(T example, Long pageAt, Long pageSize) {
        pageAt = pageAt == null ? 1 : pageAt;
        pageSize = pageSize == null ? 10 : pageSize;

        long elementStart = (pageAt - 1) * pageSize;

        TableElement tableInfo = TableManager.getTableInfo(getTClass());

        StringBuilder sb = new StringBuilder();
        sb.append("select * from ");
        sb.append(tableInfo.getTableName());
        sb.append(" where deleted = false and ");
        sb.append(CollUtil.join(tableInfo.getColumns().stream().filter(e -> {
            try {
                return e.field().get(example) != null;
            } catch (IllegalArgumentException | IllegalAccessException e2) {
                throw new RuntimeException(e2);
            }
        }).map(e -> e.COLUMN_NAME() + " = ? ").collect(Collectors.toList()), " and "));

        if (pageSize != Long.MAX_VALUE) {
            sb.append("limit ");
            sb.append(elementStart);
            sb.append(", ");
            sb.append(pageSize);
        }

        try {
            return DB.use().queryList(sb.toString(), getTClass(), tableInfo.getColumns().stream().map(e -> {
                try {
                    return e.field().get(example);
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }).filter(e -> e != null).collect(Collectors.toList()));
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
    }

    /**
     * 根据示例获取分页结果。示例中空值不做等于判断。分页的起始页为 1， 分页方式为 limit a, b
     * 
     * 如果设置 pageSize = Long.MAX_VALUE， 则表示不进行分页
     * 
     * @param example
     * @param pageAt
     * @param pageSize
     * @return
     */
    public Pager<T> list(T example, Pager<T> pager) {
        try {
            long elementStart = (pager.getPageAt() - 1) * pager.getPageSize();

            TableElement tableInfo = TableManager.getTableInfo(getTClass());
            StringBuilder sb = new StringBuilder();
            sb.append("select * from ");
            sb.append(tableInfo.getTableName());
            sb.append(" where  deleted = false and ");
            sb.append(CollUtil.join(tableInfo.getColumns().stream().filter(e -> {
                try {
                    return e.field().get(example) != null;
                } catch (IllegalArgumentException | IllegalAccessException e2) {
                    throw new RuntimeException(e2);
                }
            }).map(e -> e.COLUMN_NAME() + " = ? ").collect(Collectors.toList()), " and "));

            // 获取总数
            Integer total = DB.use().query("select count(*) from (" + sb.toString() + ") t", Integer.class, tableInfo.getColumns().stream().map(e -> {
                try {
                    return e.field().get(example);
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }).filter(e -> e != null).collect(Collectors.toList()));

            // 填充分页信息
            pager.setTotalCount(total);
            pager.setTotalPage(total / pager.getPageSize() + total % pager.getPageSize() == 0 ? 0 : 1);

            // 进行分页
            if (pager.getPageSize() != Long.MAX_VALUE) {
                sb.append("limit ");
                sb.append(elementStart);
                sb.append(", ");
                sb.append(pager.getPageSize());
            }

            // 获取数据
            List<T> result = DB.use().queryList(sb.toString(), getTClass(), tableInfo.getColumns().stream().map(e -> {
                try {
                    return e.field().get(example);
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }).filter(e -> e != null).collect(Collectors.toList()));

            // 填充数据
            pager.setData(result);
            return pager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}