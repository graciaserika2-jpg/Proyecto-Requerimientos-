
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formRecuperacion");
    const correo = document.getElementById("correo");
    const boton = document.getElementById("btnEnviar");
    const mensaje = document.getElementById("mensaje");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function mostrarMensaje(texto, tipo) {
        mensaje.textContent = texto;
        mensaje.className = `mensaje ${tipo}`;
    }

    formulario.addEventListener("submit", async (event) => {

        event.preventDefault();

        boton.disabled = true;
        boton.textContent = "Enviando...";

        try {

            const headers = {
                "Content-Type": "application/json"
            };

            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const respuesta = await fetch(
                "/api/cuentas/solicitar-recuperacion",
                {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({
                        correo: correo.value.trim()
                    })
                }
            );

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                throw new Error(
                    datos.mensaje || "No fue posible procesar la solicitud."
                );
            }

            formulario.reset();

            mostrarMensaje(
                datos.mensaje,
                "correcto"
            );

        } catch (error) {

            mostrarMensaje(
                error.message || "Ocurrió un error al enviar la solicitud.",
                "error"
            );

        } finally {

            boton.disabled = false;
            boton.textContent = "Enviar instrucciones";
        }
    });

});
