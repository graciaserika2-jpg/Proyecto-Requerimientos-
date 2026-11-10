
document.addEventListener("DOMContentLoaded", () => {

    const formPersona = document.getElementById("formPersona");
    const tablaPersonas = document.getElementById("tablaPersonas");
    const mensaje = document.getElementById("mensaje");
    const buscarPersona = document.getElementById("buscarPersona");
    const identificador = document.getElementById("identificador");
    const botonRegistrar = document.getElementById("botonRegistrar");

    let personas = [];

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

    function obtenerEstadoCuenta(persona) {

        if (!persona.tieneCuenta) {
            return "Sin cuenta";
        }

        if (persona.correoVerificado === true) {
            return "Activada";
        }

        if (persona.correoVerificado === false) {
            return "Pendiente de activación";
        }

        return "Cuenta anterior";
    }

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
                    <td colspan="5">No se encontraron alumnos.</td>
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

            const celdaEstado = document.createElement("td");
            celdaEstado.textContent = obtenerEstadoCuenta(persona);

            fila.append(
                celdaId,
                celdaNombre,
                celdaBoleta,
                celdaCorreo,
                celdaEstado
            );

            tablaPersonas.appendChild(fila);
        });
    }

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

    cargarPersonas();

});
