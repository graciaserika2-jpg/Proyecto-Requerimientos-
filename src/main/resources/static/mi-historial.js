
document.addEventListener("DOMContentLoaded", () => {

    const tabla = document.getElementById("tablaHistorial");

    const buscarPrestamo = document.getElementById("buscarPrestamo");
    const filtroEstado = document.getElementById("filtroEstado");

    const totalPrestamos = document.getElementById("totalPrestamos");
    const totalActivos = document.getElementById("totalActivos");
    const totalDevueltos = document.getElementById("totalDevueltos");

    let prestamos = [];

    // Consultar exclusivamente el historial de la cuenta autenticada.
    async function cargarHistorial() {

        try {
            mostrarMensaje("Cargando historial...");

            const respuesta = await fetch("/api/prestamos/mi-historial");

            if (!respuesta.ok) {
                throw new Error(
                    `Error al consultar el historial. Código: ${respuesta.status}`
                );
            }

            prestamos = await respuesta.json();

            actualizarResumen();
            mostrarPrestamos();

        } catch (error) {
            console.error("Error al cargar el historial:", error);

            mostrarMensaje(
                "No fue posible cargar tu historial. Intenta actualizar la página."
            );
        }
    }

    // Actualizar los contadores.
    function actualizarResumen() {

        totalPrestamos.textContent = prestamos.length;

        totalActivos.textContent = prestamos.filter(
            prestamo => prestamo.estado === "Activo"
        ).length;

        totalDevueltos.textContent = prestamos.filter(
            prestamo => prestamo.estado === "Devuelto"
        ).length;
    }

    // Mostrar un mensaje dentro de la tabla.
    function mostrarMensaje(mensaje) {

        tabla.replaceChildren();

        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = 6;
        celda.className = "historial-mensaje";
        celda.textContent = mensaje;

        fila.appendChild(celda);
        tabla.appendChild(fila);
    }

    // Formatear una fecha y hora.
    function formatearFechaHora(fecha) {

        if (!fecha) {
            return "—";
        }

        const valor = new Date(fecha);

        if (Number.isNaN(valor.getTime())) {
            return "—";
        }

        return new Intl.DateTimeFormat("es-MX", {
            dateStyle: "short",
            timeStyle: "short"
        }).format(valor);
    }

    // Formatear una fecha sin hora.
    function formatearFecha(fecha) {

        if (!fecha) {
            return "—";
        }

        const valor = new Date(`${fecha}T00:00:00`);

        if (Number.isNaN(valor.getTime())) {
            return "—";
        }

        return new Intl.DateTimeFormat("es-MX", {
            dateStyle: "short"
        }).format(valor);
    }

    // Aplicar los filtros.
    function mostrarPrestamos() {

        const texto = buscarPrestamo.value.trim().toLowerCase();
        const estadoSeleccionado = filtroEstado.value;

        const prestamosFiltrados = prestamos.filter(prestamo => {

            const nombre = (prestamo.equipoNombre || "").toLowerCase();
            const tipo = (prestamo.equipoTipo || "").toLowerCase();

            const coincideTexto =
                nombre.includes(texto) || tipo.includes(texto);

            const coincideEstado =
                !estadoSeleccionado
                || prestamo.estado === estadoSeleccionado;

            return coincideTexto && coincideEstado;
        });

        tabla.replaceChildren();

        if (prestamosFiltrados.length === 0) {

            const mensaje = prestamos.length === 0
                ? "Todavía no tienes préstamos registrados."
                : "No se encontraron préstamos con esos filtros.";

            mostrarMensaje(mensaje);
            return;
        }

        prestamosFiltrados.forEach(prestamo => {
            tabla.appendChild(crearFilaPrestamo(prestamo));
        });
    }

    // Crear una fila de la tabla.
    function crearFilaPrestamo(prestamo) {

        const fila = document.createElement("tr");

        // Equipo y tipo.
        const celdaEquipo = document.createElement("td");

        const nombreEquipo = document.createElement("span");
        nombreEquipo.className = "historial-equipo-nombre";
        nombreEquipo.textContent = prestamo.equipoNombre || "Sin nombre";

        const tipoEquipo = document.createElement("span");
        tipoEquipo.className = "historial-equipo-tipo";
        tipoEquipo.textContent = prestamo.equipoTipo || "Sin tipo";

        celdaEquipo.appendChild(nombreEquipo);
        celdaEquipo.appendChild(tipoEquipo);

        // Fecha del préstamo.
        const celdaFechaPrestamo = document.createElement("td");
        celdaFechaPrestamo.textContent =
            formatearFechaHora(prestamo.fechaPrestamo);

        // Devolución prevista.
        const celdaFechaPrevista = document.createElement("td");
        celdaFechaPrevista.textContent =
            formatearFecha(prestamo.fechaDevolucionPrevista);

        // Devolución real.
        const celdaFechaReal = document.createElement("td");
        celdaFechaReal.textContent =
            formatearFechaHora(prestamo.fechaDevolucionReal);

        // Estado.
        const celdaEstado = document.createElement("td");
        const estado = document.createElement("span");

        const estadoTexto = prestamo.estado || "Sin estado";

        estado.className = "historial-estado";
        estado.textContent = estadoTexto;

        if (estadoTexto === "Activo") {
            estado.classList.add("activo");
        } else if (estadoTexto === "Devuelto") {
            estado.classList.add("devuelto");
        }

        celdaEstado.appendChild(estado);

        // Observaciones.
        const celdaObservaciones = document.createElement("td");
        celdaObservaciones.className = "historial-observaciones";
        celdaObservaciones.textContent =
            prestamo.observaciones || "Sin observaciones";

        fila.appendChild(celdaEquipo);
        fila.appendChild(celdaFechaPrestamo);
        fila.appendChild(celdaFechaPrevista);
        fila.appendChild(celdaFechaReal);
        fila.appendChild(celdaEstado);
        fila.appendChild(celdaObservaciones);

        return fila;
    }

    // Escuchar cambios en los filtros.
    buscarPrestamo.addEventListener("input", mostrarPrestamos);
    filtroEstado.addEventListener("change", mostrarPrestamos);

    // Cargar el historial al entrar a la página.
    cargarHistorial();

});
