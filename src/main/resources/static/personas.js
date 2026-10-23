
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
            "Introduce el identificador institucional disponible. Confirmaremos el formato oficial posteriormente.";
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
        const respuesta = await fetch("/api/personas");

        if (!respuesta.ok) {
            throw new Error(await leerError(respuesta));
        }

        personas = await respuesta.json();
        mostrarPersonas();
    } catch (error) {
        tabla.replaceChildren();

        const fila = tabla.insertRow();
        const celda = fila.insertCell();

        celda.colSpan = 4;
        celda.textContent = error.message
            || "No se pudieron cargar las personas.";
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

        celda.colSpan = 4;
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
    });
}

tipoInput.addEventListener("change", actualizarIdentificador);

identificadorInput.addEventListener("input", () => {
    validarIdentificador();
});

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
    mostrarMensaje("Registrando persona...");

    try {
        const respuesta = await fetch("/api/personas", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(persona)
        });

        if (!respuesta.ok) {
            throw new Error(await leerError(respuesta));
        }

        formulario.reset();
        actualizarIdentificador();

        mostrarMensaje("Persona registrada correctamente.");
        await cargarPersonas();
    } catch (error) {
        mostrarMensaje(
            error.message || "No se pudo registrar la persona.",
            true
        );
    } finally {
        botonRegistrar.disabled = false;
    }
});

busqueda.addEventListener("input", mostrarPersonas);

actualizarIdentificador();
cargarPersonas();
