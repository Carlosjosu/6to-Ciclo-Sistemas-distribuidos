import argparse
import os
import random
import socket
import threading
import time

try:
    from central.protocolo import enviar_json, recibir_json
except ImportError:
    import sys
    sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
    from central.protocolo import enviar_json, recibir_json


class Worker:
    def __init__(self, worker_id, host, port):
        self.worker_id = worker_id
        self.host = host
        self.port = port
        self.en_curso = 0
        self.lock = threading.Lock()

    def atender(self, conn):
        try:
            with conn:
                pedido = recibir_json(conn)
                if pedido.get("tipo") == "heartbeat":
                    enviar_json(conn, {"tipo": "heartbeat_ack", "worker": self.worker_id})
                    return
                with self.lock:
                    self.en_curso += 1
                    carga = self.en_curso
                distancia = float(pedido["distancia_km"])
                inicio = time.perf_counter()
                time.sleep(distancia * 0.5 + random.uniform(0.2, 1.0))
                with self.lock:
                    self.en_curso -= 1
                    carga_final = self.en_curso
                ack = {"id": pedido["id"], "worker": self.worker_id, "estado": "completado",
                       "carga": carga_final, "t_proceso": time.perf_counter() - inicio}
                enviar_json(conn, ack)
                print(f"[WORKER {self.worker_id}] {pedido['id']} completado (carga previa {carga})",
                      flush=True)
        except (OSError, ValueError, KeyError, TypeError) as exc:
            print(f"[WORKER {self.worker_id}] error: {exc}", flush=True)

    def servir(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as servidor:
            servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            servidor.bind((self.host, self.port))
            servidor.listen()
            print(f"[WORKER {self.worker_id}] activo en {self.host}:{self.port}", flush=True)
            try:
                while True:
                    conn, _ = servidor.accept()
                    threading.Thread(target=self.atender, args=(conn,), daemon=True).start()
            except KeyboardInterrupt:
                print(f"[WORKER {self.worker_id}] detenido", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--id", default=os.getenv("WORKER_ID", "W1"))
    parser.add_argument("--host", default=os.getenv("WORKER_HOST", "0.0.0.0"))
    parser.add_argument("--port", type=int, default=int(os.getenv("WORKER_PORT", "7001")))
    parser.add_argument("--x", type=float, default=float(os.getenv("WORKER_X", "1")))
    parser.add_argument("--y", type=float, default=float(os.getenv("WORKER_Y", "1")))
    args = parser.parse_args()
    Worker(args.id, args.host, args.port).servir()


if __name__ == "__main__":
    main()
