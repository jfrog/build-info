package org.jfrog.build.extractor.clientConfiguration.client;

import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.io.InputStream;

public abstract class VoidJFrogService extends JFrogService<Void> {
    public VoidJFrogService(Log logger) {
        super(logger, JFrogServiceResponseType.EMPTY);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        throw new UnsupportedOperationException("The service '" + getClass().getSimpleName() + "' must override the setResponse method");
    }
}
