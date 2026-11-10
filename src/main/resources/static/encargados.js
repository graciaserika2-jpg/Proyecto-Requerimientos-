
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formularioEncargado");
    const mensaje = document.getElementById("mensaje");
    const lista = document.getElementById("listaEncargados");
    const botonActualizar = document.getElementById("btnActualizar");
    const botonCrear = document.getElementById("btnCrearEncargado");


    // =====================================================
    // CARGAR ENCARGADOS
    // =====================================================

    async function cargarEncargados() {

        lista.replaceChildren();

        try {

            const respuesta = await fetch("/api/encargados");

            if (!respuesta.ok) {
                throw new Error(
                    "No se pudo consultar la lista de encargados."
                );
            }

            const encargados = await respuesta.json();

            if (encargados.length === 0) {

                const fila = document.createElement("tr");
                const celda = document.createElement("td");

                celda.colSpan = 5;
                celda.textContent =
                    "Todavía no hay encargados registrados.";

                fila.appendChild(celda);
                lista.appendChild(fila);

                return;
            }

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

            mensaje.textContent = error.message;
        }
    }


    // =====================================================
    // REGISTRAR ENCARGADO
    // =====================================================

    formulario.addEventListener("submit", async evento => {

        evento.preventDefault();

        mensaje.textContent = "";

        const datos = {

            nombre: formulario.nombre.value.trim(),

            usuario: formulario.usuario.value.trim(),

            correo: formulario.correo.value.trim()
        };

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

            const respuesta = await fetch("/api/encargados", {

                method: "POST",

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
                    `No se pudo registrar el encargado (HTTP ${respuesta.status}).`
                );
            }

            mensaje.textContent = resultado.mensaje ||
                "Encargado registrado correctamente.";

            formulario.reset();

            await cargarEncargados();

        } catch (error) {

            mensaje.textContent = error.message;

        } finally {

            botonCrear.disabled = false;
            botonCrear.textContent = "Registrar encargado";
        }
    });


    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    botonActualizar.addEventListener("click", cargarEncargados);


    // =====================================================
    // CARGA INICIAL
    // =====================================================

    cargarEncargados();

});
