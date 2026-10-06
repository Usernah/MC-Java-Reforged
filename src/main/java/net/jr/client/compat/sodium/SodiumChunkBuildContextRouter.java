package net.jr.client.compat.sodium;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Keeps Sodium's worker pool global while selecting a build context for the
 * ClientLevel that submitted each job. Sodium normally creates one context per
 * worker because one renderer only has one level.
 */
public final class SodiumChunkBuildContextRouter {
    private static final Map<Object, ClientLevel> JOB_LEVELS =
        java.util.Collections.synchronizedMap(new IdentityHashMap<>());
    private static final ThreadLocal<Map<ClientLevel, Object>> CONTEXTS =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Object> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static @Nullable Field buffersField;
    private static @Nullable Field vertexTypeField;
    private static @Nullable Constructor<?> contextConstructor;
    private static @Nullable Method executeMethod;
    private static @Nullable Method cleanupMethod;

    private SodiumChunkBuildContextRouter() {
    }

    public static void register(Object job, ClientLevel level) {
        JOB_LEVELS.put(job, level);
    }

    public static void execute(Object job, Object fallbackContext) {
        Object context = contextFor(job, fallbackContext);
        ACTIVE_CONTEXT.set(context);
        try {
            invoke(job, executeMethod(job), context);
        } catch (RuntimeException exception) {
            ACTIVE_CONTEXT.remove();
            throw exception;
        }
    }

    public static void cleanup(Object fallbackContext) {
        Object context = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.remove();
        invoke(context != null ? context : fallbackContext, cleanupMethod(context != null ? context : fallbackContext));
    }

    private static Object contextFor(Object job, Object fallbackContext) {
        ClientLevel level = JOB_LEVELS.remove(job);
        if (level == null) {
            return fallbackContext;
        }

        Map<ClientLevel, Object> contexts = CONTEXTS.get();
        Object context = contexts.get(level);
        if (context == null) {
            context = createContext(level, fallbackContext);
            contexts.put(level, context);
        }
        return context;
    }

    private static Object createContext(ClientLevel level, Object fallbackContext) {
        try {
            Object buffers = buffersField(fallbackContext).get(fallbackContext);
            Object vertexType = vertexTypeField(buffers).get(buffers);
            Constructor<?> constructor = contextConstructor(fallbackContext.getClass());
            return constructor.newInstance(level, vertexType);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Sodium chunk build context", exception);
        }
    }

    private static Field buffersField(Object context) throws NoSuchFieldException {
        Field field = buffersField;
        if (field == null) {
            field = accessibleField(context.getClass(), "buffers");
            buffersField = field;
        }
        return field;
    }

    private static Field vertexTypeField(Object buffers) throws NoSuchFieldException {
        Field field = vertexTypeField;
        if (field == null) {
            field = accessibleField(buffers.getClass(), "vertexType");
            vertexTypeField = field;
        }
        return field;
    }

    private static Constructor<?> contextConstructor(Class<?> contextType) throws ReflectiveOperationException {
        Constructor<?> constructor = contextConstructor;
        if (constructor == null) {
            constructor = contextType.getConstructors()[0];
            contextConstructor = constructor;
        }
        return constructor;
    }

    private static Method executeMethod(Object job) {
        Method method = executeMethod;
        if (method == null) {
            method = publicMethod(job.getClass(), "execute", contextConstructorParameter());
            executeMethod = method;
        }
        return method;
    }

    private static Class<?> contextConstructorParameter() {
        Constructor<?> constructor = contextConstructor;
        if (constructor == null) {
            throw new IllegalStateException("Sodium chunk build context has not been initialized");
        }
        return constructor.getDeclaringClass();
    }

    private static Method cleanupMethod(Object context) {
        Method method = cleanupMethod;
        if (method == null) {
            method = publicMethod(context.getClass(), "cleanup");
            cleanupMethod = method;
        }
        return method;
    }

    private static Field accessibleField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method publicMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unsupported Sodium chunk builder", exception);
        }
    }

    private static void invoke(Object owner, Method method, Object... arguments) {
        try {
            method.invoke(owner, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke Sodium chunk builder", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Sodium chunk build failed", cause);
        }
    }
}
