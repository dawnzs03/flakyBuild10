package com.dabsquared.gitlabjenkins.gitlab.api;

import static java.util.Collections.sort;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionPoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public abstract class GitLabClientBuilder implements Comparable<GitLabClientBuilder>, ExtensionPoint, Serializable {
    public static GitLabClientBuilder getGitLabClientBuilderById(String id) {
        for (GitLabClientBuilder provider : getAllGitLabClientBuilders()) {
            if (provider.id().equals(id)) {
                return provider;
            }
        }

        throw new NoSuchElementException("unknown client-builder-id: " + id);
    }

    public static List<GitLabClientBuilder> getAllGitLabClientBuilders() {
        List<GitLabClientBuilder> builders =
                new ArrayList<>(Jenkins.getInstance().getExtensionList(GitLabClientBuilder.class));
        sort(builders);
        return builders;
    }

    private final String id;
    private final int ordinal;

    protected GitLabClientBuilder(String id, int ordinal) {
        this.id = id;
        this.ordinal = ordinal;
    }

    @NonNull
    public final String id() {
        return id;
    }

    @NonNull
    public abstract GitLabClient buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout);

    @Override
    public final int compareTo(@NonNull GitLabClientBuilder other) {
        int o = ordinal - other.ordinal;
        return o != 0 ? o : id().compareTo(other.id());
    }
}
