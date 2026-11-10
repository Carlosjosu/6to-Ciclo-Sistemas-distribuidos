import argparse
import multiprocessing
import queue
import threading

from . import config
from .generador import generador_main
from .gateway import Gateway
from .metricas import Metricas
from .procesador import procesador_main


def main():
    parser = argparse.ArgumentParser(description="Central de asignación de pedidos")
    parser.add_argument("--modo", choices=("ia", "round_robin", "menor_carga", "mas_cercano"),
                        default=config.MODO)
    parser.add_argument("--pedidos", type=int, default=30)
    parser.add_argument("--generadores", type=int, default=3)
    parser.add_argument("--intervalo", type=float, default=1.0)
    args = parser.parse_args()
    if args.pedidos < 0 or args.generadores < 1:
        raise ValueError("pedidos debe ser >= 0 y generadores debe ser >= 1")
    config.validar_config(args.modo)

    pedidos_por_generador = [args.pedidos // args.generadores] * args.generadores
    for index in range(args.pedidos % args.generadores):
        pedidos_por_generador[index] += 1
    cola_pedidos = multiprocessing.Queue()
    cola_listos = multiprocessing.Queue()
    procesos = []
    inicio = 0
    for index, cantidad in enumerate(pedidos_por_generador):
        proceso = multiprocessing.Process(
            target=generador_main,
            args=(cola_pedidos, cantidad, args.intervalo, 42, inicio, False),
        )
        procesos.append(proceso)
        inicio += cantidad
        proceso.start()
    procesador = multiprocessing.Process(target=procesador_main, args=(cola_pedidos, cola_listos))
    procesador.start()

    metricas = Metricas(args.modo, config.RESULTADOS_DIR)
    gateway = Gateway(args.modo, metricas)
    detener = threading.Event()
    heartbeat = threading.Thread(target=gateway.heartbeat, args=(detener,), daemon=True)
    heartbeat.start()
    trabajadores = []

    def consumir():
        while True:
            try:
                pedido = cola_listos.get(timeout=1)
            except queue.Empty:
                continue
            if pedido is None:
                return
            gateway.distribuir(pedido)

    for _ in range(4):
        hilo = threading.Thread(target=consumir)
        hilo.start()
        trabajadores.append(hilo)
    try:
        for proceso in procesos:
            proceso.join()
        cola_pedidos.put(None)
        procesador.join()
        for _ in trabajadores:
            cola_listos.put(None)
        for hilo in trabajadores:
            hilo.join()
        metricas.resumen()
    except KeyboardInterrupt:
        print("\n[GATEWAY] deteniendo ejecución...", flush=True)
        for proceso in procesos + [procesador]:
            if proceso.is_alive():
                proceso.terminate()
        for hilo in trabajadores:
            hilo.join(timeout=1)
    finally:
        detener.set()
        heartbeat.join(timeout=1)


if __name__ == "__main__":
    main()
