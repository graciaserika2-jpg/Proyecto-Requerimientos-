
document.addEventListener("DOMContentLoaded", () => {

    // ==========================================
    // ELEMENTOS DEL REGISTRO
    // ==========================================

    const formulario = document.getElementById("formularioEncargado");
    const mensaje = document.getElementById("mensaje");
    const lista = document.getElementById("listaEncargados");
    const botonActualizar = document.getElementById("btnActualizar");
    const botonCrear = document.getElementById("btnCrearEncargado");
    const tipoCuenta = document.getElementById("tipoCuenta");

    // ==========================================
    // ELEMENTOS DE EDICIÓN
    // ==========================================

    const modal = document.getElementById("modalEditarCuenta");
    const formularioEditar = document.getElementById("formularioEditarCuenta");
    const mensajeEdicion = document.getElementById("mensajeEdicion");

    const botonCerrarModal = document.getElementById("btnCerrarModal");
    const botonCancelarEdicion = document.getElementById("btnCancelarEdicion");
    const botonGuardarEdicion = document.getElementById("btnGuardarEdicion");

    const editarId = document.getElementById("editarId");
    const editarNombre = document.getElementById("editarNombre");
    const editarUsuario = document.getElementById("editarUsuario");
    const editarCorreo = document.getElementById("editarCorreo");

    // ==========================================
    // SEGURIDAD CSRF
    // ==========================================

    function obtenerDatosCSRF() {

        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');

        if (!tokenMeta || !headerMeta) {
            throw new Error("No se encontró el token de seguridad CSRF.");
        }

        return {
            [headerMeta.content]: tokenMeta.content
        };
    }

    // ==========================================
    // RESPUESTAS DEL SERVIDOR
    // ==========================================

    async function obtenerResultado(respuesta) {

        const resultado = await respuesta.json().catch(() => ({}));

        if (!respuesta.ok) {
            throw new Error(
                resultado.detail ||
                resultado.message ||
                resultado.error ||
                `Error del servidor (HTTP ${respuesta.status}).`
            );
        }

        return resultado;
    }

    // ==========================================
    // REGISTRO DE CUENTAS
    // ==========================================

    function actualizarBoton() {

        botonCrear.textContent = tipoCuenta.value === "ADMIN"
            ? "Registrar administrador"
            : "Registrar encargado";
    }

    // ==========================================
    // MENSAJES DE LA TABLA
    // ==========================================

    function mostrarMensajeTabla(texto) {

        lista.replaceChildren();

        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = 7;
        celda.textContent = texto;

        fila.appendChild(celda);
        lista.appendChild(fila);
    }

    // ==========================================
    // CREAR BOTÓN
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
    // CARGAR CUENTAS
    // ==========================================

    async function cargarEncargados() {

        mostrarMensajeTabla("Cargando cuentas...");

        try {

            const respuesta = await fetch("/api/encargados", {
                method: "GET",
                credentials: "same-origin"
            });

            const cuentas = await obtenerResultado(respuesta);

            if (!Array.isArray(cuentas)) {
                throw new Error("La respuesta del servidor no es válida.");
            }

            if (cuentas.length === 0) {
                mostrarMensajeTabla("Todavía no hay cuentas registradas.");
                return;
            }

            lista.replaceChildren();

            cuentas.forEach(cuenta => {

                const fila = document.createElement("tr");

                // Nombre
                const celdaNombre = document.createElement("td");
                celdaNombre.textContent = cuenta.nombre ?? "No registrado";

                // Usuario
                const celdaUsuario = document.createElement("td");
                celdaUsuario.textContent = cuenta.usuario ?? "No registrado";

                // Correo
                const celdaCorreo = document.createElement("td");
                celdaCorreo.textContent = cuenta.correo ?? "No registrado";

                // Rol
                const celdaRol = document.createElement("td");
                celdaRol.textContent = cuenta.rol ?? "No registrado";

                // Verificación del correo
                const celdaVerificacion = document.createElement("td");

                celdaVerificacion.textContent =
                    cuenta.correoVerificado === true
                        ? "Verificado"
                        : "Pendiente";

                // Estado de la cuenta
                const celdaEstado = document.createElement("td");

                celdaEstado.textContent = cuenta.activo === true
                    ? "Activa"
                    : "Inactiva";

                // Acciones: únicamente editar
                const celdaAcciones = document.createElement("td");

                const botonEditar = crearBoton(
                    "Editar",
                    "btn-accion btn-editar",
                    () => abrirModalEdicion(cuenta)
                );

                celdaAcciones.appendChild(botonEditar);

                fila.append(
                    celdaNombre,
                    celdaUsuario,
                    celdaCorreo,
                    celdaRol,
                    celdaVerificacion,
                    celdaEstado,
                    celdaAcciones
                );

                lista.appendChild(fila);
            });

        } catch (error) {

            mostrarMensajeTabla(
                "Error al cargar las cuentas: " + error.message
            );
        }
    }

    // ==========================================
    // ABRIR MODAL DE EDICIÓN
    // ==========================================

    function abrirModalEdicion(cuenta) {

        editarId.value = cuenta.id;
        editarNombre.value = cuenta.nombre ?? "";
        editarUsuario.value = cuenta.usuario ?? "";
        editarCorreo.value = cuenta.correo ?? "";

        mensajeEdicion.textContent = "";

        modal.hidden = false;
        document.body.style.overflow = "hidden";

        editarNombre.focus();
    }

    // ==========================================
    // CERRAR MODAL
    // ==========================================

    function cerrarModalEdicion() {

        modal.hidden = true;
        document.body.style.overflow = "";

        formularioEditar.reset();
        mensajeEdicion.textContent = "";
    }

    botonCerrarModal.addEventListener("click", cerrarModalEdicion);

    botonCancelarEdicion.addEventListener("click", cerrarModalEdicion);

    // ==========================================
    // CERRAR MODAL CON LA TECLA ESCAPE
    // ==========================================

    document.addEventListener("keydown", evento => {

        if (evento.key === "Escape" && !modal.hidden) {
            cerrarModalEdicion();
        }
    });

    // ==========================================
    // GUARDAR EDICIÓN
    // ==========================================

    formularioEditar.addEventListener("submit", async evento => {

        evento.preventDefault();

        mensajeEdicion.textContent = "";

        const id = editarId.value;

        const datos = {
            nombre: editarNombre.value.trim(),
            usuario: editarUsuario.value.trim(),
            correo: editarCorreo.value.trim()
        };

        botonGuardarEdicion.disabled = true;
        botonGuardarEdicion.textContent = "Guardando...";

        try {

            const respuesta = await fetch(`/api/encargados/${id}`, {
                method: "PUT",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    ...obtenerDatosCSRF()
                },
                body: JSON.stringify(datos)
            });

            const resultado = await obtenerResultado(respuesta);

            cerrarModalEdicion();

            mensaje.textContent = resultado.mensaje ||
                "Cuenta actualizada correctamente.";

            await cargarEncargados();

        } catch (error) {

            mensajeEdicion.textContent = error.message;

        } finally {

            botonGuardarEdicion.disabled = false;
            botonGuardarEdicion.textContent = "Guardar cambios";
        }
    });

    // ==========================================
    // REGISTRAR NUEVA CUENTA
    // ==========================================

    formulario.addEventListener("submit", async evento => {

        evento.preventDefault();

        mensaje.textContent = "";

        const datos = {
            nombre: formulario.nombre.value.trim(),
            usuario: formulario.usuario.value.trim(),
            correo: formulario.correo.value.trim()
        };

        const endpoint = tipoCuenta.value === "ADMIN"
            ? "/api/encargados/administradores"
            : "/api/encargados";

        botonCrear.disabled = true;
        botonCrear.textContent = "Registrando...";

        try {

            const respuesta = await fetch(endpoint, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    ...obtenerDatosCSRF()
                },
                body: JSON.stringify(datos)
            });

            const resultado = await obtenerResultado(respuesta);

            mensaje.textContent = resultado.mensaje ||
                "Cuenta registrada correctamente.";

            formulario.reset();

            tipoCuenta.value = "ENCARGADO";
            actualizarBoton();

            await cargarEncargados();

        } catch (error) {

            mensaje.textContent = error.message;

        } finally {

            botonCrear.disabled = false;
            actualizarBoton();
        }
    });

    // ==========================================
    // EVENTOS INICIALES
    // ==========================================

    tipoCuenta.addEventListener("change", actualizarBoton);

    botonActualizar.addEventListener("click", cargarEncargados);

    actualizarBoton();
    cargarEncargados();

});
