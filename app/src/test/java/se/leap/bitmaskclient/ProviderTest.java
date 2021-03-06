package se.leap.bitmaskclient;

import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import se.leap.bitmaskclient.testutils.TestSetupHelper;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by cyberta on 12.02.18.
 */
public class ProviderTest {

    @Test
    public void testEquals_sameFields_returnsTrue() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        assertTrue("Providers should be same:", p1.equals(p2));
    }

    @Test
    public void testEquals_sameFields_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        p2.setMainUrl("http://somethingsdiffer.org");
        assertFalse("Providers should be same:", p1.equals(p2));
    }

    // see ProviderManagerTest testing add(...)
    @Test
    public void testEqualsThroughSetContains_differentFields_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        p2.setMainUrl("http://somethingsdiffer.org");
        Provider p3 = new Provider("https://anotherprovider.net");

        Set<Provider> defaultProviders = new HashSet<>();
        defaultProviders.add(p1);
        defaultProviders.add(p2);

        assertTrue(defaultProviders.contains(p1));
        assertTrue(defaultProviders.contains(p2));
        assertFalse(defaultProviders.contains(p3));
    }
}
