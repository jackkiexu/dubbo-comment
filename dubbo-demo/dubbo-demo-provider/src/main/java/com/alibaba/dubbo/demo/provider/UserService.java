package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.demo.IUserService;
import org.springframework.stereotype.Component;

/**
 * Created by xjk on 1/11/18.
 */
@Component
@Service
public class UserService implements IUserService {
    @Override
    public String getUser() {
        return "dubbo接口调用成功......";
    }
}
