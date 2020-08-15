package org.jfrog.build.extractor.docker.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerLayers implements Serializable {
    Map<String, DockerLayer> digestToLayer = new HashMap<String, DockerLayer>();
    List<DockerLayer> layers = new ArrayList<DockerLayer>();

    public void addLayer(DockerLayer layer) {
        digestToLayer.put(layer.getDigest(), layer);
        layers.add(layer);
    }

    public DockerLayer getByDigest(String digest) {
        return digestToLayer.get(digest);
    }

    public List<DockerLayer> getLayers() {
        return layers;
    }
}