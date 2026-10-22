
const formularioPrestamo = document.getElementById("formPrestamo");
const selectorPersona = document.getElementById("personaId");
const selectorEquipo = document.getElementById("equipoId");
const tablaPrestamos = document.getElementById("tablaPrestamos");
const mensajePrestamo = document.getElementById("mensajePrestamo");
const filtroPrestamos = document.getElementById("filtroPrestamos");

let prestamos = [];
let personas = [];
let equipos = [];

async function consultarLista(url) {
    const respuesta = await fetch(url);

    if (!respuesta.ok) {
        throw new Error("No se pudieron consultar los datos del sistema.");
    }

    return respuesta.json();
}

function agregarOpcion(selector, valor, texto) {
    const opcion = document.createElement("option");
    opcion.value = valor;
    opcion.textContent = texto;
    selector.appendChild(opcion);
}

async function cargarOpciones() {
    try {
        [personas, equipos] = await Promise.all([
            consultarLista("/api/personas"),
            consultarLista("/api/equipos")
        ]);

        selectorPersona.replaceChildren();
        agregarOpcion(selectorPersona, "", "Selecciona una persona");

        personas.forEach(persona => {
            agregarOpcion(
                selectorPersona,
                persona.id,
                `${persona.nombre} (${persona.tipo}) - ${persona.identificador}`
            );
        });

        selectorEquipo.replaceChildren();
        agregarOpcion(selectorEquipo, "", "Selecciona un equipo");

        equipos
            .filter(equipo => equipo.estado === "Disponible")
            .forEach(equipo => {
                agregarOpcion(
                    selectorEquipo,
                    equipo.id,
                    `${equipo.nombre} - ${equipo.tipo}`
                );
            });

        if (personas.length === 0) {
            mensajePrestamo.textContent =
                "Primero registra al menos una persona en el módulo Personas.";
        } else if (selectorEquipo.options.length === 1) {
            mensajePrestamo.textContent =
                "No hay equipos disponibles para prestar.";
        }
    } catch (error) {
        mensajePrestamo.textContent = error.message;
    }
}

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
        fila.insertCell().textContent = error.message;
    }
}

formularioPrestamo.addEventListener("submit", async evento => {
    evento.preventDefault();

    mensajePrestamo.textContent = "Registrando préstamo...";

    const datos = {
        personaId: Number(selectorPersona.value),
        equipoId: Number(selectorEquipo.value),
        fechaDevolucionPrevista:
            document.getElementById("fechaDevolucionPrevista").value,
        observaciones:
            document.getElementById("observaciones").value.trim()
    };

    try {
        const respuesta = await fetch("/api/prestamos", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos)
        });

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            throw new Error(
                error.error || error.message || "No se pudo registrar el préstamo."
            );
        }

        formularioPrestamo.reset();
        mensajePrestamo.textContent = "Préstamo registrado correctamente.";

        await Promise.all([
            cargarOpciones(),
            cargarPrestamos()
        ]);
    } catch (error) {
        mensajePrestamo.textContent = error.message;
    }
});

async function devolverPrestamo(id, boton) {
    if (!confirm("¿Confirmas que deseas registrar la devolución de este equipo?")) {
        return;
    }

    boton.disabled = true;

    try {
        const respuesta = await fetch(`/api/prestamos/${id}/devolver`, {
            method: "POST"
        });

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            throw new Error(
                error.error || error.message || "No se pudo registrar la devolución."
            );
        }

        mensajePrestamo.textContent = "Devolución registrada correctamente.";

        await Promise.all([
            cargarOpciones(),
            cargarPrestamos()
        ]);
    } catch (error) {
        mensajePrestamo.textContent = error.message;
        boton.disabled = false;
    }
}

filtroPrestamos.addEventListener("change", mostrarPrestamos);

async function iniciarModuloPrestamos() {
    await Promise.all([
        cargarOpciones(),
        cargarPrestamos()
    ]);
}

iniciarModuloPrestamos();
