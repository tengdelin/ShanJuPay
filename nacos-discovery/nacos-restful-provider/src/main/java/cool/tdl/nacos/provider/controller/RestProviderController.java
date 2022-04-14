package cool.tdl.nacos.provider.controller;

import cool.tdl.nacos.provider.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tdl
 * @Date 2022/3/27 20:39
 * @description 暴露一个接口
 * @Version 1.0
 */
@RestController
public class RestProviderController {
    @GetMapping(value = "/service")
    public User service() {
        User user = new User("滕德淋", 18, "男");
        return user;
    }
}
