
document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll(".sidebar").forEach(sidebar => {

        const boton = sidebar.querySelector(".menu-toggle");

        if (!boton) return;

        boton.addEventListener("click", () => {

            const abierto = sidebar.classList.toggle("menu-open");

            boton.setAttribute("aria-expanded", String(abierto));

            boton.setAttribute(
                "aria-label",
                abierto ? "Cerrar menú" : "Abrir menú"
            );

        });

        document.addEventListener("keydown", event => {

            if (event.key === "Escape") {

                sidebar.classList.remove("menu-open");

                boton.setAttribute("aria-expanded", "false");
                boton.setAttribute("aria-label", "Abrir menú");

            }

        });

    });

});
