package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.demo.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by xjk on 1/11/18.
 */
@Controller
@RequestMapping(value="/user/*")
public class UserController {

    @Reference
    IUserService userService;//调用Dubbo暴露的接口

    @RequestMapping(value="getUser.htm")
    public @ResponseBody
    String getUser(){
        return userService.getUser();
    }
}