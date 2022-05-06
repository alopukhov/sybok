package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DirectoryScannerResolver implements SelectorResolver {
    private final CandidatesSelectorContext ctxt;
    private final Predicate<String> classNamePredicate;

    public DirectoryScannerResolver(CandidatesSelectorContext ctxt, Predicate<String> classNamePredicate) {
        this.ctxt = ctxt;
        this.classNamePredicate = classNamePredicate;
    }

    @Override
    public Resolution resolve(DirectorySelector selector, Context unused) {
        scanDir(selector.getPath(), selector);
        return Resolution.unresolved(); //not resolved yet
    }

    @Override
    public Resolution resolve(PackageSelector selector, Context unused) {
        String packageName = selector.getPackageName();
        String subDir = packageName.replace('.', File.separatorChar).trim();
        if (subDir.isEmpty()) {
            return Resolution.unresolved();
        }
        char firstChar = subDir.charAt(0);
        if (firstChar == '/' || firstChar == '\\') {
            return Resolution.unresolved();
        }
        for (Path root : ctxt.scriptRoots()) {
            Path path;
            try {
                 path = root.resolve(subDir);
            } catch (Exception e) {
                continue;
            }
            scanDir(path, root, selector);
        }
        return Resolution.unresolved(); //not resolved yet
    }

    private void scanDir(Path path, DiscoverySelector sourceSelector) {
        path = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            return;
        }
        Optional<Path> scriptRootOpt = ctxt.rootOf(path);
        if (scriptRootOpt.isPresent()) {
            scanDir(path, scriptRootOpt.get(), sourceSelector);
        }
    }

    private void scanDir(Path path, Path root, DiscoverySelector sourceSelector) {
        path = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            return;
        }
        DirectoryVisitor visitor = new DirectoryVisitor(sourceSelector, resolveBasePackage(root, path));
        try {
            Files.walkFileTree(path, visitor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String resolveBasePackage(Path root, Path path) {
        List<String> packagePath = new ArrayList<>();
        while (!path.equals(root)) {
            packagePath.add(path.getFileName().toString());
            path = path.getParent();
        }
        Collections.reverse(packagePath);
        return String.join(".", packagePath);
    }

    private class DirectoryVisitor implements FileVisitor<Path> {
        private final DiscoverySelector sourceSelector;
        private final String basePackage;
        private final List<String> visitingSubpackage = new ArrayList<>();
        private boolean first = true;

        private DirectoryVisitor(DiscoverySelector sourceSelector, String packageName) {
            this.sourceSelector = sourceSelector;
            this.basePackage = packageName + (packageName.isEmpty()? "" : ".");
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (first) {
                first = false;
            } else {
                visitingSubpackage.add(dir.getFileName().toString());
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Optional<String> fileNameWithoutExtension = ctxt.withoutScriptExtension(file.getFileName().toString());
            if (fileNameWithoutExtension.isPresent()) {
                String fullName = Stream.concat(visitingSubpackage.stream(), Stream.of(fileNameWithoutExtension.get()))
                        .collect(Collectors.joining(".", basePackage, ""));
                if (classNamePredicate.test(fullName)) {
                    ctxt.registerCandidate(fullName, sourceSelector);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (!visitingSubpackage.isEmpty()) {
                visitingSubpackage.remove(visitingSubpackage.size() - 1);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
