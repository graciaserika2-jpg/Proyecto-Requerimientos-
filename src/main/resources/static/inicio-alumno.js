
document.addEventListener("DOMContentLoaded", () => {

    const contenedor = document.getElementById("estadoRestriccion");

    if (!contenedor) {
        return;
    }

    async function cargarEstadoCuenta() {

        try {

            const respuesta = await fetch("/api/personas/mi-estado", {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                }
            });

            if (!respuesta.ok) {
                throw new Error("No fue posible consultar el estado de la cuenta.");
            }

            const estado = await respuesta.json();

            mostrarEstado(estado);

        } catch (error) {

            console.error("Error al consultar el estado de la cuenta:", error);

            contenedor.className = "estado-restriccion error";

            contenedor.innerHTML = `
                <h3>No se pudo consultar tu estado</h3>
                <p>Ocurrió un problema al consultar la información de tu cuenta.</p>
                <p>Intenta recargar la página.</p>
            `;
        }
    }


    function mostrarEstado(estado) {

        const tieneBloqueo = estado.tieneBloqueoActivo === true;

        const fechaFinSuspension = estado.fechaFinSuspension;

        const hoy = obtenerFechaLocal();

        const tieneSuspension =
            Boolean(fechaFinSuspension) &&
            hoy < fechaFinSuspension;

        // ==========================================
        // SIN RESTRICCIONES
        // ==========================================

        if (!tieneBloqueo && !tieneSuspension) {

            contenedor.className = "estado-restriccion disponible";

            contenedor.innerHTML = `
                <span class="estado-etiqueta etiqueta-disponible">
                    SIN RESTRICCIONES
                </span>

                <h3>Tu cuenta no tiene restricciones</h3>

                <p>
                    Actualmente no tienes bloqueos administrativos
                    ni suspensiones temporales.
                </p>

                <p>
                    Puedes consultar los equipos disponibles y solicitar préstamos,
                    siempre que cumplas las demás reglas del sistema.
                </p>
            `;

            return;
        }


        // ==========================================
        // CON UNA O VARIAS RESTRICCIONES
        // ==========================================

        contenedor.className = tieneBloqueo
            ? "estado-restriccion bloqueado"
            : "estado-restriccion suspendido";

        let contenido = `
            <h3>No puedes solicitar préstamos por el momento</h3>
            <p>Tu cuenta tiene las siguientes restricciones:</p>
        `;


        // ==========================================
        // BLOQUEO ADMINISTRATIVO
        // ==========================================

        if (tieneBloqueo) {

            contenido += `
                <div style="margin-top: 18px; padding: 15px; border-radius: 8px; background: #fff0f0;">

                    <span class="estado-etiqueta etiqueta-bloqueo">
                        BLOQUEO ADMINISTRATIVO ACTIVO
                    </span>

                    <h3>Bloqueo pendiente de resolución</h3>

                    <p>
                        <strong>Equipo relacionado:</strong>
                        ${escaparHTML(estado.equipoBloqueado || "No especificado")}
                    </p>

                    <p>
                        <strong>Motivo:</strong>
                        ${escaparHTML(estado.motivoBloqueo || "No especificado")}
                    </p>

                    <p>
                        <strong>Fecha del bloqueo:</strong>
                        ${formatearFecha(estado.fechaBloqueo)}
                    </p>

                    <p>
                        Para resolver esta situación, acude con ADMIN o ENCARGADO.
                        Este bloqueo debe levantarse manualmente.
                    </p>

                </div>
            `;
        }


        // ==========================================
        // SUSPENSIÓN TEMPORAL
        // ==========================================

        if (tieneSuspension) {

            contenido += `
                <div style="margin-top: 18px; padding: 15px; border-radius: 8px; background: #fff8e8;">

                    <span class="estado-etiqueta etiqueta-suspension">
                        SUSPENSIÓN TEMPORAL ACTIVA
                    </span>

                    <h3>Suspensión por devolución fuera de tiempo</h3>

                    <p>
                        Tu cuenta tiene una suspensión temporal de siete días naturales.
                    </p>

                    <p>
                        <strong>Fecha de finalización:</strong>
                        ${formatearFecha(fechaFinSuspension)}
                    </p>

                    <p>
                        Podrás volver a solicitar préstamos a partir del
                        <strong>${formatearFecha(fechaFinSuspension)}</strong>,
                        siempre que no tengas otro bloqueo administrativo activo.
                    </p>

                </div>
            `;
        }

        contenedor.innerHTML = contenido;
    }


    // ==========================================
    // FECHA LOCAL
    // ==========================================

    function obtenerFechaLocal() {

        const ahora = new Date();

        const anio = ahora.getFullYear();
        const mes = String(ahora.getMonth() + 1).padStart(2, "0");
        const dia = String(ahora.getDate()).padStart(2, "0");

        return `${anio}-${mes}-${dia}`;
    }


    // ==========================================
    // FORMATEAR FECHAS
    // ==========================================

    function formatearFecha(fecha) {

        if (!fecha) {
            return "No especificada";
        }

        const partes = fecha.substring(0, 10).split("-");

        if (partes.length !== 3) {
            return escaparHTML(fecha);
        }

        return `${partes[2]}/${partes[1]}/${partes[0]}`;
    }


    // ==========================================
    // PROTEGER TEXTO MOSTRADO EN HTML
    // ==========================================

    function escaparHTML(valor) {

        return String(valor).replace(/[&<>"']/g, caracter => {

            const caracteres = {
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#039;"
            };

            return caracteres[caracter];
        });
    }


    cargarEstadoCuenta();

});
