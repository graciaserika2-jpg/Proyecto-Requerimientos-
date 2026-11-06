
document.addEventListener("DOMContentLoaded", () => {
    const formPersona = document.getElementById("formPersona");
    const tablaPersonas = document.getElementById("tablaPersonas");
    const mensaje = document.getElementById("mensaje");
    const buscarPersona = document.getElementById("buscarPersona");
    const tipo = document.getElementById("tipo");
    const identificador = document.getElementById("identificador");
    const etiquetaIdentificador = document.getElementById("etiquetaIdentificador");
    const ayudaIdentificador = document.getElementById("ayudaIdentificador");
    const botonRegistrar = document.getElementById("botonRegistrar");

    let personas = [];

    // Encabezados para las solicitudes, incluyendo CSRF.
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

    // Muestra mensajes de éxito o error.
    function mostrarMensaje(texto, esError = false) {
        if (!mensaje) return;

        mensaje.textContent = texto;
        mensaje.className = esError ? "mensaje error" : "mensaje exito";
        mensaje.style.display = "block";
    }

    // Actualiza el identificador según el tipo de persona.
    function actualizarIdentificador() {
        if (!tipo || !identificador) return;

        identificador.disabled = tipo.value === "";

        if (tipo.value === "Alumno") {
            etiquetaIdentificador.textContent = "Número de boleta";
            ayudaIdentificador.textContent =
                "Ingresa los 10 dígitos de la boleta.";

            identificador.placeholder = "Ej. 2024630001";
            identificador.maxLength = 10;

        } else if (tipo.value === "Profesor") {
            etiquetaIdentificador.textContent = "Número de empleado";
            ayudaIdentificador.textContent =
                "Ingresa el número de empleado.";

            identificador.placeholder = "Número de empleado";
            identificador.removeAttribute("maxLength");

        } else {
            etiquetaIdentificador.textContent =
                "Boleta o identificador institucional";

            ayudaIdentificador.textContent =
                "Selecciona si registrarás un alumno o un profesor.";

            identificador.placeholder =
                "Selecciona primero el tipo de persona";
        }
    }

    // Valida el identificador.
    function validarIdentificador() {
        const valor = identificador.value.trim();

        if (!valor) {
            mostrarMensaje("Debes ingresar el identificador.", true);
            identificador.focus();
            return false;
        }

        if (tipo.value === "Alumno" && !/^\d{10}$/.test(valor)) {
            mostrarMensaje(
                "La boleta del alumno debe contener exactamente 10 dígitos.",
                true
            );

            identificador.focus();
            return false;
        }

        return true;
    }

    // Lee los errores enviados por el servidor.
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

    // Carga las personas registradas.
    async function cargarPersonas() {
        try {
            const respuesta = await fetch("/api/personas/estado-cuentas");

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            personas = await respuesta.json();
            mostrarPersonas();

        } catch (error) {
            mostrarMensaje(
                error.message || "No se pudieron cargar las personas.",
                true
            );
        }
    }

    // Muestra las personas en la tabla.
    function mostrarPersonas() {
        if (!tablaPersonas) return;

        tablaPersonas.innerHTML = "";

        const texto = buscarPersona
            ? buscarPersona.value.trim().toLowerCase()
            : "";

        const filtradas = personas.filter(persona =>
            (persona.nombre || "").toLowerCase().includes(texto) ||
            (persona.identificador || "").toLowerCase().includes(texto)
        );

        if (filtradas.length === 0) {
            tablaPersonas.innerHTML = `
                <tr>
                    <td colspan="6">No se encontraron personas.</td>
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

            const celdaTipo = document.createElement("td");
            celdaTipo.textContent = persona.tipo || "";

            const celdaIdentificador = document.createElement("td");
            celdaIdentificador.textContent = persona.identificador || "";

            const celdaCuenta = document.createElement("td");
            const tieneCuenta = persona.tieneCuenta === true;

            celdaCuenta.textContent = tieneCuenta
                ? "Cuenta creada"
                : "Sin cuenta";

            const celdaAcciones = document.createElement("td");

            // Solo aparece restablecer contraseña.
            // Ya no existe el botón Crear cuenta.
            if (tieneCuenta) {
                const botonRestablecer = document.createElement("button");

                botonRestablecer.type = "button";
                botonRestablecer.textContent = "Restablecer contraseña";
                botonRestablecer.className = "btn-editar";

                botonRestablecer.addEventListener("click", () => {
                    restablecerPassword(persona, botonRestablecer);
                });

                celdaAcciones.appendChild(botonRestablecer);
            } else {
                celdaAcciones.textContent = "—";
            }

            fila.append(
                celdaId,
                celdaNombre,
                celdaTipo,
                celdaIdentificador,
                celdaCuenta,
                celdaAcciones
            );

            tablaPersonas.appendChild(fila);
        });
    }

    // Registra a la persona y crea su cuenta automáticamente.
    async function registrarPersona(evento) {
        evento.preventDefault();

        if (!validarIdentificador()) return;

        const datos = {
            nombre: document.getElementById("nombre").value.trim(),
            tipo: tipo.value,
            identificador: identificador.value.trim()
        };

        if (!datos.nombre) {
            mostrarMensaje("Debes ingresar el nombre de la persona.", true);
            return;
        }

        botonRegistrar.disabled = true;
        botonRegistrar.textContent = "Registrando...";

        try {
            // Este endpoint registra a la persona y crea su cuenta.
            const respuesta = await fetch("/api/personas/registro-completo", {
                method: "POST",
                headers: obtenerHeaders(),
                body: JSON.stringify(datos)
            });

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const resultado = await respuesta.json();

            // El backend devuelve persona y credenciales.
            const personaRegistrada = resultado.persona;
            const credenciales = resultado.credenciales;

            // Limpia el formulario.
            formPersona.reset();
            actualizarIdentificador();

            // Actualiza la tabla.
            await cargarPersonas();

            // Muestra las credenciales inmediatamente.
            mostrarCredenciales(
                personaRegistrada.nombre,
                credenciales,
                "¡Persona registrada correctamente!"
            );

        } catch (error) {
            mostrarMensaje(
                error.message || "No se pudo registrar la persona y crear su cuenta.",
                true
            );
        } finally {
            botonRegistrar.disabled = false;
            botonRegistrar.textContent = "Registrar persona";
        }
    }

    // Restablece la contraseña de una persona que ya tiene cuenta.
    async function restablecerPassword(persona, boton) {
        const confirmar = window.confirm(
            `¿Deseas restablecer la contraseña de ${persona.nombre}?\n\n` +
            "La contraseña anterior dejará de funcionar y se generará una temporal."
        );

        if (!confirmar) return;

        boton.disabled = true;
        boton.textContent = "Restableciendo...";

        try {
            const respuesta = await fetch(
                `/api/personas/${persona.id}/restablecer-password`,
                {
                    method: "POST",
                    headers: obtenerHeaders()
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const credenciales = await respuesta.json();

            mostrarCredenciales(
                persona.nombre,
                credenciales,
                "¡Contraseña restablecida!"
            );

        } catch (error) {
            mostrarMensaje(
                error.message || "No se pudo restablecer la contraseña.",
                true
            );
        } finally {
            boton.disabled = false;
            boton.textContent = "Restablecer contraseña";
        }
    }

    // Muestra las credenciales en una ventana con opción de copiar.
    function mostrarCredenciales(
        nombre,
        credenciales,
        tituloModal = "¡Cuenta creada correctamente!"
    ) {
        const modalAnterior = document.getElementById("modalCredenciales");

        if (modalAnterior) {
            modalAnterior.remove();
        }

        const modal = document.createElement("div");
        modal.id = "modalCredenciales";

        modal.style.cssText = `
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.55);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 9999;
            padding: 20px;
        `;

        const contenido = document.createElement("div");

        contenido.style.cssText = `
            background: white;
            padding: 28px;
            border-radius: 12px;
            width: 100%;
            max-width: 430px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
        `;

        const titulo = document.createElement("h2");
        titulo.textContent = tituloModal;

        const descripcion = document.createElement("p");
        descripcion.textContent =
            "Estas son las credenciales de acceso. Cópialas y entrégalas a la persona.";

        const nombrePersona = document.createElement("p");
        nombrePersona.textContent = `Persona: ${nombre}`;

        const usuario = document.createElement("p");
        usuario.textContent = `Usuario: ${credenciales.usuario || ""}`;

        const password = document.createElement("p");
        password.textContent =
            `Contraseña temporal: ${credenciales.passwordTemporal || ""}`;

        const botonCopiar = document.createElement("button");
        botonCopiar.type = "button";
        botonCopiar.textContent = "Copiar credenciales";

        botonCopiar.style.cssText = `
            background: #2563eb;
            color: white;
            border: none;
            padding: 10px 16px;
            border-radius: 6px;
            cursor: pointer;
            margin-right: 8px;
        `;

        botonCopiar.addEventListener("click", async () => {
            const texto = [
                `Persona: ${nombre}`,
                `Usuario: ${credenciales.usuario || ""}`,
                `Contraseña temporal: ${credenciales.passwordTemporal || ""}`
            ].join("\n");

            try {
                await navigator.clipboard.writeText(texto);
                botonCopiar.textContent = "¡Copiado!";
            } catch {
                mostrarMensaje(
                    "No se pudieron copiar las credenciales. Puedes copiarlas manualmente.",
                    true
                );
            }
        });

        const botonCerrar = document.createElement("button");
        botonCerrar.type = "button";
        botonCerrar.textContent = "Cerrar";

        botonCerrar.style.cssText = `
            background: #e5e7eb;
            color: #111827;
            border: none;
            padding: 10px 16px;
            border-radius: 6px;
            cursor: pointer;
        `;

        botonCerrar.addEventListener("click", () => modal.remove());

        contenido.append(
            titulo,
            descripcion,
            nombrePersona,
            usuario,
            password,
            botonCopiar,
            botonCerrar
        );

        modal.appendChild(contenido);

        modal.addEventListener("click", evento => {
            if (evento.target === modal) {
                modal.remove();
            }
        });

        document.body.appendChild(modal);
    }

    // Eventos.
    if (formPersona) {
        formPersona.addEventListener("submit", registrarPersona);
    }

    if (buscarPersona) {
        buscarPersona.addEventListener("input", mostrarPersonas);
    }

    if (tipo) {
        tipo.addEventListener("change", actualizarIdentificador);
        actualizarIdentificador();
    }

    // Carga inicial.
    cargarPersonas();
});
