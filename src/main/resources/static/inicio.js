
async function cargarResumen() {
    const mensaje = document.getElementById("mensajeInicio");

    try {
        const [respuestaEquipos, respuestaPrestamos, respuestaPersonas] =
            await Promise.all([
                fetch("/api/equipos"),
                fetch("/api/prestamos"),
                fetch("/api/personas")
            ]);

        if (!respuestaEquipos.ok ||
            !respuestaPrestamos.ok ||
            !respuestaPersonas.ok) {
            throw new Error("No se pudo cargar el resumen del sistema.");
        }

        const equipos = await respuestaEquipos.json();
        const prestamos = await respuestaPrestamos.json();
        const personas = await respuestaPersonas.json();

        document.getElementById("totalEquipos").textContent =
            equipos.length;

        document.getElementById("equiposDisponibles").textContent =
            equipos.filter(e => e.estado === "Disponible").length;

        document.getElementById("prestamosActivos").textContent =
            prestamos.filter(p => p.estado === "Activo").length;

        document.getElementById("totalPersonas").textContent =
            personas.length;

        mensaje.textContent = "";
    } catch (error) {
        mensaje.textContent = error.message;
    }
}

cargarResumen();
