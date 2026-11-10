package ec.edu.unl.sd.practica3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sd.practica3.api.EjerciciosController;
import ec.edu.unl.sd.practica3.estacionamiento.ParkingService;
import ec.edu.unl.sd.practica3.impresion.PrintCenterService.SignalOmitido;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class EjerciciosTest {

    @Autowired
    private EjerciciosController controller;

    @Test
    void todosLosEndpointsEstanDisponibles() {
        assertThat(controller.estacionamiento(10, 4, 1).verified()).isTrue();
        assertThat(controller.cajero().verified()).isTrue();
        assertThat(controller.productorConsumidor(5, 0).verified()).isTrue();
        assertThat(controller.puente(6, 6, 40, 10, true, 3, true).verified()).isTrue();      
        assertThat(controller.impresion(3, SignalOmitido.NINGUNO, 100, 0).verified()).isTrue();
    }

    @Test
    void estacionamientoNuncaSuperaCuatro() {
        var result = controller.estacionamiento(10, 4, 5);

        assertThat(result.verified()).isTrue();
        assertThat(result.metrics()).containsEntry("maximoSimultaneos", 4);
    }

    @Test
    void estacionamientoConSemaforoEn10ViolaLaCapacidad() {
        var result = controller.estacionamiento(10, 10, 20);

        assertThat(result.verified()).isFalse();
        assertThat(result.metrics()).containsEntry("maximoSimultaneos", 10);
    }

    @Test
    void cajeroAceptaParametrosDesdeLaSolicitud() {
        var result = controller.cajero(2000, 400, 300, 100, true, 3);

        assertThat(result.verified()).isTrue();
        assertThat(result.metrics())
                .containsEntry("saldoInicial", 2000)
                .containsEntry("saldoEsperado", 1400)
                .containsEntry("ejecucionesCorrectas", 3);
    }

    @Test
    void cajeroSinMutexDetectaCondicionDeCarrera() {
        var result = controller.cajero(1000, 300, 250, 200, false, 50);

        assertThat(result.verified()).isFalse();
    }

    @Test
    void estacionamientoRechazaParametrosNoPositivos() {
        assertThatThrownBy(() -> controller.estacionamiento(0, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productorConsumidorRespetaCapacidadYConsumeTodo() {
        var result = controller.productorConsumidor(5, 0);

        assertThat(result.verified()).isTrue();
        assertThat(result.metrics())
                .containsEntry("capacidad", 5)
                .containsEntry("maximoAlmacen", 5)
                .containsEntry("piezasProducidas", 10)
                .containsEntry("piezasConsumidas", 10)
                .containsEntry("piezasRestantes", 0);
        assertThat(result.events()).hasSize(30);
    }

    @Test
    void productorConsumidorDetectaEspaciosEnCeroSinBloquear() {
        var result = controller.productorConsumidor(0, 0);

        assertThat(result.verified()).isFalse();
        assertThat(result.summary()).contains("quedaría bloqueada");
        assertThat(result.events()).anyMatch(event -> event.message().contains("ninguno puede producir"));
    }

    @Test
    void productorConsumidorDetectaElementosFalsosSinExtraerDeColaVacia() {
        var result = controller.productorConsumidor(5, 5);

        assertThat(result.verified()).isFalse();
        assertThat(result.summary()).contains("cola vacía");
        assertThat(result.events()).anyMatch(event -> event.message().contains("extraer una pieza inexistente"));
    }

        @Test
    void puenteNuncaTieneSentidosOpuestosSimultaneos() {
        var result = controller.puente(6, 6, 40, 10, true, 3, true);

        assertThat(result.verified()).isTrue();
        assertThat(result.metrics())
                .containsEntry("conflictosDetectados", 0)
                .containsEntry("vehiculosAtendidos", "12 / 12");
    }

    @Test
    void puenteSinLimitePermiteMonopolioYConLimiteNo() {
        var base = controller.puente(6, 6, 40, 10, false, 3, true);
        var justo = controller.puente(6, 6, 40, 10, true, 3, true);

        assertThat(base.verified()).isTrue();
        assertThat(base.metrics()).containsEntry("norteAntesDelPrimerSur", 6);
        assertThat((int) justo.metrics().get("norteAntesDelPrimerSur")).isLessThan(6);
    }

    @Test
    void puenteSinControlProduceConflictos() {
        var result = controller.puente(6, 6, 40, 10, true, 3, false);

        assertThat(result.verified()).isFalse();
        assertThat((int) result.metrics().get("conflictosDetectados")).isPositive();
    }

    @Test
    void puenteRechazaParametrosInvalidos() {
        assertThatThrownBy(() -> controller.puente(0, 0, 40, 10, true, 3, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.puente(2, 2, 0, 10, true, 3, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void impresionRespetaElOrden() {
        var result = controller.impresion(3, SignalOmitido.NINGUNO, 100, 1);

        assertThat(result.verified()).isTrue();
        assertThat(result.metrics())
                .containsEntry("documentos", 3)
                .containsEntry("signalOmitido", "NINGUNO")
                .containsEntry("etapasEsperadas", 9)
                .containsEntry("etapasEjecutadas", 9)
                .containsEntry("hilosBloqueados", 0)
                .containsEntry("ordenObservado", "D1 R1 I1 D2 R2 I2 D3 R3 I3");
    }

    @Test
    void omitirUnSignalBloqueaElCiclo() {
        for (SignalOmitido signal : List.of(
                SignalOmitido.REVISION,
                SignalOmitido.IMPRESION,
                SignalOmitido.DISENO)) {
            var result = controller.impresion(3, signal, 50, 1);

            assertThat(result.verified()).as("signal omitido: %s", signal).isFalse();
            assertThat(result.metrics())
                    .as("signal omitido: %s", signal)
                    .containsEntry("signalOmitido", signal.name())
                    .containsEntry("hilosBloqueados", 3);
            assertThat((int) result.metrics().get("etapasEjecutadas"))
                    .as("signal omitido: %s", signal)
                    .isLessThan(9);
        }
    }

    @Test
    void impresionRechazaParametrosInvalidos() {
        assertThatThrownBy(() -> controller.impresion(0, SignalOmitido.NINGUNO, 50, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.impresion(3, SignalOmitido.NINGUNO, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.impresion(3, SignalOmitido.NINGUNO, 50, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
