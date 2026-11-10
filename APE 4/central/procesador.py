def procesador_main(cola_pedidos, cola_listos) -> None:
    while True:
        pedido = cola_pedidos.get()
        if pedido is None:
            cola_listos.put(None)
            return
        requeridos = {"id", "cliente", "distancia_km", "prioridad", "peso_kg", "timestamp"}
        if not isinstance(pedido, dict) or not requeridos.issubset(pedido):
            print(f"[PROC] Pedido inválido descartado: {pedido}", flush=True)
            continue
        pedido = dict(pedido)
        pedido["estado"] = "pendiente"
        print(f"[PROC] {pedido['id']} listo", flush=True)
        cola_listos.put(pedido)
