package com.xiaonei.commons.jade.jadeinterface.impl.scanner;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class DAOScanner {

    private static SoftReference<DAOScanner> softReference;

    public synchronized static DAOScanner getRoseScanner() {
        if (softReference == null || softReference.get() == null) {
            DAOScanner roseScanner = new DAOScanner();
            softReference = new SoftReference<DAOScanner>(roseScanner);
        }
        return softReference.get();
    }

    // -------------------------------------------------------------

    protected Log logger = LogFactory.getLog(getClass());

    protected ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(
            Thread.currentThread().getContextClassLoader());

    private List<ResourceInfo> classesFolderResources;

    private List<ResourceInfo> jarResources;

    // -------------------------------------------------------------

    private DAOScanner() {
    }

    // -------------------------------------------------------------

    /**
     * 将要被扫描的普通类地址(比如WEB-INF/classes或target/classes之类的地址)
     * 
     * @param resourceLoader
     * @return
     * @throws IOException
     */
    public List<ResourceInfo> getClassesFolderResources() throws IOException {
        if (classesFolderResources == null) {
            List<ResourceInfo> classesFolderResources = new LinkedList<ResourceInfo>();
            Enumeration<URL> found = resourcePatternResolver.getClassLoader().getResources("");
            while (found.hasMoreElements()) {
                URL urlObject = found.nextElement();
                if ("file".equals(urlObject.getProtocol())) {
                    classesFolderResources.add(new ResourceInfo(new UrlResource(urlObject),
                            new String[] { "*" }));
                    if (logger.isDebugEnabled()) {
                        logger.debug("add classes folder: " + urlObject);
                    }
                }
            }
            this.classesFolderResources = classesFolderResources;
            if (logger.isInfoEnabled()) {
                ResourceInfo[] ret = classesFolderResources
                        .toArray(new ResourceInfo[classesFolderResources.size()]);
                logger.info("found classes resources: " + Arrays.toString(ret));
            }
        }
        return Collections.unmodifiableList(classesFolderResources);
    }

    /**
     * 将要被扫描的jar资源
     * 
     * @param resourceLoader
     * @return
     * @throws IOException
     */
    public List<ResourceInfo> getJarResources() throws IOException {
        if (jarResources == null) {
            List<ResourceInfo> jarResources = new LinkedList<ResourceInfo>();
            Enumeration<URL> found = resourcePatternResolver.getClassLoader().getResources(
                    "META-INF");
            while (found.hasMoreElements()) {
                URL urlObject = found.nextElement();
                if ("jar".equals(urlObject.getProtocol())) {
                    try {
                        String path = URLDecoder.decode(urlObject.getPath(), "UTF-8"); // fix 20%
                        if (path.startsWith("file:")) {
                            path = path.substring("file:".length(), path.lastIndexOf("!/"));
                        } else {
                            path = path.substring(0, path.lastIndexOf("!/"));
                        }
                        Resource resource = new FileSystemResource(path);
                        String[] modifier = getManifestRoseValue(resource.getFile());
                        if (modifier.length > 0) {
                            jarResources.add(new ResourceInfo(resource, modifier));
                            if (logger.isDebugEnabled()) {
                                logger.debug("add jar resource: " + path);
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("not rose jar resource: " + path);
                            }
                        }
                    } catch (Exception e) {
                        logger.error(urlObject, e);
                    }
                }
            }
            this.jarResources = jarResources;
            if (logger.isInfoEnabled()) {
                ResourceInfo[] ret = jarResources.toArray(new ResourceInfo[jarResources.size()]);
                logger.info("found rose jar resources: " + Arrays.toString(ret));
            }
        }
        return Collections.unmodifiableList(jarResources);
    }

    protected String[] getManifestRoseValue(File pathname) throws IOException {
        JarFile jarFile = new JarFile(pathname);
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return new String[0];
        }
        Attributes attributes = manifest.getMainAttributes();
        String attrValue = attributes.getValue("Rose");
        if (attrValue == null) {
            return new String[0];
        }
        String[] splits = StringUtils.split(attrValue, ",");
        ArrayList<String> result = new ArrayList<String>(splits.length);
        for (String split : splits) {
            split = split.trim();
            if (StringUtils.isNotEmpty(split)) {
                result.add(split);
            }
        }
        return result.toArray(new String[0]);
    }
}
