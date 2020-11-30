package org.jfrog.build.api.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonUtils {
    /**
     * Returns a map containing only the values of the original map that satisfy the predicate.
     * NOTE: This function filters null elements, so the predicate shouldn't be looking for one.
     */
    public static <K, V> Map<K, V> filterMapValues(Map<K, V> map, Predicate<V> predicate) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> predicate.test(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a map containing only the keys of the original map that satisfy the predicate.
     * NOTE: This function filters null elements, so the predicate shouldn't be looking for one.
     */
    public static <K, V> Map<K, V> filterMapKeys(Map<K, V> map, Predicate<K> predicate) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> predicate.test(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a map with entries from the left map which keys aren't in the right map.
     */
    public static <K, V> Map<K, V> entriesOnlyOnLeftMap(Map<K, V> left, Map<K, V> right) {
        Map<K, V> difference = new HashMap<>();
        difference.putAll(left);
        difference.putAll(right);
        difference.entrySet().removeAll(right.entrySet());
        return difference;
    }

    /**
     * Returns a new list of the concatenation of the two provided lists.
     */
    public static <T> List<T> concatLists(List<T> first, List<T> second) {
        return Stream.concat(first.stream(), second.stream())
                .collect(Collectors.toList());
    }

    /**
     * Outputs a new list after applying the provided function on every element in the provided list.
     */
    public static <F, T> ArrayList<T> transformList(Iterable<F> iterable, Function<F, T> function) {
        ArrayList<T> output = new ArrayList<>();
        iterable.forEach((t) -> output.add(function.apply(t)));
        return output;
    }

    /**
     * Returns the first element in the provided collection that satisfies the predicate.
     * If no such element found, returns the defaultValue.
     * NOTE: This function filters null elements, so the predicate shouldn't be looking for one.
     */
    public static <T> T getFirstSatisfying(Collection<T> collection, Predicate<T> predicate, T defaultValue) {
        return collection.stream()
                .filter(Objects::nonNull)
                .filter(predicate)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * Returns true if any of the elements in the provided collection satisfies the predicate.
     * Returns false otherwise.
     * NOTE: This function filters null elements, so the predicate shouldn't be looking for one.
     */
    public static <T> boolean isAnySatisfying(Collection<T> collection, Predicate<T> predicate) {
        return getFirstSatisfying(collection, predicate, null) != null;
    }

    /**
     * Returns a collection containing only the elements of the original collection that satisfy the predicate.
     * NOTE: This function filters null elements, so the predicate shouldn't be looking for one.
     */
    public static <T> Collection<T> filterCollection(Collection<T> unfiltered, Predicate<T> predicate) {
        return unfiltered.stream()
                .filter(Objects::nonNull)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Returns the last element in the collection.
     * Returns null if collection is empty.
     */
    public static <T> T getLast(Collection<T> collection) {
        return collection.stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Returns the only element in the provided collection.
     * If the collection doesn't have exactly one element, an exception is thrown.
     */
    public static <T> T getOnlyElement(Collection<T> collection) {
        if (collection.size() == 1) {
            return collection.iterator().next();
        }
        throw new IllegalArgumentException("Collection was expected to have exactly one element, but has " + collection.size());
    }

    /**
     * Write a string to a file using the provided Charset.
     */
    public static void writeByCharset(String from, File to, Charset charset) throws RuntimeException {
        try (BufferedWriter writer = Files.newBufferedWriter(to.toPath(), charset)) {
            writer.write(from);
            writer.flush();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a string from a file using the provided Charset.
     */
    public static String readByCharset(File from, Charset charset) throws IOException {
        byte[] encoded = Files.readAllBytes(from.toPath());
        return new String(encoded, charset);
    }

    /**
     * Returns a new HashSet of the provided elements.
     */
    public static <E> HashSet<E> newHashSet(E... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * Returns an empty iterable if a null is passed.
     * Useful for wrapping collections in foreach loops.
     */
    public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
        return iterable == null ? Collections.emptyList() : iterable;
    }

    /**
     * Handle 'java.io.tmpdir' system property.
     * If not defined in POSIX compliant systems - use '/tmp' folder.
     * If defined but directory does not exist - try to create it.
     *
     * @throws RuntimeException if case of missing 'java.io.tmpdir' is missing in Windows or the path is not accessible.
     */
    public static void handleJavaTmpdirProperty() {
        String tmpDirPath = FileUtils.getTempDirectoryPath();

        if (StringUtils.isNotBlank(tmpDirPath)) {
            // java.io.tmpdir is defined.
            if (FileUtils.getTempDirectory().exists()) {
                return;
            }
            // java.io.tmpdir is defined, but the directory doesn't exist.
            try {
                Files.createDirectories(Paths.get(tmpDirPath));
            } catch (IOException e) {
                throw new RuntimeException("The directory defined in the system property 'java.io.tmpdir' (" + tmpDirPath + ") doesn't exit. " +
                        "An attempt to create a directory in this path failed: ", e);
            }
        } else if (SystemUtils.IS_OS_UNIX) {
            // java.io.tmpdir is not defined and the OS is POSIX compliant system.
            System.setProperty("java.io.tmpdir", "/tmp");
        } else {
            // java.io.tmpdir is not defined and the OS is not POSIX compliant system.
            throw new RuntimeException("java.io.tmpdir system property is missing!");
        }
    }
}
