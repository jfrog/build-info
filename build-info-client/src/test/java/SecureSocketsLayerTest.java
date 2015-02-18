import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jfrog.build.client.HttpClientConfigurator;
import org.testng.annotations.Test;

/**
 * @author Lior Hasson
 */

@Test
public class SecureSocketsLayerTest {

    private static String SNI_SITE_TESTER = "https://alice.sni.velox.ch/";

    @Test
    public void testSNI() throws Exception {

        HttpClientConfigurator clientConfig = new HttpClientConfigurator().host(SNI_SITE_TESTER);

        try {
            CloseableHttpClient client = clientConfig.getClient();
            HttpResponse response = client.execute(new HttpGet(SNI_SITE_TESTER));

            //Check site available
            if (response.getStatusLine().getStatusCode() != 200)
                return;

            String theString = IOUtils.toString(response.getEntity().getContent());

            //Unfortunately - the site recognize the client do not supports SNI
            if (theString.contains("Unfortunately"))
                Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
