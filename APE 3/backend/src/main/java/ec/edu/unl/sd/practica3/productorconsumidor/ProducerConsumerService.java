package ec.edu.unl.sd.practica3.productorconsumidor;

import ec.edu.unl.sd.practica3.common.SimulationResult;
import ec.edu.unl.sd.practica3.common.EventLog;
import ec.edu.unl.sd.practica3.common.Sim;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProducerConsumerService {

    private static final int CAPACITY = 5;
    private static final int WORKERS = 2;
    private static final int PIECES_PER_WORKER = 5;

    public SimulationResult run(int initialSpaces, int initialElements) {
        if (initialSpaces < 0 || initialSpaces > CAPACITY * 2
                || initialElements < 0 || initialElements > CAPACITY * 2) {
            throw new IllegalArgumentException("Los valores de los semáforos deben estar entre 0 y 10.");
        }

        if (initialSpaces != CAPACITY || initialElements != 0) {
            return invalidInitialization(initialSpaces, initialElements);
        }

        Queue<String> warehouse = new ArrayDeque<>(CAPACITY);
        Semaphore spaces = new Semaphore(initialSpaces, true);
        Semaphore elements = new Semaphore(initialElements, true);
        Semaphore mutex = new Semaphore(1, true);
        AtomicInteger maximum = new AtomicInteger();
        List<EventLog> events = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        long startNanos = System.nanoTime();

        for (int id = 1; id <= WORKERS; id++) {
            int producerId = id;
            threads.add(Sim.start("productor-" + id, () -> {
                for (int count = 1; count <= PIECES_PER_WORKER; count++) {
                    String piece = "P" + producerId + "-" + count;
                    spaces.acquireUninterruptibly();
                    mutex.acquireUninterruptibly();
                    try {
                        warehouse.add(piece);
                        maximum.accumulateAndGet(warehouse.size(), Math::max);
                        addEvent(events, "Productor " + producerId + " agregó pieza " + piece
                                + " (almacén: " + warehouse.size() + "/" + CAPACITY + ")");
                    } finally {
                        mutex.release();
                    }
                    elements.release();
                }
            }));
        }

        for (int id = 1; id <= WORKERS; id++) {
            int consumerId = id;
            threads.add(Sim.start("consumidor-" + id, () -> {
                for (int count = 1; count <= PIECES_PER_WORKER; count++) {
                    elements.acquireUninterruptibly();
                    String piece;
                    mutex.acquireUninterruptibly();
                    try {
                        piece = warehouse.remove();
                        addEvent(events, "Consumidor " + consumerId + " retiró pieza " + piece
                                + " (almacén: " + warehouse.size() + "/" + CAPACITY + ")");
                    } finally {
                        mutex.release();
                    }
                    spaces.release();
                    addEvent(events, "Consumidor " + consumerId + " ensambló pieza " + piece);
                }
            }));
        }

        Sim.joinAll(threads);
        long elapsed = Sim.elapsedMs(startNanos);
        boolean verified = maximum.get() <= CAPACITY && warehouse.isEmpty();
        return new SimulationResult(
                verified,
                verified ? "Los productores y consumidores completaron su trabajo sin exceder la capacidad del almacén."
                        : "La simulación no respetó la capacidad o dejó piezas sin consumir.",
                Sim.metrics(
                        "capacidad", CAPACITY,
                        "productores", WORKERS,
                        "consumidores", WORKERS,
                        "espaciosIniciales", initialSpaces,
                        "elementosIniciales", initialElements,
                        "piezasProducidas", WORKERS * PIECES_PER_WORKER,
                        "piezasConsumidas", WORKERS * PIECES_PER_WORKER,
                        "maximoAlmacen", maximum.get(),
                        "piezasRestantes", warehouse.size(),
                        "tiempoConcurrenteMs", elapsed),
                List.copyOf(events));
    }

    private static SimulationResult invalidInitialization(int initialSpaces, int initialElements) {
        List<EventLog> events = new ArrayList<>();
        addEvent(events, "La inicialización no coincide con el almacén vacío: se requiere espacios=5 y elementos=0.");
        if (initialSpaces < CAPACITY) {
            addEvent(events, "Hay menos permisos de espacio que espacios físicos. Algunos productores pueden quedarse bloqueados; con espacios=0 ninguno puede producir.");
        } else if (initialSpaces > CAPACITY) {
            addEvent(events, "El semáforo espacios concede permisos de más y podría permitir superar la capacidad física.");
        }
        if (initialElements > 0) {
            addEvent(events, "elementos permite retirar piezas aunque la cola empieza vacía. En una ejecución directa, un consumidor intentaría extraer una pieza inexistente.");
        }

        String summary = initialSpaces == 0
                ? "Prueba fallida controlada: con espacios=0 los productores no pueden insertar y la ejecución real quedaría bloqueada. No se lanzaron hilos para evitar colgar la aplicación."
                : initialElements > 0
                        ? "Prueba fallida controlada: elementos anuncia piezas que no existen; un consumidor intentaría retirar de una cola vacía. No se lanzaron hilos para evitar una extracción inválida."
                        : "Prueba fallida controlada: los permisos iniciales no representan los cinco espacios disponibles del almacén vacío.";

        return new SimulationResult(
                false,
                summary,
                Sim.metrics(
                        "capacidad", CAPACITY,
                        "productores", WORKERS,
                        "consumidores", WORKERS,
                        "espaciosIniciales", initialSpaces,
                        "elementosIniciales", initialElements,
                        "piezasProducidas", 0,
                        "piezasConsumidas", 0,
                        "maximoAlmacen", 0,
                        "piezasRestantes", 0,
                        "tiempoConcurrenteMs", 0),
                List.copyOf(events));
    }

    private static void addEvent(List<EventLog> events, String message) {
        events.add(EventLog.of(message));
    }
}
