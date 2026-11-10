import json
import threading
import time

import requests

from . import config, reglas

_semaforo = threading.Semaphore(2)


def _extraer_json(texto: str) -> dict:
    inicio, fin = texto.find("{"), texto.rfind("}")
    if inicio < 0 or fin < inicio:
        raise ValueError("la IA no devolvió un objeto JSON")
    resultado = json.loads(texto[inicio:fin + 1])
    if not isinstance(resultado, dict):
        raise ValueError("la respuesta de IA no es un objeto")
    return resultado


def decidir(pedido, workers):
    inicio = time.perf_counter()
    activos = [worker for worker in workers if worker.get("activo")]
    fallback = reglas.menor_carga(workers)
    if not config.IA_API_KEY:
        print("[IA] auth fallida: falta IA_API_KEY; usando fallback", flush=True)
        return fallback["id"], "fallback", time.perf_counter() - inicio

    prompt = (
        "Eres un despachador de pedidos de delivery. Elige UN worker para el pedido. "
        "Criterios: menor tiempo total estimado. Penaliza la carga alta y la distancia grande; "
        "si el pedido es de prioridad alta, da más peso a la distancia.\n"
        f"Pedido: {json.dumps(pedido, ensure_ascii=False)}\n"
        f"Workers disponibles: {json.dumps(activos, ensure_ascii=False)}\n"
        'Responde SOLO con JSON: {"worker": "<id>", "motivo": "<máx 15 palabras>"}'
    )
    try:
        with _semaforo:
            headers = {"Authorization": "Bearer " + config.IA_API_KEY}
            if config.IA_API_STYLE == "ollama":
                url = f"{config.IA_URL}/ollama/api/generate"
                body = {
                    "model": config.IA_MODEL,
                    "prompt": prompt,
                    "stream": False,
                    "format": "json",
                    "options": {"temperature": 0},
                }
                respuesta = requests.post(
                    url, headers=headers, json=body, timeout=config.IA_TIMEOUT
                )
                respuesta.raise_for_status()
                texto = respuesta.json()["response"]
            else:
                url = f"{config.IA_URL}/api/chat/completions"
                body = {
                    "model": config.IA_MODEL,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0,
                    "stream": False,
                }
                respuesta = requests.post(
                    url, headers=headers, json=body, timeout=config.IA_TIMEOUT
                )
                respuesta.raise_for_status()
                texto = respuesta.json()["choices"][0]["message"]["content"]
        elegido = _extraer_json(texto)
        worker_id = elegido["worker"]
        if worker_id not in {worker["id"] for worker in activos}:
            raise ValueError("worker inexistente o inactivo")
        latencia = time.perf_counter() - inicio
        print(f"[IA] decisión {worker_id} ({latencia:.3f}s)", flush=True)
        return worker_id, "ia", latencia
    except (requests.RequestException, ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
        print(f"[IA] {exc}; usando fallback", flush=True)
        return fallback["id"], "fallback", time.perf_counter() - inicio
