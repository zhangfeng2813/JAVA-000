package com.geek.task;

import java.lang.reflect.Method;

public class LoderClient {

    public static void main(String[] args) {

        CustomClassLoader loader=new CustomClassLoader();
        try {
            Class helloClass=loader.loadClass("Hello");
            Object instance=helloClass.newInstance();
            Method targetMethod=helloClass.getDeclaredMethod("hello");
            targetMethod.invoke(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
