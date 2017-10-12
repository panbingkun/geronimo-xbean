/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder.archive;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class FileArchive implements Archive {

    private final ClassLoader loader;
    private final String basePackage;
    private final File dir;
    private List<String> list;
    private final MJarSupport mjar = new MJarSupport();

    public FileArchive(ClassLoader loader, URL url) {
        this.loader = loader;
        this.basePackage = "";
        this.dir = toFile(url);
    }

    public FileArchive(ClassLoader loader, File dir) {
        this.loader = loader;
        this.basePackage = "";
        this.dir = dir;
    }

    public FileArchive(ClassLoader loader, URL url, String basePackage) {
        this.loader = loader;
        this.basePackage = basePackage;
        this.dir = toFile(url);
    }

    public FileArchive(ClassLoader loader, File dir, String basePackage) {
        this.loader = loader;
        this.basePackage = basePackage;
        this.dir = dir;
    }

    public File getDir() {
        return dir;
    }

    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
        int pos = className.indexOf("<");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        pos = className.indexOf(">");
        if (pos > -1) {
            className = className.substring(0, pos);
        }
        if (!className.endsWith(".class")) {
            className = className.replace('.', '/') + ".class";
        }

        if (mjar.isMjar()) {
            final String resource = mjar.getClasses().get(className);
            if (resource != null) {
                className = resource + ".class";
            }
        }

        URL resource = loader.getResource(className);
        if (resource != null) return new BufferedInputStream(resource.openStream());

        throw new ClassNotFoundException(className);
    }


    public Class<?> loadClass(String className) throws ClassNotFoundException {
        // we assume the loader supports mjar if needed, do we want to wrap it to enforce it?
        // probably not otherwise runtime will be weird and unexpected no?
        return loader.loadClass(className);
    }

    public Iterator<Entry> iterator() {
        return new ArchiveIterator(this, _iterator());
    }

    public Iterator<String> _iterator() {
        if (list != null) return list.iterator();

        final File manifest = new File(dir, "META-INF/MANIFEST.MF");
        if (manifest.exists()) {
            InputStream is = null;
            try {
                is =  new FileInputStream(manifest);
                mjar.load(is);
            } catch (final IOException e) {
                // no-op
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
        }

        list = file(dir);
        return list.iterator();
    }

    private List<String> file(File dir) {
        List<String> classNames = new ArrayList<String>();
        if (dir.isDirectory()) {
            scanDir(dir, classNames, (basePackage.length() > 0) ? (basePackage + ".") : basePackage);
        }
        return classNames;
    }

    private void scanDir(File dir, List<String> classNames, String packageName) {
        File[] files = dir.listFiles();
        // using /tmp/. as dir we can get null
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, classNames, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String name = file.getName();
                name = name.substring(0, name.length() - 6);
                if (name.contains(".") || name.equals("module-info") /*todo?*/) continue;
                if (packageName.startsWith("META-INF.versions")) {
                    if (mjar.isMjar()) {
                        mjar.visit(packageName + name);
                        continue;
                    }
                }
                classNames.add(packageName + name);
            }
        }
    }

    private static File toFile(URL url) {
        if (!"file".equals(url.getProtocol())) throw new IllegalArgumentException("not a file url: " + url);
        String path = url.getFile();
        File dir = new File(decode(path));
        if (dir.getName().equals("META-INF")) {
            dir = dir.getParentFile(); // Scrape "META-INF" off
        }
        return dir;
    }

    public static String decode(String fileName) {
        if (fileName.indexOf('%') == -1) return fileName;

        StringBuilder result = new StringBuilder(fileName.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < fileName.length();) {
            char c = fileName.charAt(i);

            if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= fileName.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }

                    int d1 = Character.digit(fileName.charAt(i + 1), 16);
                    int d2 = Character.digit(fileName.charAt(i + 2), 16);

                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("Invalid % sequence (" + fileName.substring(i, i + 3) + ") at: " + String.valueOf(i));
                    }

                    out.write((byte) ((d1 << 4) + d2));

                    i += 3;

                } while (i < fileName.length() && fileName.charAt(i) == '%');


                result.append(out.toString());

                continue;
            } else {
                result.append(c);
            }

            i++;
        }
        return result.toString();
    }
}
