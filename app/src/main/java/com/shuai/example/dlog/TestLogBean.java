package com.shuai.example.dlog;

import java.io.Serializable;

public class TestLogBean implements Serializable {


    private String name;
    private int age;

    public TestLogBean(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public TestLogBean() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
