
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("formRestablecer");
    const token = document.getElementById("token");
    const password = document.getElementById("password");
    const confirmarPassword = document.getElementById("confirmarPassword");
    const boton = document.getElementById("btnRestablecer");
    const mensaje = document.getElementById("mensaje");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function mostrarMensaje(texto, tipo) {
        mensaje.textContent = texto;
        mensaje.className = `mensaje ${tipo}`;
    }

    formulario.addEventListener("submit", async (event) => {

        event.preventDefault();

        if (!token.value) {
            mostrarMensaje(
                "El enlace no contiene un token válido.",
                "error"
            );
            return;
        }

        if (password.value.length < 12) {
            mostrarMensaje(
                "La contraseña debe tener al menos 12 caracteres.",
                "error"
            );
            return;
        }

        if (password.value !== confirmarPassword.value) {
            mostrarMensaje(
                "Las contraseñas no coinciden.",
                "error"
            );
            return;
        }

        boton.disabled = true;
        boton.textContent = "Guardando...";

        try {

            const headers = {
                "Content-Type": "application/json"
            };

            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const respuesta = await fetch(
                "/api/cuentas/restablecer-password",
                {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({
                        token: token.value,
                        password: password.value,
                        confirmarPassword: confirmarPassword.value
                    })
                }
            );

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                throw new Error(
                    datos.mensaje || "No fue posible restablecer la contraseña."
                );
            }

            formulario.reset();

            mostrarMensaje(
                datos.mensaje + " Ya puedes iniciar sesión.",
                "correcto"
            );

            boton.style.display = "none";

        } catch (error) {

            mostrarMensaje(
                error.message || "Ocurrió un error al restablecer la contraseña.",
                "error"
            );

            boton.disabled = false;
            boton.textContent = "Guardar nueva contraseña";
        }
    });

});
