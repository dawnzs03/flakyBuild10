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
package org.apache.camel.component.sjms.jms;

public final class DestinationNameParser {

    private DestinationNameParser() {
    }

    public static boolean isTopic(String destinationName) {
        if (destinationName == null) {
            throw new IllegalArgumentException("destinationName is null");
        }
        return destinationName.startsWith("topic:");
    }

    public static boolean isReplyToTopic(String replyTo, boolean isDestinationTopic) {
        if (replyTo == null) {
            throw new IllegalArgumentException("replyTo is null");
        }
        if (replyTo.startsWith("topic:")) {
            return true;
        } else if (replyTo.startsWith("queue:")) {
            return false;
        } else {
            return isDestinationTopic;
        }
    }

    public static String getShortName(String destinationName) {
        if (destinationName == null) {
            throw new IllegalArgumentException("destinationName is null");
        }
        if (destinationName.startsWith("queue:")) {
            return destinationName.substring(6);
        } else if (destinationName.startsWith("topic:")) {
            return destinationName.substring(6);
        } else {
            return destinationName;
        }
    }
}
