package com.leyou.controllor;

import com.leyou.annotation.Controller;
import com.leyou.annotation.RequestMapping;
import com.leyou.annotation.Responsbody;
import com.leyou.pojo.UserEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lh
 * @version 1.0
 * @date 2019-09-26 16:50
 */
@Controller
@RequestMapping("/user")
public class MyControllor {

    @RequestMapping("/test.do")
    @Responsbody
    public Object test(HttpServletRequest request, HttpServletResponse response, String name, UserEntity userEntity) {
        System.out.println("request=" + request);
        System.out.println("response=" + response);
        System.out.println("name=" + name);
        System.out.println("userEntity=" + userEntity);
        System.out.println("方法执行了的");
        return userEntity;
    }
}
