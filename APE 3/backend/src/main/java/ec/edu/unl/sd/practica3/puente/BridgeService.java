package ec.edu.unl.sd.practica3.puente;

import ec.edu.unl.sd.practica3.common.EventLog;
import ec.edu.unl.sd.practica3.common.Sim;
import ec.edu.unl.sd.practica3.common.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Ejercicio 4: puente de un solo sentido activo a la vez.
 *
 * Recurso compartido: el puente. Variables protegidas por el mutex (lock):
 * direccionActual, cruzando, consecutivos y los contadores de espera.
 * La condición "puedeEntrar" reemplaza al esperar() del pseudocódigo, porque
 * Condition.await() libera el mutex mientras el vehículo espera.
 */
@Service
public class BridgeService {

    public static final int VEHICULOS_POR_SENTIDO = 6;
    public static final int CRUCE_MS_POR_DEFECTO = 40;
    public static final int LLEGADA_MS_POR_DEFECTO = 10;
    public static final int LIMITE_CONSECUTIVOS_POR_DEFECTO = 3;

    private enum Direccion {
        NORTE, SUR, NINGUNA;

        Direccion opuesta() {
            return this == NORTE ? SUR : this == SUR ? NORTE : NINGUNA;
        }
    }

    public SimulationResult run() {
        return run(VEHICULOS_POR_SENTIDO, VEHICULOS_POR_SENTIDO, CRUCE_MS_POR_DEFECTO,
                LLEGADA_MS_POR_DEFECTO, true, LIMITE_CONSECUTIVOS_POR_DEFECTO, true);
    }

    public SimulationResult run(
            int vehiculosNorte,
            int vehiculosSur,
            int cruceMs,
            int llegadaMs,
            boolean justo,
            int limiteConsecutivos,
            boolean usarControl) {
        validate(vehiculosNorte, vehiculosSur, cruceMs, llegadaMs, limiteConsecutivos);

        Puente puente = new Puente(justo, limiteConsecutivos, usarControl);
        List<EventLog> events = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        long startNanos = System.nanoTime();

        for (int i = 0; i < vehiculosNorte; i++) {
            threads.add(vehiculo(puente, events, Direccion.NORTE, i + 1,
                    (long) i * llegadaMs, cruceMs));
        }
        for (int i = 0; i < vehiculosSur; i++) {
            threads.add(vehiculo(puente, events, Direccion.SUR, i + 1,
                    (long) i * llegadaMs + Math.max(1, llegadaMs / 2), cruceMs));
        }

        Sim.joinAll(threads);
        long concurrentTime = Sim.elapsedMs(startNanos);

        boolean verified = puente.conflictos.get() == 0;
        int total = vehiculosNorte + vehiculosSur;
        String summary;
        if (!usarControl) {
            summary = verified
                    ? "Sin control no hubo conflicto en esta ejecución, pero nada lo impide: repite la prueba."
                    : "Sin mutex ni condición hubo vehículos cruzando en sentidos opuestos a la vez.";
        } else if (justo) {
            summary = "Nunca hubo sentidos opuestos simultáneos y ningún sentido monopolizó el puente.";
        } else {
            summary = "Nunca hubo sentidos opuestos simultáneos, pero el sentido que llega primero puede "
                    + "monopolizar el puente (inanición del sentido contrario).";
        }

        return new SimulationResult(
                verified,
                summary,
                Sim.metrics(
                        "vehiculosNorte", vehiculosNorte,
                        "vehiculosSur", vehiculosSur,
                        "controlActivo", usarControl,
                        "modo", justo ? "Con límite anti-monopolio (" + limiteConsecutivos + " seguidos)"
                                : "Base sin límite",
                        "maximoNorteSimultaneo", puente.maxNorte.get(),
                        "maximoSurSimultaneo", puente.maxSur.get(),
                        "conflictosDetectados", puente.conflictos.get(),
                        "cambiosDeDireccion", puente.cambios.get(),
                        "norteAntesDelPrimerSur", puente.norteAntesDelPrimerSur(),
                        "esperaMaxNorteMs", puente.esperaMaxNorte.get(),
                        "esperaMaxSurMs", puente.esperaMaxSur.get(),
                        "vehiculosAtendidos", puente.atendidos.get() + " / " + total,
                        "tiempoConcurrenteMs", concurrentTime,
                        "tiempoSecuencialEstimadoMs", (long) total * cruceMs),
                List.copyOf(events));
    }

    private Thread vehiculo(
            Puente puente, List<EventLog> events, Direccion sentido, int id, long llegadaMs, int cruceMs) {
        String nombre = (sentido == Direccion.NORTE ? "norte-" : "sur-") + id;
        return Sim.start(nombre, () -> {
            Sim.sleep(llegadaMs);
            events.add(EventLog.of("Vehículo " + nombre + " llega al puente"));
            puente.entrar(sentido, events, nombre);
            Sim.sleep(cruceMs);
            puente.salir(sentido, events, nombre);
        });
    }

    private static void validate(int norte, int sur, int cruceMs, int llegadaMs, int limite) {
        if (norte < 0 || sur < 0 || norte + sur == 0) {
            throw new IllegalArgumentException("Debe existir al menos un vehículo.");
        }
        if (norte > 50 || sur > 50) {
            throw new IllegalArgumentException("El máximo es 50 vehículos por sentido.");
        }
        if (cruceMs <= 0 || llegadaMs <= 0) {
            throw new IllegalArgumentException("cruceMs y llegadaMs deben ser mayores que cero.");
        }
        if (limite <= 0) {
            throw new IllegalArgumentException("limiteConsecutivos debe ser mayor que cero.");
        }
    }

    private static final class Puente {
        private final boolean justo;
        private final int limiteConsecutivos;
        private final boolean usarControl;

        private final Lock mutex = new ReentrantLock(true);
        private final Condition puedeEntrar = mutex.newCondition();

        private Direccion direccionActual = Direccion.NINGUNA;
        private Direccion turnoPreferente = Direccion.NINGUNA;
        private int vehiculosNorte = 0;
        private int vehiculosSur = 0;
        private int esperandoNorte = 0;
        private int esperandoSur = 0;
        private int consecutivos = 0;

        private final AtomicInteger realNorte = new AtomicInteger();
        private final AtomicInteger realSur = new AtomicInteger();
        private final AtomicInteger maxNorte = new AtomicInteger();
        private final AtomicInteger maxSur = new AtomicInteger();
        private final AtomicInteger conflictos = new AtomicInteger();
        private final AtomicInteger cambios = new AtomicInteger();
        private final AtomicInteger atendidos = new AtomicInteger();
        private final AtomicLong esperaMaxNorte = new AtomicLong();
        private final AtomicLong esperaMaxSur = new AtomicLong();
        private final AtomicInteger norteCruzaron = new AtomicInteger();
        private final AtomicInteger norteAntesPrimerSur = new AtomicInteger(-1);

        private Puente(boolean justo, int limiteConsecutivos, boolean usarControl) {
            this.justo = justo;
            this.limiteConsecutivos = limiteConsecutivos;
            this.usarControl = usarControl;
        }

        void entrar(Direccion sentido, List<EventLog> events, String nombre) {
            long llegada = System.nanoTime();
            if (usarControl) {
                mutex.lock();
                try {
                    boolean espero = false;
                    registrarEspera(sentido, +1);
                    while (!puedeEntrar(sentido)) {
                        if (!espero) {
                            events.add(EventLog.of("Vehículo " + nombre + " espera: el puente está en sentido "
                                    + direccionActual + " o es turno del otro sentido"));
                            espero = true;
                        }
                        puedeEntrar.awaitUninterruptibly();
                    }
                    registrarEspera(sentido, -1);
                    if (direccionActual != sentido) {
                        direccionActual = sentido;
                        consecutivos = 0;
                        cambios.incrementAndGet();
                        events.add(EventLog.of("El puente cambia su dirección activa a " + sentido));
                    }
                    turnoPreferente = Direccion.NINGUNA;
                    consecutivos++;
                    if (sentido == Direccion.NORTE) {
                        vehiculosNorte++;
                    } else {
                        vehiculosSur++;
                    }
                    comenzarCruce(sentido, llegada, events, nombre);
                } finally {
                    mutex.unlock();
                }
            } else {
                comenzarCruce(sentido, llegada, events, nombre);
            }
        }

        void salir(Direccion sentido, List<EventLog> events, String nombre) {
            if (sentido == Direccion.NORTE) {
                realNorte.decrementAndGet();
            } else {
                realSur.decrementAndGet();
            }
            atendidos.incrementAndGet();
            if (!usarControl) {
                events.add(EventLog.of("Vehículo " + nombre + " termina de cruzar"));
                return;
            }
            mutex.lock();
            try {
                if (sentido == Direccion.NORTE) {
                    vehiculosNorte--;
                } else {
                    vehiculosSur--;
                }
                events.add(EventLog.of("Vehículo " + nombre + " termina de cruzar"));
                if (vehiculosNorte + vehiculosSur == 0) {
                    direccionActual = Direccion.NINGUNA;
                    consecutivos = 0;
                    turnoPreferente = justo && esperandoDe(sentido.opuesta()) > 0
                            ? sentido.opuesta() : Direccion.NINGUNA;
                    events.add(EventLog.of("El puente queda libre"
                            + (turnoPreferente == Direccion.NINGUNA ? "" : "; turno para " + turnoPreferente)));
                    puedeEntrar.signalAll();
                }
            } finally {
                mutex.unlock();
            }
        }

        private boolean puedeEntrar(Direccion sentido) {
            if (direccionActual == sentido) {
                boolean cedeTurno = justo && consecutivos >= limiteConsecutivos
                        && esperandoDe(sentido.opuesta()) > 0;
                return !cedeTurno;
            }
            if (direccionActual == Direccion.NINGUNA) {
                return turnoPreferente == Direccion.NINGUNA || turnoPreferente == sentido;
            }
            return false;
        }

        private int esperandoDe(Direccion sentido) {
            return sentido == Direccion.NORTE ? esperandoNorte : esperandoSur;
        }

        private void registrarEspera(Direccion sentido, int delta) {
            if (sentido == Direccion.NORTE) {
                esperandoNorte += delta;
            } else {
                esperandoSur += delta;
            }
        }

        private void comenzarCruce(Direccion sentido, long llegadaNanos, List<EventLog> events, String nombre) {
            long espera = Sim.elapsedMs(llegadaNanos);
            AtomicInteger propio = sentido == Direccion.NORTE ? realNorte : realSur;
            AtomicInteger opuesto = sentido == Direccion.NORTE ? realSur : realNorte;
            int actual = propio.incrementAndGet();
            (sentido == Direccion.NORTE ? maxNorte : maxSur).accumulateAndGet(actual, Math::max);
            (sentido == Direccion.NORTE ? esperaMaxNorte : esperaMaxSur).accumulateAndGet(espera, Math::max);
            if (opuesto.get() > 0) {
                conflictos.incrementAndGet();
                events.add(EventLog.of("CONFLICTO: " + nombre + " cruza mientras hay vehículos en sentido contrario"));
            }
            if (sentido == Direccion.NORTE) {
                norteCruzaron.incrementAndGet();
            } else {
                norteAntesPrimerSur.compareAndSet(-1, norteCruzaron.get());
            }
            events.add(EventLog.of("Vehículo " + nombre + " cruza hacia el " + sentido));
        }

        int norteAntesDelPrimerSur() {
            int valor = norteAntesPrimerSur.get();
            return valor < 0 ? norteCruzaron.get() : valor;
        }
    }
}