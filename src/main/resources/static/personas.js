
const formulario = document.getElementById("formPersona");
const tabla = document.getElementById("tablaPersonas");
const mensaje = document.getElementById("mensaje");
const busqueda = document.getElementById("buscarPersona");

const tipoInput = document.getElementById("tipo");
const identificadorInput = document.getElementById("identificador");
const etiquetaIdentificador = document.getElementById("etiquetaIdentificador");
const ayudaIdentificador = document.getElementById("ayudaIdentificador");
const botonRegistrar = document.getElementById("botonRegistrar");

let personas = [];

function obtenerHeaders() {
    const headers = {
        "Content-Type": "application/json"
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function mostrarMensaje(texto, error = false) {
    mensaje.textContent = texto;
    mensaje.style.color = error ? "#b91c1c" : "#166534";
}

function actualizarIdentificador() {
    const tipo = tipoInput.value;

    identificadorInput.value = "";
    identificadorInput.disabled = !tipo;
    identificadorInput.setCustomValidity("");

    if (tipo === "Alumno") {
        etiquetaIdentificador.textContent = "Boleta del alumno";
        identificadorInput.placeholder = "Ejemplo: 2026123456";
        identificadorInput.maxLength = 10;
        identificadorInput.inputMode = "numeric";

        ayudaIdentificador.textContent =
            "Escribe exactamente 10 dígitos, sin espacios ni símbolos.";

    } else if (tipo === "Profesor") {
        etiquetaIdentificador.textContent =
            "Número de empleado o identificador institucional";

        identificadorInput.placeholder = "Identificador del profesor";
        identificadorInput.maxLength = 80;
        identificadorInput.inputMode = "text";

        ayudaIdentificador.textContent =
            "Introduce el identificador institucional disponible.";

    } else {
        etiquetaIdentificador.textContent =
            "Boleta o identificador institucional";

        identificadorInput.placeholder =
            "Selecciona primero el tipo de persona";

        identificadorInput.maxLength = 80;
        identificadorInput.inputMode = "text";

        ayudaIdentificador.textContent =
            "Selecciona si registrarás un alumno o un profesor.";
    }
}

function validarIdentificador() {
    const tipo = tipoInput.value;
    const identificador = identificadorInput.value;

    if (tipo === "Alumno" && !/^[0-9]{10}$/.test(identificador)) {
        identificadorInput.setCustomValidity(
            "La boleta debe tener exactamente 10 dígitos, sin letras, espacios ni símbolos."
        );

        return false;
    }

    identificadorInput.setCustomValidity("");

    return true;
}

async function leerError(respuesta) {
    const texto = await respuesta.text();

    if (!texto) {
        return "No se pudo completar la operación.";
    }

    try {
        const datos = JSON.parse(texto);

        return datos.error
            || datos.message
            || datos.detail
            || "No se pudo completar la operación.";

    } catch {
        return "No se pudo completar la operación. Verifica los datos e inténtalo nuevamente.";
    }
}

async function cargarPersonas() {
    try {
        const respuesta = await fetch("/api/personas/estado-cuentas");

        if (!respuesta.ok) {
            throw new Error(await leerError(respuesta));
        }

        personas = await respuesta.json();

        mostrarPersonas();

    } catch (error) {
        tabla.replaceChildren();

        const fila = tabla.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 5;
        celda.textContent =
            error.message || "No se pudieron cargar las personas.";
    }
}

function mostrarPersonas() {
    const texto = busqueda.value.trim().toLowerCase();

    const filtradas = personas.filter(persona =>
        (persona.nombre || "").toLowerCase().includes(texto)
        || (persona.identificador || "").toLowerCase().includes(texto)
    );

    tabla.replaceChildren();

    if (filtradas.length === 0) {
        const fila = tabla.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 5;
        celda.textContent = "No se encontraron personas.";

        return;
    }

    filtradas.forEach(persona => {
        const fila = tabla.insertRow();

        [
            persona.id,
            persona.nombre,
            persona.tipo,
            persona.identificador
        ].forEach(valor => {
            const celda = fila.insertCell();
            celda.textContent = valor ?? "";
        });

        const celdaCuenta = fila.insertCell();

        if (persona.tieneCuenta) {
            const estado = document.createElement("span");

            estado.textContent = "Cuenta creada";
            estado.style.color = "#166534";
            estado.style.fontWeight = "600";

            celdaCuenta.appendChild(estado);

        } else {
            const botonCuenta = document.createElement("button");

            botonCuenta.type = "button";
            botonCuenta.className = "btn-primary";
            botonCuenta.textContent = "Crear cuenta";

            botonCuenta.addEventListener("click", () => {
                crearCuenta(persona, botonCuenta);
            });

            celdaCuenta.appendChild(botonCuenta);
        }
    });
}

async function crearCuenta(persona, boton) {
    const confirmar = window.confirm(
        `¿Deseas crear una cuenta para ${persona.nombre}?`
    );

    if (!confirmar) {
        return;
    }

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

        mostrarMensaje(
            `Cuenta creada correctamente para ${persona.nombre}.`
        );

        await cargarPersonas();

        mostrarCredenciales(persona.nombre, credenciales);

    } catch (error) {
        mostrarMensaje(
            error.message || "No se pudo crear la cuenta.",
            true
        );

        boton.disabled = false;
        boton.textContent = "Crear cuenta";
    }
}

// Muestra las credenciales en una ventana con opción para copiarlas.
function mostrarCredenciales(nombre, credenciales) {
    // Evita dejar abierta otra ventana de credenciales.
    document.getElementById("modalCredenciales")?.remove();

    const textoCredenciales =
        `Persona: ${nombre}\n` +
        `Usuario: ${credenciales.usuario}\n` +
        `Contraseña temporal: ${credenciales.passwordTemporal}`;

    const modal = document.createElement("div");
    modal.id = "modalCredenciales";

    modal.style.cssText = `
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.55);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        z-index: 9999;
    `;

    const contenido = document.createElement("div");

    contenido.style.cssText = `
        background: white;
        width: 100%;
        max-width: 460px;
        padding: 28px;
        border-radius: 12px;
        box-shadow: 0 10px 35px rgba(0, 0, 0, 0.2);
        font-family: Arial, sans-serif;
        color: #1f2937;
    `;

    const titulo = document.createElement("h2");
    titulo.textContent = "¡Cuenta creada correctamente!";
    titulo.style.cssText = `
        margin: 0 0 12px;
        font-size: 21px;
        color: #166534;
    `;

    const descripcion = document.createElement("p");
    descripcion.textContent =
        "Estas son las credenciales de acceso. Cópialas y entrégalas a la persona.";
    descripcion.style.cssText = `
        margin: 0 0 20px;
        line-height: 1.5;
        color: #4b5563;
    `;

    const datos = document.createElement("div");
    datos.style.cssText = `
        background: #f3f4f6;
        border-radius: 8px;
        padding: 16px;
        margin-bottom: 18px;
        line-height: 1.8;
        overflow-wrap: anywhere;
    `;

    const nombreTexto = document.createElement("div");
    nombreTexto.textContent = `Persona: ${nombre}`;

    const usuarioTexto = document.createElement("div");
    usuarioTexto.textContent = `Usuario: ${credenciales.usuario}`;

    const passwordTexto = document.createElement("div");
    passwordTexto.textContent =
        `Contraseña temporal: ${credenciales.passwordTemporal}`;

    passwordTexto.style.fontWeight = "bold";

    datos.append(nombreTexto, usuarioTexto, passwordTexto);

    const aviso = document.createElement("p");
    aviso.textContent =
        "Por seguridad, la persona deberá cambiar su contraseña al iniciar sesión.";
    aviso.style.cssText = `
        font-size: 13px;
        color: #6b7280;
        line-height: 1.5;
        margin: 0 0 20px;
    `;

    const botones = document.createElement("div");
    botones.style.cssText = `
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        flex-wrap: wrap;
    `;

    const botonCopiar = document.createElement("button");
    botonCopiar.type = "button";
    botonCopiar.textContent = "Copiar credenciales";
    botonCopiar.style.cssText = `
        background: #2563eb;
        color: white;
        border: none;
        border-radius: 6px;
        padding: 11px 16px;
        cursor: pointer;
        font-weight: 600;
    `;

    const botonCerrar = document.createElement("button");
    botonCerrar.type = "button";
    botonCerrar.textContent = "Cerrar";
    botonCerrar.style.cssText = `
        background: #e5e7eb;
        color: #1f2937;
        border: none;
        border-radius: 6px;
        padding: 11px 16px;
        cursor: pointer;
        font-weight: 600;
    `;

    botonCopiar.addEventListener("click", async () => {
        try {
            await navigator.clipboard.writeText(textoCredenciales);

            botonCopiar.textContent = "¡Copiado!";
            botonCopiar.style.background = "#15803d";

        } catch (error) {
            // Alternativa para navegadores donde Clipboard API no esté disponible.
            const auxiliar = document.createElement("textarea");
            auxiliar.value = textoCredenciales;

            auxiliar.style.position = "fixed";
            auxiliar.style.opacity = "0";

            document.body.appendChild(auxiliar);
            auxiliar.select();

            const copiado = document.execCommand("copy");
            auxiliar.remove();

            if (copiado) {
                botonCopiar.textContent = "¡Copiado!";
                botonCopiar.style.background = "#15803d";
            } else {
                botonCopiar.textContent = "No se pudo copiar";
            }
        }
    });

    botonCerrar.addEventListener("click", () => {
        modal.remove();
    });

    modal.addEventListener("click", evento => {
        if (evento.target === modal) {
            modal.remove();
        }
    });

    botones.append(botonCopiar, botonCerrar);

    contenido.append(
        titulo,
        descripcion,
        datos,
        aviso,
        botones
    );

    modal.appendChild(contenido);
    document.body.appendChild(modal);

    botonCopiar.focus();
}

tipoInput.addEventListener("change", actualizarIdentificador);

identificadorInput.addEventListener("input", validarIdentificador);

formulario.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!formulario.reportValidity()) {
        return;
    }

    if (!validarIdentificador()) {
        identificadorInput.reportValidity();
        return;
    }

    const persona = {
        nombre: formulario.nombre.value.trim(),
        tipo: tipoInput.value,
        identificador: identificadorInput.value.trim()
    };

    botonRegistrar.disabled = true;
    botonRegistrar.textContent = "Registrando y creando cuenta...";

    mostrarMensaje("Registrando persona y creando cuenta...");

    try {
        const respuesta = await fetch("/api/personas/registro-completo", {
            method: "POST",
            headers: obtenerHeaders(),
            body: JSON.stringify(persona)
        });

        if (!respuesta.ok) {
            throw new Error(await leerError(respuesta));
        }

        const resultado = await respuesta.json();

        formulario.reset();
        actualizarIdentificador();

        mostrarMensaje(
            "Persona registrada y cuenta creada correctamente."
        );

        await cargarPersonas();

        mostrarCredenciales(
            resultado.persona.nombre,
            resultado.credenciales
        );

    } catch (error) {
        mostrarMensaje(
            error.message || "No se pudo registrar la persona y crear su cuenta.",
            true
        );

    } finally {
        botonRegistrar.disabled = false;
        botonRegistrar.textContent = "Registrar persona y crear cuenta";
    }
});

busqueda.addEventListener("input", mostrarPersonas);

actualizarIdentificador();
cargarPersonas();
