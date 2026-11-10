import random
import time


def generador_main(
    cola_pedidos, n: int, intervalo: float, semilla: int, inicio: int = 0,
    finalizar: bool = True
) -> None:
    rng = random.Random(semilla)
    for offset in range(n):
        pedido = {
            "id": f"P-{inicio + offset + 1:04d}",
            "cliente": f"Cliente-{inicio + offset + 1}",
            "distancia_km": round(rng.uniform(0.5, 10.0), 2),
            "prioridad": rng.choice(("alta", "media", "baja")),
            "peso_kg": round(rng.uniform(0.5, 10.0), 2),
            "timestamp": time.time(),
        }
        print(f"[GEN] {pedido}", flush=True)
        cola_pedidos.put(pedido)
        if offset + 1 < n:
            time.sleep(intervalo)
    if finalizar:
        cola_pedidos.put(None)
