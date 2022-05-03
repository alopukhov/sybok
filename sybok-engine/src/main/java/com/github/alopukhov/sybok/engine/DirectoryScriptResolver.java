package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class DirectoryScriptResolver implements SelectorResolver {
    private final SpecScriptLoader specScriptLoader;

    DirectoryScriptResolver(SpecScriptLoader specScriptLoader) {
        this.specScriptLoader = requireNonNull(specScriptLoader);
    }

    @Override
    public Resolution resolve(DirectorySelector selector, Context context) {
        Path path = selector.getPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            return Resolution.unresolved();
        }
        Optional<Path> scriptRootOpt = specScriptLoader.rootOf(path);
        if (!scriptRootOpt.isPresent()) {
            return Resolution.unresolved();
        }
        Path scriptRoot = scriptRootOpt.get();
        DirectoryVisitor visitor = new DirectoryVisitor(resolveBasePackage(scriptRoot, path));
        try {
            Files.walkFileTree(path, visitor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Set<ClassSelector> classSelectors = visitor.classNames.stream()
                .map(specScriptLoader::safeLoadClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(DiscoverySelectors::selectClass)
                .collect(Collectors.toSet());
        return classSelectors.isEmpty()? Resolution.unresolved() : Resolution.selectors(classSelectors);
    }

    private List<String> resolveBasePackage(Path root, Path path) {
        List<String> packagePath = new ArrayList<>();
        while (!path.equals(root)) {
            packagePath.add(path.getFileName().toString());
            path = path.getParent();
        }
        Collections.reverse(packagePath);
        return packagePath;
    }

    private static class DirectoryVisitor implements FileVisitor<Path> {
        private final List<String> packageName;
        private boolean first = true;
        private final List<String> classNames = new ArrayList<>();

        private DirectoryVisitor(List<String> packageName) {
            this.packageName = packageName;
        }


        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (first) {
                first = false;
            } else {
                packageName.add(dir.getFileName().toString());
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String fileName = file.getFileName().toString();
            if (fileName.toLowerCase(Locale.ROOT).endsWith("spec.groovy")) {
                String className = fileName.substring(0, fileName.length() - ".groovy".length());
                String fullName = Stream.concat(packageName.stream(), Stream.of(className))
                        .collect(Collectors.joining("."));
                classNames.add(fullName);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (!packageName.isEmpty()) {
                packageName.remove(packageName.size() - 1);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
