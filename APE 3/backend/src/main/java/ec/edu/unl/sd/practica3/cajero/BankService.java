package ec.edu.unl.sd.practica3.cajero;

import ec.edu.unl.sd.practica3.common.EventLog;
import ec.edu.unl.sd.practica3.common.Sim;
import ec.edu.unl.sd.practica3.common.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BankService {

    private static final int SALDO_INICIAL = 1_000;
    private static final int RETIRO_PROCESO_1 = 300;
    private static final int RETIRO_PROCESO_2 = 250;
    private static final int DEPOSITO_PROCESO_3 = 200;
    private static final int REPETICIONES_POR_DEFECTO = 50;
    private static final long PAUSA_NANOS = 300_000;

    public SimulationResult run() {
        return run(SALDO_INICIAL, RETIRO_PROCESO_1, RETIRO_PROCESO_2,
                DEPOSITO_PROCESO_3, true, REPETICIONES_POR_DEFECTO);
    }

    public SimulationResult run(boolean usarMutex, int repeticiones) {
        return run(SALDO_INICIAL, RETIRO_PROCESO_1, RETIRO_PROCESO_2,
                DEPOSITO_PROCESO_3, usarMutex, repeticiones);
    }

    public SimulationResult run(
            int saldoInicial,
            int retiroProceso1,
            int retiroProceso2,
            int depositoProceso3,
            boolean usarMutex,
            int repeticiones) {
        validarParametros(saldoInicial, retiroProceso1, retiroProceso2, depositoProceso3, repeticiones);
        int saldoEsperado = saldoInicial - retiroProceso1 - retiroProceso2 + depositoProceso3;
        if (repeticiones <= 0) {
            throw new IllegalArgumentException("repeticiones debe ser mayor que cero.");
        }

        int correctas = 0;
        Map<Integer, Integer> saldosFinales = new TreeMap<>();
        List<EventLog> firstRunEvents = new ArrayList<>();
        long startNanos = System.nanoTime();

        for (int repeticion = 1; repeticion <= repeticiones; repeticion++) {
            Cuenta cuenta = new Cuenta(saldoInicial);
            List<EventLog> events = Collections.synchronizedList(new ArrayList<>());
            boolean registrarEventos = repeticion == 1;

            List<Thread> processes = List.of(
                    Sim.start("proceso-1", () -> retirar(
                            cuenta, 1, retiroProceso1, usarMutex, events, registrarEventos)),
                    Sim.start("proceso-2", () -> retirar(
                            cuenta, 2, retiroProceso2, usarMutex, events, registrarEventos)),
                    Sim.start("proceso-3", () -> depositar(
                            cuenta, 3, depositoProceso3, usarMutex, events, registrarEventos)));

            Sim.joinAll(processes);

            int saldoFinal = cuenta.saldo;
            saldosFinales.merge(saldoFinal, 1, Integer::sum);
            if (saldoFinal == saldoEsperado) {
                correctas++;
            }
            if (registrarEventos) {
                events.add(EventLog.of("Operaciones terminadas. Saldo final: $" + saldoFinal));
                firstRunEvents.addAll(events);
            }
        }

        return new SimulationResult(
                correctas == repeticiones,
                usarMutex
                        ? "El mutex protegió correctamente el saldo en todas las ejecuciones."
                        : "Sin mutex se observan los efectos de la condición de carrera.",
                Sim.metrics(
                        "usaMutex", usarMutex,
                        "repeticiones", repeticiones,
                        "saldoInicial", saldoInicial,
                        "retiroProceso1", retiroProceso1,
                        "retiroProceso2", retiroProceso2,
                        "depositoProceso3", depositoProceso3,
                        "saldoEsperado", saldoEsperado,
                        "ejecucionesCorrectas", correctas,
                        "saldosFinales", saldosFinales.toString(),
                        "tiempoConcurrenteMs", Sim.elapsedMs(startNanos)),
                List.copyOf(firstRunEvents));
    }

    private static void validarParametros(
            int saldoInicial,
            int retiroProceso1,
            int retiroProceso2,
            int depositoProceso3,
            int repeticiones) {
        if (saldoInicial < 0 || retiroProceso1 < 0 || retiroProceso2 < 0
                || depositoProceso3 < 0) {
            throw new IllegalArgumentException("Los montos no pueden ser negativos.");
        }
        if (repeticiones <= 0) {
            throw new IllegalArgumentException("repeticiones debe ser mayor que cero.");
        }
    }

    private void retirar(
            Cuenta cuenta,
            int id,
            int cantidad,
            boolean usarMutex,
            List<EventLog> events,
            boolean registrarEventos) {
        if (usarMutex) {
            cuenta.mutexSaldo.lock();
        }
        try {
            addEvent(events, registrarEventos, "Proceso " + id + " intenta retirar $" + cantidad);
            int leido = cuenta.saldo;
            LockSupport.parkNanos(PAUSA_NANOS);
            if (leido >= cantidad) {
                cuenta.saldo = leido - cantidad;
                addEvent(events, registrarEventos, "Retiro realizado por el proceso " + id);
                addEvent(events, registrarEventos, "Saldo actual: $" + cuenta.saldo);
            } else {
                addEvent(events, registrarEventos, "Saldo insuficiente para el proceso " + id);
            }
        } finally {
            if (usarMutex) {
                cuenta.mutexSaldo.unlock();
            }
        }
    }

    private void depositar(
            Cuenta cuenta,
            int id,
            int cantidad,
            boolean usarMutex,
            List<EventLog> events,
            boolean registrarEventos) {
        if (usarMutex) {
            cuenta.mutexSaldo.lock();
        }
        try {
            addEvent(events, registrarEventos, "Proceso " + id + " deposita $" + cantidad);
            int leido = cuenta.saldo;
            LockSupport.parkNanos(PAUSA_NANOS);
            cuenta.saldo = leido + cantidad;
            addEvent(events, registrarEventos, "Saldo actual: $" + cuenta.saldo);
        } finally {
            if (usarMutex) {
                cuenta.mutexSaldo.unlock();
            }
        }
    }

    private static void addEvent(List<EventLog> events, boolean enabled, String message) {
        if (enabled) {
            events.add(EventLog.of(message));
        }
    }

    private static final class Cuenta {
        private int saldo;
        private final Lock mutexSaldo = new ReentrantLock();

        private Cuenta(int saldoInicial) {
            saldo = saldoInicial;
        }
    }
}
