package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.ConfigurationParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class SybokEngineOptions {
    public static final String SPOCK_SCRIPT_ENGINE_ROOTS = "sybok.script-roots";
    private final List<Path> scriptRoots;

    public static SybokEngineOptions from(ConfigurationParameters parameters) {
        List<Path> roots = parameters.get(SPOCK_SCRIPT_ENGINE_ROOTS, ValidatingRootParser.INSTANCE)
                .orElse(Collections.emptyList());
        return new SybokEngineOptions(roots);
    }

    private SybokEngineOptions(List<Path> scriptRoots) {
        this.scriptRoots = scriptRoots;
    }

    public List<Path> getScriptRoots() {
        return scriptRoots;
    }

    private enum ValidatingRootParser implements Function<String, List<Path>> {
        INSTANCE;

        @Override
        public List<Path> apply(String roots) {
            List<Path> rootPaths = Arrays.stream(roots.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Paths::get)
                    .distinct()
                    .collect(collectingAndThen(toList(), Collections::unmodifiableList));
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
