import time
from pathlib import Path


# Rutas de los archivos de comunicación, relativas a la raíz del proyecto.
comunicacion = Path(__file__).resolve().parent.parent / "Comunicacion"
entrada = comunicacion / "entrada_servidor.txt"
salida = comunicacion / "salida_servidor.txt"

print("=== SERVIDOR INICIADO ===")
print("Esperando mensajes...")

ultimo_mensaje = ""

while True:
    # Crear el archivo de entrada si todavía no existe.
    entrada.touch(exist_ok=True)

    mensaje = entrada.read_text(encoding="utf-8").strip()

    # Ignorar entradas vacías y no procesar dos veces el mismo mensaje.
    if mensaje and mensaje != ultimo_mensaje:
        print(f"Mensaje recibido: {mensaje}")

        # Procesar el mensaje convirtiéndolo a mayúsculas.
        resultado = mensaje.upper()
        salida.write_text(resultado, encoding="utf-8")

        print(f"Mensaje procesado: {resultado}")
        print("Respuesta escrita en salida_servidor.txt")
        print("-----------------------------")

        ultimo_mensaje = mensaje

    time.sleep(1)
