package org.kantega.reststop.helloworld.jaxws;

import jakarta.jws.WebService;

@WebService(endpointInterface = "org.kantega.reststop.helloworld.jaxws.HelloService")
public class HelloServiceImpl implements HelloService {
    
    public String sayHello(String receiver, String lang) {
        return ("se".equals(lang) ? "Hej" : "Hello")  +", " + receiver +"!";
    }
}
