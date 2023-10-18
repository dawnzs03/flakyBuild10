/*
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
package org.apache.camel.builder.endpoint.dsl;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;
import javax.annotation.processing.Generated;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.AbstractEndpointBuilder;

/**
 * The Dynamic Router component routes exchanges to recipients, and the
 * recipients (and their rules) may change at runtime.
 * 
 * Generated by camel build tools - do NOT edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface DynamicRouterEndpointBuilderFactory {


    /**
     * Builder for endpoint for the Dynamic Router component.
     */
    public interface DynamicRouterEndpointBuilder
            extends
                EndpointProducerBuilder {
        default AdvancedDynamicRouterEndpointBuilder advanced() {
            return (AdvancedDynamicRouterEndpointBuilder) this;
        }
        /**
         * Recipient mode: firstMatch or allMatch.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Default: firstMatch
         * Group: common
         * 
         * @param recipientMode the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder recipientMode(String recipientMode) {
            doSetProperty("recipientMode", recipientMode);
            return this;
        }
        /**
         * Flag to ensure synchronous processing.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: common
         * 
         * @param synchronous the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder synchronous(boolean synchronous) {
            doSetProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Flag to ensure synchronous processing.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: common
         * 
         * @param synchronous the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder synchronous(String synchronous) {
            doSetProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Flag to log a warning if no predicates match for an exchange.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: common
         * 
         * @param warnDroppedMessage the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder warnDroppedMessage(
                boolean warnDroppedMessage) {
            doSetProperty("warnDroppedMessage", warnDroppedMessage);
            return this;
        }
        /**
         * Flag to log a warning if no predicates match for an exchange.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: common
         * 
         * @param warnDroppedMessage the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder warnDroppedMessage(
                String warnDroppedMessage) {
            doSetProperty("warnDroppedMessage", warnDroppedMessage);
            return this;
        }
        /**
         * The destination URI for exchanges that match.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param destinationUri the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder destinationUri(
                String destinationUri) {
            doSetProperty("destinationUri", destinationUri);
            return this;
        }
        /**
         * The subscription predicate language.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Default: simple
         * Group: control
         * 
         * @param expressionLanguage the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder expressionLanguage(
                String expressionLanguage) {
            doSetProperty("expressionLanguage", expressionLanguage);
            return this;
        }
        /**
         * The subscription predicate.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param predicate the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder predicate(String predicate) {
            doSetProperty("predicate", predicate);
            return this;
        }
        /**
         * A Predicate instance in the registry.
         * 
         * The option is a: &lt;code&gt;org.apache.camel.Predicate&lt;/code&gt;
         * type.
         * 
         * Group: control
         * 
         * @param predicateBean the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder predicateBean(
                org.apache.camel.Predicate predicateBean) {
            doSetProperty("predicateBean", predicateBean);
            return this;
        }
        /**
         * A Predicate instance in the registry.
         * 
         * The option will be converted to a
         * &lt;code&gt;org.apache.camel.Predicate&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param predicateBean the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder predicateBean(String predicateBean) {
            doSetProperty("predicateBean", predicateBean);
            return this;
        }
        /**
         * The subscription priority.
         * 
         * The option is a: &lt;code&gt;java.lang.Integer&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param priority the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder priority(Integer priority) {
            doSetProperty("priority", priority);
            return this;
        }
        /**
         * The subscription priority.
         * 
         * The option will be converted to a
         * &lt;code&gt;java.lang.Integer&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param priority the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder priority(String priority) {
            doSetProperty("priority", priority);
            return this;
        }
        /**
         * The subscription ID; if unspecified, one will be assigned and
         * returned.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: control
         * 
         * @param subscriptionId the value to set
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder subscriptionId(
                String subscriptionId) {
            doSetProperty("subscriptionId", subscriptionId);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the Dynamic Router component.
     */
    public interface AdvancedDynamicRouterEndpointBuilder
            extends
                EndpointProducerBuilder {
        default DynamicRouterEndpointBuilder basic() {
            return (DynamicRouterEndpointBuilder) this;
        }
        /**
         * Whether the producer should be started lazy (on the first message).
         * By starting lazy you can use this to allow CamelContext and routes to
         * startup in situations where a producer may otherwise fail during
         * starting and cause the route to fail being started. By deferring this
         * startup to be lazy then the startup failure can be handled during
         * routing messages via Camel's routing error handlers. Beware that when
         * the first message is processed then creating and starting the
         * producer may take a little time and prolong the total processing time
         * of the processing.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: producer (advanced)
         * 
         * @param lazyStartProducer the value to set
         * @return the dsl builder
         */
        default AdvancedDynamicRouterEndpointBuilder lazyStartProducer(
                boolean lazyStartProducer) {
            doSetProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
        /**
         * Whether the producer should be started lazy (on the first message).
         * By starting lazy you can use this to allow CamelContext and routes to
         * startup in situations where a producer may otherwise fail during
         * starting and cause the route to fail being started. By deferring this
         * startup to be lazy then the startup failure can be handled during
         * routing messages via Camel's routing error handlers. Beware that when
         * the first message is processed then creating and starting the
         * producer may take a little time and prolong the total processing time
         * of the processing.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: producer (advanced)
         * 
         * @param lazyStartProducer the value to set
         * @return the dsl builder
         */
        default AdvancedDynamicRouterEndpointBuilder lazyStartProducer(
                String lazyStartProducer) {
            doSetProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
    }

    public interface DynamicRouterBuilders {
        /**
         * Dynamic Router (camel-dynamic-router)
         * The Dynamic Router component routes exchanges to recipients, and the
         * recipients (and their rules) may change at runtime.
         * 
         * Category: messaging,core
         * Since: 3.15
         * Maven coordinates: org.apache.camel:camel-dynamic-router
         * 
         * Syntax: <code>dynamic-router:channel</code>
         * 
         * Path parameter: channel (required)
         * Channel of the Dynamic Router
         * 
         * Path parameter: controlAction
         * Control channel action: subscribe or unsubscribe
         * There are 2 enums and the value can be one of: subscribe, unsubscribe
         * 
         * Path parameter: subscribeChannel
         * The channel to subscribe to
         * 
         * @param path channel
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder dynamicRouter(String path) {
            return DynamicRouterEndpointBuilderFactory.endpointBuilder("dynamic-router", path);
        }
        /**
         * Dynamic Router (camel-dynamic-router)
         * The Dynamic Router component routes exchanges to recipients, and the
         * recipients (and their rules) may change at runtime.
         * 
         * Category: messaging,core
         * Since: 3.15
         * Maven coordinates: org.apache.camel:camel-dynamic-router
         * 
         * Syntax: <code>dynamic-router:channel</code>
         * 
         * Path parameter: channel (required)
         * Channel of the Dynamic Router
         * 
         * Path parameter: controlAction
         * Control channel action: subscribe or unsubscribe
         * There are 2 enums and the value can be one of: subscribe, unsubscribe
         * 
         * Path parameter: subscribeChannel
         * The channel to subscribe to
         * 
         * @param componentName to use a custom component name for the endpoint
         * instead of the default name
         * @param path channel
         * @return the dsl builder
         */
        default DynamicRouterEndpointBuilder dynamicRouter(
                String componentName,
                String path) {
            return DynamicRouterEndpointBuilderFactory.endpointBuilder(componentName, path);
        }
    }
    static DynamicRouterEndpointBuilder endpointBuilder(
            String componentName,
            String path) {
        class DynamicRouterEndpointBuilderImpl extends AbstractEndpointBuilder implements DynamicRouterEndpointBuilder, AdvancedDynamicRouterEndpointBuilder {
            public DynamicRouterEndpointBuilderImpl(String path) {
                super(componentName, path);
            }
        }
        return new DynamicRouterEndpointBuilderImpl(path);
    }
}