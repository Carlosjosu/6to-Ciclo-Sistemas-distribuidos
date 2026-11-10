package ec.edu.unl.sd.practica3.estacionamiento;

import ec.edu.unl.sd.practica3.common.EventLog;
import ec.edu.unl.sd.practica3.common.Sim;
import ec.edu.unl.sd.practica3.common.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ParkingService {

    public static final int CAPACIDAD_FISICA = 4;
    public static final int VEHICULOS_POR_DEFECTO = 10;
    public static final int PERMISOS_POR_DEFECTO = 4;
    public static final int ESTADIA_MS_POR_DEFECTO = 20;

    public SimulationResult run() {
        return run(VEHICULOS_POR_DEFECTO, PERMISOS_POR_DEFECTO, ESTADIA_MS_POR_DEFECTO);
    }

    public SimulationResult run(int vehiculos, int permisosSemaforo, int estadiaMs) {
        validatePositive("vehiculos", vehiculos);
        validatePositive("permisosSemaforo", permisosSemaforo);
        validatePositive("estadiaMs", estadiaMs);

        Semaphore espacios = new Semaphore(permisosSemaforo, true);
        AtomicInteger dentro = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();
        List<EventLog> events = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        long startNanos = System.nanoTime();

        for (int id = 1; id <= vehiculos; id++) {
            final int vehicleId = id;
            threads.add(Sim.start("vehiculo-" + vehicleId, () -> {
                addEvent(events, "Vehículo " + vehicleId + " llega al estacionamiento");
                espacios.acquireUninterruptibly();
                try {
                    int current = dentro.incrementAndGet();
                    maximo.accumulateAndGet(current, Math::max);
                    addEvent(events, "Vehículo " + vehicleId + " ingresa");
                    Sim.sleep(estadiaMs);
                    addEvent(events, "Vehículo " + vehicleId + " sale");
                } finally {
                    dentro.decrementAndGet();
                    espacios.release();
                }
            }));
        }

        Sim.joinAll(threads);
        long concurrentTime = Sim.elapsedMs(startNanos);
        boolean verified = maximo.get() <= CAPACIDAD_FISICA;
        String summary = verified
                ? "El estacionamiento respetó su capacidad física."
                : "El semáforo permitió superar la capacidad física.";

        return new SimulationResult(
                verified,
                summary,
                Sim.metrics(
                        "vehiculos", vehiculos,
                        "permisosSemaforo", permisosSemaforo,
                        "capacidadFisica", CAPACIDAD_FISICA,
                        "maximoSimultaneos", maximo.get(),
                        "tiempoConcurrenteMs", concurrentTime,
                        "tiempoSecuencialEstimadoMs", (long) vehiculos * estadiaMs),
                List.copyOf(events));
    }

    private static void addEvent(List<EventLog> events, String message) {
        events.add(EventLog.of(message));
    }

    private static void validatePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " debe ser mayor que cero.");
        }
    }
}
