
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formularioEncargado");
    const mensaje = document.getElementById("mensaje");
    const lista = document.getElementById("listaEncargados");
    const botonActualizar = document.getElementById("btnActualizar");
    const botonCrear = document.getElementById("btnCrearEncargado");
    const tipoCuenta = document.getElementById("tipoCuenta");

    // =====================================================
    // ACTUALIZAR TEXTO DEL BOTÓN SEGÚN EL TIPO DE CUENTA
    // =====================================================

    function actualizarBoton() {

        if (tipoCuenta.value === "ADMIN") {
            botonCrear.textContent = "Registrar administrador";
        } else {
            botonCrear.textContent = "Registrar encargado";
        }
    }

    tipoCuenta.addEventListener("change", actualizarBoton);


    // =====================================================
    // MOSTRAR MENSAJE EN LA TABLA
    // =====================================================

    function mostrarMensajeTabla(texto) {

        lista.replaceChildren();

        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = 5;
        celda.textContent = texto;

        fila.appendChild(celda);
        lista.appendChild(fila);
    }


    // =====================================================
    // CARGAR ENCARGADOS
    // =====================================================

    async function cargarEncargados() {

        mostrarMensajeTabla("Cargando encargados...");

        try {

            const respuesta = await fetch("/api/encargados", {
                method: "GET",
                credentials: "same-origin"
            });

            if (!respuesta.ok) {
                throw new Error(
                    `No se pudo consultar la lista de encargados (HTTP ${respuesta.status}).`
                );
            }

            const encargados = await respuesta.json();

            if (!Array.isArray(encargados)) {
                throw new Error(
                    "La respuesta del servidor no tiene el formato esperado."
                );
            }

            if (encargados.length === 0) {

                mostrarMensajeTabla(
                    "Todavía no hay encargados registrados."
                );

                return;
            }

            lista.replaceChildren();

            encargados.forEach(encargado => {

                const fila = document.createElement("tr");

                const estadoActivacion =
                    encargado.correoVerificado === true
                        ? "Activada"
                        : "Pendiente de activación";

                const valores = [
                    encargado.nombre,
                    encargado.usuario,
                    encargado.correo,
                    encargado.rol,
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

            mostrarMensajeTabla(
                "Error al cargar los encargados: " + error.message
            );
        }
    }


    // =====================================================
    // REGISTRAR ENCARGADO O ADMINISTRADOR
    // =====================================================

    formulario.addEventListener("submit", async evento => {

        evento.preventDefault();

        mensaje.textContent = "";

        const datos = {

            nombre: formulario.nombre.value.trim(),

            usuario: formulario.usuario.value.trim(),

            correo: formulario.correo.value.trim()
        };

        const esAdministrador = tipoCuenta.value === "ADMIN";

        const endpoint = esAdministrador
            ? "/api/encargados/administradores"
            : "/api/encargados";

        botonCrear.disabled = true;
        botonCrear.textContent = "Registrando...";

        try {

            // Obtener token CSRF de Spring Security.

            const tokenMeta =
                document.querySelector('meta[name="_csrf"]');

            const headerMeta =
                document.querySelector('meta[name="_csrf_header"]');

            if (!tokenMeta || !headerMeta) {

                throw new Error(
                    "No se encontró el token CSRF. Revisa encargados.html."
                );
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

            // La lista solo contiene encargados.
            if (!esAdministrador) {
                await cargarEncargados();
            }

        } catch (error) {

            mensaje.textContent = error.message;

        } finally {

            botonCrear.disabled = false;
            actualizarBoton();
        }
    });


    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    botonActualizar.addEventListener("click", cargarEncargados);


    // =====================================================
    // CARGA INICIAL
    // =====================================================

    actualizarBoton();
    cargarEncargados();

});
