package ec.edu.unl.sd.practica3.api;

import ec.edu.unl.sd.practica3.cajero.BankService;
import ec.edu.unl.sd.practica3.common.SimulationResult;
import ec.edu.unl.sd.practica3.estacionamiento.ParkingService;
import ec.edu.unl.sd.practica3.impresion.PrintCenterService;
import ec.edu.unl.sd.practica3.productorconsumidor.ProducerConsumerService;
import ec.edu.unl.sd.practica3.puente.BridgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ejercicios")
public class EjerciciosController {

    private final ParkingService parkingService;
    private final BankService bankService;
    private final ProducerConsumerService producerConsumerService;
    private final BridgeService bridgeService;
    private final PrintCenterService printCenterService;

    public EjerciciosController(
            ParkingService parkingService,
            BankService bankService,
            ProducerConsumerService producerConsumerService,
            BridgeService bridgeService,
            PrintCenterService printCenterService) {
        this.parkingService = parkingService;
        this.bankService = bankService;
        this.producerConsumerService = producerConsumerService;
        this.bridgeService = bridgeService;
        this.printCenterService = printCenterService;
    }

    @GetMapping("/1/estacionamiento")
    public SimulationResult estacionamiento(
            @RequestParam(defaultValue = "10") int vehiculos,
            @RequestParam(defaultValue = "4") int permisosSemaforo,
            @RequestParam(defaultValue = "20") int estadiaMs) {
        return parkingService.run(vehiculos, permisosSemaforo, estadiaMs);
    }

    @GetMapping("/2/cajero")
    public SimulationResult cajero(
            @RequestParam(defaultValue = "1000") int saldoInicial,
            @RequestParam(defaultValue = "300") int retiroProceso1,
            @RequestParam(defaultValue = "250") int retiroProceso2,
            @RequestParam(defaultValue = "200") int depositoProceso3,
            @RequestParam(defaultValue = "true") boolean usarMutex,
            @RequestParam(defaultValue = "50") int repeticiones) {
        return bankService.run(
                saldoInicial,
                retiroProceso1,
                retiroProceso2,
                depositoProceso3,
                usarMutex,
                repeticiones);
    }

    public SimulationResult cajero() {
        return bankService.run();
    }

    @GetMapping("/3/productor-consumidor")
    public SimulationResult productorConsumidor(
            @RequestParam(defaultValue = "5") int espacios,
            @RequestParam(defaultValue = "0") int elementos) {
        return producerConsumerService.run(espacios, elementos);
    }

        @GetMapping("/4/puente")
    public SimulationResult puente(
            @RequestParam(defaultValue = "6") int vehiculosNorte,
            @RequestParam(defaultValue = "6") int vehiculosSur,
            @RequestParam(defaultValue = "40") int cruceMs,
            @RequestParam(defaultValue = "10") int llegadaMs,
            @RequestParam(defaultValue = "true") boolean justo,
            @RequestParam(defaultValue = "3") int limiteConsecutivos,
            @RequestParam(defaultValue = "true") boolean control) {
        return bridgeService.run(
                vehiculosNorte, vehiculosSur, cruceMs, llegadaMs, justo, limiteConsecutivos, control);
    }

    @GetMapping("/5/impresion")
    public SimulationResult impresion(
            @RequestParam(defaultValue = "3") int documentos,
            @RequestParam(defaultValue = "NINGUNO") PrintCenterService.SignalOmitido omitir,
            @RequestParam(defaultValue = "250") long timeoutMs,
            @RequestParam(defaultValue = "20") long trabajoMs) {
        return printCenterService.run(documentos, omitir, timeoutMs, trabajoMs);
    }
}
