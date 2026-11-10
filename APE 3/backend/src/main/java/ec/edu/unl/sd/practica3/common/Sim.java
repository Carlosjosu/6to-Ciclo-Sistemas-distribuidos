package ec.edu.unl.sd.practica3.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class Sim {

    private Sim() {
    }

    public static Thread start(String name, Runnable action) {
        Thread thread = new Thread(action, name);
        thread.start();
        return thread;
    }

    public static void joinAll(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("La simulación fue interrumpida.", exception);
            }
        }
    }

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La simulación fue interrumpida.", exception);
        }
    }

    public static boolean tryAcquire(Semaphore semaphore, long timeoutMs) {
        try {
            return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El hilo fue interrumpido mientras esperaba un semáforo.", exception);
        }
    }

    public static Map<String, Object> metrics(Object... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Las métricas deben recibirse como pares clave/valor.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    public static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
