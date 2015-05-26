package org.jfrog.build.launcher

import com.google.common.collect.Maps

/**
 * @author Eyal B
 */
class GradleLauncher extends Launcher{
    static final OS_WIN = System.getProperty("os.name").contains("Windows")

    protected Map<String, Object> projProp = Maps.newHashMap()

    public GradleLauncher(gradleCommandPath, gradleProjectFilePath) {
        super(gradleCommandPath, gradleProjectFilePath)
    }

    protected void createCmd() {
        buildToolVersionHandler().each {
            cmd.add(it)
            cmd.add("${gradleWrapperScript()} -v")
            cmd.add("$commandPath ${switchesToString()} ${projPropToString()} ${systemPropsToString()} " +
                    "-b $projectFilePath ${tasksToString()}"
            )
        }
    }

    @Override
    protected def buildToolVersionHandler() {
        def wrappers = []
        buildToolVersions.each {
            wrappers.add("${gradleScript()} wrapper --gradle-version $it")
        }
        wrappers
    }

    public Launcher addProjProp(String name, String value) {
        projProp.put(name, value)
        this
    }

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

    static def gradleWrapperScript(){
        (OS_WIN ? "gradlew.bat" : "./gradlew")
    }

    static def gradleScript(){
        (OS_WIN ? "gradle.bat" : "./gradle")
    }
}
