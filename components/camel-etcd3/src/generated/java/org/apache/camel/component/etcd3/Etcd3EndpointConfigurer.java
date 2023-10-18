/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.etcd3;

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
public class Etcd3EndpointConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        Etcd3Endpoint target = (Etcd3Endpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "authheaders":
        case "authHeaders": target.getConfiguration().setAuthHeaders(property(camelContext, java.util.Map.class, value)); return true;
        case "authority": target.getConfiguration().setAuthority(property(camelContext, java.lang.String.class, value)); return true;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": target.setBridgeErrorHandler(property(camelContext, boolean.class, value)); return true;
        case "connectiontimeout":
        case "connectionTimeout": target.getConfiguration().setConnectionTimeout(property(camelContext, java.time.Duration.class, value)); return true;
        case "endpoints": target.getConfiguration().setEndpoints(property(camelContext, java.lang.String[].class, value)); return true;
        case "exceptionhandler":
        case "exceptionHandler": target.setExceptionHandler(property(camelContext, org.apache.camel.spi.ExceptionHandler.class, value)); return true;
        case "exchangepattern":
        case "exchangePattern": target.setExchangePattern(property(camelContext, org.apache.camel.ExchangePattern.class, value)); return true;
        case "fromindex":
        case "fromIndex": target.getConfiguration().setFromIndex(property(camelContext, long.class, value)); return true;
        case "headers": target.getConfiguration().setHeaders(property(camelContext, java.util.Map.class, value)); return true;
        case "keepalivetime":
        case "keepAliveTime": target.getConfiguration().setKeepAliveTime(property(camelContext, java.time.Duration.class, value)); return true;
        case "keepalivetimeout":
        case "keepAliveTimeout": target.getConfiguration().setKeepAliveTimeout(property(camelContext, java.time.Duration.class, value)); return true;
        case "keycharset":
        case "keyCharset": target.getConfiguration().setKeyCharset(property(camelContext, java.lang.String.class, value)); return true;
        case "lazystartproducer":
        case "lazyStartProducer": target.setLazyStartProducer(property(camelContext, boolean.class, value)); return true;
        case "loadbalancerpolicy":
        case "loadBalancerPolicy": target.getConfiguration().setLoadBalancerPolicy(property(camelContext, java.lang.String.class, value)); return true;
        case "maxinboundmessagesize":
        case "maxInboundMessageSize": target.getConfiguration().setMaxInboundMessageSize(property(camelContext, java.lang.Integer.class, value)); return true;
        case "namespace": target.getConfiguration().setNamespace(property(camelContext, java.lang.String.class, value)); return true;
        case "password": target.getConfiguration().setPassword(property(camelContext, java.lang.String.class, value)); return true;
        case "prefix": target.getConfiguration().setPrefix(property(camelContext, boolean.class, value)); return true;
        case "retrydelay":
        case "retryDelay": target.getConfiguration().setRetryDelay(property(camelContext, long.class, value)); return true;
        case "retrymaxdelay":
        case "retryMaxDelay": target.getConfiguration().setRetryMaxDelay(property(camelContext, long.class, value)); return true;
        case "retrymaxduration":
        case "retryMaxDuration": target.getConfiguration().setRetryMaxDuration(property(camelContext, java.time.Duration.class, value)); return true;
        case "servicepath":
        case "servicePath": target.getConfiguration().setServicePath(property(camelContext, java.lang.String.class, value)); return true;
        case "sslcontext":
        case "sslContext": target.getConfiguration().setSslContext(property(camelContext, io.netty.handler.ssl.SslContext.class, value)); return true;
        case "username":
        case "userName": target.getConfiguration().setUserName(property(camelContext, java.lang.String.class, value)); return true;
        case "valuecharset":
        case "valueCharset": target.getConfiguration().setValueCharset(property(camelContext, java.lang.String.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "authheaders":
        case "authHeaders": return java.util.Map.class;
        case "authority": return java.lang.String.class;
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return boolean.class;
        case "connectiontimeout":
        case "connectionTimeout": return java.time.Duration.class;
        case "endpoints": return java.lang.String[].class;
        case "exceptionhandler":
        case "exceptionHandler": return org.apache.camel.spi.ExceptionHandler.class;
        case "exchangepattern":
        case "exchangePattern": return org.apache.camel.ExchangePattern.class;
        case "fromindex":
        case "fromIndex": return long.class;
        case "headers": return java.util.Map.class;
        case "keepalivetime":
        case "keepAliveTime": return java.time.Duration.class;
        case "keepalivetimeout":
        case "keepAliveTimeout": return java.time.Duration.class;
        case "keycharset":
        case "keyCharset": return java.lang.String.class;
        case "lazystartproducer":
        case "lazyStartProducer": return boolean.class;
        case "loadbalancerpolicy":
        case "loadBalancerPolicy": return java.lang.String.class;
        case "maxinboundmessagesize":
        case "maxInboundMessageSize": return java.lang.Integer.class;
        case "namespace": return java.lang.String.class;
        case "password": return java.lang.String.class;
        case "prefix": return boolean.class;
        case "retrydelay":
        case "retryDelay": return long.class;
        case "retrymaxdelay":
        case "retryMaxDelay": return long.class;
        case "retrymaxduration":
        case "retryMaxDuration": return java.time.Duration.class;
        case "servicepath":
        case "servicePath": return java.lang.String.class;
        case "sslcontext":
        case "sslContext": return io.netty.handler.ssl.SslContext.class;
        case "username":
        case "userName": return java.lang.String.class;
        case "valuecharset":
        case "valueCharset": return java.lang.String.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        Etcd3Endpoint target = (Etcd3Endpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "authheaders":
        case "authHeaders": return target.getConfiguration().getAuthHeaders();
        case "authority": return target.getConfiguration().getAuthority();
        case "bridgeerrorhandler":
        case "bridgeErrorHandler": return target.isBridgeErrorHandler();
        case "connectiontimeout":
        case "connectionTimeout": return target.getConfiguration().getConnectionTimeout();
        case "endpoints": return target.getConfiguration().getEndpoints();
        case "exceptionhandler":
        case "exceptionHandler": return target.getExceptionHandler();
        case "exchangepattern":
        case "exchangePattern": return target.getExchangePattern();
        case "fromindex":
        case "fromIndex": return target.getConfiguration().getFromIndex();
        case "headers": return target.getConfiguration().getHeaders();
        case "keepalivetime":
        case "keepAliveTime": return target.getConfiguration().getKeepAliveTime();
        case "keepalivetimeout":
        case "keepAliveTimeout": return target.getConfiguration().getKeepAliveTimeout();
        case "keycharset":
        case "keyCharset": return target.getConfiguration().getKeyCharset();
        case "lazystartproducer":
        case "lazyStartProducer": return target.isLazyStartProducer();
        case "loadbalancerpolicy":
        case "loadBalancerPolicy": return target.getConfiguration().getLoadBalancerPolicy();
        case "maxinboundmessagesize":
        case "maxInboundMessageSize": return target.getConfiguration().getMaxInboundMessageSize();
        case "namespace": return target.getConfiguration().getNamespace();
        case "password": return target.getConfiguration().getPassword();
        case "prefix": return target.getConfiguration().isPrefix();
        case "retrydelay":
        case "retryDelay": return target.getConfiguration().getRetryDelay();
        case "retrymaxdelay":
        case "retryMaxDelay": return target.getConfiguration().getRetryMaxDelay();
        case "retrymaxduration":
        case "retryMaxDuration": return target.getConfiguration().getRetryMaxDuration();
        case "servicepath":
        case "servicePath": return target.getConfiguration().getServicePath();
        case "sslcontext":
        case "sslContext": return target.getConfiguration().getSslContext();
        case "username":
        case "userName": return target.getConfiguration().getUserName();
        case "valuecharset":
        case "valueCharset": return target.getConfiguration().getValueCharset();
        default: return null;
        }
    }

    @Override
    public Object getCollectionValueType(Object target, String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "authheaders":
        case "authHeaders": return java.lang.String.class;
        case "headers": return java.lang.String.class;
        default: return null;
        }
    }
}

