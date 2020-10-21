package com.geek.task;

import java.io.*;

/**
 * 自定义类加载器
 */
public class CustomClassLoader extends ClassLoader{

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classContent=loadByteArray();
        return defineClass(name,byteConvertor(classContent),0,classContent.length);
    }

    //根据规则转换对应的字节码
    private byte[] byteConvertor(byte[] bytes){
        byte[] newByte=new byte[bytes.length];
        if(bytes!=null && bytes.length>0){
            for(int i=0;i<bytes.length;i++){
                newByte[i]=(byte)(255-bytes[i]);
            }
        }
        return newByte;
    }

    //读取指定的文件内容
    private byte[] loadByteArray(){
        String filePath="D://Hello.xlass";
        InputStream is=null;
        ByteArrayOutputStream outputStream=null;
        try {
            File file=new File(filePath);
            is=new FileInputStream(file);
            outputStream=new ByteArrayOutputStream();
            int i=0;
            while ((i=is.read())!=-1){
                outputStream.write(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(is!=null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return outputStream.toByteArray();
    }
}
