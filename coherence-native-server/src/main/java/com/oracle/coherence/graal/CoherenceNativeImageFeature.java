package com.oracle.coherence.graal;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.internal.classgraph.ClassGraph;
import com.tangosol.internal.classgraph.ScanResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.schema.annotation.PortableType;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CoherenceNativeImageFeature implements Feature {
    List<Class<?>> handledSuperTypes = List.of(
            ElementProcessor.class,
            PortableObject.class,
            PofSerializer.class,
            ExternalizableLite.class,
            DocumentPreprocessor.class,
            DocumentElementPreprocessor.ElementPreprocessor.class,
            NamespaceHandler.class,
            CacheConfigNamespaceHandler.Extension.class,
            OperationalConfigNamespaceHandler.Extension.class
    );

    Set<Class<?>> processedTypes = ConcurrentHashMap.newKeySet();

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(access.getApplicationClassPath())
                .overrideClassLoaders(imageClassLoader)
                .enableAllInfo()
                .scan(Runtime.getRuntime().availableProcessors())) {

            scanResult.getAllClasses().forEach(classInfo -> {
                try {
                    var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                    if (clazz.getAnnotation(PortableType.class) != null) {
                        logRegistration(PortableType.class, clazz);
                        registerClass(clazz);
                        // TODO an we refine this?
                        RuntimeReflection.register(clazz.getMethods());
                    }
                    for (Class<?> handledSuperType : handledSuperTypes) {
                        logRegistration(handledSuperType, clazz);
                        registerClass(clazz);
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    // ignore: due to incomplete classpath
                }
            });
        }

        /* Dump processed elements into json */
        if (getProcessedElementsPath() != null) {
            writeToFile(getProcessedElementsPath(), processedTypes.stream()
                    .map(Class::getTypeName)
                    .sorted()
                    .map(c -> "\"" + c + "\"")
                    .collect(Collectors.joining(",\n ", "[\n ", "\n]"))
            );
        }
    }

    private void logRegistration(Class<?> portableTypeClass, Class<?> clazz) {
        if (Boolean.parseBoolean(System.getProperty("com.oracle.coherence.graal.logRegistrations", "false"))) {
            System.out.println(portableTypeClass.getTypeName() + ": " + clazz);
        }

        if (getProcessedElementsPath() != null) {
            processedTypes.add(clazz);
        }
    }

    private static String getProcessedElementsPath() {
        return System.getProperty("com.oracle.coherence.graal.processedElementsPath");
    }

    private static void registerClass(Class<?> clazz) {
        /* Register all members: a new API is coming where this is one line */
        RuntimeReflection.register(clazz);
        RuntimeReflection.registerAllClasses(clazz);
        RuntimeReflection.registerAllDeclaredClasses(clazz);
        RuntimeReflection.registerAllDeclaredMethods(clazz);
        RuntimeReflection.registerAllMethods(clazz);
        RuntimeReflection.registerAllDeclaredConstructors(clazz);
        RuntimeReflection.registerAllConstructors(clazz);
        RuntimeReflection.registerAllFields(clazz);
        RuntimeReflection.registerAllDeclaredFields(clazz);
        RuntimeReflection.registerAllNestMembers(clazz);
        RuntimeReflection.registerAllPermittedSubclasses(clazz);
        RuntimeReflection.registerAllRecordComponents(clazz);
        RuntimeReflection.registerAllSigners(clazz);

        try {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                RuntimeReflection.register(constructor);
            }
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (declaredMethod.getAnnotation(Injectable.class) != null ||
                        declaredMethod.getName().equals("$deserializeLambda$")) {
                    RuntimeReflection.register(declaredMethod);
                }
            }
        } catch (LinkageError e) {
            // ignore, can't link class
        }
    }

    private static void writeToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
