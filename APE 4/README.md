# Práctica 4: asignación distribuida de pedidos

Sistema distribuido de delivery con Python 3.11. El flujo es:

```text
Generador -> Queue IPC -> Procesador -> Gateway -> Worker TCP -> ACK
```

El Gateway puede seleccionar workers mediante `ia`, `round_robin`,
`menor_carga` o `mas_cercano`.

## Requisitos

- Python 3.11.
- `requests`.
- Dos workers TCP disponibles.
- Para modo `ia`: acceso al servidor Open WebUI del laboratorio y una API key.

## Instalación

Desde la raíz del repositorio:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

Edita `.env`. La API key debe escribirse solamente en ese archivo local:

```env
IA_URL=http://10.20.202.199:3000
IA_API_KEY=CLAVE_REAL_ENTREGADA_POR_EL_DOCENTE
IA_MODEL=qwen3-coder:30b
IA_API_STYLE=openai
IA_TIMEOUT=20
WORKER_TIMEOUT=10
MODO=ia
WORKERS=W1=127.0.0.1:7001:1:1,W2=127.0.0.1:7002:8:7
RESULTADOS_DIR=resultados
```

No uses `<` ni `>` en el valor de `IA_API_KEY` cuando cargues el archivo con
`source`, porque Bash los interpreta como redirecciones.

Carga la configuración antes de ejecutar:

```bash
set -a
source .env
set +a
```

## Prueba local paso a paso

Se necesitan tres terminales porque los dos workers son servidores que deben
permanecer escuchando mientras el proceso central genera y distribuye pedidos.

### Terminal 1: worker W1

```bash
python3 worker/worker.py --id W1 --port 7001 --x 1 --y 1
```

### Terminal 2: worker W2

```bash
python3 worker/worker.py --id W2 --port 7002 --x 8 --y 7
```

### Terminal 3: central con reglas

Para probar la red sin depender de la IA:

```bash
set -a
source .env
set +a
export WORKERS="W1=127.0.0.1:7001:1:1,W2=127.0.0.1:7002:8:7"

python3 -m central.main \
  --modo round_robin \
  --pedidos 4 \
  --generadores 1 \
  --intervalo 0.2
```

Debes observar logs `[GEN]`, `[PROC]`, `[GATEWAY]` y `[WORKER W1/W2]`.
El resultado queda en:

```bash
cat resultados/resultados_round_robin.csv
```

### Prueba con IA

Verifica primero la conectividad y el modelo:

```bash
set -a
source .env
set +a

curl --connect-timeout 5 \
  -H "Authorization: Bearer $IA_API_KEY" \
  "$IA_URL/api/models"
```

La respuesta debe incluir `qwen3-coder:30b`. Después ejecuta:

```bash
python3 -m central.main \
  --modo ia \
  --pedidos 4 \
  --generadores 1 \
  --intervalo 0.2
```

Una decisión exitosa muestra `[IA] decisión ...` y registra
`decision=ia`. Si la IA no responde, hay un error HTTP o devuelve JSON
inválido, se muestra `[IA] ... usando fallback` y el sistema usa
`menor_carga`.

El resultado se consulta con:

```bash
cat resultados/resultados_ia.csv
```

## Objetivos del sistema

1. **Generar pedidos:** `central/generador.py` crea pedidos simulados con
   cliente, distancia, prioridad, peso y timestamp.
2. **Consultar la IA local:** `central/agente_ia.py` envía el pedido y la
   carga/distancia de workers a `POST /api/chat/completions`.
3. **Distribuir pedidos:** `central/gateway.py` usa sockets TCP, espera ACK,
   actualiza cargas y reintenta ante workers caídos.
4. **Registrar y evaluar:** `central/metricas.py` genera un CSV por modo con
   tiempos, worker, decisión, carga y distancia.

Las columnas de los CSV son:

```text
id,modo,worker,decision,t_decision,t_total,carga_en_decision,distancia
```

Para comparar los modos con el mismo volumen:

```bash
for modo in round_robin menor_carga mas_cercano; do
  python3 -m central.main --modo "$modo" --pedidos 30 --generadores 3 --intervalo 0.2
done
```

El modo `ia` requiere una API key válida y acceso al servidor.

## Docker Compose

Docker inicia los dos workers y el central sin usar tres terminales:

```bash
cp .env.example .env
# Edita .env y coloca la API key si usarás modo ia.
docker compose up --build
```

En Docker, `docker-compose.yml` reemplaza `WORKERS` por los nombres internos
`worker1` y `worker2`. No uses esos nombres para la ejecución local.

Para ejecutar una prueba puntual:

```bash
docker compose run --rm central \
  --modo round_robin \
  --pedidos 4 \
  --generadores 1 \
  --intervalo 0.2
```

Detén los servicios con:

```bash
docker compose down
```

Los CSV persisten en `./resultados/`.

## Worker en otra computadora

En la computadora del worker:

```bash
WORKER_ID=W2 WORKER_X=8 WORKER_Y=7 \
docker compose -f docker-compose.worker.yml up --build -d
```

En la computadora central, configura `WORKERS` con IPs reales:

```env
WORKERS=W1=192.168.1.20:7001:1:1,W2=192.168.1.21:7001:8:7
```

El puerto TCP debe estar permitido por el firewall.

## Archivos que se suben al repositorio

Se suben el código fuente, Dockerfiles, Compose, `requirements.txt`,
`.env.example`, `.dockerignore`, `.gitignore` y este README.

No se suben:

- `.env` ni API keys.
- `resultados/` ni archivos CSV generados.
- `__pycache__/`, `*.pyc` ni entornos virtuales.

Comprueba las reglas antes de publicar:

```bash
git check-ignore -v --no-index \
  .env resultados/resultados_ia.csv central/__pycache__/cache.pyc
```

Si un archivo privado ya fue agregado al índice, quítalo sin borrarlo localmente:

```bash
git rm --cached -- .env
git rm -r --cached -- resultados
```

Nunca incluyas una API key en commits, capturas, logs o archivos de resultados.
