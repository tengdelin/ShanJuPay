package cool.tdl.microservice.service2.service;


import cool.tdl.microservice.service2.api.Service2Api;

/**
 * @author Administrator
 * @version 1.0
 **/
@org.apache.dubbo.config.annotation.Service
public class Service2ApiImpl implements Service2Api {
    public String dubboService2() {

        return "dubboService2";
    }
}
