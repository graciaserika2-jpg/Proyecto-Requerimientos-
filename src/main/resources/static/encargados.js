
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formularioEncargado");
    const mensaje = document.getElementById("mensaje");
    const lista = document.getElementById("listaEncargados");
    const botonActualizar = document.getElementById("btnActualizar");
    const botonCrear = document.getElementById("btnCrearEncargado");
    const tipoCuenta = document.getElementById("tipoCuenta");

    function actualizarBoton() {
        botonCrear.textContent = tipoCuenta.value === "ADMIN"
            ? "Registrar administrador"
            : "Registrar encargado";
    }

    function mostrarMensajeTabla(texto) {
        lista.replaceChildren();

        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = 5;
        celda.textContent = texto;

        fila.appendChild(celda);
        lista.appendChild(fila);
    }

    async function cargarEncargados() {

        mostrarMensajeTabla("Cargando cuentas...");

        try {
            const respuesta = await fetch("/api/encargados", {
                method: "GET",
                credentials: "same-origin"
            });

            if (!respuesta.ok) {
                throw new Error(
                    `No se pudo consultar la lista (HTTP ${respuesta.status}).`
                );
            }

            const cuentas = await respuesta.json();

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

                const estadoActivacion = cuenta.correoVerificado === true
                    ? "Activada"
                    : "Pendiente de activación";

                const valores = [
                    cuenta.nombre,
                    cuenta.usuario,
                    cuenta.correo,
                    cuenta.rol,
                    estadoActivacion
                ];

                valores.forEach(valor => {
                    const celda = document.createElement("td");
                    celda.textContent = valor ?? "No registrado";
                    fila.appendChild(celda);
                });

                lista.appendChild(fila);
            });

        } catch (error) {
            mostrarMensajeTabla("Error al cargar las cuentas: " + error.message);
        }
    }

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

            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');

            if (!tokenMeta || !headerMeta) {
                throw new Error("No se encontró el token de seguridad CSRF.");
            }

            const respuesta = await fetch(endpoint, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    [headerMeta.content]: tokenMeta.content
                },
                body: JSON.stringify(datos)
            });

            const resultado = await respuesta.json().catch(() => ({}));

            if (!respuesta.ok) {
                throw new Error(
                    resultado.detail ||
                    resultado.message ||
                    resultado.error ||
                    `No se pudo registrar la cuenta (HTTP ${respuesta.status}).`
                );
            }

            mensaje.textContent = resultado.mensaje ||
                "Cuenta registrada correctamente.";

            formulario.reset();
            tipoCuenta.value = "ENCARGADO";
            actualizarBoton();

            // Actualizar la tabla después de registrar cualquiera de los dos roles.
            await cargarEncargados();

        } catch (error) {
            mensaje.textContent = error.message;

        } finally {
            botonCrear.disabled = false;
            actualizarBoton();
        }
    });

    tipoCuenta.addEventListener("change", actualizarBoton);

    botonActualizar.addEventListener("click", cargarEncargados);

    actualizarBoton();
    cargarEncargados();
});
