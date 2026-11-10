import json
import socket
from typing import Any


def enviar_json(sock: socket.socket, obj: dict[str, Any]) -> None:
    sock.sendall((json.dumps(obj, separators=(",", ":")) + "\n").encode("utf-8"))


def recibir_json(sock: socket.socket) -> dict[str, Any]:
    datos = bytearray()
    while b"\n" not in datos:
        bloque = sock.recv(4096)
        if not bloque:
            break
        datos.extend(bloque)
        if len(datos) > 1_048_576:
            raise ValueError("mensaje TCP demasiado grande")
    if not datos:
        raise ValueError("mensaje TCP vacío")
    try:
        mensaje = json.loads(bytes(datos).split(b"\n", 1)[0].decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        raise ValueError("JSON TCP inválido") from exc
    if not isinstance(mensaje, dict):
        raise ValueError("el mensaje debe ser un objeto JSON")
    return mensaje
