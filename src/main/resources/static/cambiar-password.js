
const formulario = document.getElementById("formCambiarPassword");
const mensaje = document.getElementById("mensaje");
const boton = document.getElementById("btnCambiarPassword");

formulario.addEventListener("submit", async (event) => {
    event.preventDefault();

    const passwordActual =
        document.getElementById("passwordActual").value;

    const passwordNueva =
        document.getElementById("passwordNueva").value;

    const confirmarPassword =
        document.getElementById("confirmarPassword").value;

    if (passwordNueva.length < 12) {
        mostrarMensaje(
            "La nueva contraseña debe tener al menos 12 caracteres.",
            false
        );
        return;
    }

    if (passwordNueva !== confirmarPassword) {
        mostrarMensaje("Las nuevas contraseñas no coinciden.", false);
        return;
    }

    // Recuperar el token CSRF que Thymeleaf colocó en el formulario.
    const csrfToken = document.querySelector(
        'input[name="_csrf"]'
    )?.value;

    if (!csrfToken) {
        mostrarMensaje(
            "No se encontró el token de seguridad. Recarga la página e intenta nuevamente.",
            false
        );
        return;
    }

    boton.disabled = true;
    boton.textContent = "Guardando...";

    try {
        const respuesta = await fetch("/api/mi-cuenta/cambiar-password", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-CSRF-TOKEN": csrfToken
            },
            body: JSON.stringify({
                passwordActual,
                passwordNueva,
                confirmarPassword
            })
        });

        const textoRespuesta = await respuesta.text();

        let datos = {};

        try {
            datos = JSON.parse(textoRespuesta);
        } catch (error) {
            datos = {};
        }

        if (!respuesta.ok) {
            if (respuesta.status === 403) {
                mostrarMensaje(
                    "El servidor rechazó la solicitud por seguridad (403). Recarga la página e intenta nuevamente.",
                    false
                );
            } else {
                mostrarMensaje(
                    datos.mensaje ||
                    `Error del servidor (${respuesta.status}). Revisa los datos e intenta nuevamente.`,
                    false
                );
            }
            return;
        }

        mostrarMensaje(
            datos.mensaje || "Contraseña actualizada correctamente.",
            true
        );

        formulario.reset();

        setTimeout(() => {
            const formularioLogout = document.createElement("form");
            formularioLogout.method = "POST";
            formularioLogout.action = "/logout";

            const csrfToken = document.querySelector(
                'input[name="_csrf"]'
            )?.value;

            if (csrfToken) {
                const inputCsrf = document.createElement("input");
                inputCsrf.type = "hidden";
                inputCsrf.name = "_csrf";
                inputCsrf.value = csrfToken;
                formularioLogout.appendChild(inputCsrf);
            }

            document.body.appendChild(formularioLogout);
            formularioLogout.submit();
        }, 1800);

    } catch (error) {
        console.error("Error al cambiar contraseña:", error);

        mostrarMensaje(
            "No se pudo establecer comunicación con el servidor.",
            false
        );
    } finally {
        boton.disabled = false;
        boton.textContent = "Guardar nueva contraseña";
    }
});

function mostrarMensaje(texto, correcto) {
    mensaje.textContent = texto;
    mensaje.className = correcto
        ? "mensaje correcto"
        : "mensaje error";
    mensaje.hidden = false;
}
