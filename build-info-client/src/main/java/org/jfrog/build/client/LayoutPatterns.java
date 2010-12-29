package org.jfrog.build.client;

/**
 * Constants that denote different patterns that will be used for ivy/gradle.
 *
 * @author Tomer Cohen
 */
public interface LayoutPatterns {
    String M2_PER_MODULE_PATTERN = "[revision]/[artifact]-[revision](-[classifier]).[ext]";
    String M2_PATTERN = "[organisation]/[module]/" + M2_PER_MODULE_PATTERN;
    String M2_IVY_PATTERN = "[organisation]/[module]/[revision]/ivy-[revision].xml";
}
