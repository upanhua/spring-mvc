package com.leyou.pojo;

/**
 * @author lh
 * @version 1.0
 * @date 2019-09-26 16:59
 */
public class UserEntity {
    String age;
    String sex;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                ", age='" + age + '\'' +
                ", sex='" + sex + '\'' +
                '}';
    }
}
