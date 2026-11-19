package net.jr.client.compat.sodium;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SodiumChunkBuildContextRouter {
    private static final Map<Object, JobRoute> JOB_ROUTES = new IdentityHashMap<>();
    private static final ThreadLocal<Map<ClientLevel, Object>> CONTEXTS =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Object> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final Map<Class<?>, Field> BUFFERS_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VERTEX_TYPE_FIELDS = new ConcurrentHashMap<>();
    private static final Map<ConstructorKey, Constructor<?>> CONTEXT_CONSTRUCTORS =
        new ConcurrentHashMap<>();
    private static final Map<MethodKey, Method> EXECUTE_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> CLEANUP_METHODS = new ConcurrentHashMap<>();

    private SodiumChunkBuildContextRouter() {
    }

    public static void register(Object builder, Object job, ClientLevel level) {
        synchronized (JOB_ROUTES) {
            JOB_ROUTES.put(job, new JobRoute(builder, level));
        }
    }

    public static void discardBuilder(Object builder, ClientLevel level) {
        synchronized (JOB_ROUTES) {
            JOB_ROUTES.entrySet().removeIf(entry -> entry.getValue().builder == builder);
        }
        Map<ClientLevel, Object> contexts = CONTEXTS.get();
        contexts.remove(level);
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
        }
    }

    public static void execute(Object job, Object fallbackContext) {
        Object context = contextFor(job, fallbackContext);
        ACTIVE_CONTEXT.set(context);
        try {
            invoke(job, executeMethod(job.getClass(), context.getClass()), context);
        } catch (RuntimeException exception) {
            ACTIVE_CONTEXT.remove();
            throw exception;
        }
    }

    public static void cleanup(Object fallbackContext) {
        Object context = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.remove();
        Object resolved = context != null ? context : fallbackContext;
        invoke(resolved, cleanupMethod(resolved.getClass()));
    }

    private static Object contextFor(Object job, Object fallbackContext) {
        JobRoute route;
        synchronized (JOB_ROUTES) {
            route = JOB_ROUTES.remove(job);
        }
        if (route == null) {
            return fallbackContext;
        }

        Map<ClientLevel, Object> contexts = CONTEXTS.get();
        return contexts.computeIfAbsent(
            route.level,
            level -> createContext(level, fallbackContext)
        );
    }

    private static Object createContext(ClientLevel level, Object fallbackContext) {
        try {
            Object buffers = field(BUFFERS_FIELDS, fallbackContext.getClass(), "buffers")
                .get(fallbackContext);
            Object vertexType = field(VERTEX_TYPE_FIELDS, buffers.getClass(), "vertexType")
                .get(buffers);
            Constructor<?> constructor = contextConstructor(
                fallbackContext.getClass(),
                vertexType.getClass()
            );
            return constructor.newInstance(level, vertexType);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Sodium chunk build context", exception);
        }
    }

    private static Field field(Map<Class<?>, Field> cache, Class<?> type, String name) {
        return cache.computeIfAbsent(type, ignored -> {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException(
                    "Unsupported Sodium field " + type.getName() + "." + name,
                    exception
                );
            }
        });
    }

    private static Constructor<?> contextConstructor(Class<?> contextType, Class<?> vertexType) {
        return CONTEXT_CONSTRUCTORS.computeIfAbsent(
            new ConstructorKey(contextType, vertexType),
            ignored -> {
                for (Constructor<?> constructor : contextType.getConstructors()) {
                    Class<?>[] parameters = constructor.getParameterTypes();
                    if (parameters.length == 2
                            && parameters[0].isAssignableFrom(ClientLevel.class)
                            && parameters[1].isAssignableFrom(vertexType)) {
                        return constructor;
                    }
                }
                throw new IllegalStateException(
                    "Unsupported Sodium context constructor: " + contextType.getName()
                );
            }
        );
    }

    private static Method executeMethod(Class<?> jobType, Class<?> contextType) {
        return EXECUTE_METHODS.computeIfAbsent(
            new MethodKey(jobType, contextType),
            ignored -> publicMethod(jobType, "execute", contextType)
        );
    }

    private static Method cleanupMethod(Class<?> contextType) {
        return CLEANUP_METHODS.computeIfAbsent(
            contextType,
            ignored -> publicMethod(contextType, "cleanup")
        );
    }

    private static Method publicMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(
                "Unsupported Sodium method " + type.getName() + "." + name,
                exception
            );
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

    private record JobRoute(Object builder, ClientLevel level) {
    }

    private record ConstructorKey(Class<?> contextType, Class<?> vertexType) {
    }

    private record MethodKey(Class<?> ownerType, Class<?> parameterType) {
    }
}
