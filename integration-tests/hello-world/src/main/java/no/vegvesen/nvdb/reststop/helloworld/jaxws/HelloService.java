/*
 * Copyright 2018 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.vegvesen.nvdb.reststop.helloworld.jaxws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 *
 */
@WebService(serviceName = "HelloService",
        name = "Hello",
        targetNamespace = "http://reststop.kantega.org/ws/hello-1.0",
        wsdlLocation = "META-INF/wsdl/HelloService.wsdl")

public class HelloService {


    @WebMethod(operationName = "greet")
    @WebResult(name = "messageResult")
    public String sayHello(@WebParam(name = "receiver") String receiver, @WebParam(name = "lang") String lang) {
        return ("se".equals(lang) ? "Hej" : "Hello")  +", " + receiver +"!";
    }
}
