import os
from dataclasses import dataclass


@dataclass(frozen=True)
class WorkerConfig:
    id: str
    host: str
    port: int
    x: float
    y: float


def _workers() -> list[WorkerConfig]:
    value = os.getenv("WORKERS", "W1=localhost:7001:1:1,W2=localhost:7002:8:7")
    workers = []
    for item in value.split(","):
        if not item.strip():
            continue
        try:
            worker_id, address = item.split("=", 1)
            host, port, x, y = address.split(":")
            workers.append(WorkerConfig(worker_id, host, int(port), float(x), float(y)))
        except ValueError as exc:
            raise ValueError(
                "WORKERS debe tener el formato ID=host:puerto:x:y,ID=host:puerto:x:y"
            ) from exc
    if not workers:
        raise ValueError("WORKERS debe definir al menos un worker")
    return workers


IA_URL = os.getenv("IA_URL", "http://ip_servidor:3000").rstrip("/")
IA_API_KEY = os.getenv("IA_API_KEY", "")
IA_MODEL = os.getenv("IA_MODEL", "qwen3-coder:30b")
IA_API_STYLE = os.getenv("IA_API_STYLE", "openai").lower()
IA_TIMEOUT = float(os.getenv("IA_TIMEOUT", "20"))
WORKER_TIMEOUT = float(os.getenv("WORKER_TIMEOUT", "10"))
GATEWAY_PORT = int(os.getenv("GATEWAY_PORT", "6000"))
MODO = os.getenv("MODO", "ia").lower()
RESULTADOS_DIR = os.getenv("RESULTADOS_DIR", "resultados")
WORKERS = _workers()


def validar_config(modo: str) -> None:
    if modo not in {"ia", "round_robin", "menor_carga", "mas_cercano"}:
        raise ValueError(f"Modo no válido: {modo}")
    if IA_API_STYLE not in {"openai", "ollama"}:
        raise ValueError("IA_API_STYLE debe ser openai u ollama")
    if modo == "ia" and not IA_API_KEY:
        raise ValueError("IA_API_KEY es obligatoria en modo ia")
