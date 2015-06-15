package utils

import com.google.common.collect.Lists
import org.jfrog.artifactory.client.Artifactory

/**
 * @author Lior Hasson  
 */
class TestProfileBuilder {
    private List<TestProfile> testProfiles = new ArrayList<TestProfile>();
    private List<ConfigObject> confList = new ArrayList<ConfigObject>()
    private File sourceFile
    private Artifactory artifactory
    private def splitByProperty = []
    private def config

    TestProfileBuilder(config, file, artifactory) {
        this.config = config
        this.sourceFile = file
        this.artifactory = artifactory
    }

    TestProfileBuilder splitByProperty(splitByProperty) {
        this.splitByProperty.add(splitByProperty)
        this
    }

    public List<TestProfile> build(){
        //Read the config file and creates the first ConfigObject (father).
        //From that object we will fork other object according to the split properties
        confList.add(new ConfigSlurper().parse(sourceFile.toURI().toURL()))
        profileMultiplicity()

        confList.each { configObject ->
            TestProfile testSetup = new TestProfile(configObject, artifactory, config)
            testSetup.initSpecialProperties()
            testSetup.createBuildLauncher()
            testProfiles << testSetup
        }

        testProfiles
    }

    private void profileMultiplicity() {
        splitByProperty.each { prop ->
            def tmp = []
            confList.each { testProfile ->
                if (testProfile.flatten().get(prop)) {
                    testProfile.flatten().get(prop).each { val ->
                        ConfigObject clone = deepCopy(testProfile)
                        def node = clone
                        def leaf = clone
                        def leafProp
                        prop.split('\\.').each {
                            node = leaf
                            leaf = node.get(it)
                            leafProp = it
                        }
                        node.put(leafProp, "$val")
                        tmp << clone
                    }
                }
                else {
                    tmp << testProfile
                }
            }
            confList = tmp
        }
    }

    /**
     * Deep copy for {@link ConfigObject}
     * @param orig - instance to copy
     * @return
     */
    private def deepCopy(ConfigObject orig) {
        ConfigObject copy = new ConfigObject()
        orig.keySet().each { key ->
            def value = orig.get(key)
            if (value instanceof ConfigObject) {
                value = deepCopy(value)
            }
            if(value instanceof ArrayList){
                def list = Lists.newArrayList()
                list.addAll(value)
                copy.put(key, list)
            }
            else{
                copy.put(key, value)
            }
        }
        return copy
    }
}
