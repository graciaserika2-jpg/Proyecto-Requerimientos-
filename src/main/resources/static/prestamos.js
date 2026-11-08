
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
const campoObservaciones = document.getElementById("observaciones");

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

let prestamos = [];
let equiposDisponibles = [];

let temporizadorPersona;
let solicitudPersona = 0;


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


// Al escribir, se elimina la selección anterior.
campoBuscarEquipo.addEventListener("input", () => {
    invalidarSeleccionEquipo();

    mostrarEquiposDisponibles(campoBuscarEquipo.value);
});


// Al entrar al buscador, se muestran los equipos disponibles.
campoBuscarEquipo.addEventListener("focus", () => {
    if (!selectorEquipo.value) {
        mostrarEquiposDisponibles(campoBuscarEquipo.value);
    }
});


// Botón para quitar el equipo seleccionado.
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
            equipoId: Number(selectorEquipo.value),
            observaciones: campoObservaciones.value.trim()
        };

        const respuesta = await fetch("/api/prestamos", {
            method: "POST",
            headers: obtenerHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify(datos)
        });

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));

            throw new Error(
                error.error ||
                error.message ||
                "No se pudo registrar el préstamo."
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
// Mostrar préstamos
// -------------------------

function mostrarPrestamos() {
    const filtro = filtroPrestamos.value;

    const filtrados = prestamos.filter(prestamo =>
        filtro === "Todos" || prestamo.estado === filtro
    );

    tablaPrestamos.replaceChildren();

    if (filtrados.length === 0) {
        const fila = tablaPrestamos.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 8;
        celda.textContent = "No hay préstamos para mostrar.";

        return;
    }

    filtrados.forEach(prestamo => {
        const fila = tablaPrestamos.insertRow();

        // ID del préstamo
        fila.insertCell().textContent = prestamo.id;

        // Nombre de la persona
        fila.insertCell().textContent =
            prestamo.persona?.nombre || "Sin nombre";

        // Boleta o número de empleado
        const celdaIdentificador = fila.insertCell();

        const tipoPersona = prestamo.persona?.tipo;
        const identificador = prestamo.persona?.identificador;

        celdaIdentificador.textContent = identificador
            ? `${obtenerEtiquetaIdentificador(tipoPersona)}: ${identificador}`
            : "No registrado";

        // Equipo
        fila.insertCell().textContent =
            prestamo.equipo?.nombre || "Sin equipo";

        // Fecha del préstamo
        fila.insertCell().textContent =
            formatearFechaHora(prestamo.fechaPrestamo);

        // Fecha real de devolución
        const fechaDevolucion =
            prestamo.estado === "Activo"
                ? "Pendiente"
                : prestamo.fechaDevolucionReal
                    ? formatearFechaHora(prestamo.fechaDevolucionReal)
                    : "No registrada";

        fila.insertCell().textContent = fechaDevolucion;

        // Estado
        fila.insertCell().textContent = prestamo.estado;

        // Acción
        const celdaAccion = fila.insertCell();

        if (prestamo.estado === "Activo") {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.textContent = "Registrar devolución";

            boton.addEventListener("click", () =>
                devolverPrestamo(prestamo.id, boton)
            );

            celdaAccion.appendChild(boton);

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
// Registrar una devolución
// -------------------------

async function devolverPrestamo(id, boton) {
    if (!confirm(
        "¿Confirmas que deseas registrar la devolución de este equipo?"
    )) {
        return;
    }

    boton.disabled = true;

    try {
        const respuesta = await fetch(
            `/api/prestamos/${id}/devolver`,
            {
                method: "POST",
                headers: obtenerHeaders()
            }
        );

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));

            throw new Error(
                error.error ||
                error.message ||
                "No se pudo registrar la devolución."
            );
        }

        mensajePrestamo.textContent =
            "Devolución registrada correctamente.";

        await Promise.all([
            cargarPrestamos(),
            cargarEquiposDisponibles()
        ]);

    } catch (error) {
        mensajePrestamo.textContent = error.message;

        boton.disabled = false;
    }
}


// -------------------------
// Inicializar módulo
// -------------------------

filtroPrestamos.addEventListener("change", mostrarPrestamos);

async function iniciarModuloPrestamos() {
    await Promise.all([
        cargarPrestamos(),
        cargarEquiposDisponibles()
    ]);
}

iniciarModuloPrestamos();
