package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.spockframework.runtime.SpecUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

class CandidatesSelector {
    private final SpecScriptLoader specScriptLoader;

    CandidatesSelector(SpecScriptLoader specScriptLoader) {
        this.specScriptLoader = requireNonNull(specScriptLoader, "specScriptLoader");
    }

    Collection<Class<?>> selectCandidates(EngineDiscoveryRequest discoveryRequest) {
        List<String> classNames = new ArrayList<>();
        for (FileSelector s : discoveryRequest.getSelectorsByType(FileSelector.class)) {
            resolveFile(s).ifPresent(classNames::add);
        }
        for (DirectorySelector s : discoveryRequest.getSelectorsByType(DirectorySelector.class)) {
            classNames.addAll(resolveDir(s));
        }
        return classNames.stream()
                .distinct()
                .map(specScriptLoader::safeLoadClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(SpecUtil::isRunnableSpec)
                .collect(toList());
    }

    private Collection<String> resolveDir(DirectorySelector selector) {
        Path path = selector.getPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            return Collections.emptyList();
        }
        Optional<Path> scriptRootOpt = specScriptLoader.rootOf(path);
        if (!scriptRootOpt.isPresent()) {
            return Collections.emptyList();
        }
        Path scriptRoot = scriptRootOpt.get();
        DirectoryVisitor visitor = new DirectoryVisitor(resolveBasePackage(scriptRoot, path));
        try {
            Files.walkFileTree(path, visitor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return visitor.classNames;
    }

    private Optional<String> resolveFile(FileSelector selector) {
        Path path = selector.getPath().toAbsolutePath().normalize();
        if (!isCandidateFile(path.getFileName().toString()) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        Optional<Path> scriptRootOpt = specScriptLoader.rootOf(path);
        if (!scriptRootOpt.isPresent() || scriptRootOpt.get().equals(path)) {
            return Optional.empty();
        }
        String className = resolveClassName(scriptRootOpt.get(), path);
        return Optional.of(className);
    }

    private boolean isCandidateFile(String filename) {
        return filename.endsWith("Spec.groovy");
    }

    private String resolveClassName(Path root, Path file) {
        List<String> elements = new ArrayList<>();
        String filename = file.getFileName().toString();
        elements.add(filename.substring(0, filename.length() - ".groovy".length()));
        file = file.getParent();
        while (!root.equals(file)) {
            elements.add(file.getFileName().toString());
            file = file.getParent();
        }
        Collections.reverse(elements);
        return String.join(".", elements);
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

    private class DirectoryVisitor implements FileVisitor<Path> {
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
            if (isCandidateFile(fileName)) {
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
