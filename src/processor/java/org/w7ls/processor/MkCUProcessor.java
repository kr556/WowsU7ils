package org.w7ls.processor;

import org.w7ls.annotation.cuda.CUHeader;
import org.w7ls.annotation.cuda.CUTypes;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.w7ls.annotation.cuda.CUHeader")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MkCUProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            return true;
        }

        StringBuilder header = new StringBuilder();
        String fnames = "";

        if (annotations.contains(CUHeader.class)) {
            for (Element el : roundEnv.getElementsAnnotatedWith(CUHeader.class)) {
                if (el.getKind() != ElementKind.METHOD) continue;

                ExecutableElement method = (ExecutableElement) el;
                String kernelName = method.getAnnotation(CUHeader.class).cudaCode();
                if (kernelName.isEmpty()) kernelName = method.getSimpleName().toString();

                String params = method.getParameters().stream()
                        .map(p -> CUTypes.toC(p.asType().toString()).cname + " " + p.getSimpleName())
                        .collect(Collectors.joining(", "));

                header.append(String.format(
                        "extern \"C\" __global__ void %s(%s);\n",
                        kernelName, params
                ));

                fnames = processingEnv.getElementUtils().getPackageOf(el).getQualifiedName().toString();
            }

            System.out.println(fnames);

            try {
                FileObject file = processingEnv.getFiler()
                        .createResource(StandardLocation.SOURCE_OUTPUT, "", "cuda/" + fnames +".cu");
                System.out.println("cuda/" + fnames +".cu");

                try (Writer w = file.openWriter()) {
                    w.write(header.toString());
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }

        return true;
    }
}