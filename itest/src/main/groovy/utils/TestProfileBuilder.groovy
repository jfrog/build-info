package utils

import com.google.common.collect.Lists
import org.jfrog.artifactory.client.Artifactory

/**
 * @author Lior Hasson  
 */
class TestProfileBuilder {
    private List<TestProfile> testProfiles = new ArrayList<TestProfile>();
    private def config
    private File sourceFile
    private def splitByProperty = []
    private List<ConfigObject> confList = new ArrayList<ConfigObject>()
    protected Artifactory artifactory

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
        //Read the config file and creates the first ConfigObject (father); from that object we will
        //fork other object according to the split properties
        confList.add(new ConfigSlurper().parse(sourceFile.toURI().toURL()))
        profileMultiplicity()

        confList.each { configObject ->
            def testSetup = new TestProfile(configObject, artifactory)
            testSetup.initSpecialProperties(config)
            testSetup.createBuildLauncher()
            testProfiles << testSetup
        }

        testProfiles
    }


    private void profileMultiplicity() {
        splitByProperty.each { prop ->
            def tmp = []
            confList.each { testProfile ->
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
            confList = tmp
        }
    }


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
