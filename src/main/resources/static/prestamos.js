
const formularioPrestamo = document.getElementById("formPrestamo");

const campoBuscarPersona = document.getElementById("buscarPersona");
const selectorPersona = document.getElementById("personaId");
const resultadosPersonas = document.getElementById("resultadosPersonas");
const personaSeleccionada = document.getElementById("personaSeleccionada");

const listaEquipos = document.getElementById("listaEquipos");
const campoBuscarEquipo = document.getElementById("buscarEquipo");
const selectorEquipo = document.getElementById("equipoId");
const resultadosEquipos = document.getElementById("resultadosEquipos");
const equipoSeleccionado = document.getElementById("equipoSeleccionado");

const tablaPrestamos = document.getElementById("tablaPrestamos");
const mensajePrestamo = document.getElementById("mensajePrestamo");
const filtroPrestamos = document.getElementById("filtroPrestamos");
const campoObservaciones = document.getElementById("observaciones");

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

let prestamos = [];
let equiposDisponibles = [];

let temporizadorPersona;
let temporizadorEquipo;
let solicitudPersona = 0;

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

function invalidarSeleccionPersona() {
    selectorPersona.value = "";
    personaSeleccionada.textContent = "";
}

function invalidarSeleccionEquipo() {
    selectorEquipo.value = "";
    equipoSeleccionado.textContent = "";
    listaEquipos.value = "";
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

    mostrarMensajeBusqueda(resultadosPersonas, "Buscando personas...");

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
                `${persona.tipo} · Identificador: ${persona.identificador}`;

            boton.append(nombre, detalles);

            boton.addEventListener("click", () => {
                selectorPersona.value = persona.id;
                campoBuscarPersona.value =
                    `${persona.nombre} (${persona.tipo})`;

                personaSeleccionada.textContent =
                    `Persona seleccionada: ${persona.nombre}`;

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
// Cargar equipos disponibles
// -------------------------

function obtenerTextoEquipo(equipo) {
    const datos = [
        `ID: ${equipo.id}`,
        equipo.nombre,
        equipo.tipo,
        equipo.numeroSerie ? `Serie: ${equipo.numeroSerie}` : null
    ].filter(Boolean);

    return datos.join(" — ");
}

async function cargarEquiposDisponibles() {
    try {
        const equipos = await consultarLista("/api/equipos");

        equiposDisponibles = equipos.filter(equipo =>
            equipo.activo !== false &&
            equipo.estado?.toLowerCase() === "disponible"
        );

        listaEquipos.replaceChildren();

        const opcionInicial = document.createElement("option");
        opcionInicial.value = "";
        opcionInicial.textContent = equiposDisponibles.length > 0
            ? "Selecciona un equipo disponible..."
            : "No hay equipos disponibles";

        listaEquipos.appendChild(opcionInicial);

        equiposDisponibles.forEach(equipo => {
            const opcion = document.createElement("option");

            opcion.value = equipo.id;
            opcion.textContent = obtenerTextoEquipo(equipo);

            listaEquipos.appendChild(opcion);
        });

    } catch (error) {
        listaEquipos.replaceChildren();

        const opcion = document.createElement("option");
        opcion.value = "";
        opcion.textContent = "No se pudieron cargar los equipos";

        listaEquipos.appendChild(opcion);

        mensajePrestamo.textContent = error.message;
    }
}

// Selección mediante lista desplegable.

listaEquipos.addEventListener("change", () => {
    const id = listaEquipos.value;

    invalidarSeleccionEquipo();
    ocultarResultados(resultadosEquipos);

    if (!id) {
        campoBuscarEquipo.value = "";
        return;
    }

    const equipo = equiposDisponibles.find(
        elemento => String(elemento.id) === id
    );

    if (!equipo) {
        campoBuscarEquipo.value = "";
        return;
    }

    selectorEquipo.value = equipo.id;
    campoBuscarEquipo.value = obtenerTextoEquipo(equipo);

    equipoSeleccionado.textContent =
        `Equipo seleccionado: ${equipo.nombre}`;
});

// -------------------------
// Búsqueda manual de equipos
// -------------------------

function buscarEquiposLocalmente(texto) {
    const termino = texto.trim().toLocaleLowerCase();

    if (termino.length === 0) {
        ocultarResultados(resultadosEquipos);
        return;
    }

    const encontrados = equiposDisponibles.filter(equipo => {
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

    if (encontrados.length === 0) {
        mostrarMensajeBusqueda(
            resultadosEquipos,
            "No se encontraron equipos disponibles con ese dato."
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

        boton.append(nombre, detalles);

        boton.addEventListener("click", () => {
            selectorEquipo.value = equipo.id;
            listaEquipos.value = String(equipo.id);
            campoBuscarEquipo.value = obtenerTextoEquipo(equipo);

            equipoSeleccionado.textContent =
                `Equipo seleccionado: ${equipo.nombre}`;

            ocultarResultados(resultadosEquipos);
        });

        resultadosEquipos.appendChild(boton);
    });

    resultadosEquipos.classList.add("visible");
}

campoBuscarEquipo.addEventListener("input", () => {
    invalidarSeleccionEquipo();

    buscarEquiposLocalmente(campoBuscarEquipo.value);
});

// -------------------------
// Registrar un préstamo
// -------------------------

formularioPrestamo.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!selectorPersona.value || !selectorEquipo.value) {
        mensajePrestamo.textContent =
            "Busca y selecciona una persona y un equipo disponible.";
        return;
    }

    // Comprobar que el equipo siga en nuestra lista de disponibles.
    const equipoValido = equiposDisponibles.some(
        equipo => String(equipo.id) === selectorEquipo.value
    );

    if (!equipoValido) {
        mensajePrestamo.textContent =
            "El equipo seleccionado ya no está disponible. Actualiza la selección.";
        return;
    }

    mensajePrestamo.textContent = "Registrando préstamo...";

    const botonEnviar = formularioPrestamo.querySelector(
        'button[type="submit"]'
    );

    botonEnviar.disabled = true;

    const datos = {
        personaId: Number(selectorPersona.value),
        equipoId: Number(selectorEquipo.value),
        observaciones: campoObservaciones.value.trim()
    };

    try {
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

        celda.colSpan = 7;
        celda.textContent = "No hay préstamos para mostrar.";
        return;
    }

    filtrados.forEach(prestamo => {
        const fila = tablaPrestamos.insertRow();

        const valores = [
            prestamo.id,
            prestamo.persona?.nombre || "Sin nombre",
            prestamo.equipo?.nombre || "Sin equipo",
            prestamo.fechaPrestamo
                ? new Date(prestamo.fechaPrestamo).toLocaleString("es-MX")
                : "—",
            prestamo.fechaDevolucionPrevista || "—",
            prestamo.estado
        ];

        valores.forEach(valor => {
            fila.insertCell().textContent = valor ?? "";
        });

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

        celda.colSpan = 7;
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
