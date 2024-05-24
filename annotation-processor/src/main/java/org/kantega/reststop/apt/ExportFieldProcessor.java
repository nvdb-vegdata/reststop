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

package org.kantega.reststop.apt;

import org.kantega.reststop.api.Plugin;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 *
 */
@SupportedAnnotationTypes("org.kantega.reststop.api.Export")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ExportFieldProcessor extends AbstractProcessor {


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for(Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                Element classElement = element.getEnclosingElement();
                Plugin plugin = classElement.getAnnotation(Plugin.class);
                if(plugin == null) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "When using @Export on fields, your class must be annotated as @Plugin", classElement);
                } else {
                    if (!element.getModifiers().contains(Modifier.FINAL)) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Export annotated fields must be declared final", element);
                    }
                }
            }
        }
        return false;
    }
}
