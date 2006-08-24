/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.jmx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.WeakHashMap;


/**
 * @version $Revision: $ $Date: $
 */
public class JMXStrategyFinder {

    private final static String PROPERTY_NAME = "org.apache.xbean.jmx.WrapperStrategyClass";
    private final static String PATH = "META-INF/org.apache.xbean.jmx.StrategyFinder/";
    private final static WeakHashMap classCache = new WeakHashMap();

    /**
     * Creates a new instance of the given key
     *
     * @param key is the key to add to the path to find a text file
     *            containing the factory name
     * @return a newly created instance
     */
    public static JMXWrappingStrategy newInstance(String key) throws JMXServiceException {

        if (key == null) key = "xbean.default";

        JMXWrappingStrategy result = null;
        try {
            synchronized (classCache) {
                Class clazz = (Class) classCache.get(key);
                if (clazz == null) {
                    clazz = loadClass(doFindServiceWrapperProperies(key));
                    classCache.put(key, clazz);
                }
                result = (JMXWrappingStrategy) clazz.newInstance();
            }
        }
        catch (ClassNotFoundException doNothing) {
            throw new JMXServiceException(doNothing);
        }
        catch (IOException doNothing) {
            throw new JMXServiceException(doNothing);
        }
        catch (InstantiationException doNothing) {
            throw new JMXServiceException(doNothing);
        }
        catch (IllegalAccessException doNothing) {
            throw new JMXServiceException(doNothing);
        }
        return result;
    }

    private static Class loadClass(Properties properties) throws ClassNotFoundException, IOException {

        String className = properties.getProperty(PROPERTY_NAME);
        if (className == null) {
            throw new IOException("Expected property is missing: " + PROPERTY_NAME);
        }
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException e) {
            return JMXStrategyFinder.class.getClassLoader().loadClass(className);
        }
    }

    private static Properties doFindServiceWrapperProperies(String key) throws IOException {
        String uri = PATH + key;

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(uri);
        if (in == null) {
            in = JMXStrategyFinder.class.getClassLoader().getResourceAsStream(uri);
            if (in == null) {
                throw new IOException("Could not find strategy class for resource: " + uri);
            }
        }

        BufferedInputStream reader = null;
        try {
            reader = new BufferedInputStream(in);
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
            }
        }
    }
}
