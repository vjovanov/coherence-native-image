package com.oracle.coherence.graal;

import com.tangosol.internal.classgraph.ClassGraph;
import com.tangosol.internal.classgraph.ScanResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.schema.annotation.PortableType;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

@SuppressWarnings("unused")
public class CoherenceNativeImageFeature implements Feature {
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
                        registerClass(clazz);
                        try {
                            RuntimeReflection.register(clazz.getConstructor(PofReader.class));
                        } catch (NoSuchMethodException e) {
                            // ignore if there is no constructor
                        }
                        // methods could be refined
                        RuntimeReflection.register(clazz.getMethods());
                    } else if (ExternalizableLite.class.isAssignableFrom(clazz)) {
                        registerClass(clazz);
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    // ignore: due to incomplete classpath
                }
            });
        }

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
    }
}
