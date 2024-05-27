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

package no.vegvesen.nvdb.reststop.wicket;

import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.kantega.reststop.api.*;
import no.vegvesen.nvdb.reststop.servlet.api.FilterPhase;
import org.kantega.reststop.servlet.api.ServletBuilder;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.util.Properties;

/**
 *
 */
@Plugin
public class WicketPlugin  {


    @Export
    private final WebApplication wicketApplication;

    @Export
    private final Filter wicketFilter;
    private final WicketFilter realWicketfilter;

    public WicketPlugin(ServletBuilder servletBuilder) throws ServletException {

            wicketApplication = new WicketApplication();

            realWicketfilter = new WicketFilter(wicketApplication);

            Properties properties = new Properties();
            String filterPath = "/wicket/*";
            properties.setProperty(WicketFilter.FILTER_MAPPING_PARAM, filterPath);

            realWicketfilter.init(servletBuilder.filterConfig("wicket", properties));

            wicketFilter = servletBuilder.filter(realWicketfilter, FilterPhase.USER, filterPath);

    }

    @PreDestroy
    public void destroy() {
        realWicketfilter.destroy();
    }
}
