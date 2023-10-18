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
 * Send messages to AWS MQ.
 * 
 * Generated by camel build tools - do NOT edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface MQ2EndpointBuilderFactory {


    /**
     * Builder for endpoint for the AWS MQ component.
     */
    public interface MQ2EndpointBuilder extends EndpointProducerBuilder {
        default AdvancedMQ2EndpointBuilder advanced() {
            return (AdvancedMQ2EndpointBuilder) this;
        }
        /**
         * The operation to perform. It can be
         * listBrokers,createBroker,deleteBroker.
         * 
         * The option is a:
         * &lt;code&gt;org.apache.camel.component.aws2.mq.MQ2Operations&lt;/code&gt; type.
         * 
         * Required: true
         * Group: producer
         * 
         * @param operation the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder operation(
                org.apache.camel.component.aws2.mq.MQ2Operations operation) {
            doSetProperty("operation", operation);
            return this;
        }
        /**
         * The operation to perform. It can be
         * listBrokers,createBroker,deleteBroker.
         * 
         * The option will be converted to a
         * &lt;code&gt;org.apache.camel.component.aws2.mq.MQ2Operations&lt;/code&gt; type.
         * 
         * Required: true
         * Group: producer
         * 
         * @param operation the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder operation(String operation) {
            doSetProperty("operation", operation);
            return this;
        }
        /**
         * Set the need for overidding the endpoint. This option needs to be
         * used in combination with uriEndpointOverride option.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: producer
         * 
         * @param overrideEndpoint the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder overrideEndpoint(boolean overrideEndpoint) {
            doSetProperty("overrideEndpoint", overrideEndpoint);
            return this;
        }
        /**
         * Set the need for overidding the endpoint. This option needs to be
         * used in combination with uriEndpointOverride option.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: producer
         * 
         * @param overrideEndpoint the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder overrideEndpoint(String overrideEndpoint) {
            doSetProperty("overrideEndpoint", overrideEndpoint);
            return this;
        }
        /**
         * If we want to use a POJO request as body or not.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: producer
         * 
         * @param pojoRequest the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder pojoRequest(boolean pojoRequest) {
            doSetProperty("pojoRequest", pojoRequest);
            return this;
        }
        /**
         * If we want to use a POJO request as body or not.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: producer
         * 
         * @param pojoRequest the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder pojoRequest(String pojoRequest) {
            doSetProperty("pojoRequest", pojoRequest);
            return this;
        }
        /**
         * The region in which MQ client needs to work. When using this
         * parameter, the configuration will expect the lowercase name of the
         * region (for example ap-east-1) You'll need to use the name
         * Region.EU_WEST_1.id().
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: producer
         * 
         * @param region the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder region(String region) {
            doSetProperty("region", region);
            return this;
        }
        /**
         * Set the overriding uri endpoint. This option needs to be used in
         * combination with overrideEndpoint option.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: producer
         * 
         * @param uriEndpointOverride the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder uriEndpointOverride(
                String uriEndpointOverride) {
            doSetProperty("uriEndpointOverride", uriEndpointOverride);
            return this;
        }
        /**
         * To define a proxy host when instantiating the MQ client.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: proxy
         * 
         * @param proxyHost the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder proxyHost(String proxyHost) {
            doSetProperty("proxyHost", proxyHost);
            return this;
        }
        /**
         * To define a proxy port when instantiating the MQ client.
         * 
         * The option is a: &lt;code&gt;java.lang.Integer&lt;/code&gt; type.
         * 
         * Group: proxy
         * 
         * @param proxyPort the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder proxyPort(Integer proxyPort) {
            doSetProperty("proxyPort", proxyPort);
            return this;
        }
        /**
         * To define a proxy port when instantiating the MQ client.
         * 
         * The option will be converted to a
         * &lt;code&gt;java.lang.Integer&lt;/code&gt; type.
         * 
         * Group: proxy
         * 
         * @param proxyPort the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder proxyPort(String proxyPort) {
            doSetProperty("proxyPort", proxyPort);
            return this;
        }
        /**
         * To define a proxy protocol when instantiating the MQ client.
         * 
         * The option is a:
         * &lt;code&gt;software.amazon.awssdk.core.Protocol&lt;/code&gt; type.
         * 
         * Default: HTTPS
         * Group: proxy
         * 
         * @param proxyProtocol the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder proxyProtocol(
                software.amazon.awssdk.core.Protocol proxyProtocol) {
            doSetProperty("proxyProtocol", proxyProtocol);
            return this;
        }
        /**
         * To define a proxy protocol when instantiating the MQ client.
         * 
         * The option will be converted to a
         * &lt;code&gt;software.amazon.awssdk.core.Protocol&lt;/code&gt; type.
         * 
         * Default: HTTPS
         * Group: proxy
         * 
         * @param proxyProtocol the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder proxyProtocol(String proxyProtocol) {
            doSetProperty("proxyProtocol", proxyProtocol);
            return this;
        }
        /**
         * Amazon AWS Access Key.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: security
         * 
         * @param accessKey the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder accessKey(String accessKey) {
            doSetProperty("accessKey", accessKey);
            return this;
        }
        /**
         * If using a profile credentials provider this parameter will set the
         * profile name.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: security
         * 
         * @param profileCredentialsName the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder profileCredentialsName(
                String profileCredentialsName) {
            doSetProperty("profileCredentialsName", profileCredentialsName);
            return this;
        }
        /**
         * Amazon AWS Secret Key.
         * 
         * The option is a: &lt;code&gt;java.lang.String&lt;/code&gt; type.
         * 
         * Group: security
         * 
         * @param secretKey the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder secretKey(String secretKey) {
            doSetProperty("secretKey", secretKey);
            return this;
        }
        /**
         * If we want to trust all certificates in case of overriding the
         * endpoint.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: security
         * 
         * @param trustAllCertificates the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder trustAllCertificates(
                boolean trustAllCertificates) {
            doSetProperty("trustAllCertificates", trustAllCertificates);
            return this;
        }
        /**
         * If we want to trust all certificates in case of overriding the
         * endpoint.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: security
         * 
         * @param trustAllCertificates the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder trustAllCertificates(
                String trustAllCertificates) {
            doSetProperty("trustAllCertificates", trustAllCertificates);
            return this;
        }
        /**
         * Set whether the MQ client should expect to load credentials through a
         * default credentials provider or to expect static credentials to be
         * passed in.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: security
         * 
         * @param useDefaultCredentialsProvider the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder useDefaultCredentialsProvider(
                boolean useDefaultCredentialsProvider) {
            doSetProperty("useDefaultCredentialsProvider", useDefaultCredentialsProvider);
            return this;
        }
        /**
         * Set whether the MQ client should expect to load credentials through a
         * default credentials provider or to expect static credentials to be
         * passed in.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: security
         * 
         * @param useDefaultCredentialsProvider the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder useDefaultCredentialsProvider(
                String useDefaultCredentialsProvider) {
            doSetProperty("useDefaultCredentialsProvider", useDefaultCredentialsProvider);
            return this;
        }
        /**
         * Set whether the MQ client should expect to load credentials through a
         * profile credentials provider.
         * 
         * The option is a: &lt;code&gt;boolean&lt;/code&gt; type.
         * 
         * Default: false
         * Group: security
         * 
         * @param useProfileCredentialsProvider the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder useProfileCredentialsProvider(
                boolean useProfileCredentialsProvider) {
            doSetProperty("useProfileCredentialsProvider", useProfileCredentialsProvider);
            return this;
        }
        /**
         * Set whether the MQ client should expect to load credentials through a
         * profile credentials provider.
         * 
         * The option will be converted to a &lt;code&gt;boolean&lt;/code&gt;
         * type.
         * 
         * Default: false
         * Group: security
         * 
         * @param useProfileCredentialsProvider the value to set
         * @return the dsl builder
         */
        default MQ2EndpointBuilder useProfileCredentialsProvider(
                String useProfileCredentialsProvider) {
            doSetProperty("useProfileCredentialsProvider", useProfileCredentialsProvider);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the AWS MQ component.
     */
    public interface AdvancedMQ2EndpointBuilder
            extends
                EndpointProducerBuilder {
        default MQ2EndpointBuilder basic() {
            return (MQ2EndpointBuilder) this;
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
        default AdvancedMQ2EndpointBuilder lazyStartProducer(
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
        default AdvancedMQ2EndpointBuilder lazyStartProducer(
                String lazyStartProducer) {
            doSetProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
        /**
         * To use a existing configured AmazonMQClient as client.
         * 
         * The option is a:
         * &lt;code&gt;software.amazon.awssdk.services.mq.MqClient&lt;/code&gt;
         * type.
         * 
         * Group: advanced
         * 
         * @param amazonMqClient the value to set
         * @return the dsl builder
         */
        default AdvancedMQ2EndpointBuilder amazonMqClient(
                software.amazon.awssdk.services.mq.MqClient amazonMqClient) {
            doSetProperty("amazonMqClient", amazonMqClient);
            return this;
        }
        /**
         * To use a existing configured AmazonMQClient as client.
         * 
         * The option will be converted to a
         * &lt;code&gt;software.amazon.awssdk.services.mq.MqClient&lt;/code&gt;
         * type.
         * 
         * Group: advanced
         * 
         * @param amazonMqClient the value to set
         * @return the dsl builder
         */
        default AdvancedMQ2EndpointBuilder amazonMqClient(String amazonMqClient) {
            doSetProperty("amazonMqClient", amazonMqClient);
            return this;
        }
    }

    public interface MQ2Builders {
        /**
         * AWS MQ (camel-aws2-mq)
         * Send messages to AWS MQ.
         * 
         * Category: cloud,messaging
         * Since: 3.1
         * Maven coordinates: org.apache.camel:camel-aws2-mq
         * 
         * @return the dsl builder for the headers' name.
         */
        default MQ2HeaderNameBuilder aws2Mq() {
            return MQ2HeaderNameBuilder.INSTANCE;
        }
        /**
         * AWS MQ (camel-aws2-mq)
         * Send messages to AWS MQ.
         * 
         * Category: cloud,messaging
         * Since: 3.1
         * Maven coordinates: org.apache.camel:camel-aws2-mq
         * 
         * Syntax: <code>aws2-mq:label</code>
         * 
         * Path parameter: label (required)
         * Logical name
         * 
         * @param path label
         * @return the dsl builder
         */
        default MQ2EndpointBuilder aws2Mq(String path) {
            return MQ2EndpointBuilderFactory.endpointBuilder("aws2-mq", path);
        }
        /**
         * AWS MQ (camel-aws2-mq)
         * Send messages to AWS MQ.
         * 
         * Category: cloud,messaging
         * Since: 3.1
         * Maven coordinates: org.apache.camel:camel-aws2-mq
         * 
         * Syntax: <code>aws2-mq:label</code>
         * 
         * Path parameter: label (required)
         * Logical name
         * 
         * @param componentName to use a custom component name for the endpoint
         * instead of the default name
         * @param path label
         * @return the dsl builder
         */
        default MQ2EndpointBuilder aws2Mq(String componentName, String path) {
            return MQ2EndpointBuilderFactory.endpointBuilder(componentName, path);
        }
    }

    /**
     * The builder of headers' name for the AWS MQ component.
     */
    public static class MQ2HeaderNameBuilder {
        /**
         * The internal instance of the builder used to access to all the
         * methods representing the name of headers.
         */
        private static final MQ2HeaderNameBuilder INSTANCE = new MQ2HeaderNameBuilder();

        /**
         * The operation we want to perform.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQOperation}.
         */
        public String awsMQOperation() {
            return "AwsMQOperation";
        }

        /**
         * The number of results that must be retrieved from listBrokers
         * operation.
         * 
         * The option is a: {@code Integer} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQMaxResults}.
         */
        public String awsMQMaxResults() {
            return "AwsMQMaxResults";
        }

        /**
         * The broker name.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerName}.
         */
        public String awsMQBrokerName() {
            return "AwsMQBrokerName";
        }

        /**
         * The Broker Engine for MQ.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerEngine}.
         */
        public String awsMQBrokerEngine() {
            return "AwsMQBrokerEngine";
        }

        /**
         * The Broker Engine Version for MQ. Currently you can choose between
         * 5.15.6 and 5.15.0 of ACTIVEMQ.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerEngineVersion}.
         */
        public String awsMQBrokerEngineVersion() {
            return "AwsMQBrokerEngineVersion";
        }

        /**
         * The broker id.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerID}.
         */
        public String awsMQBrokerID() {
            return "AwsMQBrokerID";
        }

        /**
         * A list of information about the configuration.
         * 
         * The option is a: {@code
         * software.amazon.awssdk.services.mq.model.ConfigurationId} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQConfigurationID}.
         */
        public String awsMQConfigurationID() {
            return "AwsMQConfigurationID";
        }

        /**
         * The deployment mode for the broker in the createBroker operation.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerDeploymentMode}.
         */
        public String awsMQBrokerDeploymentMode() {
            return "AwsMQBrokerDeploymentMode";
        }

        /**
         * The instance type for the MQ machine in the createBroker operation.
         * 
         * The option is a: {@code String} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerInstanceType}.
         */
        public String awsMQBrokerInstanceType() {
            return "AwsMQBrokerInstanceType";
        }

        /**
         * The list of users for MQ.
         * 
         * The option is a: {@code List<User>} type.
         * 
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerUsers}.
         */
        public String awsMQBrokerUsers() {
            return "AwsMQBrokerUsers";
        }

        /**
         * If the MQ instance must be publicly available or not.
         * 
         * The option is a: {@code Boolean} type.
         * 
         * Default: false
         * Group: producer
         * 
         * @return the name of the header {@code AwsMQBrokerPubliclyAccessible}.
         */
        public String awsMQBrokerPubliclyAccessible() {
            return "AwsMQBrokerPubliclyAccessible";
        }
    }
    static MQ2EndpointBuilder endpointBuilder(String componentName, String path) {
        class MQ2EndpointBuilderImpl extends AbstractEndpointBuilder implements MQ2EndpointBuilder, AdvancedMQ2EndpointBuilder {
            public MQ2EndpointBuilderImpl(String path) {
                super(componentName, path);
            }
        }
        return new MQ2EndpointBuilderImpl(path);
    }
}