const formulario = document.getElementById("formularioEncargado");
const mensaje = document.getElementById("mensaje");
const lista = document.getElementById("listaEncargados");
const botonActualizar = document.getElementById("btnActualizar");

async function cargarEncargados() {
    lista.replaceChildren();

    try {
        const respuesta = await fetch("/api/encargados");

        if (!respuesta.ok) {
            throw new Error("No se pudo consultar la lista de encargados.");
        }

        const encargados = await respuesta.json();

        if (encargados.length === 0) {
            const fila = document.createElement("tr");
            const celda = document.createElement("td");

            celda.colSpan = 3;
            celda.textContent = "Todavía no hay encargados registrados.";

            fila.appendChild(celda);
            lista.appendChild(fila);
            return;
        }

        encargados.forEach(encargado => {
            const fila = document.createElement("tr");

            [encargado.nombre, encargado.usuario, encargado.rol].forEach(valor => {
                const celda = document.createElement("td");
                celda.textContent = valor ?? "";
                fila.appendChild(celda);
            });

            lista.appendChild(fila);
        });
    } catch (error) {
        mensaje.textContent = error.message;
    }
}

formulario.addEventListener("submit", async evento => {
    evento.preventDefault();
    mensaje.textContent = "";

    const datos = {
        nombre: formulario.nombre.value.trim(),
        usuario: formulario.usuario.value.trim(),
        password: formulario.password.value
    };

    try {
        // Obtener el token CSRF generado por Spring Security.
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');

        if (!tokenMeta || !headerMeta) {
            throw new Error(
                "No se encontró el token de seguridad CSRF. Revisa encargados.html."
            );
        }

        const token = tokenMeta.content;
        const nombreHeader = headerMeta.content;

        const respuesta = await fetch("/api/encargados", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [nombreHeader]: token
            },
            body: JSON.stringify(datos)
        });

        const resultado = await respuesta.json().catch(() => ({}));

        if (!respuesta.ok) {
            throw new Error(
                resultado.detail ||
                resultado.error ||
                resultado.message ||
                `No se pudo crear la cuenta (HTTP ${respuesta.status}).`
            );
        }

        mensaje.textContent = "Cuenta creada correctamente.";
        formulario.reset();

        await cargarEncargados();
    } catch (error) {
        mensaje.textContent = error.message;
    }
});

botonActualizar.addEventListener("click", cargarEncargados);

cargarEncargados();