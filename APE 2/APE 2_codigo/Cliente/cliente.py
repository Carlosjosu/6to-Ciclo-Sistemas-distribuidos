import time

# Rutas de los archivos de comunicación
entrada = "../Comunicacion/entrada_servidor.txt"
salida = "../Comunicacion/salida_servidor.txt"

print("=== CLIENTE ===")

mensaje = input("Ingrese un mensaje: ")

# Escribir el mensaje para el servidor
with open(entrada, "w", encoding="utf-8") as archivo:
    archivo.write(mensaje)

print("Mensaje enviado al servidor.")
print("Esperando respuesta...")

# Simular latencia de comunicación
time.sleep(2)

# Leer la respuesta del servidor
with open(salida, "r", encoding="utf-8") as archivo:
    respuesta = archivo.read().strip()

print(f"Respuesta del servidor: {respuesta}")