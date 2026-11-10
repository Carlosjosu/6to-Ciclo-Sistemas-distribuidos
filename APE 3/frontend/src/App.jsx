import { Component, useState } from 'react';
import { exercises } from './exercises';

function App() {
  const [activeId, setActiveId] = useState(1);
  const [params, setParams] = useState({ vehiculos: 10, permisosSemaforo: 4, estadiaMs: 20 });
  const [bankParams, setBankParams] = useState({
    saldoInicial: 1000,
    retiroProceso1: 300,
    retiroProceso2: 250,
    depositoProceso3: 200,
    usarMutex: true,
    repeticiones: 50,
  });
  const [semaphoreParams, setSemaphoreParams] = useState({ espacios: 5, elementos: 0 });
  const [bridgeParams, setBridgeParams] = useState({
    vehiculosNorte: 6,
    vehiculosSur: 6,
    cruceMs: 40,
    llegadaMs: 10,
    limiteConsecutivos: 3,
    justo: true,
    control: true,
  });
  const [printParams, setPrintParams] = useState({
    documentos: 3,
    omitir: 'NINGUNO',
    timeoutMs: 250,
    trabajoMs: 20,
  });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const activeExercise = exercises.find(({ id }) => id === activeId) ?? exercises[0];

  function selectExercise(id) {
    setActiveId(id);
    setResult(null);
    setError('');
  }

  function updateParam(event) {
    setParams({ ...params, [event.target.name]: event.target.value });
  }

  function updateSemaphoreParam(event) {
    setSemaphoreParams({ ...semaphoreParams, [event.target.name]: event.target.value });
  }

  function updateBankParam(event) {
    const { name, type, checked, value } = event.target;
    setBankParams({ ...bankParams, [name]: type === 'checkbox' ? checked : value });
  }

  function updateBridgeParam(event) {
    const { name, type, checked, value } = event.target;
    setBridgeParams({ ...bridgeParams, [name]: type === 'checkbox' ? checked : value });
  }

  function updatePrintParam(event) {
    setPrintParams({ ...printParams, [event.target.name]: event.target.value });
  }

  async function runParking() {
    setError('');
    setResult(null);
    const query = new URLSearchParams(params);

    try {
      const response = await fetch(`/api/ejercicios/1/estacionamiento?${query}`);
      if (!response.ok) {
        throw new Error(`El servidor respondió con ${response.status}.`);
      }
      setResult(await response.json());
    } catch (requestError) {
      setError(`No se pudo ejecutar la simulación: ${requestError.message}`);
    }
  }

  async function runBank() {
    setError('');
    setResult(null);
    try {
      const query = new URLSearchParams(bankParams);
      const response = await fetch(`/api/ejercicios/2/cajero?${query}`);
      if (!response.ok) {
        throw new Error(`El servidor respondió con ${response.status}.`);
      }
      const bankResult = await response.json();
      if (!isBankResult(bankResult)) {
        throw new Error('La respuesta del cajero no tiene el formato esperado.');
      }
      setResult(bankResult);
    } catch (requestError) {
      setError(`No se pudo ejecutar la simulación: ${requestError.message}`);
    }
  }

  async function runProducerConsumer() {
    setError('');
    setResult(null);
    try {
      const query = new URLSearchParams(semaphoreParams);
      const response = await fetch(`/api/ejercicios/3/productor-consumidor?${query}`);
      if (!response.ok) {
        throw new Error(`El servidor respondió con ${response.status}.`);
      }
      setResult(await response.json());
    } catch (requestError) {
      setError(`No se pudo ejecutar la simulación: ${requestError.message}`);
    }
  }

  async function runBridge() {
    setError('');
    setResult(null);
    try {
      const query = new URLSearchParams(bridgeParams);
      const response = await fetch(`/api/ejercicios/4/puente?${query}`);
      if (!response.ok) {
        throw new Error(`El servidor respondió con ${response.status}.`);
      }
      setResult(await response.json());
    } catch (requestError) {
      setError(`No se pudo ejecutar la simulación: ${requestError.message}`);
    }
  }

  async function runPrintCenter() {
    setError('');
    setResult(null);
    try {
      const query = new URLSearchParams(printParams);
      const response = await fetch(`/api/ejercicios/5/impresion?${query}`);
      if (!response.ok) {
        throw new Error(`El servidor respondió con ${response.status}.`);
      }
      setResult(await response.json());
    } catch (requestError) {
      setError(`No se pudo ejecutar la simulación: ${requestError.message}`);
    }
  }

  return (
    <main className="app">
      <header>
        <p className="eyebrow">Sistemas Distribuidos · APE 3</p>
        <h1>Sincronización de procesos</h1>
        <p className="intro">Esqueleto inicial para que cada participante implemente un ejercicio.</p>
      </header>

      <nav className="exercise-tabs" aria-label="Ejercicios">
        {exercises.map((exercise) => (
          <button
            className={exercise.id === activeId ? 'tab active' : 'tab'}
            key={exercise.id}
            onClick={() => selectExercise(exercise.id)}
          >
            Ejercicio {exercise.id}
          </button>
        ))}
      </nav>

      <section className="panel">
        <h2>{activeExercise.name}</h2>
        <p>Concepto asignado: <strong>{activeExercise.concept}</strong></p>
        {activeId === 1 ? (
          <>
            <p>Configura la cantidad de vehículos, los permisos del semáforo y el tiempo de estadía.</p>
            <div className="form-grid">
              <label>
                Vehículos
                <input name="vehiculos" type="number" min="1" value={params.vehiculos} onChange={updateParam} />
              </label>
              <label>
                Permisos del semáforo
                <input name="permisosSemaforo" type="number" min="1" value={params.permisosSemaforo} onChange={updateParam} />
              </label>
              <label>
                Estadía (ms)
                <input name="estadiaMs" type="number" min="1" value={params.estadiaMs} onChange={updateParam} />
              </label>
            </div>
            <button className="primary" onClick={runParking}>Ejecutar simulación</button>
            {error && <p className="error" role="alert">{error}</p>}
            {result && <SimulationResult result={result} />}
          </>
        ) : activeId === 2 ? (
          <>
            <p>Ingresa el saldo y las operaciones de los tres procesos. El mutex protege cada lectura y actualización del saldo compartido.</p>
            <div className="form-grid bank-form">
              <label>
                Saldo inicial ($)
                <input name="saldoInicial" type="number" min="0" value={bankParams.saldoInicial} onChange={updateBankParam} />
              </label>
              <label>
                Retiro proceso 1 ($)
                <input name="retiroProceso1" type="number" min="0" value={bankParams.retiroProceso1} onChange={updateBankParam} />
              </label>
              <label>
                Retiro proceso 2 ($)
                <input name="retiroProceso2" type="number" min="0" value={bankParams.retiroProceso2} onChange={updateBankParam} />
              </label>
              <label>
                Depósito proceso 3 ($)
                <input name="depositoProceso3" type="number" min="0" value={bankParams.depositoProceso3} onChange={updateBankParam} />
              </label>
              <label>
                Repeticiones
                <input name="repeticiones" type="number" min="1" value={bankParams.repeticiones} onChange={updateBankParam} />
              </label>
              <label className="checkbox-label">
                <input name="usarMutex" type="checkbox" checked={bankParams.usarMutex} onChange={updateBankParam} />
                Usar mutex
              </label>
            </div>
            <p className="hint">Desactiva el mutex para comparar el resultado y observar la condición de carrera.</p>
            <button className="primary" onClick={runBank}>Ejecutar cajero</button>
            {error && <p className="error" role="alert">{error}</p>}
            {result && <BankResult result={result} />}
          </>
        ) : activeId === 3 ? (
          <>
            <p>Dos productores fabrican cinco piezas cada uno y dos consumidores las ensamblan. El almacén tiene capacidad para cinco piezas.</p>
            <div className="form-grid semaphore-form">
              <label>
                Valor inicial de espacios
                <input name="espacios" type="number" min="0" max="10" value={semaphoreParams.espacios} onChange={updateSemaphoreParam} />
              </label>
              <label>
                Valor inicial de elementos
                <input name="elementos" type="number" min="0" max="10" value={semaphoreParams.elementos} onChange={updateSemaphoreParam} />
              </label>
              <div className="mutex-value"><strong>mutex_almacen</strong><span>1 (fijo)</span></div>
            </div>
            <p className="hint">Configuración correcta: espacios = 5 y elementos = 0. Prueba otros valores para ver el diagnóstico sin bloquear la aplicación.</p>
            <button className="primary" onClick={runProducerConsumer}>Probar sincronización</button>
            {error && <p className="error" role="alert">{error}</p>}
            {result && <ProducerConsumerResult result={result} />}
          </>
        ) : activeId === 4 ? (
          <>
            <p>El puente admite una sola dirección activa a la vez. Varios vehículos pueden cruzar en el mismo sentido, pero nunca hacia el norte y hacia el sur simultáneamente.</p>
            <div className="form-grid">
              <label>
                Vehículos hacia el norte
                <input name="vehiculosNorte" type="number" min="0" max="50" value={bridgeParams.vehiculosNorte} onChange={updateBridgeParam} />
              </label>
              <label>
                Vehículos hacia el sur
                <input name="vehiculosSur" type="number" min="0" max="50" value={bridgeParams.vehiculosSur} onChange={updateBridgeParam} />
              </label>
              <label>
                Tiempo de cruce (ms)
                <input name="cruceMs" type="number" min="1" value={bridgeParams.cruceMs} onChange={updateBridgeParam} />
              </label>
              <label>
                Separación de llegadas (ms)
                <input name="llegadaMs" type="number" min="1" value={bridgeParams.llegadaMs} onChange={updateBridgeParam} />
              </label>
              <label>
                Límite de vehículos seguidos
                <input name="limiteConsecutivos" type="number" min="1" value={bridgeParams.limiteConsecutivos} disabled={!bridgeParams.justo} onChange={updateBridgeParam} />
              </label>
              <div className="mutex-value"><strong>Mutex + Condition</strong><span>{bridgeParams.control ? 'Activos' : 'Eliminados'}</span></div>
            </div>
            <div className="toggle-row">
              <label className="toggle">
                <input name="justo" type="checkbox" checked={bridgeParams.justo} onChange={updateBridgeParam} />
                Reto: evitar que una dirección monopolice el puente
              </label>
              <label className="toggle">
                <input name="control" type="checkbox" checked={bridgeParams.control} onChange={updateBridgeParam} />
                Usar mutex y condición (desmarca para ver el conflicto)
              </label>
            </div>
            <p className="hint">Con el reto desactivado se ejecuta el algoritmo base del pseudocódigo: si los vehículos de un sentido llegan sin pausa, el otro sentido espera hasta que terminen todos.</p>
            <button className="primary" onClick={runBridge}>Ejecutar simulación</button>
            {error && <p className="error" role="alert">{error}</p>}
            {result && <BridgeResult result={result} />}
          </>
        ) : activeId === 5 ? (
          <>
            <p>Los documentos pasan por diseño, revisión e impresión en ese orden. Omite una señal para observar cómo se interrumpe el relevo.</p>
            <div className="form-grid">
              <label>
                Documentos
                <input name="documentos" type="number" min="1" value={printParams.documentos} onChange={updatePrintParam} />
              </label>
              <label>
                Señal a omitir
                <select name="omitir" value={printParams.omitir} onChange={updatePrintParam}>
                  <option value="NINGUNO">Ninguna (secuencia correcta)</option>
                  <option value="REVISION">Diseño → revisión</option>
                  <option value="IMPRESION">Revisión → impresión</option>
                  <option value="DISENO">Impresión → diseño</option>
                </select>
              </label>
              <label>
                Tiempo de espera (ms)
                <input name="timeoutMs" type="number" min="1" value={printParams.timeoutMs} onChange={updatePrintParam} />
              </label>
              <label>
                Trabajo por etapa (ms)
                <input name="trabajoMs" type="number" min="0" value={printParams.trabajoMs} onChange={updatePrintParam} />
              </label>
            </div>
            <button className="primary" onClick={runPrintCenter}>Ejecutar centro de impresión</button>
            {error && <p className="error" role="alert">{error}</p>}
            {result && <PrintCenterResult result={result} />}
          </>
        ) : (
          <div className="assignment-space">
            <p>Espacio asignado para desarrollar este ejercicio.</p>
          </div>
        )}
      </section>
    </main>
  );
}

function SimulationResult({ result }) {
  return (
    <div className="simulation-result">
      <div className={result.verified ? 'verdict success' : 'verdict failure'}>
        {result.verified ? 'Capacidad respetada' : 'Capacidad superada'}
      </div>
      <p>{result.summary}</p>
      <dl className="metrics">
        <div><dt>Máximo simultáneo</dt><dd>{result.metrics.maximoSimultaneos}</dd></div>
        <div><dt>Capacidad física</dt><dd>{result.metrics.capacidadFisica}</dd></div>
        <div><dt>Tiempo concurrente</dt><dd>{result.metrics.tiempoConcurrenteMs} ms</dd></div>
        <div><dt>Tiempo secuencial estimado</dt><dd>{result.metrics.tiempoSecuencialEstimadoMs} ms</dd></div>
      </dl>
      <details className="log-details">
        <summary>
          <span>Bitácora de ejecución</span>
          <span className="event-count">{result.events.length} eventos</span>
        </summary>
        <ol className="events">
          {result.events.map((event, index) => (
            <li className="event-item" key={`${event.timestamp}-${index}`}>
              <span className="event-marker">{index + 1}</span>
              <div className="event-content">
                <div className="event-meta">
                  <span className="event-thread">{event.thread}</span>
                  <time dateTime={event.timestamp}>{formatEventTime(event.timestamp)}</time>
                </div>
                <p>{event.message}</p>
              </div>
            </li>
          ))}
        </ol>
      </details>
    </div>
  );
}

function ProducerConsumerResult({ result }) {
  return (
    <div className="simulation-result">
      <div className={result.verified ? 'verdict success' : 'verdict failure'}>
        {result.verified ? 'Sincronización correcta' : 'Error de sincronización'}
      </div>
      <p>{result.summary}</p>
      <dl className="metrics">
        <div><dt>Capacidad del almacén</dt><dd>{result.metrics.capacidad}</dd></div>
        <div><dt>Inicialización espacios / elementos</dt><dd>{result.metrics.espaciosIniciales} / {result.metrics.elementosIniciales}</dd></div>
        <div><dt>Máximo almacenado</dt><dd>{result.metrics.maximoAlmacen}</dd></div>
        <div><dt>Piezas producidas</dt><dd>{result.metrics.piezasProducidas}</dd></div>
        <div><dt>Piezas consumidas</dt><dd>{result.metrics.piezasConsumidas}</dd></div>
        <div><dt>Piezas restantes</dt><dd>{result.metrics.piezasRestantes}</dd></div>
        <div><dt>Tiempo de ejecución</dt><dd>{result.metrics.tiempoConcurrenteMs} ms</dd></div>
      </dl>
      <section className="analysis">
        <h3>Análisis de los semáforos</h3>
        <p><strong>Si espacios inicia en 0:</strong> ningún productor puede insertar piezas, aunque el almacén esté vacío. Si ningún otro proceso aumenta ese semáforo, los productores quedan bloqueados y los consumidores también esperan piezas: se produce un interbloqueo.</p>
        <p><strong>Si elementos inicia en 5:</strong> los consumidores creen que hay cinco piezas cuando el almacén está vacío. Intentarán retirar elementos inexistentes; el contador queda desincronizado con la cola y la implementación puede fallar o desbloquear operaciones inválidas.</p>
      </section>
      <details className="log-details">
        <summary><span>Bitácora de ejecución</span><span className="event-count">{result.events.length} eventos</span></summary>
        <ol className="events">
          {result.events.map((event, index) => (
            <li className="event-item" key={`${event.timestamp}-${index}`}>
              <span className="event-marker">{index + 1}</span>
              <div className="event-content">
                <div className="event-meta"><span className="event-thread">{event.thread}</span><time dateTime={event.timestamp}>{formatEventTime(event.timestamp)}</time></div>
                <p>{event.message}</p>
              </div>
            </li>
          ))}
        </ol>
      </details>
    </div>
  );
}

function BridgeResult({ result }) {
  const metrics = result.metrics;

  return (
    <div className="simulation-result">
      <div className={result.verified ? 'verdict success' : 'verdict failure'}>
        {result.verified ? 'Nunca cruzaron sentidos opuestos' : 'Conflicto: sentidos opuestos a la vez'}
      </div>
      <p>{result.summary}</p>
      <dl className="metrics">
        <div><dt>Modo</dt><dd>{metrics.modo}</dd></div>
        <div><dt>Conflictos detectados</dt><dd>{metrics.conflictosDetectados}</dd></div>
        <div><dt>Máx. simultáneos norte / sur</dt><dd>{metrics.maximoNorteSimultaneo} / {metrics.maximoSurSimultaneo}</dd></div>
        <div><dt>Cambios de dirección</dt><dd>{metrics.cambiosDeDireccion}</dd></div>
        <div><dt>Norte antes del primer sur</dt><dd>{metrics.norteAntesDelPrimerSur}</dd></div>
        <div><dt>Espera máx. norte / sur</dt><dd>{metrics.esperaMaxNorteMs} ms / {metrics.esperaMaxSurMs} ms</dd></div>
        <div><dt>Vehículos atendidos</dt><dd>{metrics.vehiculosAtendidos}</dd></div>
        <div><dt>Tiempo de ejecución</dt><dd>{metrics.tiempoConcurrenteMs} ms</dd></div>
        <div><dt>Tiempo secuencial estimado</dt><dd>{metrics.tiempoSecuencialEstimadoMs} ms</dd></div>
      </dl>
      <section className="analysis">
        <h3>Análisis de inanición</h3>
        <p><strong>Algoritmo base:</strong> mientras haya al menos un vehículo cruzando hacia el norte, la dirección sigue siendo NORTE y el contador nunca llega a 0. Si llegan vehículos al norte de forma continua, los del sur esperan indefinidamente: sufren inanición.</p>
        <p><strong>Solución:</strong> se limita cuántos vehículos seguidos del mismo sentido pueden entrar cuando el sentido contrario está esperando. Al vaciarse el puente, el turno pasa al sentido que esperaba.</p>
      </section>
      <details className="log-details">
        <summary><span>Bitácora de ejecución</span><span className="event-count">{result.events.length} eventos</span></summary>
        <ol className="events">
          {result.events.map((event, index) => (
            <li className="event-item" key={`${event.timestamp}-${index}`}>
              <span className="event-marker">{index + 1}</span>
              <div className="event-content">
                <div className="event-meta"><span className="event-thread">{event.thread}</span><time dateTime={event.timestamp}>{formatEventTime(event.timestamp)}</time></div>
                <p>{event.message}</p>
              </div>
            </li>
          ))}
        </ol>
      </details>
    </div>
  );
}

function isBankResult(value) {
  return Boolean(
    value
      && typeof value === 'object'
      && typeof value.verified === 'boolean'
      && typeof value.summary === 'string'
      && value.metrics
      && typeof value.metrics === 'object'
      && Array.isArray(value.events),
  );
}

function BankResult({ result }) {
  const metrics = result.metrics;
  const correctas = metrics.ejecucionesCorrectas ?? 0;
  const repeticiones = metrics.repeticiones ?? 0;

  return (
    <div className="simulation-result">
      <div className={result.verified ? 'verdict success' : 'verdict failure'}>
        {result.verified ? 'Saldo protegido' : 'Condición de carrera detectada'}
      </div>
      <p>{result.summary}</p>
      <dl className="metrics">
        <div><dt>Saldo esperado</dt><dd>${metrics.saldoEsperado ?? '-'}</dd></div>
        <div><dt>Ejecuciones correctas</dt><dd>{correctas} / {repeticiones}</dd></div>
        <div><dt>Mutex</dt><dd>{metrics.usaMutex ? 'Activo' : 'Desactivado'}</dd></div>
        <div><dt>Saldos finales observados</dt><dd>{metrics.saldosFinales ?? '-'}</dd></div>
        <div><dt>Tiempo de ejecución</dt><dd>{metrics.tiempoConcurrenteMs ?? '-'} ms</dd></div>
      </dl>
      <details className="log-details">
        <summary><span>Bitácora de ejecución</span><span className="event-count">{result.events.length} eventos</span></summary>
        <ol className="events"> 
          {result.events.map((event, index) => (
            <li className="event-item" key={`${event.timestamp}-${index}`}>
              <span className="event-marker">{index + 1}</span>
              <div className="event-content">
                <div className="event-meta"><span className="event-thread">{event.thread}</span><time dateTime={event.timestamp}>{formatEventTime(event.timestamp)}</time></div>
                <p>{event.message}</p>
              </div>
            </li>
          ))}
        </ol>
      </details>
    </div>
  );
}

function PrintCenterResult({ result }) {
  return (
    <div className="simulation-result">
      <div className={result.verified ? 'verdict success' : 'verdict failure'}>
        {result.verified ? 'Relevo completado' : 'Relevo interrumpido'}
      </div>
      <p>{result.summary}</p>
      <dl className="metrics">
        <div><dt>Señal omitida</dt><dd>{result.metrics.signalOmitido}</dd></div>
        <div><dt>Etapas ejecutadas</dt><dd>{result.metrics.etapasEjecutadas} / {result.metrics.etapasEsperadas}</dd></div>
        <div><dt>Hilos bloqueados</dt><dd>{result.metrics.hilosBloqueados}</dd></div>
        <div><dt>Documentos</dt><dd>{result.metrics.documentos}</dd></div>
      </dl>
      <section className="analysis">
        <h3>Orden observado</h3>
        <p className="observed-order">{result.metrics.ordenObservado || 'No se completó ninguna etapa.'}</p>
      </section>
      <details className="log-details">
        <summary><span>Bitácora de ejecución</span><span className="event-count">{result.events.length} eventos</span></summary>
        <ol className="events">
          {result.events.map((event, index) => (
            <li className="event-item" key={`${event.timestamp}-${index}`}>
              <span className="event-marker">{index + 1}</span>
              <div className="event-content">
                <div className="event-meta">
                  <span className="event-thread">{event.thread}</span>
                  <time dateTime={event.timestamp}>{formatEventTime(event.timestamp)}</time>
                </div>
                <p>{event.message}</p>
              </div>
            </li>
          ))}
        </ol>
      </details>
    </div>
  );
}

function formatEventTime(timestamp) {
  return new Intl.DateTimeFormat('es-EC', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3,
  }).format(new Date(timestamp));
}

class AppErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <main className="app">
          <section className="panel">
            <h1>No se pudo mostrar este ejercicio</h1>
            <p className="error">{this.state.error.message}</p>
            <button className="primary" onClick={() => window.location.reload()}>
              Recargar aplicación
            </button>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}

export default function AppRoot() {
  return (
    <AppErrorBoundary>
      <App />
    </AppErrorBoundary>
  );
}