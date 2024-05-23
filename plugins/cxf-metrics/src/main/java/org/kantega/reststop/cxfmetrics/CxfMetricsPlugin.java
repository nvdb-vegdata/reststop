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

package org.kantega.reststop.cxfmetrics;

import com.codahale.metrics.MetricRegistry;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.kantega.reststop.api.Export;
import org.kantega.reststop.api.Plugin;
import org.kantega.reststop.cxf.EndpointCustomizer;

import javax.xml.namespace.QName;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 *
 */
@Plugin
public class CxfMetricsPlugin implements EndpointCustomizer {


    private final MetricRegistry metricRegistry;

    @Export
    private final EndpointCustomizer endpointCustomizer = this;

    public CxfMetricsPlugin(MetricRegistry metricRegistry) {

        this.metricRegistry = metricRegistry;
    }

    @Override
    public void customizeEndpoint(Endpoint endpoint) {
// Utkommentert fordi SOAP ikke skal brukes av Skriv
//
//        EndpointImpl e = (EndpointImpl) endpoint;
//
//        e.getServer().getEndpoint().getInInterceptors().add(new TimingBeforeInterceptor(Phase.RECEIVE));
//        e.getServer().getEndpoint().getOutInterceptors().add(new TimingAfterInterceptor(Phase.SEND, metricRegistry));
    }

    private class TimingBeforeInterceptor extends AbstractPhaseInterceptor<Message> {
        public TimingBeforeInterceptor(String phase) {
            super(phase);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            message.getExchange().put("time_before", System.nanoTime());
        }
    }

    private class TimingAfterInterceptor extends AbstractPhaseInterceptor<Message> {

        private final MetricRegistry registry;

        private TimingAfterInterceptor(String phase, MetricRegistry registry) {
            super(phase);
            this.registry = registry;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            Long time_before = (Long) message.getExchange().get("time_before");
            if(time_before != null) {
                QName operation = (QName) message.get(Message.WSDL_OPERATION);

                String requestUri = (String) message.getExchange().getInMessage().get(Message.REQUEST_URI);

                String name = name("WS", operation.toString(), requestUri );

                registry.timer(name).update(System.nanoTime() - time_before, TimeUnit.NANOSECONDS);

            }
        }
    }
}
