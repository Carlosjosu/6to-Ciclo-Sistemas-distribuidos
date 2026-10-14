# 6to-Ciclo-Sistemas-distribuidos

Repositorio de actividades prácticas de la asignatura **Sistemas
Distribuidos**. El proyecto contiene dos actividades (APE 1 y APE 2)
relacionadas con el análisis y la implementación de arquitecturas
distribuidas.

## Contenido del repositorio

```text
.
├── APE 1/
│   └── ape_guia  1 distribuidos-signed-signed-firmado.docx.pdf
├── APE 2/
│   ├── ape_guia  2 distribuidos-signed-signed-firmado .pdf
│   └── APE 2_codigo/
│       ├── Cliente/
│       │   └── cliente.py
│       ├── Servidor/
│       │   └── servidor.py
│       └── Comunicacion/
│           ├── entrada_servidor.txt
│           └── salida_servidor.txt
└── README.md
```

## APE 1: análisis de arquitecturas distribuidas

La primera actividad corresponde a un análisis colaborativo de las
principales arquitecturas utilizadas en sistemas distribuidos:

- **Cliente-servidor:** los clientes realizan solicitudes y un servidor
  central procesa y responde esas solicitudes.
- **Peer-to-peer (P2P):** los nodos pueden actuar como clientes y servidores,
  compartiendo recursos directamente.
- **Multicapa (n-tier):** el sistema se divide en capas, por ejemplo
  presentación, lógica de negocio y acceso a datos.
- **Microservicios:** la aplicación se separa en servicios pequeños e
  independientes, cada uno con una responsabilidad concreta.

El objetivo de la APE 1 es comparar estas arquitecturas considerando su
estructura, comunicación, ventajas, desventajas, escalabilidad, tolerancia a
fallos y casos de uso. La guía de la actividad se encuentra en la carpeta
[`APE 1/`](<./APE 1/>).

## APE 2: comunicación cliente-servidor con archivos

En la segunda actividad se implementó una simulación sencilla de un sistema
distribuido con dos componentes independientes:

1. El **cliente** solicita un mensaje al usuario y lo escribe en
   `entrada_servidor.txt`.
2. El **servidor** revisa periódicamente ese archivo.
3. Cuando encuentra un mensaje nuevo, lo procesa convirtiéndolo a mayúsculas.
4. El servidor escribe la respuesta en `salida_servidor.txt`.
5. El cliente espera brevemente, lee la respuesta y la muestra en pantalla.

La comunicación no utiliza sockets ni una conexión directa. Se realiza
indirectamente mediante dos archivos de texto compartidos. Esto permite
observar el desacoplamiento entre el cliente y el servidor: ambos programas
conocen el canal de comunicación, pero no necesitan conocer la implementación
interna del otro.

### Funciones implementadas

#### Servidor

El archivo [`servidor.py`](<./APE 2/APE 2_codigo/Servidor/servidor.py>):

- permanece ejecutándose y revisa la entrada cada segundo;
- crea el archivo de entrada si todavía no existe;
- ignora archivos vacíos;
- detecta y procesa únicamente mensajes nuevos consecutivos;
- convierte el texto recibido a mayúsculas;
- guarda el resultado en `salida_servidor.txt`.

#### Cliente

El archivo [`cliente.py`](<./APE 2/APE 2_codigo/Cliente/cliente.py>):

- solicita un texto mediante la consola;
- escribe el texto en `entrada_servidor.txt`;
- simula la latencia de la comunicación esperando dos segundos;
- lee `salida_servidor.txt`;
- muestra la respuesta recibida.

### Cómo ejecutar la APE 2

Se necesita Python 3 y dos terminales abiertas. Primero se inicia el
servidor:

```bash
cd "APE 2/APE 2_codigo/Servidor"
python3 servidor.py
```

En una segunda terminal se ejecuta el cliente:

```bash
cd "APE 2/APE 2_codigo/Cliente"
python3 cliente.py
```

Después se escribe un mensaje cuando el cliente lo solicite. Por ejemplo:

```text
Ingrese un mensaje: hola sistemas distribuidos
Mensaje enviado al servidor.
Esperando respuesta...
Respuesta del servidor: HOLA SISTEMAS DISTRIBUIDOS
```

Mientras se ejecutan los programas, los archivos
[`entrada_servidor.txt`](<./APE 2/APE 2_codigo/Comunicacion/entrada_servidor.txt>)
y
[`salida_servidor.txt`](<./APE 2/APE 2_codigo/Comunicacion/salida_servidor.txt>)
permiten comprobar el intercambio de información. Para detener el servidor se
puede presionar `Ctrl+C`.

### Limitaciones de esta implementación

Esta práctica demuestra el concepto de comunicación distribuida, pero utiliza
un mecanismo básico basado en archivos. Por esa razón:

- la respuesta no es inmediata, porque el servidor consulta la entrada cada
  segundo;
- un cliente puede sobrescribir el mensaje de otro si varios se ejecutan al
  mismo tiempo;
- no existen identificadores de petición ni confirmaciones de recepción;
- si el servidor no está activo, el cliente puede escribir la entrada, pero no
  habrá un procesamiento nuevo;
- no se incluyen bloqueos de archivos, autenticación ni cifrado.

En un sistema real, estas limitaciones podrían resolverse utilizando sockets,
colas de mensajes, identificadores únicos, confirmaciones y mecanismos de
sincronización.
