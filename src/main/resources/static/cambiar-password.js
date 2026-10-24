
document.addEventListener("DOMContentLoaded", () => {
    const formulario = document.getElementById("formCambiarPassword");
    const passwordActual = document.getElementById("passwordActual");
    const passwordNueva = document.getElementById("passwordNueva");
    const confirmarPassword = document.getElementById("confirmarPassword");
    const mensaje = document.getElementById("mensaje");

    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        mensaje.textContent = "";

        if (passwordNueva.value !== confirmarPassword.value) {
            mensaje.textContent = "Las nuevas contraseñas no coinciden.";
            return;
        }

        if (passwordNueva.value.length < 12) {
            mensaje.textContent =
                "La nueva contraseña debe tener al menos 12 caracteres.";
            return;
        }

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const encabezados = {
            "Content-Type": "application/json"
        };

        if (csrfToken && csrfHeader) {
            encabezados[csrfHeader] = csrfToken;
        }

        try {
            const respuesta = await fetch("/api/mi-cuenta/cambiar-password", {
                method: "POST",
                headers: encabezados,
                body: JSON.stringify({
                    passwordActual: passwordActual.value,
                    passwordNueva: passwordNueva.value,
                    confirmarPassword: confirmarPassword.value
                })
            });

            const datos = await respuesta.json();

            if (!respuesta.ok) {
                mensaje.textContent =
                    datos.mensaje || "No se pudo cambiar la contraseña.";
                return;
            }

            mensaje.textContent = "Contraseña actualizada correctamente.";

            formulario.reset();

            setTimeout(() => {
                window.location.href = "/inicio";
            }, 1200);

        } catch (error) {
            mensaje.textContent =
                "No se pudo conectar con el servidor. Intenta nuevamente.";
        }
    });
});
