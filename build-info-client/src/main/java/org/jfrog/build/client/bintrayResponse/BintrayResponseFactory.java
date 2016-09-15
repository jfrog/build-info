package org.jfrog.build.client.bintrayResponse;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * Class that handles BintrayResponse creation according to the status retrieved from http response.
 *
 * @author Aviad Shikloshi
 */
public class BintrayResponseFactory {

    /**
     * @param status code from HttpResponse upon which we are deciding the outcome of the request
     * @param parser JsonParser that is initialized with the HttpResponse body content as an InputStream
     * @return BintrayResponse object which can be printed in a readable way on the screen
     * if any Json call will due to IOException and we will not be able to retrieve the information
     * we will create an Response with only the status code.
     */
    public static BintrayResponse createResponse(int status, JsonParser parser) {
        BintrayResponse response;
        try {
            switch (status) {
                case (200):
                    response = createSuccessResponse(parser);
                    break;
                default:
                    response = createFailedResponse(parser);
            }
        } catch (IOException e) {
            response = new EmptyBintrayResponse(status);
        }
        return response;
    }

    private static BintrayResponse createSuccessResponse(JsonParser parser) throws IOException {
        BintraySuccess successResponse = parser.readValueAs(BintraySuccess.class);
        return successResponse;
    }

    private static BintrayResponse createFailedResponse(JsonParser parser) throws IOException {
        BintrayFailure failedResponse = parser.readValueAs(BintrayFailure.class);
        return failedResponse;
    }

}
