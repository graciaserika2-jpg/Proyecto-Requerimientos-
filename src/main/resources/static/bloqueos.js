
document.addEventListener("DOMContentLoaded", () => {

    const tablaActivos = document.getElementById("tablaBloqueosActivos");
    const tablaHistorial = document.getElementById("tablaHistorialBloqueos");

    const modal = document.getElementById("modalLevantarBloqueo");
    const formulario = document.getElementById("formLevantarBloqueo");
    const campoMotivo = document.getElementById("motivoLevantamiento");
    const cancelar = document.getElementById("cancelarLevantarBloqueo");

    let bloqueoSeleccionado = null;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function obtenerHeaders() {
        const headers = {
            "Content-Type": "application/json"
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        return headers;
    }

    async function consultar(url, opciones = {}) {

        const respuesta = await fetch(url, {
            credentials: "same-origin",
            ...opciones,
            headers: {
                ...obtenerHeaders(),
                ...(opciones.headers || {})
            }
        });

        if (!respuesta.ok) {
            throw new Error("Error HTTP " + respuesta.status);
        }

        return respuesta.json();
    }

    function formatearFecha(fecha) {

        if (!fecha) return "—";

        const fechaObjeto = new Date(fecha);

        if (Number.isNaN(fechaObjeto.getTime())) {
            return String(fecha);
        }

        return fechaObjeto.toLocaleString("es-MX", {
            dateStyle: "short",
            timeStyle: "short"
        });
    }

    function crearCelda(fila, contenido) {

        const celda = document.createElement("td");

        celda.textContent = contenido ?? "—";

        fila.appendChild(celda);

        return celda;
    }

    function abrirModal(idBloqueo) {

        bloqueoSeleccionado = idBloqueo;

        formulario.reset();

        modal.hidden = false;

        campoMotivo.focus();
    }

    function cerrarModal() {

        modal.hidden = true;

        formulario.reset();

        bloqueoSeleccionado = null;
    }

    async function cargarBloqueosActivos() {

        try {

            const bloqueos = await consultar(
                "/api/prestamos/bloqueos/activos"
            );

            tablaActivos.innerHTML = "";

            if (!bloqueos.length) {

                tablaActivos.innerHTML =
                    '<tr><td colspan="6">No hay bloqueos activos.</td></tr>';

                return;
            }

            bloqueos.forEach(bloqueo => {

                const fila = document.createElement("tr");

                crearCelda(fila, bloqueo.persona?.nombre);
                crearCelda(fila, bloqueo.persona?.identificador);
                crearCelda(fila, bloqueo.equipo);
                crearCelda(fila, formatearFecha(bloqueo.fechaBloqueo));
                crearCelda(fila, bloqueo.motivo);

                const celdaAcciones = document.createElement("td");

                const boton = document.createElement("button");

                boton.type = "button";
                boton.textContent = "Levantar bloqueo";
                boton.className = "btn-editar";

                boton.addEventListener("click", () => {
                    abrirModal(bloqueo.id);
                });

                celdaAcciones.appendChild(boton);

                fila.appendChild(celdaAcciones);

                tablaActivos.appendChild(fila);
            });

        } catch (error) {

            console.error("Error al cargar bloqueos activos:", error);

            tablaActivos.innerHTML =
                '<tr><td colspan="6">No se pudieron cargar los bloqueos.</td></tr>';
        }
    }

    async function cargarHistorialBloqueos() {

        try {

            const historial = await consultar(
                "/api/prestamos/bloqueos/historial"
            );

            tablaHistorial.innerHTML = "";

            if (!historial.length) {

                tablaHistorial.innerHTML =
                    '<tr><td colspan="9">Todavía no hay registros.</td></tr>';

                return;
            }

            historial.forEach(bloqueo => {

                const fila = document.createElement("tr");

                crearCelda(fila, bloqueo.persona?.nombre);
                crearCelda(fila, bloqueo.persona?.identificador);
                crearCelda(fila, bloqueo.equipo);
                crearCelda(fila, formatearFecha(bloqueo.fechaBloqueo));
                crearCelda(fila, bloqueo.motivo);

                crearCelda(
                    fila,
                    bloqueo.activo ? "Activo" : "Levantado"
                );

                crearCelda(
                    fila,
                    formatearFecha(bloqueo.fechaLevantamiento)
                );

                crearCelda(fila, bloqueo.levantadoPor);
                crearCelda(fila, bloqueo.motivoLevantamiento);

                tablaHistorial.appendChild(fila);
            });

        } catch (error) {

            console.error("Error al cargar historial:", error);

            tablaHistorial.innerHTML =
                '<tr><td colspan="9">No se pudo cargar el historial.</td></tr>';
        }
    }

    async function cargarTodo() {

        await Promise.all([
            cargarBloqueosActivos(),
            cargarHistorialBloqueos()
        ]);
    }

    formulario.addEventListener("submit", async evento => {

        evento.preventDefault();

        if (bloqueoSeleccionado === null) {
            return;
        }

        const motivo = campoMotivo.value.trim();

        if (!motivo) {

            alert("Debes escribir el motivo del levantamiento.");

            campoMotivo.focus();

            return;
        }

        try {

            await consultar(
                `/api/prestamos/bloqueos/${bloqueoSeleccionado}/levantar`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        motivoLevantamiento: motivo
                    })
                }
            );

            alert("El bloqueo se levantó correctamente.");

            cerrarModal();

            await cargarTodo();

        } catch (error) {

            console.error("Error al levantar bloqueo:", error);

            alert(
                "No se pudo levantar el bloqueo. Revisa la consola para conocer el error."
            );
        }
    });

    cancelar.addEventListener("click", cerrarModal);

    modal.addEventListener("click", evento => {

        if (
            evento.target === modal ||
            evento.target.classList.contains("modal-backdrop")
        ) {
            cerrarModal();
        }
    });

    cargarTodo();

});
