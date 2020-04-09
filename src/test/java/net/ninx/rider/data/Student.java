package net.ninx.rider.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.ninx.rider.data.annotations.Column;
import net.ninx.rider.data.annotations.Mapper;
import net.ninx.rider.data.annotations.ResultEntity;
import net.ninx.rider.data.annotations.Table;
import net.ninx.rider.data.base.BaseMapper;
import net.ninx.rider.data.base.BasePo;

@Data
@EqualsAndHashCode(callSuper = false)
@Table
@ResultEntity
public class Student extends BasePo {
    
    @Column
    private String name;

    @Column
    private Integer age;

    @Column
    private Double source;


    @Mapper
    public static abstract class StudentMapper extends BaseMapper<Student>{

    }

}