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

package no.vegvesen.nvdb.reststop.helloworld;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class HelloServiceIT {

    @Test
    public void shouldRespond() throws TransformerException {

        Dispatch<Source> helloPort = getDispatch();

        Source invoke = helloPort.invoke(new StreamSource(getClass().getResourceAsStream("helloRequest.xml")));

        DOMResult result = new DOMResult();
        TransformerFactory.newInstance().newTransformer().transform(invoke, result);

        Document doc = (Document) result.getNode();
        String textContent = doc.getDocumentElement().getTextContent();

        assertThat(textContent, is("Hello, Joe!"));
    }

    // Replaced with a REST integration test in HelloWorldIT
//    @Test(expected = WebServiceException.class)
//    public void shouldFailBecauseOfNullReceiver() throws TransformerException {
//
//        Dispatch<Source> helloPort = getDispatch();
//
//        helloPort.invoke(new StreamSource(getClass().getResourceAsStream("helloRequest-fail.xml")));
//
//    }

    private Dispatch<Source> getDispatch() {
        Service helloService = Service.create(getClass().getResource("/META-INF/wsdl/HelloService.wsdl"),
                new QName("http://reststop.kantega.org/ws/hello-1.0", "HelloService"));

        Dispatch<Source> helloPort = helloService.createDispatch(new QName("http://reststop.kantega.org/ws/hello-1.0", "HelloPort"),
                Source.class, Service.Mode.PAYLOAD);

        Map<String,Object> rc = helloPort.getRequestContext();
        rc.put(BindingProvider.USERNAME_PROPERTY, "joe");
        rc.put(BindingProvider.PASSWORD_PROPERTY, "joe");
        rc.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:" + Utils.readPort() + "/ws/hello-1.0");
        return helloPort;
    }
}
