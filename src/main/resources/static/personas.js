
document.addEventListener("DOMContentLoaded", () => {

    // ==========================================
    // ELEMENTOS DEL HTML
    // ==========================================

    const formPersona = document.getElementById("formPersona");
    const tablaPersonas = document.getElementById("tablaPersonas");
    const mensaje = document.getElementById("mensaje");
    const buscarPersona = document.getElementById("buscarPersona");
    const identificador = document.getElementById("identificador");
    const botonRegistrar = document.getElementById("botonRegistrar");

    // Elementos del modal de edición
    const modalEditarAlumno = document.getElementById("modalEditarAlumno");
    const formEditarAlumno = document.getElementById("formEditarAlumno");
    const editarIdAlumno = document.getElementById("editarIdAlumno");
    const editarNombre = document.getElementById("editarNombre");
    const editarBoleta = document.getElementById("editarBoleta");
    const cerrarModalEditar = document.getElementById("cerrarModalEditar");
    const cancelarEdicion = document.getElementById("cancelarEdicion");
    const guardarEdicion = document.getElementById("guardarEdicion");
    const mensajeEditarAlumno = document.getElementById("mensajeEditarAlumno");

    let personas = [];
    let alumnoEnEdicion = null;

    // ==========================================
    // HEADERS Y MENSAJES
    // ==========================================

    function obtenerHeaders() {

        const headers = {
            "Content-Type": "application/json"
        };

        const csrfToken = document.querySelector('meta[name="_csrf"]');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }

        return headers;
    }

    function mostrarMensaje(texto, esError = false) {

        if (!mensaje) return;

        mensaje.textContent = texto;

        mensaje.className = esError
            ? "mensaje error"
            : "mensaje exito";

        mensaje.style.display = "block";
    }

    function mostrarMensajeModal(texto, esError = false) {

        if (!mensajeEditarAlumno) return;

        mensajeEditarAlumno.textContent = texto;

        mensajeEditarAlumno.className = esError
            ? "mensaje error"
            : "mensaje exito";

        mensajeEditarAlumno.style.display = "block";
    }

    async function leerError(respuesta) {

        try {

            const datos = await respuesta.json();

            return datos.mensaje ||
                datos.message ||
                datos.detail ||
                "Ocurrió un error al procesar la solicitud.";

        } catch {

            return "Ocurrió un error al procesar la solicitud.";
        }
    }

    // ==========================================
    // VALIDACIONES
    // ==========================================

    function validarBoleta() {

        const valor = identificador.value.trim();

        if (!/^\d{10}$/.test(valor)) {

            mostrarMensaje(
                "La boleta debe contener exactamente 10 dígitos.",
                true
            );

            identificador.focus();

            return false;
        }

        return true;
    }

    // ==========================================
    // CARGAR ALUMNOS
    // ==========================================

    async function cargarPersonas() {

        try {

            const respuesta = await fetch(
                "/api/personas/estado-cuentas"
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const datos = await respuesta.json();

            personas = datos.filter(persona =>
                (persona.tipo || "").toLowerCase() === "alumno"
            );

            mostrarPersonas();

        } catch (error) {

            mostrarMensaje(
                error.message || "No se pudieron cargar los alumnos.",
                true
            );
        }
    }

    // ==========================================
    // ESTADOS
    // ==========================================

    function obtenerEstadoCuenta(persona) {

        if (!persona.tieneCuenta) {
            return "Sin cuenta";
        }

        if (persona.cuentaActiva === false) {
            return "Inactiva";
        }

        if (persona.correoVerificado === true) {
            return "Activada";
        }

        if (persona.correoVerificado === false) {
            return "Pendiente de activación";
        }

        return "Cuenta anterior";
    }

    function obtenerEstadoAlumno(persona) {

        return persona.activo
            ? "Activo"
            : "Dado de baja";
    }

    // ==========================================
    // CREAR BOTONES
    // ==========================================

    function crearBoton(texto, clase, accion) {

        const boton = document.createElement("button");

        boton.type = "button";
        boton.textContent = texto;
        boton.className = clase;

        boton.addEventListener("click", accion);

        return boton;
    }

    // ==========================================
    // MOSTRAR ALUMNOS
    // ==========================================

    function mostrarPersonas() {

        if (!tablaPersonas) return;

        tablaPersonas.innerHTML = "";

        const texto = buscarPersona
            ? buscarPersona.value.trim().toLowerCase()
            : "";

        const filtradas = personas.filter(persona => {

            const id = String(persona.id ?? "");
            const nombre = (persona.nombre || "").toLowerCase();

            const boleta = String(
                persona.identificador || ""
            ).toLowerCase();

            const correo = (persona.correo || "").toLowerCase();

            return id.includes(texto) ||
                nombre.includes(texto) ||
                boleta.includes(texto) ||
                correo.includes(texto);
        });

        if (filtradas.length === 0) {

            tablaPersonas.innerHTML = `
                <tr>
                    <td colspan="7">No se encontraron alumnos.</td>
                </tr>
            `;

            return;
        }

        filtradas.forEach(persona => {

            const fila = document.createElement("tr");

            const celdaId = document.createElement("td");
            celdaId.textContent = persona.id;

            const celdaNombre = document.createElement("td");
            celdaNombre.textContent = persona.nombre || "";

            const celdaBoleta = document.createElement("td");
            celdaBoleta.textContent =
                persona.identificador || "";

            const celdaCorreo = document.createElement("td");
            celdaCorreo.textContent = persona.correo || "—";

            const celdaEstadoAlumno = document.createElement("td");
            celdaEstadoAlumno.textContent =
                obtenerEstadoAlumno(persona);

            const celdaEstadoCuenta = document.createElement("td");
            celdaEstadoCuenta.textContent =
                obtenerEstadoCuenta(persona);

            const celdaAcciones = document.createElement("td");

            const contenedorAcciones = document.createElement("div");
            contenedorAcciones.className = "acciones-alumno";

            if (persona.activo) {

                const botonEditar = crearBoton(
                    "Editar",
                    "btn-accion btn-editar",
                    () => abrirModalEdicion(persona)
                );

                const botonBaja = crearBoton(
                    "Dar de baja",
                    "btn-accion btn-baja",
                    () => darDeBajaAlumno(persona)
                );

                contenedorAcciones.append(
                    botonEditar,
                    botonBaja
                );

            } else {

                const botonReactivar = crearBoton(
                    "Reactivar",
                    "btn-accion btn-reactivar",
                    () => reactivarAlumno(persona)
                );

                contenedorAcciones.appendChild(botonReactivar);
            }

            celdaAcciones.appendChild(contenedorAcciones);

            fila.append(
                celdaId,
                celdaNombre,
                celdaBoleta,
                celdaCorreo,
                celdaEstadoAlumno,
                celdaEstadoCuenta,
                celdaAcciones
            );

            tablaPersonas.appendChild(fila);
        });
    }

    // ==========================================
    // ABRIR MODAL DE EDICIÓN
    // ==========================================

    function abrirModalEdicion(persona) {

        if (!modalEditarAlumno || !formEditarAlumno) {
            mostrarMensaje(
                "No se encontró la ventana de edición en el HTML.",
                true
            );
            return;
        }

        alumnoEnEdicion = persona;

        // Llenar los campos con los datos actuales
        editarIdAlumno.value = persona.id;
        editarNombre.value = persona.nombre || "";
        editarBoleta.value = persona.identificador || "";

        // Limpiar mensajes anteriores
        formEditarAlumno.reset();

        editarIdAlumno.value = persona.id;
        editarNombre.value = persona.nombre || "";
        editarBoleta.value = persona.identificador || "";

        if (mensajeEditarAlumno) {
            mensajeEditarAlumno.textContent = "";
            mensajeEditarAlumno.style.display = "none";
        }

        // Mostrar modal
        modalEditarAlumno.hidden = false;

        // Colocar el cursor en el nombre
        editarNombre.focus();
    }

    // ==========================================
    // CERRAR MODAL DE EDICIÓN
    // ==========================================

    function cerrarModalEdicion() {

        if (!modalEditarAlumno) return;

        modalEditarAlumno.hidden = true;

        if (formEditarAlumno) {
            formEditarAlumno.reset();
        }

        if (mensajeEditarAlumno) {
            mensajeEditarAlumno.textContent = "";
            mensajeEditarAlumno.style.display = "none";
        }

        alumnoEnEdicion = null;
    }

    // ==========================================
    // GUARDAR CAMBIOS DEL ALUMNO
    // ==========================================

    async function guardarCambiosAlumno(evento) {

        evento.preventDefault();

        if (!alumnoEnEdicion) {
            mostrarMensajeModal(
                "No se encontró el alumno que deseas editar.",
                true
            );
            return;
        }

        const nuevoNombre = editarNombre.value.trim();
        const nuevaBoleta = editarBoleta.value.trim();

        if (!nuevoNombre) {

            mostrarMensajeModal(
                "El nombre del alumno es obligatorio.",
                true
            );

            editarNombre.focus();

            return;
        }

        if (nuevoNombre.length > 150) {

            mostrarMensajeModal(
                "El nombre no puede superar los 150 caracteres.",
                true
            );

            editarNombre.focus();

            return;
        }

        if (!/^\d{10}$/.test(nuevaBoleta)) {

            mostrarMensajeModal(
                "La boleta debe contener exactamente 10 dígitos.",
                true
            );

            editarBoleta.focus();

            return;
        }

        guardarEdicion.disabled = true;
        guardarEdicion.textContent = "Guardando...";

        try {

            const respuesta = await fetch(
                `/api/personas/${alumnoEnEdicion.id}`,
                {
                    method: "PUT",
                    headers: obtenerHeaders(),
                    body: JSON.stringify({
                        nombre: nuevoNombre,
                        identificador: nuevaBoleta
                    })
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const nombreAnterior = alumnoEnEdicion.nombre;

            cerrarModalEdicion();

            await cargarPersonas();

            mostrarMensaje(
                `Los datos de ${nombreAnterior} fueron actualizados correctamente.`
            );

        } catch (error) {

            mostrarMensajeModal(
                error.message || "No se pudo editar al alumno.",
                true
            );

        } finally {

            guardarEdicion.disabled = false;
            guardarEdicion.textContent = "Guardar cambios";
        }
    }

    // ==========================================
    // DAR DE BAJA
    // ==========================================

    async function darDeBajaAlumno(persona) {

        const confirmar = confirm(
            `¿Estás seguro de dar de baja a ${persona.nombre}?\n\n` +
            "Su cuenta quedará desactivada y no podrá iniciar sesión. " +
            "Su historial de préstamos se conservará."
        );

        if (!confirmar) return;

        try {

            const respuesta = await fetch(
                `/api/personas/${persona.id}/baja`,
                {
                    method: "PATCH",
                    headers: obtenerHeaders()
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            await cargarPersonas();

            mostrarMensaje(
                `El alumno ${persona.nombre} fue dado de baja correctamente.`
            );

        } catch (error) {

            mostrarMensaje(
                error.message || "No se pudo dar de baja al alumno.",
                true
            );
        }
    }

    // ==========================================
    // REACTIVAR
    // ==========================================

    async function reactivarAlumno(persona) {

        const confirmar = confirm(
            `¿Deseas reactivar a ${persona.nombre}?\n\n` +
            "Su cuenta volverá a estar habilitada."
        );

        if (!confirmar) return;

        try {

            const respuesta = await fetch(
                `/api/personas/${persona.id}/reactivar`,
                {
                    method: "PATCH",
                    headers: obtenerHeaders()
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            await cargarPersonas();

            mostrarMensaje(
                `El alumno ${persona.nombre} fue reactivado correctamente.`
            );

        } catch (error) {

            mostrarMensaje(
                error.message || "No se pudo reactivar al alumno.",
                true
            );
        }
    }

    // ==========================================
    // REGISTRAR ALUMNO
    // ==========================================

    async function registrarPersona(evento) {

        evento.preventDefault();

        if (!validarBoleta()) return;

        const datos = {
            nombre: document.getElementById("nombre").value.trim(),
            tipo: "Alumno",
            identificador: identificador.value.trim(),
            correo: document.getElementById("correo").value.trim()
        };

        if (!datos.nombre) {

            mostrarMensaje(
                "Debes ingresar el nombre del alumno.",
                true
            );

            return;
        }

        if (!datos.correo) {

            mostrarMensaje(
                "Debes ingresar el correo electrónico.",
                true
            );

            return;
        }

        botonRegistrar.disabled = true;
        botonRegistrar.textContent = "Registrando...";

        try {

            const respuesta = await fetch(
                "/api/personas/registro-completo",
                {
                    method: "POST",
                    headers: obtenerHeaders(),
                    body: JSON.stringify(datos)
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const resultado = await respuesta.json();

            formPersona.reset();

            await cargarPersonas();

            mostrarMensaje(
                `El alumno ${resultado.persona.nombre} fue registrado correctamente. ` +
                `Se solicitó el envío del enlace de activación a ${resultado.correo}.`
            );

        } catch (error) {

            mostrarMensaje(
                error.message ||
                "No se pudo registrar al alumno.",
                true
            );

        } finally {

            botonRegistrar.disabled = false;
            botonRegistrar.textContent = "Registrar alumno";
        }
    }

    // ==========================================
    // EVENTOS DEL MODAL
    // ==========================================

    if (formEditarAlumno) {
        formEditarAlumno.addEventListener(
            "submit",
            guardarCambiosAlumno
        );
    }

    if (cerrarModalEditar) {
        cerrarModalEditar.addEventListener(
            "click",
            cerrarModalEdicion
        );
    }

    if (cancelarEdicion) {
        cancelarEdicion.addEventListener(
            "click",
            cerrarModalEdicion
        );
    }

    if (modalEditarAlumno) {

        // Cerrar al pulsar fuera de la ventana
        modalEditarAlumno.addEventListener("click", evento => {

            if (evento.target === modalEditarAlumno) {
                cerrarModalEdicion();
            }
        });

        // Cerrar al pulsar Escape
        document.addEventListener("keydown", evento => {

            if (
                evento.key === "Escape" &&
                !modalEditarAlumno.hidden
            ) {
                cerrarModalEdicion();
            }
        });
    }

    // ==========================================
    // EVENTOS GENERALES
    // ==========================================

    if (formPersona) {
        formPersona.addEventListener(
            "submit",
            registrarPersona
        );
    }

    if (buscarPersona) {
        buscarPersona.addEventListener(
            "input",
            mostrarPersonas
        );
    }

    // ==========================================
    // INICIO
    // ==========================================

    cargarPersonas();

});
