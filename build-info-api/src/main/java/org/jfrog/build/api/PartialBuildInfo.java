package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

import static org.jfrog.build.api.BuildBean.MODULES;

/**
 * Contains the partial build information along with the repositories it used to download dependencies,
 */
public class PartialBuildInfo implements Serializable {
    @XStreamAlias(MODULES)
    private List<Module> modules;
    private HashSet<String> ResolutionRepositories;

    private PartialBuildInfo() {

        ResolutionRepositories = new HashSet();
    }

    public List<Module> getModules() {
        return modules;
    }

    public HashSet<String> getResolutionRepositories() {
        return ResolutionRepositories;
    }

    public static PartialBuildInfo FromBuildInfo(Build build, HashSet<String> resolverRepositories) {
        PartialBuildInfo self = new PartialBuildInfo();
        self.modules = build.getModules();
        self.ResolutionRepositories = resolverRepositories;
        return self;
    }

}
