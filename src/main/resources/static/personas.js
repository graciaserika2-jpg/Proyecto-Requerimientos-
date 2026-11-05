
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

    // Obtiene los encabezados necesarios, incluyendo CSRF.
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

    // Actualiza la etiqueta y ayuda del identificador según el tipo.
    function actualizarIdentificador() {
        if (!tipo || !identificador) return;

        if (tipo.value === "Alumno") {
            if (etiquetaIdentificador) {
                etiquetaIdentificador.textContent = "Número de boleta";
            }

            if (ayudaIdentificador) {
                ayudaIdentificador.textContent =
                    "Ingresa los 10 dígitos de la boleta.";
            }

            identificador.placeholder = "Ej. 2024630001";
            identificador.maxLength = 10;
        } else {
            if (etiquetaIdentificador) {
                etiquetaIdentificador.textContent = "Número de empleado";
            }

            if (ayudaIdentificador) {
                ayudaIdentificador.textContent =
                    "Ingresa el número de empleado.";
            }

            identificador.placeholder = "Número de empleado";
            identificador.removeAttribute("maxLength");
        }
    }

    // Valida el identificador de acuerdo con el tipo de persona.
    function validarIdentificador() {
        if (!tipo || !identificador) return true;

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

    // Interpreta los errores enviados por el servidor.
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

    // Carga las personas desde el servidor.
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

    // Dibuja la tabla y aplica el filtro de búsqueda.
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

            const tieneCuenta = persona.tieneCuenta === true;

            fila.innerHTML = `
                <td>${persona.id}</td>
                <td></td>
                <td>${persona.tipo || ""}</td>
                <td>${persona.identificador || ""}</td>
                <td></td>
                <td></td>
            `;

            // Nombre
            fila.children[1].textContent = persona.nombre || "";

            // Estado de la cuenta
            fila.children[4].textContent = tieneCuenta
                ? "Cuenta creada"
                : "Sin cuenta";

            // Acciones
            const celdaAcciones = fila.children[5];

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
                const botonCrear = document.createElement("button");

                botonCrear.type = "button";
                botonCrear.textContent = "Crear cuenta";
                botonCrear.className = "btn-editar";

                botonCrear.addEventListener("click", () => {
                    crearCuenta(persona, botonCrear);
                });

                celdaAcciones.appendChild(botonCrear);
            }

            tablaPersonas.appendChild(fila);
        });
    }

    // Registra una persona.
    async function registrarPersona(evento) {
        evento.preventDefault();

        if (!validarIdentificador()) return;

        const datos = {
            nombre: formPersona.nombre.value.trim(),
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
            const respuesta = await fetch("/api/personas", {
                method: "POST",
                headers: obtenerHeaders(),
                body: JSON.stringify(datos)
            });

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            mostrarMensaje("Persona registrada correctamente.");

            formPersona.reset();
            actualizarIdentificador();

            await cargarPersonas();
        } catch (error) {
            mostrarMensaje(
                error.message || "No se pudo registrar la persona.",
                true
            );
        } finally {
            botonRegistrar.disabled = false;
            botonRegistrar.textContent = "Registrar persona";
        }
    }

    // Crea una cuenta para una persona que todavía no tiene.
    async function crearCuenta(persona, boton) {
        const confirmar = window.confirm(
            `¿Deseas crear una cuenta para ${persona.nombre}?`
        );

        if (!confirmar) return;

        boton.disabled = true;
        boton.textContent = "Creando...";

        try {
            const respuesta = await fetch(
                `/api/personas/${persona.id}/cuenta`,
                {
                    method: "POST",
                    headers: obtenerHeaders()
                }
            );

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const credenciales = await respuesta.json();

            mostrarMensaje(`Cuenta creada para ${persona.nombre}.`);

            mostrarCredenciales(
                persona.nombre,
                credenciales,
                "¡Cuenta creada correctamente!"
            );

            await cargarPersonas();
        } catch (error) {
            mostrarMensaje(
                error.message || "No se pudo crear la cuenta.",
                true
            );
        } finally {
            boton.disabled = false;
            boton.textContent = "Crear cuenta";
        }
    }

    // Restablece la contraseña de una cuenta existente.
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

            mostrarMensaje(
                `Contraseña restablecida para ${persona.nombre}.`
            );

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

    // Muestra las credenciales temporales en un modal con opción de copiar.
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

    // Eventos
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

    // Carga inicial
    cargarPersonas();
});
