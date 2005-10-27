/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @org.xbean.XBean element="soup"
 *  description="This is a tasty soup"
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */              

// START SNIPPET: bean
public class SoupService {
    private static final Log log = LogFactory.getLog(SoupService.class);

    /**
     * @org.xbean.FactoryMethod
     */
    public static SoupService newSoup(String type) {
        return new SoupService(type, System.currentTimeMillis());
    }

    private final String type;
    private final long createTime;
    private boolean exists = false;

    private SoupService(String type, long createTime) {
        this.type = type;
        this.createTime = createTime;
    }

    /**
     * @org.xbean.InitMethod
     */
    public void make() {
        log.info("Making " + type + "soup");
        exists = true;
    }

    /**
     * @org.xbean.DestroyMethod
     */
    public void eat() {
        log.info("Mummmm " + type + "soup is yummie!");
        exists = false;
    }

    public boolean exists() {
        return exists;
    }

    public String getType() {
        return type;
    }

    public long getCreateTime() {
        return createTime;
    }
}
// END SNIPPET: bean

