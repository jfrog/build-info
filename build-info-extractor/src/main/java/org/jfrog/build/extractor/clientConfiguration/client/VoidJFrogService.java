package org.jfrog.build.extractor.clientConfiguration.client;

import org.jfrog.build.api.util.Log;

public abstract class VoidJFrogService extends JFrogService<Void> {
    public VoidJFrogService(Log logger) {
        super(Void.class, logger, JFrogServiceResponseType.EMPTY);
    }
}
