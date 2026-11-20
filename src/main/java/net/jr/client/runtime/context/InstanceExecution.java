package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

public final class InstanceExecution {
    private InstanceExecution() {
    }

    public static SessionToken captureToken() {
        return InstanceResolver.captureCurrentToken();
    }

    public static boolean isValid(SessionToken token) {
        return InstanceResolver.isTokenValid(token);
    }

    public static Runnable capture(Runnable action) {
        Objects.requireNonNull(action, "action");
        SessionToken token = captureToken();
        return () -> {
            if (isValid(token)) {
                InstanceResolver.requireToken(token).slot().runForSlot(action);
            }
        };
    }

    public static <T> Callable<T> capture(Callable<T> action) {
        Objects.requireNonNull(action, "action");
        SessionToken token = captureToken();
        return () -> {
            if (!isValid(token)) {
                throw new CancellationException("Local client session expired before async execution");
            }
            try {
                return InstanceResolver.requireToken(token).slot().runForSlot(() -> {
                    try {
                        return action.call();
                    } catch (RuntimeException | Error error) {
                        throw error;
                    } catch (Exception error) {
                        throw new CapturedException(error);
                    }
                });
            } catch (CapturedException error) {
                throw (Exception)error.getCause();
            }
        };
    }

    public static Executor contextual(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return action -> executor.execute(capture(action));
    }

    public static Runnable wrapScheduled(Runnable action) {
        Objects.requireNonNull(action, "action");
        SessionToken token = InstanceResolver.captureScheduledTokenOrNull();
        if (token == null) {
            return action;
        }
        return () -> {
            if (isValid(token)) {
                InstanceResolver.requireToken(token).slot().runForSlot(action);
            }
        };
    }

    private static final class CapturedException extends RuntimeException {
        private CapturedException(Exception cause) {
            super(cause);
        }
    }
}
