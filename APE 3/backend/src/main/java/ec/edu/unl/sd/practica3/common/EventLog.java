package ec.edu.unl.sd.practica3.common;

import java.time.Instant;

public record EventLog(Instant timestamp, String thread, String message) {

    public static EventLog of(String message) {
        return new EventLog(Instant.now(), Thread.currentThread().getName(), message);
    }
}
