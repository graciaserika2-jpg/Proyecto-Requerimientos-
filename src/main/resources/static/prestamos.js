const formularioPrestamo = document.getElementById("formPrestamo");

const campoBuscarPersona = document.getElementById("buscarPersona");
const selectorPersona = document.getElementById("personaId");
const resultadosPersonas = document.getElementById("resultadosPersonas");
const personaSeleccionada = document.getElementById("personaSeleccionada");

const campoBuscarEquipo = document.getElementById("buscarEquipo");
const selectorEquipo = document.getElementById("equipoId");
const resultadosEquipos = document.getElementById("resultadosEquipos");

const equipoSeleccionado = document.getElementById("equipoSeleccionado");
const nombreEquipoSeleccionado = document.getElementById("nombreEquipoSeleccionado");
const detallesEquipoSeleccionado = document.getElementById("detallesEquipoSeleccionado");
const quitarEquipo = document.getElementById("quitarEquipo");

const tablaPrestamos = document.getElementById("tablaPrestamos");
const mensajePrestamo = document.getElementById("mensajePrestamo");
const filtroPrestamos = document.getElementById("filtroPrestamos");
const buscarHistorial = document.getElementById("buscarHistorial");

// Elementos del formulario de devolución
const modalDevolucion = document.getElementById("modalDevolucion");
const formularioDevolucion = document.getElementById("formDevolucion");
const condicionDevolucion = document.getElementById("condicionDevolucion");
const grupoDetallesDanio = document.getElementById("grupoDetallesDanio");
const detallesDanio = document.getElementById("detallesDanio");
const cancelarDevolucion = document.getElementById("cancelarDevolucion");

// Elementos del formulario de mantenimiento
const modalRevision = document.getElementById("modalRevision");
const formularioRevision = document.getElementById("formRevision");
const revisionMantenimiento = document.getElementById("revisionMantenimiento");
const cancelarRevision = document.getElementById("cancelarRevision");

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

let prestamos = [];
let equiposDisponibles = [];

let temporizadorPersona;
let solicitudPersona = 0;

let prestamoDevolucionId = null;
let prestamoRevisionId = null;


// -------------------------
// Configuración general
// -------------------------

function obtenerHeaders(extra = {}) {
    const headers = { ...extra };

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

async function consultarLista(url) {
    const respuesta = await fetch(url);

    if (!respuesta.ok) {
        throw new Error("No se pudieron consultar los datos del sistema.");
    }

    return respuesta.json();
}

async function obtenerMensajeError(respuesta, mensajePredeterminado) {
    const error = await respuesta.json().catch(() => ({}));

    return error.error ||
        error.message ||
        mensajePredeterminado;
}

function mostrarMensajeBusqueda(contenedor, mensaje) {
    contenedor.replaceChildren();

    const elemento = document.createElement("div");
    elemento.className = "resultado-vacio";
    elemento.textContent = mensaje;

    contenedor.appendChild(elemento);
    contenedor.classList.add("visible");
}

function ocultarResultados(contenedor) {
    contenedor.replaceChildren();
    contenedor.classList.remove("visible");
}


// -------------------------
// Etiquetas de personas
// -------------------------

function obtenerEtiquetaIdentificador(tipo) {
    if (tipo?.toLowerCase() === "alumno") {
        return "Boleta";
    }

    if (tipo?.toLowerCase() === "profesor") {
        return "No. de empleado";
    }

    return "Identificador";
}

function obtenerTextoIdentificador(persona) {
    const etiqueta = obtenerEtiquetaIdentificador(persona?.tipo);
    const identificador = persona?.identificador;

    return `${etiqueta}: ${identificador || "No registrado"}`;
}


// -------------------------
// Selecciones
// -------------------------

function invalidarSeleccionPersona() {
    selectorPersona.value = "";
    personaSeleccionada.textContent = "";
}

function invalidarSeleccionEquipo() {
    selectorEquipo.value = "";
    equipoSeleccionado.hidden = true;
    nombreEquipoSeleccionado.textContent = "";
    detallesEquipoSeleccionado.textContent = "";
}

function seleccionarEquipo(equipo) {
    selectorEquipo.value = String(equipo.id);

    nombreEquipoSeleccionado.textContent = equipo.nombre;
    detallesEquipoSeleccionado.textContent = obtenerTextoEquipo(equipo);

    equipoSeleccionado.hidden = false;

    campoBuscarEquipo.value = "";

    ocultarResultados(resultadosEquipos);
}


// -------------------------
// Búsqueda de personas
// -------------------------

async function buscarPersonas(texto) {
    const numeroSolicitud = ++solicitudPersona;
    const termino = texto.trim();

    if (termino.length < 2) {
        ocultarResultados(resultadosPersonas);
        return;
    }

    mostrarMensajeBusqueda(
        resultadosPersonas,
        "Buscando personas..."
    );

    try {
        const personas = await consultarLista(
            `/api/personas/buscar?texto=${encodeURIComponent(termino)}`
        );

        if (numeroSolicitud !== solicitudPersona) return;

        const activas = personas.filter(
            persona => persona.activo !== false
        );

        if (activas.length === 0) {
            mostrarMensajeBusqueda(
                resultadosPersonas,
                "No se encontraron personas registradas."
            );
            return;
        }

        resultadosPersonas.replaceChildren();

        activas.forEach(persona => {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "resultado-busqueda";
            boton.setAttribute("role", "option");

            const nombre = document.createElement("strong");
            nombre.textContent = persona.nombre;

            const detalles = document.createElement("small");
            detalles.textContent =
                `${persona.tipo} · ${obtenerTextoIdentificador(persona)}`;

            boton.append(nombre, detalles);

            boton.addEventListener("click", () => {
                selectorPersona.value = String(persona.id);

                campoBuscarPersona.value =
                    `${persona.nombre} (${persona.identificador || "Sin identificador"})`;

                personaSeleccionada.textContent =
                    `Persona seleccionada: ${persona.nombre} · ${persona.tipo} · ${obtenerTextoIdentificador(persona)}`;

                ocultarResultados(resultadosPersonas);
            });

            resultadosPersonas.appendChild(boton);
        });

        resultadosPersonas.classList.add("visible");

    } catch (error) {
        if (numeroSolicitud !== solicitudPersona) return;

        mostrarMensajeBusqueda(
            resultadosPersonas,
            "No se pudo buscar. Intenta nuevamente."
        );
    }
}

campoBuscarPersona.addEventListener("input", () => {
    clearTimeout(temporizadorPersona);

    invalidarSeleccionPersona();

    temporizadorPersona = setTimeout(
        () => buscarPersonas(campoBuscarPersona.value),
        300
    );
});


// -------------------------
// Equipos disponibles
// -------------------------

function obtenerTextoEquipo(equipo) {
    const datos = [
        `ID: ${equipo.id}`,
        equipo.nombre,
        equipo.tipo,
        equipo.numeroSerie
            ? `Serie: ${equipo.numeroSerie}`
            : null
    ].filter(Boolean);

    return datos.join(" · ");
}

async function cargarEquiposDisponibles() {
    try {
        const equipos = await consultarLista("/api/equipos");

        equiposDisponibles = equipos.filter(equipo =>
            equipo.activo !== false &&
            equipo.estado?.toLowerCase() === "disponible"
        );

    } catch (error) {
        equiposDisponibles = [];

        mensajePrestamo.textContent =
            "No se pudieron cargar los equipos disponibles.";
    }
}


// -------------------------
// Mostrar resultados de equipos
// -------------------------

function mostrarEquiposDisponibles(texto = "") {
    const termino = texto.trim().toLocaleLowerCase();

    let encontrados = equiposDisponibles;

    if (termino.length > 0) {
        encontrados = equiposDisponibles.filter(equipo => {
            const datos = [
                equipo.id,
                equipo.nombre,
                equipo.tipo,
                equipo.descripcion,
                equipo.numeroSerie
            ];

            return datos.some(dato =>
                dato !== null &&
                dato !== undefined &&
                String(dato).toLocaleLowerCase().includes(termino)
            );
        });
    }

    if (encontrados.length === 0) {
        mostrarMensajeBusqueda(
            resultadosEquipos,
            termino.length > 0
                ? "No se encontraron equipos disponibles con ese dato."
                : "Actualmente no hay equipos disponibles."
        );

        return;
    }

    resultadosEquipos.replaceChildren();

    encontrados.forEach(equipo => {
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "resultado-busqueda";
        boton.setAttribute("role", "option");

        const nombre = document.createElement("strong");
        nombre.textContent = equipo.nombre;

        const detalles = document.createElement("small");
        detalles.textContent = obtenerTextoEquipo(equipo);

        const disponibilidad = document.createElement("small");
        disponibilidad.textContent = "Disponible";

        boton.append(nombre, detalles, disponibilidad);

        boton.addEventListener("click", () => {
            seleccionarEquipo(equipo);
        });

        resultadosEquipos.appendChild(boton);
    });

    resultadosEquipos.classList.add("visible");
}

campoBuscarEquipo.addEventListener("input", () => {
    invalidarSeleccionEquipo();

    mostrarEquiposDisponibles(campoBuscarEquipo.value);
});

campoBuscarEquipo.addEventListener("focus", () => {
    if (!selectorEquipo.value) {
        mostrarEquiposDisponibles(campoBuscarEquipo.value);
    }
});

quitarEquipo.addEventListener("click", () => {
    invalidarSeleccionEquipo();

    campoBuscarEquipo.value = "";
    campoBuscarEquipo.focus();

    mostrarEquiposDisponibles();
});


// -------------------------
// Registrar un préstamo
// -------------------------

formularioPrestamo.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!selectorPersona.value || !selectorEquipo.value) {
        mensajePrestamo.textContent =
            "Debes buscar y seleccionar una persona y un equipo disponible.";

        return;
    }

    const botonEnviar = formularioPrestamo.querySelector(
        'button[type="submit"]'
    );

    botonEnviar.disabled = true;

    try {
        mensajePrestamo.textContent =
            "Verificando disponibilidad del equipo...";

        const equiposActualizados = await consultarLista("/api/equipos");

        equiposDisponibles = equiposActualizados.filter(equipo =>
            equipo.activo !== false &&
            equipo.estado?.toLowerCase() === "disponible"
        );

        const equipoValido = equiposDisponibles.find(
            equipo => String(equipo.id) === selectorEquipo.value
        );

        if (!equipoValido) {
            invalidarSeleccionEquipo();

            mensajePrestamo.textContent =
                "El equipo seleccionado ya no está disponible. Selecciona otro.";

            mostrarEquiposDisponibles();
            return;
        }

        mensajePrestamo.textContent = "Registrando préstamo...";

        const datos = {
            personaId: Number(selectorPersona.value),
            equipoId: Number(selectorEquipo.value)
        };

        const respuesta = await fetch("/api/prestamos", {
            method: "POST",
            headers: obtenerHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify(datos)
        });

        if (!respuesta.ok) {
            throw new Error(
                await obtenerMensajeError(
                    respuesta,
                    "No se pudo registrar el préstamo."
                )
            );
        }

        formularioPrestamo.reset();

        invalidarSeleccionPersona();
        invalidarSeleccionEquipo();

        ocultarResultados(resultadosPersonas);
        ocultarResultados(resultadosEquipos);

        mensajePrestamo.textContent =
            "Préstamo registrado correctamente.";

        await Promise.all([
            cargarPrestamos(),
            cargarEquiposDisponibles()
        ]);

    } catch (error) {
        mensajePrestamo.textContent = error.message;

    } finally {
        botonEnviar.disabled = false;
    }
});


// -------------------------
// Formato de fechas
// -------------------------

function formatearFechaHora(valor) {
    if (!valor) {
        return "—";
    }

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
        return valor;
    }

    return fecha.toLocaleString("es-MX", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}


// -------------------------
// Búsqueda y mostrar préstamos
// -------------------------

function normalizarTextoBusqueda(valor) {
    return String(valor ?? "")
        .toLocaleLowerCase("es-MX")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}

function mostrarPrestamos() {
    const filtro = filtroPrestamos.value;
    const termino = normalizarTextoBusqueda(buscarHistorial.value);

    const filtrados = prestamos.filter(prestamo => {
        const coincideEstado =
            filtro === "Todos" || prestamo.estado === filtro;

        const datosBusqueda = [
            prestamo.id,
            prestamo.persona?.nombre,
            prestamo.persona?.identificador,
            prestamo.persona?.tipo,
            prestamo.equipo?.id,
            prestamo.equipo?.nombre,
            prestamo.equipo?.tipo,
            prestamo.equipo?.numeroSerie
        ];

        const coincideBusqueda =
            termino === "" ||
            datosBusqueda.some(dato =>
                normalizarTextoBusqueda(dato).includes(termino)
            );

        return coincideEstado && coincideBusqueda;
    });

    tablaPrestamos.replaceChildren();

    if (filtrados.length === 0) {
        const fila = tablaPrestamos.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 8;
        celda.textContent = termino
            ? "No se encontraron préstamos que coincidan con la búsqueda y el filtro seleccionado."
            : "No hay préstamos para mostrar.";

        return;
    }

    filtrados.forEach(prestamo => {
        const fila = tablaPrestamos.insertRow();

        fila.insertCell().textContent = prestamo.id;

        fila.insertCell().textContent =
            prestamo.persona?.nombre || "Sin nombre";

        const celdaIdentificador = fila.insertCell();

        const esAlumno =
            prestamo.persona?.tipo?.toLowerCase() === "alumno";

        if (esAlumno && prestamo.persona?.identificador) {
            celdaIdentificador.textContent =
                String(prestamo.persona.identificador).replace(/\D/g, "");
        } else {
            celdaIdentificador.textContent = "—";
        }

        fila.insertCell().textContent =
            prestamo.equipo?.nombre || "Sin equipo";

        fila.insertCell().textContent =
            formatearFechaHora(prestamo.fechaPrestamo);

        const fechaDevolucion =
            ["Activo", "Vencido"].includes(prestamo.estado)
                ? "Pendiente"
                : prestamo.fechaDevolucionReal
                    ? formatearFechaHora(prestamo.fechaDevolucionReal)
                    : "No registrada";

        fila.insertCell().textContent = fechaDevolucion;

        fila.insertCell().textContent = prestamo.estado;

        const celdaAccion = fila.insertCell();
        celdaAccion.className = "acciones-prestamo";

        if (["Activo", "Vencido"].includes(prestamo.estado)) {

            const botonDevolucion = document.createElement("button");
            botonDevolucion.type = "button";
            botonDevolucion.className = "btn-registrar-devolucion";
            botonDevolucion.textContent = "Registrar devolución";

            botonDevolucion.addEventListener("click", () =>
                abrirFormularioDevolucion(prestamo.id)
            );

            celdaAccion.appendChild(botonDevolucion);

        } else if (
            prestamo.estado === "Devuelto" &&
            prestamo.condicionDevolucion === "CON_DANOS"
        ) {

            if (prestamo.mantenimientoAtendido === true) {

                const estado = document.createElement("span");
                estado.className = "estado-mantenimiento-atendido";
                estado.textContent = "Mantenimiento atendido";

                celdaAccion.appendChild(estado);

            } else {

                const estado = document.createElement("span");
                estado.className = "estado-mantenimiento-pendiente";
                estado.textContent = "Pendiente de revisión";

                const botonRevision = document.createElement("button");
                botonRevision.type = "button";
                botonRevision.className = "btn-revision-mantenimiento";
                botonRevision.textContent = "Registrar revisión";

                botonRevision.addEventListener("click", () =>
                    abrirFormularioRevision(prestamo.id)
                );

                celdaAccion.append(estado, botonRevision);
            }

        } else {
            celdaAccion.textContent = "Sin acciones";
        }
    });
}

async function cargarPrestamos() {
    try {
        prestamos = await consultarLista("/api/prestamos");

        mostrarPrestamos();

    } catch (error) {
        tablaPrestamos.replaceChildren();

        const fila = tablaPrestamos.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 8;
        celda.textContent = error.message;
    }
}


// -------------------------
// Formulario de devolución
// -------------------------

function abrirFormularioDevolucion(id) {
    prestamoDevolucionId = id;

    formularioDevolucion.reset();

    grupoDetallesDanio.hidden = true;
    detallesDanio.required = false;

    modalDevolucion.showModal();
}

condicionDevolucion.addEventListener("change", () => {
    const tieneDanios = condicionDevolucion.value === "CON_DANOS";

    grupoDetallesDanio.hidden = !tieneDanios;
    detallesDanio.required = tieneDanios;

    if (!tieneDanios) {
        detallesDanio.value = "";
    }
});

cancelarDevolucion.addEventListener("click", () => {
    modalDevolucion.close();
    prestamoDevolucionId = null;
});

modalDevolucion.addEventListener("click", evento => {
    if (evento.target === modalDevolucion) {
        modalDevolucion.close();
        prestamoDevolucionId = null;
    }
});

formularioDevolucion.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!prestamoDevolucionId) {
        return;
    }

    const condicion = condicionDevolucion.value;
    const descripcion = detallesDanio.value.trim();

    if (!condicion) {
        alert("Selecciona la condición en la que se recibe el equipo.");
        return;
    }

    if (condicion === "CON_DANOS" && !descripcion) {
        alert("Describe los daños encontrados.");
        detallesDanio.focus();
        return;
    }

    const botonGuardar = formularioDevolucion.querySelector(
        'button[type="submit"]'
    );

    botonGuardar.disabled = true;
    cancelarDevolucion.disabled = true;

    try {
        const respuesta = await fetch(
            `/api/prestamos/${prestamoDevolucionId}/devolver`,
            {
                method: "POST",
                headers: obtenerHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({
                    condicionDevolucion: condicion,
                    detallesDanio: descripcion
                })
            }
        );

        if (!respuesta.ok) {
            throw new Error(
                await obtenerMensajeError(
                    respuesta,
                    "No se pudo registrar la devolución."
                )
            );
        }

        modalDevolucion.close();

        mensajePrestamo.textContent =
            condicion === "CON_DANOS"
                ? "Devolución registrada. El equipo pasó a Mantenimiento."
                : "Devolución registrada. El equipo volvió a estar Disponible.";

        prestamoDevolucionId = null;

        await Promise.all([
            cargarPrestamos(),
            cargarEquiposDisponibles()
        ]);

    } catch (error) {
        mensajePrestamo.textContent = error.message;

    } finally {
        botonGuardar.disabled = false;
        cancelarDevolucion.disabled = false;
    }
});


// -------------------------
// Formulario de revisión de mantenimiento
// -------------------------

function abrirFormularioRevision(id) {
    prestamoRevisionId = id;

    formularioRevision.reset();

    modalRevision.showModal();
}

cancelarRevision.addEventListener("click", () => {
    modalRevision.close();
    prestamoRevisionId = null;
});

modalRevision.addEventListener("click", evento => {
    if (evento.target === modalRevision) {
        modalRevision.close();
        prestamoRevisionId = null;
    }
});

formularioRevision.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!prestamoRevisionId) {
        return;
    }

    const descripcion = revisionMantenimiento.value.trim();

    if (!descripcion) {
        alert("Describe la revisión realizada.");
        revisionMantenimiento.focus();
        return;
    }

    const botonGuardar = formularioRevision.querySelector(
        'button[type="submit"]'
    );

    botonGuardar.disabled = true;
    cancelarRevision.disabled = true;

    try {
        const respuesta = await fetch(
            `/api/prestamos/${prestamoRevisionId}/revision-mantenimiento`,
            {
                method: "POST",
                headers: obtenerHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({
                    revisionMantenimiento: descripcion
                })
            }
        );

        if (!respuesta.ok) {
            throw new Error(
                await obtenerMensajeError(
                    respuesta,
                    "No se pudo registrar la revisión de mantenimiento."
                )
            );
        }

        modalRevision.close();

        prestamoRevisionId = null;

        mensajePrestamo.textContent =
            "Revisión registrada correctamente. Se actualizó el historial del préstamo.";

        await Promise.all([
            cargarPrestamos(),
            cargarEquiposDisponibles()
        ]);

    } catch (error) {
        mensajePrestamo.textContent = error.message;

    } finally {
        botonGuardar.disabled = false;
        cancelarRevision.disabled = false;
    }
});


// -------------------------
// Inicializar módulo
// -------------------------

filtroPrestamos.addEventListener("change", mostrarPrestamos);
buscarHistorial.addEventListener("input", mostrarPrestamos);

async function iniciarModuloPrestamos() {
    await Promise.all([
        cargarPrestamos(),
        cargarEquiposDisponibles()
    ]);
}

iniciarModuloPrestamos();