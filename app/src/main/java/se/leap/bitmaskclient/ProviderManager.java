package se.leap.bitmaskclient;

import android.content.res.AssetManager;

import com.pedrogomez.renderers.AdapteeCollection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderManager implements AdapteeCollection<Provider> {

    private AssetManager assetsManager;
    private File externalFilesDir;
    private Set<Provider> defaultProviders;
    private Set<Provider> customProviders;
    private Set<URL> defaultProviderURLs;
    private Set<URL> customProviderURLs;

    private static ProviderManager instance;

    final private static String URLS = "urls";

    public static ProviderManager getInstance(AssetManager assetsManager, File externalFilesDir) {
        if (instance == null)
            instance = new ProviderManager(assetsManager, externalFilesDir);

        return instance;
    }

    private ProviderManager(AssetManager assetManager, File externalFilesDir) {
        this.assetsManager = assetManager;
        addDefaultProviders(assetManager);
        addCustomProviders(externalFilesDir);
    }

    private void addDefaultProviders(AssetManager assets_manager) {
        try {
            defaultProviders = providersFromAssets(URLS, assets_manager.list(URLS));
            defaultProviderURLs = getProviderUrlSetFromProviderSet(defaultProviders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<URL> getProviderUrlSetFromProviderSet(Set<Provider> providers) {
        HashSet<URL> providerUrls = new HashSet<>();
        for (Provider provider : providers) {
            providerUrls.add(provider.getMainUrl().getUrl());
        }
        return providerUrls;
    }

    private Set<Provider> providersFromAssets(String directory, String[] relativeFilePaths) {
        Set<Provider> providers = new HashSet<>();

            for (String file : relativeFilePaths) {
                String mainUrl = null;
                String certificate = null;
                String providerDefinition = null;
                try {
                    String provider = file.substring(0, file.length() - ".url".length());
                    InputStream provider_file = assetsManager.open(directory + "/" + file);
                    mainUrl = extractMainUrlFromInputStream(provider_file);
                    certificate = ConfigHelper.loadInputStreamAsString(assetsManager.open(provider + ".pem"));
                    providerDefinition = ConfigHelper.loadInputStreamAsString(assetsManager.open(provider + ".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    providers.add(new Provider(new URL(mainUrl), certificate, providerDefinition));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

        return providers;
    }


    private void addCustomProviders(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
        customProviders = externalFilesDir != null && externalFilesDir.isDirectory() ?
                providersFromFiles(externalFilesDir.list()) :
                new HashSet<Provider>();
        customProviderURLs = getProviderUrlSetFromProviderSet(customProviders);
    }

    private Set<Provider> providersFromFiles(String[] files) {
        Set<Provider> providers = new HashSet<>();
        try {
            for (String file : files) {
                String mainUrl = extractMainUrlFromInputStream(ConfigHelper.getInputStreamFrom(externalFilesDir.getAbsolutePath() + "/" + file));
                providers.add(new Provider(new URL(mainUrl)));
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return providers;
    }

    private String extractMainUrlFromInputStream(InputStream inputStream) {
        String mainUrl = "";

        JSONObject fileContents = inputStreamToJson(inputStream);
        if (fileContents != null)
            mainUrl = fileContents.optString(Provider.MAIN_URL);
        return mainUrl;
    }

    private JSONObject inputStreamToJson(InputStream inputStream) {
        JSONObject json = null;
        try {
            byte[] bytes = new byte[inputStream.available()];
            if (inputStream.read(bytes) > 0)
                json = new JSONObject(new String(bytes));
            inputStream.reset();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public List<Provider> providers() {
        List<Provider> allProviders = new ArrayList<>();
        allProviders.addAll(defaultProviders);
        if(customProviders != null)
            allProviders.addAll(customProviders);
        //add an option to add a custom provider
        //TODO: refactor me?
        allProviders.add(new Provider());
        return allProviders;
    }

    @Override
    public int size() {
        return providers().size();
    }

    @Override
    public Provider get(int index) {
        Iterator<Provider> iterator = providers().iterator();
        while (iterator.hasNext() && index > 0) {
            iterator.next();
            index--;
        }
        return iterator.next();
    }

    @Override
    public boolean add(Provider element) {
        return element != null &&
                !defaultProviderURLs.contains(element.getMainUrl().getUrl()) &&
                customProviders.add(element) &&
                customProviderURLs.add(element.getMainUrl().getUrl());
    }

    @Override
    public boolean remove(Object element) {
        return element instanceof Provider &&
                customProviders.remove(element) &&
                customProviderURLs.remove(((Provider) element).getMainUrl().getUrl());
    }

    @Override
    public boolean addAll(Collection<? extends Provider> elements) {
        Iterator iterator = elements.iterator();
        boolean addedAll = true;
        while (iterator.hasNext()) {
            Provider p = (Provider) iterator.next();
            addedAll = customProviders.add(p) &&
                    customProviderURLs.add(p.getMainUrl().getUrl()) &&
                    addedAll;
        }
        return addedAll;
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        Iterator iterator = elements.iterator();
        boolean removedAll = true;
        try {
            while (iterator.hasNext()) {
                Provider p = (Provider) iterator.next();
                removedAll = ((defaultProviders.remove(p) && defaultProviderURLs.remove(p.getMainUrl().getUrl())) ||
                        (customProviders.remove(p) && customProviderURLs.remove(p.getMainUrl().getUrl()))) &&
                        removedAll;
            }
        } catch (ClassCastException e) {
            return false;
        }

        return removedAll;
    }

    @Override
    public void clear() {
        defaultProviders.clear();
        customProviders.clear();
        customProviderURLs.clear();
        defaultProviderURLs.clear();
    }

    //FIXME: removed custom providers should be deleted here as well
    void saveCustomProvidersToFile() {
        try {
            for (Provider provider : customProviders) {
                File providerFile = new File(externalFilesDir, provider.getName() + ".json");
                if (!providerFile.exists()) {
                    FileWriter writer = new FileWriter(providerFile);
                    writer.write(provider.toJson().toString());
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
