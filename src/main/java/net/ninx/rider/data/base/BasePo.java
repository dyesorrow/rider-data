package net.ninx.rider.data.base;

import java.util.Date;

import net.ninx.rider.data.annotations.Column;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础 BasePo, 非必须
 */
@Getter
@Setter
public class BasePo {

    @Column
    private Long id;
    @Column
    private Date createTime;
    @Column
    private Date updateTime;
    @Column
    private Boolean deleted = false;

}