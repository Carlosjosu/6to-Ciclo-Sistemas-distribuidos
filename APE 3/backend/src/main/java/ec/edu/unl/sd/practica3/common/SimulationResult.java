package ec.edu.unl.sd.practica3.common;

import java.util.List;
import java.util.Map;

public record SimulationResult(
        boolean verified,
        String summary,
        Map<String, Object> metrics,
        List<EventLog> events) {

    public static SimulationResult pending(String exercise) {
        return new SimulationResult(
                true,
                exercise + " con espacio de implementación asignado.",
                Map.of("status", "BASE"),
                List.of(EventLog.of("Estructura inicial del ejercicio.")));
    }
}
