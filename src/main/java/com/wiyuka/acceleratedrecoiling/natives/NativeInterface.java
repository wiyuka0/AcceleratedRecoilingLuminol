package com.wiyuka.acceleratedrecoiling.natives;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeInterface {

    private static final Logger LOGGER = Logger.getLogger(NativeInterface.class.getName());

    public static boolean isVectorApiAvailable() {
        try {
            Class.forName("jdk.incubator.vector.Vector");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public enum BackendType {
        FFM("FFM", () -> loadReflectively("com.wiyuka.acceleratedrecoiling.natives.FFMBackend")),
        JNI("JNI", JNIBackend::new),
        JAVA_SIMD("Java SIMD", () -> {
            if (!isVectorApiAvailable()) throw new UnsupportedOperationException("Vector API not available");
            return loadReflectively("com.wiyuka.acceleratedrecoiling.natives.JavaSIMDBackend");
        }),
        JAVA_VANILLA("Java Vanilla", JavaVanillaBackend::new),
        JAVA("Pure Java", JavaBackend::new),
        AUTO("Auto", null);

        private final String displayName;
        private final Supplier<INativeBackend> loader;

        BackendType(String displayName, Supplier<INativeBackend> loader) {
            this.displayName = displayName;
            this.loader = loader;
        }

        public String getDisplayName() { return displayName; }

        public INativeBackend tryLoad() {
            if (this == AUTO) return null;
            try {
                LOGGER.info("Attempting to load " + this.displayName + " backend...");
                INativeBackend instance = loader.get();
                instance.initialize();
                return instance;
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING,
                        this.displayName + " backend failed to load. Reason: " + t.getMessage(),
                        t);
                return null;
            }
        }
    }

    private static INativeBackend backend;
    private static boolean isInitialized = false;

    private static final List<BackendType> AUTO_FALLBACK_CHAIN = Arrays.asList(
            BackendType.FFM,
            BackendType.JNI,
            BackendType.JAVA_SIMD,
            BackendType.JAVA
    );

    public static void initialize() {
        if (AVX2.hasAVX2()) {
            initialize(BackendType.AUTO);
        } else if (isVectorApiAvailable()) {
            initialize(BackendType.JAVA_SIMD);
        } else {
            initialize(BackendType.JAVA);
        }
    }

    public static void initialize(BackendType preferredType) {
        if (isInitialized) return;

        LOGGER.info("Initializing NativeInterface with preferred backend: " + preferredType);

        backend = getBackend(preferredType);

        if (backend != null) {
            LOGGER.info("Successfully selected and initialized backend: " + backend.getName());
            isInitialized = true;
        } else {
            throw new IllegalStateException("Failed to initialize ANY backend!");
        }
    }

    private static INativeBackend getBackend(BackendType preferredType) {
        INativeBackend instance = null;

        if (preferredType != BackendType.AUTO) {
            instance = preferredType.tryLoad();
            if (instance != null) return instance;

            LOGGER.warning("Preferred " + preferredType.getDisplayName() +
                    " backend failed. Falling back to AUTO chain...");
        }

        LOGGER.info("Detected Java Version: " + Runtime.version().feature());

        for (BackendType type : AUTO_FALLBACK_CHAIN) {
            if (type == preferredType) continue;

            if (type == BackendType.FFM && Runtime.version().feature() < 21) continue;

            instance = type.tryLoad();
            if (instance != null) return instance;
        }

        return null;
    }

    private static INativeBackend loadReflectively(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (INativeBackend) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Reflection load failed for " + className, e);
        }
    }

    public static void applyConfig() {
        if (backend != null) {
            backend.applyConfig();
        }
    }

    public static void destroy() {
        if (backend != null) {
            backend.destroy();
            backend = null;
        }
        isInitialized = false;
    }

    public static PushResult push(double[] locations, double[] aabb, int[] resultSizeOut) {
        if (backend == null) {
            resultSizeOut[0] = 0;
            return null;
        }
        return backend.push(locations, aabb, resultSizeOut);
    }
}