package io.micronaut.docs.inject.generics;

// tag::class[]
public class V6 implements CylinderProvider {
    @Override
    public int getCylinders() {
        return 6;
    }
}
// end::class[]
