let profesoresArchivo = [];
let leyendoProfesores = false;
const form = document.querySelector("form");
const seleccion = new Set();
function resumen() {
    const lista = document.querySelector("#resumen");
    lista.replaceChildren();
    const campos = new FormData(form);
    document.querySelector("#resumen-preferencias").textContent =
        "Franjas: "+(campos.getAll("franja").join(", ") || "todas")+
        ". Profesores: "+(profesoresArchivo.join(", ") || "sin preferencia")+
        ". Volumen: "+(campos.get("volumen") || "pendiente")+
        ". Cercanía: "+campos.get("cercania")+". Promedio: "+campos.get("promedio");
    for (const id of seleccion) {
        const fila = form.querySelector('input[name="materia"][value="'+id+'"]').closest("div");
        const item = document.createElement("li");
        item.textContent = fila.querySelector("span").textContent+" · "+fila.querySelector("select").value;
        lista.append(item);
    }
}
form.addEventListener("change", event => {
    const campo = event.target;
    if (campo.name === "materia") {
        if (campo.checked) seleccion.add(campo.value);
        else seleccion.delete(campo.value);
    }
    resumen();
});
for (const campo of form.querySelectorAll('input[name="materia"]:checked')) seleccion.add(campo.value);
resumen();
form.addEventListener("formdata", event => {
    event.formData.delete("materia");
    for (const id of seleccion) event.formData.append("materia", id);
});

function descargarSolicitud() {
    if (leyendoProfesores) return false;
    if (!form.reportValidity() || !seleccion.size) {
        if (!seleccion.size) alert("Selecciona al menos una materia.");
        return false;
    }
    const materias = {};
    for (const id of seleccion) {
        const fila = form.querySelector('input[name="materia"][value="'+id+'"]').closest("div");
        materias[fila.querySelector("span").textContent] = "Prioridad "+fila.querySelector("select").value;
    }
    const profesores = profesoresArchivo;
    const datos = new FormData(form);
    const detalles = {franjas_horarias: datos.getAll("franja"), cercania: Number(datos.get("cercania")),
        volumen_de_compañeros: datos.get("volumen"), promedio: Number(datos.get("promedio"))};
    const contenido = JSON.stringify({materias, profesores, detalles}, null, 2);
    form.elements.solicitud_json.value = contenido;
    const url = URL.createObjectURL(new Blob([contenido], {type:"application/json"}));
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
        document.querySelector("#estado-profesores").textContent = profesoresArchivo.length+" profesores leídos.";
    } catch (error) {
        campo.setCustomValidity(error.message);
        document.querySelector("#estado-profesores").textContent = error.message;
    } finally {
        leyendoProfesores = false;
        resumen();
    }
});
