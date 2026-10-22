
const formulario = document.getElementById("formPersona");
const tabla = document.getElementById("tablaPersonas");
const mensaje = document.getElementById("mensaje");
const busqueda = document.getElementById("buscarPersona");

let personas = [];

async function cargarPersonas() {
    try {
        const respuesta = await fetch("/api/personas");

        if (!respuesta.ok) {
            throw new Error("No se pudieron consultar las personas.");
        }

        personas = await respuesta.json();
        mostrarPersonas();
    } catch (error) {
        tabla.replaceChildren();
        const fila = tabla.insertRow();
        fila.insertCell().textContent = error.message;
    }
}

function mostrarPersonas() {
    const texto = busqueda.value.trim().toLowerCase();

    const filtradas = personas.filter(persona =>
        (persona.nombre || "").toLowerCase().includes(texto) ||
        (persona.identificador || "").toLowerCase().includes(texto)
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

formulario.addEventListener("submit", async evento => {
    evento.preventDefault();
    mensaje.textContent = "Registrando persona...";

    const persona = {
        nombre: formulario.nombre.value.trim(),
        tipo: formulario.tipo.value,
        identificador: formulario.identificador.value.trim()
    };

    try {
        const respuesta = await fetch("/api/personas", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(persona)
        });

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            throw new Error(error.message || error.error ||
                "No se pudo registrar la persona.");
        }

        formulario.reset();
        mensaje.textContent = "Persona registrada correctamente.";
        await cargarPersonas();
    } catch (error) {
        mensaje.textContent = error.message;
    }
});

busqueda.addEventListener("input", mostrarPersonas);

cargarPersonas();
