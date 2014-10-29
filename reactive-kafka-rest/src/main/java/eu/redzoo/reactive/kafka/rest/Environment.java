package eu.redzoo.reactive.kafka.rest;

import java.io.IOException;
import java.io.InputStream;
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
        
        Optional<URL> props = readResource(appname + ".properties");
        Optional<ImmutableMap<String, String>> configs = props.map(url -> loadProperties(url));

        return configs.orElseGet(ImmutableMap::of);
    }
    
    
    private static Optional<URL> readResource(String resourcename) {
        URL url = Resources.getResource(resourcename);

        return Optional.ofNullable(url);
    }


    
    private static ImmutableMap<String, String> loadProperties(URL url) {
        Properties props = new Properties();
            
        InputStream is = null;
        try {
            is = url.openStream();
            props.load(is);
            
            Map<String, String> map = Maps.newHashMap();
            props.forEach((key, value) -> map.put(key.toString(), value.toString()));
            
            return ImmutableMap.copyOf(map);
            
        } catch (IOException ioe) {
            LOG.warning("error occured reading properties file " + url + " " + ioe.toString());
        } finally {
            Closeables.closeQuietly(is);
        }
        
        return ImmutableMap.of();
    }
     
}



