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

package no.vegvesen.nvdb.reststop.developmentconsole;

import no.vegvesen.nvdb.reststop.classloaderutils.PluginClassLoader;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import no.vegvesen.nvdb.reststop.servlet.api.FilterPhase;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import no.vegvesen.nvdb.reststop.api.ReststopPluginManager;
import no.vegvesen.nvdb.reststop.core.DefaultReststopPluginManager;
import no.vegvesen.nvdb.reststop.core.PluginState;
import no.vegvesen.nvdb.reststop.servlet.api.ServletBuilder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 *
 */
@Plugin
public class DevelopmentConsolePlugin {


    private final ReststopPluginManager pluginManager;
    private final VelocityEngine velocityEngine;

    @Export
    private final Filter devConsole;

    @Export
    private final Filter redirect;
    private final Collection<PluginClassLoader> classLoaders;

    public DevelopmentConsolePlugin(ServletBuilder servletBuilder, Collection<PluginClassLoader> classLoaders, ReststopPluginManager pluginManager, VelocityEngine velocityEngine) {
        this.classLoaders = classLoaders;
        this.pluginManager = pluginManager;
        this.velocityEngine = velocityEngine;

        devConsole = servletBuilder.filter(new DevelopentConsole(), FilterPhase.PRE_UNMARSHAL, "/dev/");
        redirect = servletBuilder.redirectFrom("/dev").to("dev/");
    }

    public class DevelopentConsole implements Filter {


        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

            HttpServletRequest req = (HttpServletRequest) servletRequest;

            HttpServletResponse resp = (HttpServletResponse) servletResponse;

            resp.setContentType("text/html");

            PluginState pluginState = ((DefaultReststopPluginManager) pluginManager).getPluginState();
            VelocityContext context = new VelocityContext();
            context.put("contextPath", req.getContextPath());
            context.put("pluginClassloaders", getPluginClassLoaders(pluginState));
            context.put("dateTool", new DateTool());
            context.put("obfTool", new ObfTool());
            context.put("consoleTool", new ConsoleTool());


            context.put("pluginInfos", getPluginInfos(pluginState));
            velocityEngine.getTemplate("templates/console.vm").merge(context, resp.getWriter());
        }

        private List<PluginInfo> getPluginInfos(PluginState pluginState) {
            List<PluginInfo> infos = new ArrayList<>();
            for (PluginClassLoader classLoader : pluginState.getClassLoaders()) {
                infos.add(classLoader.getPluginInfo());
            }
            return infos;
        }

        private Map<ClassLoader, Collection<Object>> getPluginClassLoaders(PluginState pluginState) {
            Map<ClassLoader, Collection<Object>> map = new IdentityHashMap<>();

            Map<PluginInfo, ClassLoader> infos = new IdentityHashMap<>();

            for (ClassLoader classLoader : pluginState.getClassLoaders()) {
                if ( classLoader instanceof PluginClassLoader loader && !map.containsKey(classLoader)) {
                    map.put(classLoader, new ArrayList<>());
                    infos.put(loader.getPluginInfo(), classLoader);
                }
            }
            for (Object plugin : pluginState.getPlugins()) {
                map.get(pluginState.getClassLoader(plugin)).add(plugin);
            }

            List<PluginInfo> sorted = PluginInfo.resolveClassloaderOrder(new ArrayList<>(infos.keySet()));

            Map<ClassLoader, Collection<Object>> map2 = new LinkedHashMap<>();

            for (PluginInfo info : sorted) {
                ClassLoader classLoader = infos.get(info);
                map2.put(classLoader, map.get(classLoader));
            }



            return map2;
        }


        @Override
        public void destroy() {

        }
    }
}
