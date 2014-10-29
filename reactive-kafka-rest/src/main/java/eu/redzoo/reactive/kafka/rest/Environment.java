/*
 * Copyright (c) 2014 Gregor Roth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.redzoo.reactive.kafka.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;






public class Environment {
    private static final Logger LOG = Logger.getLogger(Environment.class.getName());
    
    private final ImmutableMap<String, String> configs; 
    
    
    public Environment(String appname) {
        configs = load(appname);
    }
    
    public Optional<String> getConfigValue(String name) {
        return Optional.ofNullable(configs.get(name));
    }

    public ImmutableMap<String, String> getConfigValues(String... names) {
        return getConfigValues(ImmutableSet.copyOf(names));
    }
        
    public ImmutableMap<String, String> getConfigValues(ImmutableSet<String> names) {
        return ImmutableMap.copyOf(Maps.filterKeys(configs, name -> names.contains(name)));
    }
    
    
    
    private static ImmutableMap<String, String> load(String appname) {

        // get the location url
        Optional<URL> optionalUrl = Optional.empty();
        try {
            String location = System.getProperty("reactive-kafka-rest");
            optionalUrl = Optional.of((location == null) ? Resources.getResource(appname + ".properties")
                                                         : new URL(location));
        } catch (MalformedURLException mue) {
            LOG.warning("error occured by loading config " + appname + " " + mue.toString());
        }
            
        
        // load the config 
        return optionalUrl.map(url -> loadProperties(url))
                          .orElseGet(ImmutableMap::of);
    }

    
    
    private static ImmutableMap<String, String> loadProperties(URL url) {
        Map<String, String> configs = Maps.newHashMap();
        
        InputStream is = null;
        try {
            is = url.openStream();
            Properties props = new Properties();
            props.load(is);
            
            props.forEach((key, value) -> configs.put(key.toString(), value.toString()));            
        } catch (IOException ioe) {
            LOG.warning("error occured reading properties file " + url + " " + ioe.toString());
        } finally {
            Closeables.closeQuietly(is);
        }
        
        return ImmutableMap.copyOf(configs);
    }
     
}



