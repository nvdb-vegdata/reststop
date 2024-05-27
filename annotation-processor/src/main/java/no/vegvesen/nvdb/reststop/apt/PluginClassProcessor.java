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

package no.vegvesen.nvdb.reststop.apt;

import no.vegvesen.nvdb.reststop.api.Config;
import no.vegvesen.nvdb.reststop.classloaderutils.config.PluginConfigParam;
import no.vegvesen.nvdb.reststop.classloaderutils.config.PluginConfigParams;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@SupportedAnnotationTypes("no.vegvesen.nvdb.reststop.api.Plugin")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class PluginClassProcessor extends AbstractProcessor {

    private Set<String> pluginClasses = new TreeSet<>();
    private File pluginsDescriptorFile;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            FileObject resource = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/services/ReststopPlugin/simple.txt");
            pluginsDescriptorFile = new File(resource.toUri().getPath());
            String content = resource.getCharContent(true).toString();

            pluginClasses.addAll(Arrays.asList(content.split("\n")));
            pluginClasses.remove("");

        } catch (FileNotFoundException | NoSuchFileException e) {
            //Ignore
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {

                List<? extends Element> enclosedElements = element.getEnclosedElements();

                List<String> parameterNames = new ArrayList<>();

                PluginConfigParams params = new PluginConfigParams();

                for (Element enclosedElement : enclosedElements) {
                    if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {

                        ExecutableElement constructor = (ExecutableElement) enclosedElement;

                        List<? extends VariableElement> parameters = constructor.getParameters();

                        for (VariableElement parameter : parameters) {
                            Name simpleName = parameter.getSimpleName();
                            parameterNames.add(simpleName.toString());
                            Config configAnnotation = parameter.getAnnotation(Config.class);
                            if(configAnnotation != null) {
                                PluginConfigParam param = new PluginConfigParam();
                                param.setType(parameter.asType().toString());
                                param.setDefaultValue(configAnnotation.defaultValue());
                                param.setDoc(configAnnotation.doc());
                                String name = configAnnotation.property();
                                if(name.equals("")) {
                                    name = parameter.getSimpleName().toString();
                                }
                                param.setParamName(name);
                                param.setRequired(configAnnotation.required());
                                params.add(param);
                            }

                        }

                    }
                }

                TypeElement clazzElem = (TypeElement) element;
                PackageElement packageElement = (PackageElement) clazzElem.getEnclosingElement();

                try {
                    FileObject parameterNamesFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                            packageElement.getQualifiedName(),
                            clazzElem.getSimpleName() + ".parameternames",
                            element);

                    try (Writer writer = parameterNamesFile.openWriter()) {
                        writer.append(parameterNames.stream().collect(Collectors.joining(",")));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


                try {
                    FileObject configParams = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                            packageElement.getQualifiedName(),
                            clazzElem.getSimpleName() + ".config-params",
                            element);

                    try (OutputStream outputStream = configParams.openOutputStream()) {
                        new ParamsMarshaller().marshall(params, outputStream);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                pluginClasses.add(clazzElem.getQualifiedName().toString());


            }
            pluginsDescriptorFile.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(pluginsDescriptorFile), "utf-8")) {
                writer.append(pluginClasses.stream().collect(Collectors.joining("\n")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }


}
