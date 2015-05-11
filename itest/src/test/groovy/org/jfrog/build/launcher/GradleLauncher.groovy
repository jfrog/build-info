package org.jfrog.build.launcher

import com.google.common.collect.Maps
import com.google.common.collect.Sets

/**
 * @author Eyal B
 */
class GradleLauncher extends Launcher{
    protected Map<String, Object> projProp = Maps.newHashMap()

    public GradleLauncher(gradleCommandPath, gradleProjectFilePath) {
        super(gradleCommandPath, gradleProjectFilePath)
    }

    public Launcher addProjProp(String name, String value) {
        projProp.put(name, value)
        this
    }

    /*    protected def envVarsToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(var in envVars) {
            def key = var.key.startsWith("-P") ? var.key : "-P${var.key}"
            sb.append(key).append("=").append(var.value)
            if (c++ < envVars.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }*/

    private def projPropToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(var in projProp) {
            def key = var.key.startsWith("-P") ? var.key : "-P${var.key}"
            sb.append(key).append("=").append(var.value)
            if (c++ < projProp.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    protected void createCmd() {
        cmd = "$commandPath ${switchesToString()} ${projPropToString()} ${systemPropsToString()} " +
                "-b $projectFilePath ${tasksToString()}"
    }
}
