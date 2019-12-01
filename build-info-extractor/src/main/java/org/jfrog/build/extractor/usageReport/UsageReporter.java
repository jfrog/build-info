package org.jfrog.build.extractor.usageReport;

/**
 * Created by Bar Belity on 20/11/2019.
 */
public class UsageReporter {
    private String productId;
    private FeatureId[] features;

    public UsageReporter(String productId, String[] featureIds) {
        this.productId = productId;
        this.features = new FeatureId[featureIds.length];
        int i = 0;
        for (String featureId : featureIds) {
            features[i] = new FeatureId(featureId);
            i++;
        }
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public FeatureId[] getFeatures() {
        return features;
    }

    public void setFeatures(FeatureId[] featureId) {
        this.features = featureId;
    }

    public class FeatureId {
        private String featureId;

        public FeatureId (String featureId) {
            this.featureId = featureId;
        }

        public String getFeatureId() {
            return featureId;
        }

        public void setFeatureId(String featureId) {
            this.featureId = featureId;
        }
    }
}
