/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.language;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExtendedPropertyConfigurerGetter;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.spi.ConfigurerStrategy;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.support.component.PropertyConfigurerSupport;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class LanguageEndpointConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        LanguageEndpoint target = (LanguageEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "allowcontextmapall":
        case "allowContextMapAll": target.setAllowContextMapAll(property(camelContext, boolean.class, value)); return true;
        case "binary": target.setBinary(property(camelContext, boolean.class, value)); return true;
        case "cachescript":
        case "cacheScript": target.setCacheScript(property(camelContext, boolean.class, value)); return true;
        case "contentcache":
        case "contentCache": target.setContentCache(property(camelContext, boolean.class, value)); return true;
        case "lazystartproducer":
        case "lazyStartProducer": target.setLazyStartProducer(property(camelContext, boolean.class, value)); return true;
        case "resulttype":
        case "resultType": target.setResultType(property(camelContext, java.lang.String.class, value)); return true;
        case "script": target.setScript(property(camelContext, java.lang.String.class, value)); return true;
        case "transform": target.setTransform(property(camelContext, boolean.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "allowcontextmapall":
        case "allowContextMapAll": return boolean.class;
        case "binary": return boolean.class;
        case "cachescript":
        case "cacheScript": return boolean.class;
        case "contentcache":
        case "contentCache": return boolean.class;
        case "lazystartproducer":
        case "lazyStartProducer": return boolean.class;
        case "resulttype":
        case "resultType": return java.lang.String.class;
        case "script": return java.lang.String.class;
        case "transform": return boolean.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        LanguageEndpoint target = (LanguageEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "allowcontextmapall":
        case "allowContextMapAll": return target.isAllowContextMapAll();
        case "binary": return target.isBinary();
        case "cachescript":
        case "cacheScript": return target.isCacheScript();
        case "contentcache":
        case "contentCache": return target.isContentCache();
        case "lazystartproducer":
        case "lazyStartProducer": return target.isLazyStartProducer();
        case "resulttype":
        case "resultType": return target.getResultType();
        case "script": return target.getScript();
        case "transform": return target.isTransform();
        default: return null;
        }
    }
}

