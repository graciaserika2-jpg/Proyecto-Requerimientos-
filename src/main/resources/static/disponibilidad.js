
document.addEventListener("DOMContentLoaded", () => {

    const tabla = document.getElementById("tablaDisponibilidad");

    const buscarEquipo = document.getElementById("buscarEquipo");
    const filtroTipo = document.getElementById("filtroTipo");
    const filtroEstado = document.getElementById("filtroEstado");

    const totalEquipos = document.getElementById("totalEquipos");
    const totalDisponibles = document.getElementById("totalDisponibles");
    const totalPrestados = document.getElementById("totalPrestados");

    let equipos = [];

    // Consultar los equipos registrados.
    async function cargarEquipos() {

        try {
            tabla.innerHTML = `
                <tr>
                    <td colspan="4" class="disponibilidad-mensaje">
                        Cargando equipos...
                    </td>
                </tr>
            `;

            const respuesta = await fetch("/api/disponibilidad/equipos");

            if (!respuesta.ok) {
                throw new Error(
                    `Error al consultar los equipos. Código: ${respuesta.status}`
                );
            }

            equipos = await respuesta.json();

            actualizarResumen();
            mostrarEquipos();

        } catch (error) {
            console.error("Error al cargar la disponibilidad:", error);

            mostrarMensaje("No fue posible cargar los equipos. Intenta actualizar la página.");
        }
    }

    // Actualizar los contadores.
    function actualizarResumen() {

        totalEquipos.textContent = equipos.length;

        totalDisponibles.textContent = equipos.filter(
            equipo => equipo.estado === "Disponible"
        ).length;

        totalPrestados.textContent = equipos.filter(
            equipo => equipo.estado === "Prestado"
        ).length;
    }

    // Mostrar un mensaje dentro de la tabla.
    function mostrarMensaje(mensaje) {

        tabla.replaceChildren();

        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = 4;
        celda.className = "disponibilidad-mensaje";
        celda.textContent = mensaje;

        fila.appendChild(celda);
        tabla.appendChild(fila);
    }

    // Aplicar los filtros.
    function mostrarEquipos() {

        const texto = buscarEquipo.value.trim().toLowerCase();
        const tipoSeleccionado = filtroTipo.value;
        const estadoSeleccionado = filtroEstado.value;

        const equiposFiltrados = equipos.filter(equipo => {

            const nombre = (equipo.nombre || "").toLowerCase();
            const descripcion = (equipo.descripcion || "").toLowerCase();

            const coincideTexto =
                nombre.includes(texto) || descripcion.includes(texto);

            const coincideTipo =
                !tipoSeleccionado || equipo.tipo === tipoSeleccionado;

            const coincideEstado =
                !estadoSeleccionado || equipo.estado === estadoSeleccionado;

            return coincideTexto && coincideTipo && coincideEstado;
        });

        tabla.replaceChildren();

        if (equiposFiltrados.length === 0) {
            mostrarMensaje("No se encontraron equipos con esos filtros.");
            return;
        }

        equiposFiltrados.forEach(equipo => {
            tabla.appendChild(crearFilaEquipo(equipo));
        });
    }

    // Crear una fila de la tabla.
    function crearFilaEquipo(equipo) {

        const fila = document.createElement("tr");

        const celdaNombre = document.createElement("td");
        celdaNombre.className = "equipo-nombre";
        celdaNombre.textContent = equipo.nombre || "Sin nombre";

        const celdaTipo = document.createElement("td");
        const tipo = document.createElement("span");

        tipo.className = "equipo-tipo";
        tipo.textContent = equipo.tipo || "Sin tipo";

        celdaTipo.appendChild(tipo);

        const celdaDescripcion = document.createElement("td");
        celdaDescripcion.textContent =
            equipo.descripcion || "Sin descripción";

        const celdaEstado = document.createElement("td");
        const estado = document.createElement("span");

        const estadoTexto = equipo.estado || "Sin estado";

        estado.className = "equipo-estado";
        estado.textContent = estadoTexto;

        switch (estadoTexto) {

            case "Disponible":
                estado.classList.add("disponible");
                break;

            case "Prestado":
                estado.classList.add("prestado");
                break;

            case "Mantenimiento":
                estado.classList.add("mantenimiento");
                break;
        }

        celdaEstado.appendChild(estado);

        fila.appendChild(celdaNombre);
        fila.appendChild(celdaTipo);
        fila.appendChild(celdaDescripcion);
        fila.appendChild(celdaEstado);

        return fila;
    }

    // Escuchar cambios en los filtros.
    buscarEquipo.addEventListener("input", mostrarEquipos);
    filtroTipo.addEventListener("change", mostrarEquipos);
    filtroEstado.addEventListener("change", mostrarEquipos);

    // Cargar los equipos al entrar a la página.
    cargarEquipos();

});
