/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.platform.http;

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
public class PlatformHttpEndpointConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        PlatformHttpEndpoint target = (PlatformHttpEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": target.setBridgeErrorHandler(property(camelContext, boolean.class, value)); return true;
        case "consumes": target.setConsumes(property(camelContext, java.lang.String.class, value)); return true;
        case "exceptionhandler":
        case "exceptionHandler": target.setExceptionHandler(property(camelContext, org.apache.camel.spi.ExceptionHandler.class, value)); return true;
        case "exchangepattern":
        case "exchangePattern": target.setExchangePattern(property(camelContext, org.apache.camel.ExchangePattern.class, value)); return true;
        case "filenameextwhitelist":
        case "fileNameExtWhitelist": target.setFileNameExtWhitelist(property(camelContext, java.lang.String.class, value)); return true;
        case "headerfilterstrategy":
        case "headerFilterStrategy": target.setHeaderFilterStrategy(property(camelContext, org.apache.camel.spi.HeaderFilterStrategy.class, value)); return true;
        case "httpmethodrestrict":
        case "httpMethodRestrict": target.setHttpMethodRestrict(property(camelContext, java.lang.String.class, value)); return true;
        case "matchonuriprefix":
        case "matchOnUriPrefix": target.setMatchOnUriPrefix(property(camelContext, boolean.class, value)); return true;
        case "muteexception":
        case "muteException": target.setMuteException(property(camelContext, boolean.class, value)); return true;
        case "platformhttpengine":
        case "platformHttpEngine": target.setPlatformHttpEngine(property(camelContext, org.apache.camel.component.platform.http.spi.PlatformHttpEngine.class, value)); return true;
        case "produces": target.setProduces(property(camelContext, java.lang.String.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return boolean.class;
        case "consumes": return java.lang.String.class;
        case "exceptionhandler":
        case "exceptionHandler": return org.apache.camel.spi.ExceptionHandler.class;
        case "exchangepattern":
        case "exchangePattern": return org.apache.camel.ExchangePattern.class;
        case "filenameextwhitelist":
        case "fileNameExtWhitelist": return java.lang.String.class;
        case "headerfilterstrategy":
        case "headerFilterStrategy": return org.apache.camel.spi.HeaderFilterStrategy.class;
        case "httpmethodrestrict":
        case "httpMethodRestrict": return java.lang.String.class;
        case "matchonuriprefix":
        case "matchOnUriPrefix": return boolean.class;
        case "muteexception":
        case "muteException": return boolean.class;
        case "platformhttpengine":
        case "platformHttpEngine": return org.apache.camel.component.platform.http.spi.PlatformHttpEngine.class;
        case "produces": return java.lang.String.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        PlatformHttpEndpoint target = (PlatformHttpEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return target.isBridgeErrorHandler();
        case "consumes": return target.getConsumes();
        case "exceptionhandler":
        case "exceptionHandler": return target.getExceptionHandler();
        case "exchangepattern":
        case "exchangePattern": return target.getExchangePattern();
        case "filenameextwhitelist":
        case "fileNameExtWhitelist": return target.getFileNameExtWhitelist();
        case "headerfilterstrategy":
        case "headerFilterStrategy": return target.getHeaderFilterStrategy();
        case "httpmethodrestrict":
        case "httpMethodRestrict": return target.getHttpMethodRestrict();
        case "matchonuriprefix":
        case "matchOnUriPrefix": return target.isMatchOnUriPrefix();
        case "muteexception":
        case "muteException": return target.isMuteException();
        case "platformhttpengine":
        case "platformHttpEngine": return target.getPlatformHttpEngine();
        case "produces": return target.getProduces();
        default: return null;
        }
    }
}

