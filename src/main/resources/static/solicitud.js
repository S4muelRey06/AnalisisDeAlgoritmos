let profesoresArchivo = [];
let leyendoProfesores = false;

const form = document.querySelector("form");
const materiasElegidas = [];
const campoMateria = document.querySelector("#materia-nombre");
const campoPrioridad = document.querySelector("#materia-prioridad");
const listaMaterias = document.querySelector("#materias-agregadas");
const errorMateria = document.querySelector("#error-materia");
const buscadorCatalogo = document.querySelector("#buscar-materia-catalogo");
const botonesCatalogo = document.querySelectorAll(".catalogo-boton");

function mostrarErrorMateria(mensaje) {
    errorMateria.textContent = mensaje;
    errorMateria.hidden = !mensaje;
}

function resumen() {
    const lista = document.querySelector("#resumen");
    lista.replaceChildren();

    for (const materia of materiasElegidas) {
        const item = document.createElement("li");
        item.textContent = materia.nombre + " · Prioridad " + materia.prioridad;
        lista.append(item);
    }

    const campos = new FormData(form);
    document.querySelector("#resumen-preferencias").textContent =
        "Franjas: " + (campos.getAll("franja").join(", ") || "todas") +
        ". Profesores: " + (profesoresArchivo.join(", ") || "sin preferencia") +
        ". Volumen: " + (campos.get("volumen") || "pendiente") +
        ". Cercanía: " + campos.get("cercania") + ". Promedio: " + campos.get("promedio");
}

function dibujarMaterias() {
    listaMaterias.replaceChildren();

    for (const materia of materiasElegidas) {
        const fila = document.createElement("li");
        fila.className = "subject-row";

        const nombre = document.createElement("strong");
        nombre.className = "subject-name";
        nombre.textContent = materia.nombre;

        const prioridad = document.createElement("select");
        prioridad.setAttribute("aria-label", "Prioridad de " + materia.nombre);
        for (const nivel of ["Alta", "Media", "Baja"]) {
            const opcion = document.createElement("option");
            opcion.value = nivel;
            opcion.textContent = nivel;
            opcion.selected = nivel === materia.prioridad;
            prioridad.append(opcion);
        }
        prioridad.addEventListener("change", () => {
            materia.prioridad = prioridad.value;
            resumen();
        });

        const quitar = document.createElement("button");
        quitar.type = "button";
        quitar.className = "secondary";
        quitar.textContent = "Quitar";
        quitar.setAttribute("aria-label", "Quitar " + materia.nombre);
        quitar.addEventListener("click", () => {
            materiasElegidas.splice(materiasElegidas.indexOf(materia), 1);
            dibujarMaterias();
        });

        fila.append(nombre, prioridad, quitar);
        listaMaterias.append(fila);
    }
    resumen();
}

function agregarMateria() {
    const nombreIngresado = campoMateria.value.trim();
    if (!nombreIngresado) {
        mostrarErrorMateria("Escribe el nombre de una materia.");
        campoMateria.focus();
        return;
    }

    // El nombre se toma del catálogo para enviarlo igual que está en la BD.
    let nombreCatalogo = "";
    for (const boton of botonesCatalogo) {
        if (boton.getAttribute("data-nombre").toLowerCase() === nombreIngresado.toLowerCase()) {
            nombreCatalogo = boton.getAttribute("data-nombre");
            break;
        }
    }
    if (!nombreCatalogo) {
        mostrarErrorMateria("La materia no está registrada en la base de datos. Consulta el catálogo.");
        campoMateria.focus();
        return;
    }
    if (materiasElegidas.some(materia => materia.nombre === nombreCatalogo)) {
        mostrarErrorMateria("Esta materia ya fue agregada.");
        campoMateria.focus();
        return;
    }

    materiasElegidas.push({ nombre: nombreCatalogo, prioridad: campoPrioridad.value });
    campoMateria.value = "";
    mostrarErrorMateria("");
    dibujarMaterias();
    campoMateria.focus();
}

// El catálogo solo completa el campo. La materia se agrega con el botón del formulario.
for (const boton of botonesCatalogo) {
    boton.addEventListener("click", () => {
        campoMateria.value = boton.getAttribute("data-nombre");
        mostrarErrorMateria("");
        campoMateria.focus();
    });
}

buscadorCatalogo.addEventListener("input", () => {
    const busqueda = buscadorCatalogo.value.trim().toLowerCase();
    let encontrados = 0;
    for (const boton of botonesCatalogo) {
        const coincide = boton.getAttribute("data-nombre").toLowerCase().includes(busqueda);
        boton.parentElement.hidden = !coincide;
        if (coincide) encontrados++;
    }
    document.querySelector("#catalogo-sin-resultados").hidden = encontrados > 0;
});

document.querySelector("#agregar-materia").addEventListener("click", agregarMateria);
campoMateria.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        agregarMateria();
    }
});
form.addEventListener("change", resumen);
form.querySelector('[name="promedio"]').addEventListener("input", resumen);

// Mantiene el JSON que ya procesa HorarioController.
function descargarSolicitud() {
    if (leyendoProfesores) return false;
    if (campoMateria.value.trim()) {
        mostrarErrorMateria("Pulsa 'Agregar materia' para incluir el nombre que escribiste.");
        campoMateria.focus();
        return false;
    }
    if (!form.reportValidity() || materiasElegidas.length === 0) {
        if (materiasElegidas.length === 0) mostrarErrorMateria("Agrega al menos una materia.");
        return false;
    }

    const materias = {};
    for (const materia of materiasElegidas) {
        materias[materia.nombre] = "Prioridad " + materia.prioridad;
    }
    const datos = new FormData(form);
    const detalles = {
        franjas_horarias: datos.getAll("franja"),
        cercania: Number(datos.get("cercania")),
        volumen_de_compañeros: datos.get("volumen"),
        promedio: Number(datos.get("promedio"))
    };
    const contenido = JSON.stringify({ materias, profesores: profesoresArchivo, detalles }, null, 2);
    form.elements.solicitud_json.value = contenido;

    const url = URL.createObjectURL(new Blob([contenido], { type: "application/json" }));
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = "solicitud-horario.json";
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return true;
}

document.querySelector("#descargar").addEventListener("click", descargarSolicitud);
form.addEventListener("submit", event => {
    if (!descargarSolicitud()) event.preventDefault();
});

// Lectura de profesores: la misma lógica del proyecto original.
document.querySelector("#archivo-profesores").addEventListener("change", async event => {
    const campo = event.target;
    const archivo = campo.files[0];
    profesoresArchivo = [];
    campo.setCustomValidity("");
    leyendoProfesores = true;
    try {
        if (archivo) {
            if (archivo.size > 100000) throw new Error("El archivo supera 100 KB.");
            const texto = (await archivo.text()).replace(/^\uFEFF/, "").trim();
            const nombres = texto.startsWith("[") ? JSON.parse(texto) : texto.split(/\r?\n/).filter(n => n.trim());
            if (!Array.isArray(nombres) || nombres.some(n => typeof n !== "string" || !n.trim()))
                throw new Error("Se requiere una lista de nombres.");
            profesoresArchivo = [...new Set(nombres.map(n => n.trim()))];
        }
        document.querySelector("#estado-profesores").textContent = profesoresArchivo.length + " profesores leídos.";
    } catch (error) {
        campo.setCustomValidity(error.message);
        document.querySelector("#estado-profesores").textContent = error.message;
    } finally {
        leyendoProfesores = false;
        resumen();
    }
});

dibujarMaterias();
