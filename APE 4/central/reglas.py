import math


def _activos(workers):
    return [worker for worker in workers if worker.get("activo")]


def round_robin(workers, indice=0):
    activos = _activos(workers)
    if not activos:
        raise ValueError("no hay workers activos")
    return activos[indice % len(activos)]


def menor_carga(workers):
    activos = _activos(workers)
    if not activos:
        raise ValueError("no hay workers activos")
    return min(activos, key=lambda worker: (worker["carga"], worker["id"]))


def mas_cercano(workers):
    activos = _activos(workers)
    if not activos:
        raise ValueError("no hay workers activos")
    return min(activos, key=lambda worker: (worker["distancia_km"], worker["id"]))


def puntaje_combinado(workers, w1=1.0, w2=1.0):
    activos = _activos(workers)
    if not activos:
        raise ValueError("no hay workers activos")
    return min(activos, key=lambda worker: worker["carga"] * w1 + worker["distancia_km"] * w2)
