import math
import socket
import threading
import time

from . import agente_ia, config, reglas
from .metricas import Metricas
from .protocolo import enviar_json, recibir_json


class Gateway:
    def __init__(self, modo, metricas):
        self.modo = modo
        self.metricas = metricas
        self.indice_rr = 0
        self.lock = threading.Lock()
        self.estado_workers = {
            worker.id: {"id": worker.id, "host": worker.host, "port": worker.port,
                        "x": worker.x, "y": worker.y, "carga": 0, "activo": True}
            for worker in config.WORKERS
        }

    def _candidatos(self, pedido):
        with self.lock:
            resultado = []
            for worker in self.estado_workers.values():
                if worker["activo"]:
                    copia = dict(worker)
                    copia["distancia_km"] = math.hypot(
                        pedido["distancia_km"] - worker["x"], pedido["distancia_km"] - worker["y"]
                    )
                    resultado.append(copia)
            return resultado

    def _decidir(self, pedido, candidatos):
        if self.modo == "ia":
            return agente_ia.decidir(pedido, candidatos)
        with self.lock:
            indice = self.indice_rr
            self.indice_rr += 1
        if self.modo == "round_robin":
            worker = reglas.round_robin(candidatos, indice)
        elif self.modo == "mas_cercano":
            worker = reglas.mas_cercano(candidatos)
        else:
            worker = reglas.menor_carga(candidatos)
        return worker["id"], "fallback", 0.0

    def _marcar_carga(self, worker_id, delta):
        with self.lock:
            worker = self.estado_workers[worker_id]
            worker["carga"] = max(0, worker["carga"] + delta)

    def _marcar_caido(self, worker_id):
        with self.lock:
            self.estado_workers[worker_id]["activo"] = False
        print(f"[GATEWAY] {worker_id} sin respuesta; marcado inactivo", flush=True)

    def _enviar(self, worker, pedido):
        with socket.create_connection(
            (worker["host"], worker["port"]), timeout=config.WORKER_TIMEOUT
        ) as sock:
            sock.settimeout(config.WORKER_TIMEOUT)
            enviar_json(sock, pedido)
            return recibir_json(sock)

    def distribuir(self, pedido):
        inicio = time.perf_counter()
        intentados = set()
        for _ in range(2):
            candidatos = [worker for worker in self._candidatos(pedido) if worker["id"] not in intentados]
            if not candidatos:
                break
            worker_id, origen, t_decision = self._decidir(pedido, candidatos)
            worker = next(worker for worker in candidatos if worker["id"] == worker_id)
            intentados.add(worker_id)
            self._marcar_carga(worker_id, 1)
            try:
                ack = self._enviar(worker, pedido)
                if ack.get("estado") != "completado":
                    raise ValueError("ACK inválido")
                with self.lock:
                    self.estado_workers[worker_id]["carga"] = int(ack["carga"])
                print(f"[GATEWAY] {pedido['id']} -> {worker_id} completado", flush=True)
                self.metricas.agregar({
                    "id": pedido["id"], "modo": self.modo, "worker": worker_id,
                    "decision": origen, "t_decision": f"{t_decision:.6f}",
                    "t_total": f"{time.perf_counter() - inicio:.6f}",
                    "carga_en_decision": worker["carga"], "distancia": pedido["distancia_km"],
                })
                return ack
            except (OSError, ValueError, KeyError, TypeError) as exc:
                self._marcar_caido(worker_id)
                self._marcar_carga(worker_id, -1)
                print(f"[GATEWAY] reintento para {pedido['id']}: {exc}", flush=True)
        print(f"[GATEWAY] pedido {pedido['id']} no entregado", flush=True)
        return None

    def heartbeat(self, detener):
        while not detener.wait(5):
            for worker in self.estado_workers.values():
                if worker["activo"]:
                    continue
                try:
                    with socket.create_connection((worker["host"], worker["port"]), timeout=1) as sock:
                        sock.settimeout(1)
                        enviar_json(sock, {"tipo": "heartbeat"})
                        respuesta = recibir_json(sock)
                        if respuesta.get("tipo") != "heartbeat_ack":
                            raise ValueError("respuesta de heartbeat inválida")
                    with self.lock:
                        worker["activo"] = True
                        worker["carga"] = 0
                    print(f"[GATEWAY] {worker['id']} reactivado", flush=True)
                except OSError:
                    continue
