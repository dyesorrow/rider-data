package net.ninx.rider.data.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * 
 * 扫描包下路径 包括本地文件和jar包文件
 * 
 * @author https://blog.csdn.net/qq_35507342/article/details/73733622
 */

public class ClassScanner {

    private Set<Class<?>> clazzList = new HashSet<Class<?>>();

    private final ClassLoader classLoader = ClassScanner.class.getClassLoader();// 默认使用的类加载器

    /**
     * 扫描时过滤，需要设置就重写此方法
     * @param clazz
     * @return
     */
    protected Class<?> filter(Class<?> clazz){
        return clazz;
    }

    /**
     * 扫描类所在包以及等级之下的包的所有的类
     * 
     * @throws ClassNotFoundException
     */
    public Set<Class<?>> findAllClass(Class<?> clazz) throws ClassNotFoundException {
        String pkg = clazz.getPackage().getName();
        URL url = this.classLoader.getResource(pkg.replace(".", "/"));
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            this.findClassLocal(pkg);
        } else if ("jar".equals(protocol)) {
            this.findClassInJar(pkg);
        }
        return this.clazzList;
    }

    /**
     * 本地查找
     * 
     * @param packName
     * @throws ClassNotFoundException
     * 
     */
    private void findClassLocal(final String packName) throws ClassNotFoundException{
        URI url = null;
        try {
            url = classLoader.getResource(packName.replace(".", "/")).toURI();
        } catch (URISyntaxException e1) {
            throw new RuntimeException("No strategy resource found");
        }
        File file = new File(url);
        for (File chiFile : file.listFiles()) {
            if (chiFile.isDirectory()) {
                findClassLocal(packName + "." + chiFile.getName());
            }
            if (chiFile.getName().endsWith(".class")) {
                Class<?> clazz = null;
                clazz = classLoader.loadClass(packName + "." + chiFile.getName().replace(".class", ""));
                clazzList.add(filter(clazz));
            }
        }
    }

    /**
     * jar包查找
     * 
     * @param packName
     * @throws ClassNotFoundException
     * 
     */
    private void findClassInJar(final String packName) throws ClassNotFoundException {
        String pathName = packName.replace(".", "/");
        JarFile jarFile = null;
        try {
            URL url = classLoader.getResource(pathName);
            JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
            jarFile = jarURLConnection.getJarFile();
        } catch (IOException e) {
            throw new RuntimeException("No strategy resource found");
        }
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName();
            if (jarEntryName.contains(pathName) && !jarEntryName.equals(pathName + "/")) {
                // 递归遍历子目录
                if (jarEntry.isDirectory()) {
                    String clazzName = jarEntry.getName().replace("/", ".");
                    int endIndex = clazzName.lastIndexOf(".");
                    String prefix = null;
                    if (endIndex > 0) {
                        prefix = clazzName.substring(0, endIndex);
                    }
                    findClassInJar(prefix);
                }
                if (jarEntry.getName().endsWith(".class")) {
                    Class<?> clazz = null;
                    clazz = classLoader.loadClass(jarEntry.getName().replace("/", ".").replace(".class", ""));
                    clazzList.add(filter(clazz));
                }
            }

        }

    }
}
