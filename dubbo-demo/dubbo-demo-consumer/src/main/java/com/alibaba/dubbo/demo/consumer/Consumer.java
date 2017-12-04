package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        for(int i = 0; i < 100000000; i++){
            try {
                DemoService demoService = (DemoService) context.getBean("demoService"); // 获取远程服务代理
                String hello = demoService.sayHello("world"); // 执行远程方法

                System.out.println(hello + ", " + new Date()); // 显示调用结果
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (BeansException e) {
                e.printStackTrace();
            }
        }

    }
}
