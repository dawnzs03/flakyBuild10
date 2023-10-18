/*
 * Camel ApiCollection generated by camel-api-component-maven-plugin
 */
package org.apache.camel.component.dhis2.internal;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.component.dhis2.Dhis2Configuration;
import org.apache.camel.component.dhis2.Dhis2PostEndpointConfiguration;
import org.apache.camel.component.dhis2.Dhis2ResourceTablesEndpointConfiguration;
import org.apache.camel.component.dhis2.Dhis2GetEndpointConfiguration;
import org.apache.camel.component.dhis2.Dhis2DeleteEndpointConfiguration;
import org.apache.camel.component.dhis2.Dhis2PutEndpointConfiguration;

import org.apache.camel.support.component.ApiCollection;
import org.apache.camel.support.component.ApiMethod;
import org.apache.camel.support.component.ApiMethodHelper;

/**
 * Camel {@link ApiCollection} for Dhis2
 */
public final class Dhis2ApiCollection extends ApiCollection<Dhis2ApiName, Dhis2Configuration> {

    private static Dhis2ApiCollection collection;

    private Dhis2ApiCollection() {
        final Map<String, String> aliases = new HashMap<>();
        final Map<Dhis2ApiName, ApiMethodHelper<? extends ApiMethod>> apiHelpers = new EnumMap<>(Dhis2ApiName.class);
        final Map<Class<? extends ApiMethod>, Dhis2ApiName> apiMethods = new HashMap<>();

        List<String> nullableArgs;

        aliases.clear();
        nullableArgs = Arrays.asList("resource", "queryParams");
        apiHelpers.put(Dhis2ApiName.POST, new ApiMethodHelper<>(Dhis2PostApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Dhis2PostApiMethod.class, Dhis2ApiName.POST);

        aliases.clear();
        nullableArgs = Arrays.asList("skipEvents", "skipAggregate", "lastYears", "interval");
        apiHelpers.put(Dhis2ApiName.RESOURCE_TABLES, new ApiMethodHelper<>(Dhis2ResourceTablesApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Dhis2ResourceTablesApiMethod.class, Dhis2ApiName.RESOURCE_TABLES);

        aliases.clear();
        nullableArgs = Arrays.asList("paging", "fields", "filter", "rootJunction", "queryParams");
        apiHelpers.put(Dhis2ApiName.GET, new ApiMethodHelper<>(Dhis2GetApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Dhis2GetApiMethod.class, Dhis2ApiName.GET);

        aliases.clear();
        nullableArgs = Arrays.asList("resource", "queryParams");
        apiHelpers.put(Dhis2ApiName.DELETE, new ApiMethodHelper<>(Dhis2DeleteApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Dhis2DeleteApiMethod.class, Dhis2ApiName.DELETE);

        aliases.clear();
        nullableArgs = Arrays.asList("resource", "queryParams");
        apiHelpers.put(Dhis2ApiName.PUT, new ApiMethodHelper<>(Dhis2PutApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Dhis2PutApiMethod.class, Dhis2ApiName.PUT);

        setApiHelpers(apiHelpers);
        setApiMethods(apiMethods);
    }

    public Dhis2Configuration getEndpointConfiguration(Dhis2ApiName apiName) {
        Dhis2Configuration result = null;
        switch (apiName) {
            case POST:
                result = new Dhis2PostEndpointConfiguration();
                break;
            case RESOURCE_TABLES:
                result = new Dhis2ResourceTablesEndpointConfiguration();
                break;
            case GET:
                result = new Dhis2GetEndpointConfiguration();
                break;
            case DELETE:
                result = new Dhis2DeleteEndpointConfiguration();
                break;
            case PUT:
                result = new Dhis2PutEndpointConfiguration();
                break;
        }
        return result;
    }

    public static synchronized Dhis2ApiCollection getCollection() {
        if (collection == null) {
            collection = new Dhis2ApiCollection();
        }
        return collection;
    }
}
