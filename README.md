# 6to Ciclo - Sistemas Distribuidos

Repositorio unificado de las actividades prácticas de Sistemas Distribuidos.
Las prácticas originales se conservaron en sus carpetas de origen; aquí se
agregaron copias independientes para poder instalar y probar todo desde esta
carpeta.

## Contenido

| APE | Tema | Documentación |
|---|---|---|
| APE 1 | Análisis de arquitecturas distribuidas | [`APE 1/`](<./APE 1/>) |
| APE 2 | Comunicación cliente-servidor mediante archivos | [`APE 2/`](<./APE 2/>) |
| APE 3 | Sincronización de procesos con Spring Boot, React y Vite | [`APE 3/README.md`](<./APE 3/README.md>) |
| APE 4 | Asignación distribuida de pedidos con Python, IPC y TCP | [`APE 4/README.md`](<./APE 4/README.md>) |
| APE 5 | MQTT, SQLite y puente MQTT → RabbitMQ | [`APE 5/README.md`](<./APE 5/README.md>) |
| APE 6 | Microservicios Spring Boot consumidores de RabbitMQ | [`APE 6/README.md`](<./APE 6/README.md>) |

## APE 1: análisis de arquitecturas distribuidas

La guía de esta actividad está en [`APE 1/`](<./APE 1/>). Es una actividad
teórica de análisis y comparación de arquitecturas distribuidas:

- cliente-servidor;
- peer-to-peer (P2P);
- arquitectura multicapa;
- microservicios.

El documento analiza la estructura, comunicación, ventajas, desventajas,
escalabilidad, tolerancia a fallos y casos de uso de cada arquitectura. No
requiere instalación ni ejecución de código; para realizarla se debe leer la
guía PDF incluida en la carpeta.

## APE 2: comunicación cliente-servidor con archivos

La implementación está en
[`APE 2/APE 2_codigo/`](<./APE 2/APE%202_codigo/>). El cliente y el servidor
se comunican mediante los archivos
`Comunicacion/entrada_servidor.txt` y `Comunicacion/salida_servidor.txt`.
El servidor lee el mensaje, lo convierte a mayúsculas y escribe la respuesta.

Requisitos: Python 3.8 o superior. No necesita dependencias externas.

En una terminal, inicia el servidor:

```bash
cd "APE 2/APE 2_codigo/Servidor"
python3 servidor.py
```

En otra terminal, inicia el cliente:

```bash
cd "APE 2/APE 2_codigo/Cliente"
python3 cliente.py
```

Cuando el cliente solicite un mensaje, escribe por ejemplo
`hola sistemas distribuidos`. La respuesta esperada es:

```text
Respuesta del servidor: HOLA SISTEMAS DISTRIBUIDOS
```

Para detener el servidor presiona `Ctrl+C`. La guía PDF de la actividad está
en [`APE 2/`](<./APE 2/>).

## APE 3: sincronización de procesos

La copia está en [`APE 3/`](<./APE 3/>). Incluye un backend Spring Boot con
los ejercicios de estacionamiento, cajero, productor-consumidor, centro de
impresión y puente, además de un frontend React/Vite.

Requisitos: Java JDK 21, Maven 3.9 o superior, Node.js 20 LTS o superior y
npm.

```bash
cd "APE 3/frontend"
npm ci
npm run build

cd ../backend
mvn test
mvn spring-boot:run
```

En otra terminal, desde `6to-Ciclo-Sistemas-distribuidos/APE 3`:

```bash
cd frontend
npm run dev
```

Abrir <http://localhost:5173>. El backend queda disponible en
<http://localhost:8080>. La documentación detallada y los endpoints están en
[`APE 3/README.md`](<./APE 3/README.md>).

## APE 4: asignación distribuida de pedidos

La copia está en [`APE 4/`](<./APE 4/>). Es un sistema de delivery
implementado con Python 3.11 que comunica un proceso central con dos workers
TCP mediante una cola IPC. El Gateway puede seleccionar workers con
`round_robin`, `menor_carga`, `mas_cercano` o `ia`, y registra los resultados
en archivos CSV.

Requisitos: Python 3.11, `requests`, Docker Compose opcional y dos workers TCP.
Para instalarlo sin depender de una API de IA:

```bash
cd "APE 4"
python3 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
```

En tres terminales, con el entorno virtual activado:

```bash
python3 worker/worker.py --id W1 --port 7001 --x 1 --y 1
python3 worker/worker.py --id W2 --port 7002 --x 8 --y 7
python3 -m central.main --modo round_robin --pedidos 4 --generadores 1 --intervalo 0.2
```

El resultado se guarda en `resultados/resultados_round_robin.csv`. La
configuración de IA es opcional; nunca se debe guardar una API key en el
repositorio. La guía completa está en [`APE 4/README.md`](<./APE 4/README.md>).

## APE 5: MQTT, SQLite y RabbitMQ

La copia está en [`APE 5/`](<./APE 5/>), con el proyecto ejecutable dentro de
[`APE 5/practica5/`](<./APE 5/practica5/>). Los sensores publican GPS,
temperatura y combustible por MQTT; el suscriptor guarda datos en SQLite y el
puente envía los mensajes relevantes a RabbitMQ.

Requisitos: Docker Compose, Python 3.10 o superior y puertos libres `1883`,
`9001`, `5672` y `15672`.

```bash
cd "APE 5/practica5"
python3 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
docker compose up -d
```

En tres terminales, con el entorno virtual activado:

```bash
python suscriptor.py
python mqtt_rabbitmq_bridge.py
python simulador_sensores.py
```

RabbitMQ se puede revisar en <http://localhost:15672> con `guest`/`guest`.
Al terminar:

```bash
docker compose down
```

La documentación completa, reglas de negocio y capturas están en
[`APE 5/README.md`](<./APE 5/README.md>).

## APE 6: integración con microservicios

La copia está en [`APE 6/`](<./APE 6/>). Esta práctica reutiliza el simulador
de APE 5 y agrega `fleet-monitor`, una aplicación Spring Boot que consume las
colas de RabbitMQ y expone una API REST.

Requisitos: Docker Compose, Python 3.10 o superior, JDK 21, Maven y puertos
libres `1883`, `5672`, `15672` y `8080`.

Primero inicia el broker:

```bash
cd "APE 6/practica6"
docker compose -f docker-compose-rabbitmq.yml up -d
```

En otra terminal, inicia el servicio:

```bash
cd "APE 6/practica6/fleet-monitor"
mvn spring-boot:run
```

En dos terminales adicionales, desde `6to-Ciclo-Sistemas-distribuidos`:

```bash
python "APE 6/practica6/mqtt_rabbitmq_bridge.py"
python "APE 6/practica5/simulador_sensores.py"
```

Verifica la API:

```bash
curl http://localhost:8080/api/fleet/status
curl http://localhost:8080/api/fleet/vehicle/VH-001/telemetria
```

La guía completa de ejecución distribuida, Docker y capturas está en
[`APE 6/README.md`](<./APE 6/README.md>).

## Detener APE 5 y APE 6

Para detener APE 5:

```bash
cd "APE 5/practica5"
docker compose down
```

Para detener APE 6:

```bash
cd "APE 6/practica6"
docker compose -f docker-compose-rabbitmq.yml down
```

> Las carpetas `APE 3`, `APE 4`, `APE 5` y `APE 6` son copias agregadas. No sustituyen
> ni eliminan las carpetas originales que están fuera de este repositorio.
