
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

const modalReportes = document.getElementById("modalReportes");
const contenidoReportes = document.getElementById("contenidoReportes");
const tituloModalReportes = document.getElementById("tituloModalReportes");

let equipos = [];
let equipoEnEdicion = null;
let equipoActualReportes = null;

// Contiene el historial de préstamos con daños.
// null significa que todavía no se pudo consultar.
let historialPrestamos = null;


// ==========================================
// 2. OBTENER TOKEN CSRF
// ==========================================

function obtenerCabecerasCSRF() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const nombreCabecera = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (!token || !nombreCabecera) {
        throw new Error(
            "No se encontró el token de seguridad CSRF. Recarga la página e inténtalo nuevamente."
        );
    }

    return {
        [nombreCabecera]: token
    };
}


// ==========================================
// 3. CARGAR EQUIPOS E HISTORIAL DE REPORTES
// ==========================================

async function cargarEquipos() {

    tablaEquipos.replaceChildren();

    const fila = document.createElement("tr");
    const celda = document.createElement("td");

    celda.colSpan = 7;
    celda.className = "table-message";
    celda.textContent = "Cargando artículos...";

    fila.appendChild(celda);
    tablaEquipos.appendChild(fila);

    try {

        const respuesta = await fetch("/api/equipos");

        if (!respuesta.ok) {
            throw new Error(
                `No se pudieron consultar los artículos. Error ${respuesta.status}`
            );
        }

        equipos = await respuesta.json();

        // Consultar también los préstamos para conocer los reportes históricos.
        // Si falla esta consulta, el inventario seguirá funcionando.
        try {

            const respuestaPrestamos = await fetch("/api/prestamos");

            if (!respuestaPrestamos.ok) {
                throw new Error(
                    `Error ${respuestaPrestamos.status} al consultar el historial`
                );
            }

            const prestamos = await respuestaPrestamos.json();

            historialPrestamos = prestamos.filter(prestamo =>
                prestamo.condicionDevolucion === "CON_DANOS"
            );

        } catch (error) {

            console.error("No se pudo cargar el historial de reportes:", error);

            historialPrestamos = null;
        }

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
// 4. MOSTRAR LOS EQUIPOS EN LA TABLA
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


        // ==========================================
        // COLUMNA DE REPORTES
        // ==========================================

        const celdaReportes = document.createElement("td");

        const reportesDelEquipo = historialPrestamos === null
            ? null
            : historialPrestamos.filter(prestamo =>
                prestamo.equipo
                && Number(prestamo.equipo.id) === Number(equipo.id)
            );

        const tieneReportes =
            reportesDelEquipo !== null && reportesDelEquipo.length > 0;

        // Si el historial no se pudo consultar, no afirmamos que no existen reportes.
        if (historialPrestamos === null) {

            const mensaje = document.createElement("span");
            mensaje.className = "reportes-no-disponibles";
            mensaje.textContent = "No disponible";

            celdaReportes.appendChild(mensaje);

        } else if (tieneReportes || equipo.estado === "Mantenimiento") {

            const botonReportes = document.createElement("button");

            botonReportes.textContent = "Ver reportes";
            botonReportes.type = "button";
            botonReportes.className = "btn-reportes";

            botonReportes.addEventListener("click", () => {
                abrirModalReportes(equipo);
            });

            celdaReportes.appendChild(botonReportes);

        } else {

            const mensaje = document.createElement("span");
            mensaje.className = "sin-reportes";
            mensaje.textContent = "Sin reportes";

            celdaReportes.appendChild(mensaje);
        }

        fila.appendChild(celdaReportes);


        // ==========================================
        // COLUMNA DE ACCIONES
        // ==========================================

        const celdaAcciones = document.createElement("td");

        // Botón editar
        const botonEditar = document.createElement("button");

        botonEditar.textContent = "Editar";
        botonEditar.type = "button";
        botonEditar.className = "btn-editar";

        botonEditar.addEventListener("click", () => {
            editarEquipo(equipo);
        });

        celdaAcciones.appendChild(botonEditar);

        // Botón eliminar
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
                    {
                        method: "DELETE",
                        headers: obtenerCabecerasCSRF()
                    }
                );

                if (!respuesta.ok) {

                    const detalle = await respuesta.text();

                    throw new Error(
                        detalle || `Error del servidor: ${respuesta.status}`
                    );
                }

                await cargarEquipos();

                alert("Artículo eliminado correctamente.");

            } catch (error) {

                console.error("Error al eliminar el artículo:", error);

                alert(
                    error.message ||
                    "No se pudo eliminar el artículo. Revisa la consola."
                );
            }
        });

        celdaAcciones.appendChild(botonEliminar);

        fila.appendChild(celdaAcciones);
        tablaEquipos.appendChild(fila);
    });

    contadorEquipos.textContent =
        `Mostrando ${equiposFiltrados.length} de ${equipos.length} artículos`;
}


// ==========================================
// 5. CREAR CELDAS DE FORMA SEGURA
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

    celda.colSpan = 7;
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
// 6. ACTUALIZAR ESTADÍSTICAS
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
// 7. ACTUALIZAR FILTRO DE TIPOS
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
// 8. ABRIR Y CERRAR EL FORMULARIO DE EQUIPO
// ==========================================

function abrirModal() {

    equipoEnEdicion = null;

    formEquipo.reset();

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";

    btnGuardarEquipo.textContent = "Guardar artículo";

    document.getElementById("tituloModal").textContent =
        "Registrar artículo";

    modalEquipo.hidden = false;

    document.getElementById("nombre").focus();
}


function cerrarModal() {

    modalEquipo.hidden = true;

    formEquipo.reset();

    equipoEnEdicion = null;

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";
}


function editarEquipo(equipo) {

    equipoEnEdicion = equipo.id;

    formEquipo.reset();

    document.getElementById("nombre").value =
        equipo.nombre || "";

    document.getElementById("tipo").value =
        equipo.tipo || "";

    document.getElementById("descripcion").value =
        equipo.descripcion || "";

    document.getElementById("numeroSerie").value =
        equipo.numeroSerie || "";

    document.getElementById("estado").value =
        equipo.estado || "Disponible";

    mensajeFormulario.textContent = "";
    mensajeFormulario.className = "form-message";

    btnGuardarEquipo.textContent = "Guardar cambios";

    document.getElementById("tituloModal").textContent =
        "Editar artículo";

    modalEquipo.hidden = false;

    document.getElementById("nombre").focus();
}


// ==========================================
// 9. CONSULTAR REPORTES DE DAÑOS
// ==========================================

async function abrirModalReportes(equipo) {

    equipoActualReportes = equipo;

    tituloModalReportes.textContent =
        `Reportes de daños: ${equipo.nombre}`;

    contenidoReportes.replaceChildren();

    const mensajeCarga = document.createElement("p");
    mensajeCarga.textContent = "Cargando reportes...";

    contenidoReportes.appendChild(mensajeCarga);

    modalReportes.hidden = false;

    await cargarReportesEquipo(equipo);
}


async function cargarReportesEquipo(equipo) {

    contenidoReportes.replaceChildren();

    const mensajeCarga = document.createElement("p");
    mensajeCarga.textContent = "Consultando historial de daños...";

    contenidoReportes.appendChild(mensajeCarga);

    try {

        // Consultar siempre el historial actualizado desde el servidor.
        const respuesta = await fetch("/api/prestamos");

        if (!respuesta.ok) {
            throw new Error(
                `No se pudieron consultar los reportes. Error ${respuesta.status}`
            );
        }

        const prestamos = await respuesta.json();

        const reportes = prestamos.filter(prestamo =>
            prestamo.equipo
            && Number(prestamo.equipo.id) === Number(equipo.id)
            && prestamo.condicionDevolucion === "CON_DANOS"
        );

        mostrarReportesEquipo(equipo, reportes);

    } catch (error) {

        console.error("Error al consultar reportes:", error);

        contenidoReportes.replaceChildren();

        const mensaje = document.createElement("p");
        mensaje.className = "form-message error";
        mensaje.textContent =
            error.message || "No se pudieron cargar los reportes.";

        contenidoReportes.appendChild(mensaje);
    }
}


// ==========================================
// 10. MOSTRAR HISTORIAL DE REPORTES
// ==========================================

function mostrarReportesEquipo(equipo, reportes) {

    contenidoReportes.replaceChildren();

    if (reportes.length === 0) {

        const mensaje = document.createElement("p");

        mensaje.textContent =
            "Este artículo no tiene reportes de daños registrados. "
            + "Si se encuentra en mantenimiento, verifica su condición antes de cambiar su estado.";

        contenidoReportes.appendChild(mensaje);
        return;
    }

    const resumen = document.createElement("p");

    const pendientes = reportes.filter(
        reporte => reporte.mantenimientoAtendido !== true
    ).length;

    resumen.textContent =
        `Se encontraron ${reportes.length} reporte(s). `
        + `${pendientes} pendiente(s) de revisión.`;

    contenidoReportes.appendChild(resumen);

    // Ordenar del reporte más reciente al más antiguo.
    reportes.sort((a, b) => {

        const fechaA = new Date(a.fechaDevolucionReal || 0).getTime();
        const fechaB = new Date(b.fechaDevolucionReal || 0).getTime();

        return fechaB - fechaA;
    });

    reportes.forEach(reporte => {

        const tarjeta = document.createElement("article");
        tarjeta.className = "report-card";

        const titulo = document.createElement("h3");
        titulo.textContent = `Reporte del préstamo #${reporte.id}`;

        tarjeta.appendChild(titulo);

        const atendido = reporte.mantenimientoAtendido === true;

        const estado = document.createElement("p");
        estado.className = atendido
            ? "report-attended"
            : "report-pending";

        estado.textContent = atendido
            ? "✓ Reporte atendido"
            : "● Pendiente de revisión";

        tarjeta.appendChild(estado);


        // Datos de la persona y devolución
        const metadatos = document.createElement("div");
        metadatos.className = "report-meta";

        agregarDatoReporte(
            metadatos,
            "Persona",
            reporte.persona?.nombre || "No disponible"
        );

        agregarDatoReporte(
            metadatos,
            "Identificador",
            reporte.persona?.identificador || "No disponible"
        );

        agregarDatoReporte(
            metadatos,
            "Tipo de persona",
            reporte.persona?.tipo || "No disponible"
        );

        agregarDatoReporte(
            metadatos,
            "Fecha de devolución",
            formatearFecha(reporte.fechaDevolucionReal)
        );

        agregarDatoReporte(
            metadatos,
            "Recibido por",
            reporte.recibidoPor || "No disponible"
        );

        tarjeta.appendChild(metadatos);


        // Descripción original del daño
        const etiquetaDanio = document.createElement("strong");
        etiquetaDanio.textContent = "Descripción original del daño";

        tarjeta.appendChild(etiquetaDanio);

        const descripcionDanio = document.createElement("div");
        descripcionDanio.className = "report-description";
        descripcionDanio.textContent =
            reporte.detallesDanio || "No se proporcionó una descripción.";

        tarjeta.appendChild(descripcionDanio);


        // Si ya fue atendido, mostrar la revisión guardada.
        if (atendido) {

            const tituloRevision = document.createElement("strong");
            tituloRevision.textContent = "Revisión realizada";

            tarjeta.appendChild(tituloRevision);

            const revision = document.createElement("div");
            revision.className = "report-review";
            revision.textContent =
                reporte.revisionMantenimiento || "Sin descripción de revisión.";

            tarjeta.appendChild(revision);

            const datosRevision = document.createElement("div");
            datosRevision.className = "report-meta";

            agregarDatoReporte(
                datosRevision,
                "Revisado por",
                reporte.revisadoPor || "No disponible"
            );

            agregarDatoReporte(
                datosRevision,
                "Fecha de revisión",
                formatearFecha(reporte.fechaRevision)
            );

            tarjeta.appendChild(datosRevision);

        } else {

            // Formulario para registrar la revisión.
            const formulario = document.createElement("form");
            formulario.className = "report-form";
            formulario.dataset.prestamoId = reporte.id;

            const etiqueta = document.createElement("label");
            etiqueta.textContent =
                "Describe qué encontraste o qué reparación realizaste";

            const textarea = document.createElement("textarea");
            textarea.name = "revisionMantenimiento";
            textarea.placeholder =
                "Ej. Se revisó la carcasa, se ajustó la pieza y se comprobó el funcionamiento...";
            textarea.required = true;
            textarea.maxLength = 1000;

            const boton = document.createElement("button");
            boton.type = "submit";
            boton.className = "btn-primary";
            boton.textContent = "Guardar revisión";

            const mensaje = document.createElement("p");
            mensaje.className = "report-message";
            mensaje.setAttribute("role", "status");

            formulario.appendChild(etiqueta);
            formulario.appendChild(textarea);
            formulario.appendChild(boton);
            formulario.appendChild(mensaje);

            formulario.addEventListener("submit", async evento => {

                await enviarRevisionMantenimiento(
                    evento,
                    formulario,
                    reporte.id,
                    equipo
                );
            });

            tarjeta.appendChild(formulario);
        }

        contenidoReportes.appendChild(tarjeta);
    });
}


// ==========================================
// 11. ELEMENTOS DE INFORMACIÓN DEL REPORTE
// ==========================================

function agregarDatoReporte(contenedor, etiqueta, valor) {

    const elemento = document.createElement("div");

    const nombre = document.createElement("strong");
    nombre.textContent = `${etiqueta}: `;

    const contenido = document.createElement("span");
    contenido.textContent = valor;

    elemento.appendChild(nombre);
    elemento.appendChild(contenido);

    contenedor.appendChild(elemento);
}


function formatearFecha(fecha) {

    if (!fecha) {
        return "No disponible";
    }

    const valor = new Date(fecha);

    if (Number.isNaN(valor.getTime())) {
        return fecha;
    }

    return valor.toLocaleString("es-MX", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}


// ==========================================
// 12. GUARDAR REVISIÓN DE MANTENIMIENTO
// ==========================================

async function enviarRevisionMantenimiento(
    evento,
    formulario,
    prestamoId,
    equipo
) {

    evento.preventDefault();

    const textarea = formulario.querySelector(
        '[name="revisionMantenimiento"]'
    );

    const boton = formulario.querySelector('button[type="submit"]');
    const mensaje = formulario.querySelector(".report-message");

    const revisionMantenimiento = textarea.value.trim();

    if (!revisionMantenimiento) {

        mensaje.textContent =
            "Escribe qué encontraste o qué reparación realizaste.";

        mensaje.className = "report-message error";
        return;
    }

    boton.disabled = true;
    boton.textContent = "Guardando revisión...";

    mensaje.textContent = "";
    mensaje.className = "report-message";

    try {

        const respuesta = await fetch(
            `/api/prestamos/${prestamoId}/revision-mantenimiento`,
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json",
                    ...obtenerCabecerasCSRF()
                },

                body: JSON.stringify({
                    revisionMantenimiento: revisionMantenimiento
                })
            }
        );

        if (!respuesta.ok) {

            const detalle = await respuesta.text();

            let mensajeError =
                `No se pudo guardar la revisión. Error ${respuesta.status}`;

            try {

                const datosError = JSON.parse(detalle);

                mensajeError =
                    datosError.message ||
                    datosError.error ||
                    mensajeError;

            } catch {

                if (detalle && !detalle.trim().startsWith("<!DOCTYPE")) {
                    mensajeError = detalle;
                }
            }

            throw new Error(mensajeError);
        }

        alert(
            "La revisión se registró correctamente. "
            + "El estado del equipo se actualizará si ya no tiene reportes pendientes."
        );

        // Actualizar inventario e historial.
        await cargarEquipos();

        // Volver a consultar los reportes del equipo.
        await cargarReportesEquipo(equipo);

    } catch (error) {

        console.error("Error al guardar la revisión:", error);

        mensaje.textContent =
            error.message || "Ocurrió un error al guardar la revisión.";

        mensaje.className = "report-message error";

        boton.disabled = false;
        boton.textContent = "Guardar revisión";
    }
}


// ==========================================
// 13. CERRAR VENTANA DE REPORTES
// ==========================================

function cerrarModalReportes() {

    modalReportes.hidden = true;
    contenidoReportes.replaceChildren();
    equipoActualReportes = null;
}

document.getElementById("cerrarModalReportes")
    .addEventListener("click", cerrarModalReportes);

document.getElementById("cerrarModalReportesFondo")
    .addEventListener("click", cerrarModalReportes);


// ==========================================
// 14. EVENTOS DEL MODAL DE EQUIPO
// ==========================================

btnNuevoEquipo.addEventListener("click", abrirModal);

document.getElementById("cerrarModal")
    .addEventListener("click", cerrarModal);

document.getElementById("cancelarEquipo")
    .addEventListener("click", cerrarModal);

document.getElementById("cerrarModalFondo")
    .addEventListener("click", cerrarModal);

document.addEventListener("keydown", evento => {

    if (evento.key === "Escape") {

        if (!modalReportes.hidden) {
            cerrarModalReportes();
            return;
        }

        if (!modalEquipo.hidden) {
            cerrarModal();
        }
    }
});


// ==========================================
// 15. REGISTRAR O ACTUALIZAR UN ARTÍCULO
// ==========================================

formEquipo.addEventListener("submit", async evento => {

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
                "Content-Type": "application/json",
                ...obtenerCabecerasCSRF()
            },

            body: JSON.stringify(equipo)
        });

        if (!respuesta.ok) {

            const detalle = await respuesta.text();

            let mensajeError =
                `Error del servidor: ${respuesta.status}`;

            try {

                const datosError = JSON.parse(detalle);

                mensajeError =
                    datosError.error ||
                    datosError.message ||
                    mensajeError;

            } catch {

                if (detalle && !detalle.trim().startsWith("<!DOCTYPE")) {
                    mensajeError = detalle;
                }
            }

            throw new Error(mensajeError);
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

        mensajeFormulario.textContent =
            error.message ||
            (editando
                ? "No se pudo actualizar el artículo. Inténtalo de nuevo."
                : "No se pudo guardar el artículo. Inténtalo de nuevo.");

        mensajeFormulario.className = "form-message error";

    } finally {

        btnGuardarEquipo.disabled = false;

        btnGuardarEquipo.textContent = editando
            ? "Guardar cambios"
            : "Guardar artículo";
    }
});


// ==========================================
// 16. ACTIVAR BUSCADOR Y FILTROS
// ==========================================

buscarEquipo.addEventListener("input", mostrarEquipos);

filtroTipo.addEventListener("change", mostrarEquipos);

filtroEstado.addEventListener("change", mostrarEquipos);


// ==========================================
// 17. INICIAR LA PÁGINA
// ==========================================

cargarEquipos();
