package cool.tdl.nacos.consumer.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author tdl
 * @Date 2022/3/27 21:53
 * @description 用户实体类
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String name;
    private int age;
    private String sex;
}
