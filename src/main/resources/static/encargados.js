
const formulario = document.getElementById("formularioEncargado");
const mensaje = document.getElementById("mensaje");
const lista = document.getElementById("listaEncargados");
const botonActualizar = document.getElementById("btnActualizar");
const botonCrear = document.getElementById("btnCrearEncargado");

const panelCredenciales = document.getElementById("panelCredenciales");
const nombreGenerado = document.getElementById("nombreGenerado");
const usuarioGenerado = document.getElementById("usuarioGenerado");
const passwordGenerado = document.getElementById("passwordGenerado");
const botonCopiar = document.getElementById("btnCopiarCredenciales");
const mensajeCopia = document.getElementById("mensajeCopia");


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

            [
                encargado.nombre,
                encargado.usuario,
                encargado.rol
            ].forEach(valor => {

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


// REGISTRAR ENCARGADO
formulario.addEventListener("submit", async evento => {

    evento.preventDefault();

    mensaje.textContent = "";
    mensajeCopia.textContent = "";
    panelCredenciales.hidden = true;

    const datos = {
        nombre: formulario.nombre.value.trim(),
        usuario: formulario.usuario.value.trim()
    };

    botonCrear.disabled = true;
    botonCrear.textContent = "Creando cuenta...";

    try {

        // Obtener el token CSRF de Spring Security.
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

        // Mostrar las credenciales generadas.
        nombreGenerado.textContent = resultado.nombre;
        usuarioGenerado.textContent = resultado.usuario;
        passwordGenerado.textContent = resultado.passwordTemporal;

        panelCredenciales.hidden = false;

        mensaje.textContent = resultado.mensaje;

        formulario.reset();

        await cargarEncargados();

    } catch (error) {

        mensaje.textContent = error.message;

    } finally {

        botonCrear.disabled = false;
        botonCrear.textContent = "Crear cuenta";
    }
});


// COPIAR CREDENCIALES
botonCopiar.addEventListener("click", async () => {

    const texto = 
        "Nombre: " + nombreGenerado.textContent + "\n" +
        "Usuario: " + usuarioGenerado.textContent + "\n" +
        "Contraseña temporal: " + passwordGenerado.textContent;

    try {

        await navigator.clipboard.writeText(texto);

        mensajeCopia.textContent = "Credenciales copiadas correctamente.";

    } catch (error) {

        mensajeCopia.textContent =
            "No se pudieron copiar automáticamente. Puedes seleccionar y copiar las credenciales manualmente.";
    }
});


// ACTUALIZAR LISTA
botonActualizar.addEventListener("click", cargarEncargados);


// CARGA INICIAL
cargarEncargados();
