package cool.tdl.microservice.service1.service;


import cool.tdl.microservice.service1.api.Service1Api;
import cool.tdl.microservice.service2.api.Service2Api;

/**
 * @author Administrator
 * @version 1.0
 **/
@org.apache.dubbo.config.annotation.Service
public class Service1ApiImpl implements Service1Api {

    @org.apache.dubbo.config.annotation.Reference
    Service2Api service2Api;

    public String dubboService1() {
        //远程调用service2
        String s = service2Api.dubboService2();
        return "dubboService1|"+s;
    }
}
