package net.ninx.rider.data;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import cn.hutool.db.ds.simple.SimpleDataSource;
import net.ninx.rider.data.Student.StudentMapper;

public class App {

    public static void main(final String[] args) throws ClassNotFoundException, SQLException {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Student.class);
        classes.add(StudentMapper.class);
        DB.init(App.class, classes, new SimpleDataSource("jdbc:sqlite:data.db", "SA", ""));
        DB.use().getConnection();
        // DB.use().getConnection().setAutoCommit(false);

        StudentMapper mapper = DB.use().mapper(StudentMapper.class);

        Student student = new Student();
        student.setAge(10);
        student.setName("test");
        student.setSource(100.0);
        mapper.create(student);

        Student example = new Student();
        example.setName("test");
        System.out.println(mapper.getOne(example));
    }
}