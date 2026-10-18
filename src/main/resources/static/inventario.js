
// ==========================================
// 1. ELEMENTOS DE LA PÁGINA
// ==========================================

const tablaEquipos = document.getElementById("tablaEquipos");
const buscarEquipo = document.getElementById("buscarEquipo");
const filtroTipo = document.getElementById("filtroTipo");
const filtroEstado = document.getElementById("filtroEstado");

const totalEquipos = document.getElementById("totalEquipos");
const equiposDisponibles = document.getElementById("equiposDisponibles");
const equiposPrestados = document.getElementById("equiposPrestados");
const contadorEquipos = document.getElementById("contadorEquipos");

const modalEquipo = document.getElementById("modalEquipo");
const formEquipo = document.getElementById("formEquipo");
const btnNuevoEquipo = document.getElementById("btnNuevoEquipo");
const btnGuardarEquipo = document.getElementById("guardarEquipo");
const mensajeFormulario = document.getElementById("mensajeFormulario");

let equipos = [];
let equipoEnEdicion = null;

// ==========================================
// 2. CARGAR EQUIPOS DESDE SPRING BOOT
// ==========================================

async function cargarEquipos() {
    tablaEquipos.replaceChildren();

    const fila = document.createElement("tr");
    const celda = document.createElement("td");

    celda.colSpan = 5;
    celda.className = "table-message";
    celda.textContent = "Cargando artículos...";

    fila.appendChild(celda);
    tablaEquipos.appendChild(fila);

    try {
        const respuesta = await fetch("/api/equipos");

        if (!respuesta.ok) {
            throw new Error("No se pudieron consultar los artículos.");
        }

        equipos = await respuesta.json();

        actualizarFiltroTipos();
        mostrarEquipos();

    } catch (error) {
        console.error("Error al cargar el inventario:", error);

        mostrarMensajeTabla(
            "No se pudo cargar el inventario. Comprueba que Spring Boot esté funcionando."
        );

        totalEquipos.textContent = "—";
        equiposDisponibles.textContent = "—";
        equiposPrestados.textContent = "—";
        contadorEquipos.textContent = "No se pudieron cargar los registros.";
    }
}

// ==========================================
// 3. MOSTRAR LOS EQUIPOS EN LA TABLA
// ==========================================

function mostrarEquipos() {
    const busqueda = buscarEquipo.value.trim().toLowerCase();
    const tipoSeleccionado = filtroTipo.value;
    const estadoSeleccionado = filtroEstado.value;

    const equiposFiltrados = equipos.filter((equipo) => {
        const texto = [
            equipo.id,
            equipo.nombre,
            equipo.tipo,
            equipo.descripcion,
            equipo.numeroSerie
        ].join(" ").toLowerCase();

        const coincideBusqueda = texto.includes(busqueda);

        const coincideTipo =
            !tipoSeleccionado || equipo.tipo === tipoSeleccionado;

        const coincideEstado =
            !estadoSeleccionado || equipo.estado === estadoSeleccionado;

        return coincideBusqueda && coincideTipo && coincideEstado;
    });

    actualizarEstadisticas();
    tablaEquipos.replaceChildren();

    if (equiposFiltrados.length === 0) {
        mostrarMensajeTabla(
            equipos.length === 0
                ? "Todavía no hay artículos registrados."
                : "No se encontraron artículos con esos filtros."
        );

        contadorEquipos.textContent = "0 artículos encontrados";
        return;
    }

    equiposFiltrados.forEach((equipo) => {
        const fila = document.createElement("tr");

        // ID
        fila.appendChild(crearCelda(equipo.id ?? "—"));

        // Nombre y descripción
        const celdaNombre = document.createElement("td");

        const nombre = document.createElement("div");
        nombre.className = "article-name";
        nombre.textContent = equipo.nombre || "Sin nombre";

        celdaNombre.appendChild(nombre);

        if (equipo.descripcion) {
            const descripcion = document.createElement("div");
            descripcion.className = "article-description";
            descripcion.textContent = equipo.descripcion;
            descripcion.title = equipo.descripcion;
            celdaNombre.appendChild(descripcion);
        }

        fila.appendChild(celdaNombre);

        // Tipo
        const celdaTipo = document.createElement("td");
        const etiquetaTipo = document.createElement("span");

        etiquetaTipo.className = "type-badge";
        etiquetaTipo.textContent = equipo.tipo || "Sin tipo";

        celdaTipo.appendChild(etiquetaTipo);
        fila.appendChild(celdaTipo);

        // Número de serie
        fila.appendChild(
            crearCelda(equipo.numeroSerie || "Sin número de serie")
        );

        // Estado
        const celdaEstado = document.createElement("td");
        const etiquetaEstado = document.createElement("span");
        const estado = equipo.estado || "Sin estado";

        etiquetaEstado.className =
            "status-badge " + obtenerClaseEstado(estado);

        etiquetaEstado.textContent = estado;

        celdaEstado.appendChild(etiquetaEstado);
        fila.appendChild(celdaEstado);

        // Botón para editar el artículo

        // Botones de acciones
        const celdaAcciones = document.createElement("td");

        // Botón Editar
        const botonEditar = document.createElement("button");

        botonEditar.textContent = "Editar";
        botonEditar.type = "button";
        botonEditar.className = "btn-editar";

        botonEditar.addEventListener("click", () => {
            editarEquipo(equipo);
        });

        celdaAcciones.appendChild(botonEditar);

        // Botón Eliminar
        const botonEliminar = document.createElement("button");

        botonEliminar.textContent = "Eliminar";
        botonEliminar.type = "button";
        botonEliminar.className = "btn-eliminar";

        botonEliminar.addEventListener("click", async () => {
            const confirmar = confirm(
                `¿Deseas eliminar el artículo "${equipo.nombre}"?`
            );

            if (!confirmar) {
                return;
            }

            try {
                const respuesta = await fetch(
                    `/api/equipos/${equipo.id}`,
                    { method: "DELETE" }
                );

                if (!respuesta.ok) {
                    throw new Error(
                        `Error del servidor: ${respuesta.status}`
                    );
                }

                await cargarEquipos();
                mostrarEquipos();

                alert("Artículo eliminado correctamente.");

            } catch (error) {
                console.error("Error al eliminar el artículo:", error);
                alert(
                    "No se pudo eliminar el artículo. Revisa la consola."
                );
            }
        });

        celdaAcciones.appendChild(botonEliminar);
        fila.appendChild(celdaAcciones);

        fila.appendChild(celdaAcciones);

        tablaEquipos.appendChild(fila);
    });

    contadorEquipos.textContent =
        `Mostrando ${equiposFiltrados.length} de ${equipos.length} artículos`;
}

// ==========================================
// 4. CREAR CELDAS DE FORMA SEGURA
// ==========================================

function crearCelda(valor) {
    const celda = document.createElement("td");
    celda.textContent = String(valor);
    return celda;
}

function mostrarMensajeTabla(mensaje) {
    tablaEquipos.replaceChildren();

    const fila = document.createElement("tr");
    const celda = document.createElement("td");

    celda.colSpan = 5;
    celda.className = "table-message";
    celda.textContent = mensaje;

    fila.appendChild(celda);
    tablaEquipos.appendChild(fila);
}

function obtenerClaseEstado(estado) {
    switch (estado.toLowerCase()) {
        case "disponible":
            return "status-disponible";

        case "prestado":
            return "status-prestado";

        case "mantenimiento":
            return "status-mantenimiento";

        default:
            return "";
    }
}

// ==========================================
// 5. ACTUALIZAR ESTADÍSTICAS
// ==========================================

function actualizarEstadisticas() {
    totalEquipos.textContent = equipos.length;

    equiposDisponibles.textContent = equipos.filter(
        equipo => (equipo.estado || "").toLowerCase() === "disponible"
    ).length;

    equiposPrestados.textContent = equipos.filter(
        equipo => (equipo.estado || "").toLowerCase() === "prestado"
    ).length;
}

// ==========================================
// 6. ACTUALIZAR FILTRO DE TIPOS
// ==========================================

function actualizarFiltroTipos() {
    const tipoAnterior = filtroTipo.value;

    filtroTipo.replaceChildren();

    const opcionTodos = document.createElement("option");
    opcionTodos.value = "";
    opcionTodos.textContent = "Todos los tipos";
    filtroTipo.appendChild(opcionTodos);

    const tipos = [
        ...new Set(
            equipos
                .map(equipo => equipo.tipo)
                .filter(tipo => tipo && tipo.trim() !== "")
        )
    ].sort((a, b) => a.localeCompare(b, "es"));

    tipos.forEach((tipo) => {
        const opcion = document.createElement("option");
        opcion.value = tipo;
        opcion.textContent = tipo;
        filtroTipo.appendChild(opcion);
    });

    if (tipos.includes(tipoAnterior)) {
        filtroTipo.value = tipoAnterior;
    }
}

// ==========================================
// 7. ABRIR Y CERRAR EL FORMULARIO
// ==========================================

function abrirModal() {
    equipoEnEdicion = null;
    formEquipo.reset();

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";

    btnGuardarEquipo.textContent = "Guardar artículo";

    modalEquipo.hidden = false;
    document.getElementById("nombre").focus();

}

function cerrarModal() {
    modalEquipo.hidden = true;
    formEquipo.reset();

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";
}

function editarEquipo(equipo) {
    equipoEnEdicion = equipo.id;

    formEquipo.reset();

    document.getElementById("nombre").value = equipo.nombre || "";
    document.getElementById("tipo").value = equipo.tipo || "";
    document.getElementById("descripcion").value = equipo.descripcion || "";
    document.getElementById("numeroSerie").value = equipo.numeroSerie || "";
    document.getElementById("estado").value = equipo.estado || "Disponible";

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";

    btnGuardarEquipo.textContent = "Guardar cambios";
    modalEquipo.hidden = false;

    document.getElementById("nombre").focus();
}

btnNuevoEquipo.addEventListener("click", abrirModal);

document.getElementById("cerrarModal")
    .addEventListener("click", cerrarModal);

document.getElementById("cancelarEquipo")
    .addEventListener("click", cerrarModal);

document.getElementById("cerrarModalFondo")
    .addEventListener("click", cerrarModal);

document.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape" && !modalEquipo.hidden) {
        cerrarModal();
    }
});

// ==========================================
// 8. REGISTRAR UN ARTÍCULO EN MYSQL
// ==========================================

formEquipo.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const equipo = {
        nombre: document.getElementById("nombre").value.trim(),
        tipo: document.getElementById("tipo").value,
        descripcion: document.getElementById("descripcion").value.trim(),
        numeroSerie: document.getElementById("numeroSerie").value.trim(),
        estado: document.getElementById("estado").value
    };

    const editando = equipoEnEdicion !== null;
    const url = editando
        ? `/api/equipos/${equipoEnEdicion}`
        : "/api/equipos";

    const metodo = editando ? "PUT" : "POST";

    console.log("Artículo en edición:", equipoEnEdicion);
    console.log("Solicitud:", metodo, url, equipo);

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";

    btnGuardarEquipo.disabled = true;
    btnGuardarEquipo.textContent = editando
        ? "Guardando cambios..."
        : "Guardando...";

    try {
        const respuesta = await fetch(url, {
            method: metodo,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(equipo)
        });

        if (!respuesta.ok) {
            const detalle = await respuesta.text();
            throw new Error(
                detalle || `Error del servidor: ${respuesta.status}`
            );
        }

        mensajeFormulario.textContent = editando
            ? "Artículo actualizado correctamente."
            : "Artículo registrado correctamente.";

        mensajeFormulario.className = "form-message success";

        await cargarEquipos();

        buscarEquipo.value = "";
        filtroTipo.value = "";
        filtroEstado.value = "";

        mostrarEquipos();

        setTimeout(() => {
            cerrarModal();
        }, 700);

    } catch (error) {
        console.error("Error al guardar el artículo:", error);

        mensajeFormulario.textContent = editando
            ? "No se pudo actualizar el artículo. Inténtalo de nuevo."
            : "No se pudo guardar el artículo. Inténtalo de nuevo.";

        mensajeFormulario.className = "form-message error";

    } finally {
        btnGuardarEquipo.disabled = false;
        btnGuardarEquipo.textContent = editando
            ? "Guardar cambios"
            : "Guardar artículo";
    }
});




// ==========================================
// 9. ACTIVAR BUSCADOR Y FILTROS
// ==========================================

buscarEquipo.addEventListener("input", mostrarEquipos);
filtroTipo.addEventListener("change", mostrarEquipos);
filtroEstado.addEventListener("change", mostrarEquipos);

// ==========================================
// 10. INICIAR LA PÁGINA
// ==========================================

cargarEquipos();
