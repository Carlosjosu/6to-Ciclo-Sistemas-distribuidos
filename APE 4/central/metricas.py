import csv
import math
import os
import threading


class Metricas:
    campos = ["id", "modo", "worker", "decision", "t_decision", "t_total",
              "carga_en_decision", "distancia"]

    def __init__(self, modo, directorio):
        self.modo = modo
        self.directorio = directorio
        self.registros = []
        self._lock = threading.Lock()

    def agregar(self, registro):
        with self._lock:
            self.registros.append(registro)

    def guardar(self):
        os.makedirs(self.directorio, exist_ok=True)
        ruta = os.path.join(self.directorio, f"resultados_{self.modo}.csv")
        with open(ruta, "w", newline="", encoding="utf-8") as archivo:
            escritor = csv.DictWriter(archivo, fieldnames=self.campos)
            escritor.writeheader()
            escritor.writerows(self.registros)

    def resumen(self):
        self.guardar()
        if not self.registros:
            print("[GATEWAY] Sin métricas", flush=True)
            return
        tiempos = sorted(float(item["t_total"]) for item in self.registros)
        p95 = tiempos[min(len(tiempos) - 1, math.ceil(len(tiempos) * 0.95) - 1)]
        por_worker = {}
        for item in self.registros:
            por_worker[item["worker"]] = por_worker.get(item["worker"], 0) + 1
        ia = sum(item["decision"] == "ia" for item in self.registros)
        print(f"[GATEWAY] total={sum(tiempos):.3f}s medio={sum(tiempos)/len(tiempos):.3f}s "
              f"p95={p95:.3f}s workers={por_worker} IA={ia/len(tiempos)*100:.1f}% "
              f"fallback={(len(tiempos)-ia)/len(tiempos)*100:.1f}%", flush=True)
