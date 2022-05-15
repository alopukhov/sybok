package io.github.alopukhov.sybok.engine;

import org.junit.platform.engine.ConfigurationParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

class SybokEngineOptions {
    private static final String DELEGATING_ENGINE_IDS = "sybok.delegate-engine-ids";
    private static final String SCRIPT_ENGINE_ROOTS = "sybok.script-roots";
    private static final String ALTER_CLASSLOADER = "sybok.alter-classloader";
    private static final boolean ALTER_CLASSLOADER_DEFAULT = false;
    private static final String DELEGATE_SPECIFIC_PREFIX = "sybok.delegate.";
    private static final String ALTER_CLASSLOADER_SUFFIX = ".alter-classloader";

    private final List<Path> scriptRoots;
    private final List<String> engineIds;
    private final boolean alterClassloaderDefault;
    private final ConfigurationParameters parameters;

    static SybokEngineOptions from(ConfigurationParameters parameters) {
        List<Path> roots = parameters.get(SCRIPT_ENGINE_ROOTS, ValidatingRootParser.INSTANCE)
                .orElseGet(Collections::emptyList);
        Collection<String> engineIds = parameters.get(DELEGATING_ENGINE_IDS, TrimStringsTransformer.INSTANCE)
                .orElseGet(Collections::emptyList);
        boolean switchClassloader = parameters.getBoolean(ALTER_CLASSLOADER)
                .orElse(ALTER_CLASSLOADER_DEFAULT);
        return new SybokEngineOptions(roots, engineIds, switchClassloader, parameters);
    }

    private SybokEngineOptions(Collection<Path> scriptRoots,
                               Collection<String> engineIds,
                               boolean alterClassloaderDefault, ConfigurationParameters parameters) {
        this.scriptRoots = unmodifiableList(new ArrayList<>(scriptRoots));
        this.engineIds = unmodifiableList(new ArrayList<>(engineIds));
        this.alterClassloaderDefault = alterClassloaderDefault;
        this.parameters = parameters;
    }

    public List<Path> getScriptRoots() {
        return scriptRoots;
    }

    public List<String> getEngineIds() {
        return engineIds;
    }

    public boolean alterClassloader(String engineId) {
        String key = DELEGATE_SPECIFIC_PREFIX + engineId + ALTER_CLASSLOADER_SUFFIX;
        return parameters.getBoolean(key).orElse(alterClassloaderDefault);
    }

    private enum TrimStringsTransformer implements Function<String, Collection<String>> {
        INSTANCE;

        @Override
        public Collection<String> apply(String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(trimmed -> !trimmed.isEmpty())
                    .collect(toList());
        }
    }

    private enum ValidatingRootParser implements Function<String, List<Path>> {
        INSTANCE;

        @Override
        public List<Path> apply(String roots) {
            List<Path> rootPaths = TrimStringsTransformer.INSTANCE.apply(roots)
                    .stream()
                    .map(Paths::get)
                    .distinct()
                    .collect(toList());
            validateRoots(rootPaths);
            return rootPaths;
        }

        private static void validateRoots(List<Path> scriptRoots) {
            validateRootDirsExists(scriptRoots);
            validateRootsNotOverlaps(scriptRoots);
        }

        private static void validateRootsNotOverlaps(List<Path> scriptRoots) {
            for (int i = 0; i < scriptRoots.size(); i++) {
                for (int j = 0; j < scriptRoots.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    Path iRoot = scriptRoots.get(i);
                    Path jRoot = scriptRoots.get(j);
                    Path iRootNormalized = iRoot.toAbsolutePath().normalize();
                    Path jRootNormalized = jRoot.toAbsolutePath().normalize();
                    if (jRootNormalized.startsWith(iRootNormalized)) {
                        String msg = "Bad spock script engine roots configuration: " +
                                iRoot + " (" + iRootNormalized + ")" +
                                " is parent of " +
                                jRoot + " (" + jRootNormalized + ")";
                        throw new IllegalArgumentException(msg);
                    }
                }
            }
        }

        private static void validateRootDirsExists(List<Path> scriptRoots) {
            for (Path root : scriptRoots) {
                if (!Files.isDirectory(root)) {
                    throw new IllegalStateException("Bad spock script engine root (does not exists or not a directory): "
                            + root);
                }
            }
        }

        @Override
        public String toString() {
            return ValidatingRootParser.class.getSimpleName();
        }
    }
}
