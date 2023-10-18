/*
 * Camel ApiName Enumeration generated by camel-api-component-maven-plugin
 */
package org.apache.camel.component.as2.internal;

import org.apache.camel.support.component.ApiName;

/**
 * Camel {@link ApiName} Enumeration for Component AS2
 */
public enum AS2ApiName implements ApiName {

    CLIENT("client"),

    SERVER("server");


    private final String name;

    private AS2ApiName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
