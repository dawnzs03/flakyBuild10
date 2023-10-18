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
package org.apache.camel.processor;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;

/**
 * A task that EIPs and internal routing engine uses to store state when processing an {@link Exchange}.
 *
 * @see org.apache.camel.processor.PooledExchangeTaskFactory
 */
public interface PooledExchangeTask extends Runnable {

    /**
     * Prepares the task for the given exchange and its callback
     *
     * @param exchange the exchange
     * @param callback the callback
     */
    void prepare(Exchange exchange, AsyncCallback callback);

    /**
     * Resets the task after its done and can be reused for another exchange.
     */
    void reset();
}
