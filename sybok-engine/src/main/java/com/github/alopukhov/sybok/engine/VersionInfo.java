package com.github.alopukhov.sybok.engine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

class VersionInfo {
    private static final String GROUP;
    private static final String ARTIFACT;
    private static final String VERSION;

    static {
        Properties properties = new Properties();
        try (InputStream is = VersionInfo.class.getResourceAsStream("/META-INF/sybok/version-info.properties");
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (Exception e) {
            // ignore
        }
        GROUP = properties.getProperty("group");
        ARTIFACT = properties.getProperty("artifact");
        VERSION = properties.getProperty("version");
    }

    public static String group() {
        return GROUP;
    }

    public static String artifact() {
        return ARTIFACT;
    }

    public static String version() {
        return VERSION;
    }

    private VersionInfo() {
        throw new UnsupportedOperationException();
    }
}
