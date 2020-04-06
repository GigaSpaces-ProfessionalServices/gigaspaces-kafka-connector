package com.gigaspaces.kafka.connector.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class Loader {

    private static final Logger logger = Logger.getLogger(Loader.class.getName());
    public static ClassLoader classLoader;

    public static Map<String, Class> loadJar(String pathToJar) throws Exception {
        JarFile jarFile = new JarFile(pathToJar);
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + pathToJar + "!/")};
        Loader.classLoader = URLClassLoader.newInstance(urls);

        Map<String, Class> classes = new HashMap<String, Class>();
        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            try {
                Class c = classLoader.loadClass(className);
                classes.put(c.getSimpleName(), c);
            } catch (ClassNotFoundException exp) {
                logger.warning(exp.getMessage());
            }
        }
        return classes;
    }

}
