
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formularioAdministrador");
    const lista = document.getElementById("listaAdministradores");
    const mensaje = document.getElementById("mensajeAdministrador");
    const botonCrear = document.getElementById("btnCrearAdministrador");
    const botonActualizar = document.getElementById("btnActualizarAdministradores");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function obtenerHeaders() {
        const headers = {
            "Content-Type": "application/json"
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        return headers;
    }

    function mostrarMensaje(texto, tipo) {
        mensaje.textContent = texto;
        mensaje.className = "form-message " + tipo;
    }

    function escaparHTML(valor) {
        return String(valor ?? "").replace(/[&<>"']/g, caracter => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[caracter]);
    }

    async function cargarAdministradores() {

        lista.innerHTML = `
            <tr>
                <td colspan="4">Cargando administradores...</td>
            </tr>
        `;

        try {
            const respuesta = await fetch("/api/encargados/administradores", {
                method: "GET",
                headers: obtenerHeaders()
            });

            if (!respuesta.ok) {
                throw new Error("No fue posible consultar los administradores.");
            }

            const administradores = await respuesta.json();

            if (!Array.isArray(administradores) || administradores.length === 0) {
                lista.innerHTML = `
                    <tr>
                        <td colspan="4">No hay administradores registrados.</td>
                    </tr>
                `;
                return;
            }

            lista.innerHTML = administradores.map(administrador => {

                const activado = administrador.correoVerificado === true;

                return `
                    <tr>
                        <td>${escaparHTML(administrador.nombre)}</td>
                        <td>${escaparHTML(administrador.usuario)}</td>
                        <td>${escaparHTML(administrador.correo)}</td>
                        <td>
                            ${activado
                                ? '<span class="status-badge status-success">Activada</span>'
                                : '<span class="status-badge status-warning">Pendiente de activación</span>'
                            }
                        </td>
                    </tr>
                `;

            }).join("");

        } catch (error) {
            console.error(error);

            lista.innerHTML = `
                <tr>
                    <td colspan="4">Ocurrió un error al cargar los administradores.</td>
                </tr>
            `;
        }
    }

    formulario.addEventListener("submit", async (evento) => {

        evento.preventDefault();

        const datos = {
            nombre: document.getElementById("nombre").value.trim(),
            usuario: document.getElementById("usuario").value.trim(),
            correo: document.getElementById("correo").value.trim()
        };

        if (!datos.nombre || !datos.usuario || !datos.correo) {
            mostrarMensaje("Completa todos los campos.", "error");
            return;
        }

        botonCrear.disabled = true;
        botonCrear.textContent = "Registrando...";

        mostrarMensaje("", "");

        try {

            const respuesta = await fetch("/api/encargados/administradores", {
                method: "POST",
                headers: obtenerHeaders(),
                body: JSON.stringify(datos)
            });

            const resultado = await respuesta.json().catch(() => ({}));

            if (!respuesta.ok) {
                throw new Error(
                    resultado.message ||
                    resultado.mensaje ||
                    "No fue posible registrar al administrador."
                );
            }

            mostrarMensaje(
                resultado.mensaje ||
                "Administrador registrado. Se envió el enlace de activación.",
                "success"
            );

            formulario.reset();

            await cargarAdministradores();

        } catch (error) {

            console.error(error);
            mostrarMensaje(error.message, "error");

        } finally {

            botonCrear.disabled = false;
            botonCrear.textContent = "Registrar administrador";

        }
    });

    botonActualizar.addEventListener("click", cargarAdministradores);

    cargarAdministradores();

});
