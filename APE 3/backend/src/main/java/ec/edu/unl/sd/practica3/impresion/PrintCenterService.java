package ec.edu.unl.sd.practica3.impresion;

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
public class PrintCenterService {

    public enum SignalOmitido {
        NINGUNO,
        REVISION,
        IMPRESION,
        DISENO
    }

    private static final int DEFAULT_DOCUMENTS = 3;
    private static final long DEFAULT_TIMEOUT_MS = 250;
    private static final long DEFAULT_WORK_MS = 20;

    public SimulationResult run() {
        return run(DEFAULT_DOCUMENTS, SignalOmitido.NINGUNO, DEFAULT_TIMEOUT_MS, DEFAULT_WORK_MS);
    }

    public SimulationResult run(
            int documentos,
            SignalOmitido omitir,
            long timeoutMs,
            long trabajoMs) {
        if (documentos <= 0) {
            throw new IllegalArgumentException("documentos debe ser mayor que cero.");
        }
        if (omitir == null) {
            throw new IllegalArgumentException("omitir debe ser una señal válida.");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs debe ser mayor que cero.");
        }
        if (trabajoMs < 0) {
            throw new IllegalArgumentException("trabajoMs no puede ser negativo.");
        }

        Semaphore turnoDiseno = new Semaphore(1, true);
        Semaphore turnoRevision = new Semaphore(0, true);
        Semaphore turnoImpresion = new Semaphore(0, true);
        List<String> orden = Collections.synchronizedList(new ArrayList<>());
        List<EventLog> events = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger blocked = new AtomicInteger();

        List<Thread> threads = List.of(
                Sim.start("diseno", () -> processStage(
                        "Diseño", "D", documentos, turnoDiseno, turnoRevision,
                        omitir == SignalOmitido.REVISION, timeoutMs, trabajoMs,
                        orden, events, blocked)),
                Sim.start("revision", () -> processStage(
                        "Revisión", "R", documentos, turnoRevision, turnoImpresion,
                        omitir == SignalOmitido.IMPRESION, timeoutMs, trabajoMs,
                        orden, events, blocked)),
                Sim.start("impresion", () -> processStage(
                        "Impresión", "I", documentos, turnoImpresion, turnoDiseno,
                        omitir == SignalOmitido.DISENO, timeoutMs, trabajoMs,
                        orden, events, blocked)));

        Sim.joinAll(threads);

        List<String> expectedOrder = new ArrayList<>();
        for (int document = 1; document <= documentos; document++) {
            expectedOrder.add("D" + document);
            expectedOrder.add("R" + document);
            expectedOrder.add("I" + document);
        }
        boolean verified = orden.equals(expectedOrder) && blocked.get() == 0;
        String summary = verified
                ? "Diseño, revisión e impresión completaron los documentos en el orden establecido."
                : "La secuencia se interrumpió: uno o más hilos agotaron el tiempo de espera por una señal.";

        return new SimulationResult(
                verified,
                summary,
                Sim.metrics(
                        "documentos", documentos,
                        "signalOmitido", omitir.name(),
                        "etapasEsperadas", expectedOrder.size(),
                        "etapasEjecutadas", orden.size(),
                        "hilosBloqueados", blocked.get(),
                        "ordenObservado", String.join(" ", orden)),
                List.copyOf(events));
    }

    private static void processStage(
            String stageName,
            String stageCode,
            int documents,
            Semaphore ownTurn,
            Semaphore nextTurn,
            boolean omitSignal,
            long timeoutMs,
            long workMs,
            List<String> order,
            List<EventLog> events,
            AtomicInteger blocked) {
        for (int document = 1; document <= documents; document++) {
            if (!Sim.tryAcquire(ownTurn, timeoutMs)) {
                blocked.incrementAndGet();
                events.add(EventLog.of(stageName + " agotó el tiempo esperando el documento " + document));
                return;
            }

            order.add(stageCode + document);
            events.add(EventLog.of(stageName + " procesó el documento " + document));
            Sim.sleep(workMs);
            if (omitSignal) {
                events.add(EventLog.of(stageName + " omitió la señal para la siguiente etapa"));
            } else {
                nextTurn.release();
            }
        }
    }
}
