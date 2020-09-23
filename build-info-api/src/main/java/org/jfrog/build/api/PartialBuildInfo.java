package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.HashSet;
import java.util.List;

/**
 * Contains the partial build information along with the repositories it used to download dependencies,
 */
public class PartialBuildInfo extends BaseBuildBean {
    @XStreamAlias(MODULES)
    private List<Module> modules;
    private HashSet<String> ResolutionRepositories;
    private List<Vcs> vcs;

    private PartialBuildInfo() {

        ResolutionRepositories = new HashSet();
    }

    public static PartialBuildInfo FromBuildInfo(Build build, HashSet<String> resolverRepositories) {
        PartialBuildInfo self = new PartialBuildInfo();
        self.setModules(build.getModules());
        self.setResolutionRepositories(resolverRepositories);
        self.setProperties(build.getProperties());
        self.setVcs(build.getVcs());
        return self;
    }

    public HashSet<String> getResolutionRepositories() {
        return ResolutionRepositories;
    }

    public void setResolutionRepositories(HashSet<String> repositories) {
        this.ResolutionRepositories = repositories;
    }

    public List<Vcs> getVcs() {
        return vcs;
    }

    public void setVcs(List<Vcs> vcs) {
        this.vcs = vcs;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

}
