package com.health.project;

import com.health.project.repository.*;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("demo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectApplicationTests {
    @LocalServerPort int port;
    @Autowired EstudianteRepository estudiantes;
    @Autowired MateriaRepository materias;
    @Autowired ClaseRepository clases;

    @Autowired tools.jackson.databind.ObjectMapper json;


    private HttpRequest propuesta(long alumno, long materia, long profesor) throws Exception {
        var url=URI.create("http://localhost:"+port+"/estudiantes/"+alumno+"/horario");
        String datos="materia="+materia+"&prioridad_"+materia+"=Alta&profesor="+profesor
            +"&cercania=0&promedio=3.7&volumen=libre";
        var pagina=HttpClient.newHttpClient().send(HttpRequest.newBuilder(url)
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(datos)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,pagina.statusCode());
        var firma=java.util.regex.Pattern.compile("name=\"firma\" value=\"([^\"]+)").matcher(pagina.body());
        assertTrue(firma.find());
        return HttpRequest.newBuilder(URI.create(url+"/confirmar"))
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("firma="+firma.group(1))).build();
    }


    @Test
    void registroValidaCreaYRechazaCorreoRepetido() throws Exception {
        var cliente = HttpClient.newHttpClient();
        var url = URI.create("http://localhost:"+port+"/estudiantes/nuevo");
        long cantidad = estudiantes.count();
        assertEquals(200,cliente.send(HttpRequest.newBuilder(url).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        String datos = "nombre=Estudiante+Nuevo&correo=nuevo%40example.org&promedio=4.3";
        var peticion = HttpRequest.newBuilder(url).header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(datos)).build();
        var respuesta = cliente.send(peticion,HttpResponse.BodyHandlers.ofString());
        assertEquals(302,respuesta.statusCode());
        assertEquals(cantidad+1,estudiantes.count());
        var nuevo = estudiantes.findAll().stream().filter(e -> e.getCorreo().equals("nuevo@example.org")).findFirst().orElseThrow();
        assertEquals("/estudiantes/"+nuevo.getId()+"/horario",URI.create(respuesta.headers().firstValue("location").orElseThrow()).getPath());
        assertEquals(4.3,nuevo.getPromedio());
        var horario = cliente.send(HttpRequest.newBuilder(url.resolve(respuesta.headers().firstValue("location").orElseThrow())).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,horario.statusCode());
        assertTrue(horario.body().contains("Estudiante Nuevo"));
        var duplicada = cliente.send(peticion,HttpResponse.BodyHandlers.ofString());
        assertEquals(200,duplicada.statusCode());
        assertTrue(duplicada.body().contains("ya está registrado"));
        assertTrue(duplicada.body().contains("Estudiante Nuevo"));
        for (String invalido : java.util.List.of(
                "nombre=+++&correo=otro%40example.org&promedio=4",
                "nombre=Prueba&correo=invalido&promedio=4",
                "nombre=Prueba&correo=otro%40example.org&promedio=6")) {
            var r = cliente.send(HttpRequest.newBuilder(url).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(invalido)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,r.statusCode());
            assertTrue(r.body().contains("field-error"));
        }
        assertEquals(cantidad+1,estudiantes.count());
    }

    @Test
    void cambioFallidoConservaInscripcionAnterior() throws Exception {
        var opciones=clases.findAll();
        var ana=opciones.stream().filter(c -> c.getProfesor().getNombre().equals("Ana Solano")
            && c.getMateria().getNombre().equals("Seminario de Diseño")).findFirst().orElseThrow();
        var leo=opciones.stream().filter(c -> c.getProfesor().getNombre().equals("Leo Duarte")
            && c.getMateria().getNombre().equals("Seminario de Diseño")).findFirst().orElseThrow();
        long alumno=estudiantes.findAll().getFirst().getId();
        var http=HttpClient.newHttpClient();
        var primero=propuesta(alumno,ana.getMateria().getId(),ana.getProfesor().getId());
        assertTrue(http.send(primero,HttpResponse.BodyHandlers.ofString()).body().contains("Horario confirmado."));
        var guardado=estudiantes.findById(alumno).orElseThrow().getHorarioConfirmado();
        var antes=clases.findById(ana.getId()).orElseThrow();
        int ocupacion=antes.getCuposOcupados();
        double suma=antes.getSumaPromedios();
        var cambio=propuesta(alumno,leo.getMateria().getId(),leo.getProfesor().getId());
        leo.setCuposOcupados(leo.getCupos());
        leo.setSumaPromedios(leo.getCupos()*4.0);
        clases.save(leo);
        var respuesta=http.send(cambio,HttpResponse.BodyHandlers.ofString());
        assertTrue(respuesta.body().contains("sin cupos"));
        assertEquals(guardado,estudiantes.findById(alumno).orElseThrow().getHorarioConfirmado());
        var despues=clases.findById(ana.getId()).orElseThrow();
        assertEquals(ocupacion,despues.getCuposOcupados());
        assertEquals(suma,despues.getSumaPromedios(),0.000001);
        var lleno=clases.findById(leo.getId()).orElseThrow();
        assertEquals(lleno.getCupos(),lleno.getCuposOcupados());
        assertEquals(leo.getCupos()*4.0,lleno.getSumaPromedios(),0.000001);
    }

    @Test
    void ultimoCupoSoloSeConfirmaUnaVez() throws Exception {
        var grupo=clases.findAll().stream().filter(c -> c.getMateria().getNombre().equals("Taller de Prototipos")).findFirst().orElseThrow();
        int inicial=grupo.getCuposOcupados();
        double suma=grupo.getSumaPromedios();
        grupo.setCupos(inicial+1);
        clases.save(grupo);
        var alumnos=estudiantes.findAll();
        var cliente=HttpClient.newHttpClient();
        var peticiones=new java.util.ArrayList<HttpRequest>();
        for (var alumno:alumnos.subList(0,2)) {
            var url=URI.create("http://localhost:"+port+"/estudiantes/"+alumno.getId()+"/horario");
            String datos="materia="+grupo.getMateria().getId()+"&prioridad_"+grupo.getMateria().getId()+"=Alta&cercania=0&promedio=3.7&volumen=libre";
            var pagina=cliente.send(HttpRequest.newBuilder(url).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(datos)).build(),HttpResponse.BodyHandlers.ofString());
            var token=java.util.regex.Pattern.compile("name=\"firma\" value=\"([^\"]+)").matcher(pagina.body());
            assertTrue(token.find());
            assertEquals(3.7,estudiantes.findById(alumno.getId()).orElseThrow().getPromedio());
            peticiones.add(HttpRequest.newBuilder(URI.create(url+"/confirmar")).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("firma="+token.group(1))).build());
        }
        var solicitudes=peticiones.stream().map(r -> cliente.sendAsync(r,HttpResponse.BodyHandlers.ofString())).toList();
        var mensajes=solicitudes.stream().map(r -> r.join().body()).toList();
        assertEquals(1,mensajes.stream().filter(m -> m.contains("Horario confirmado.")).count());
        assertEquals(1,mensajes.stream().filter(m -> m.contains("sin cupos")).count());
        int ganador=mensajes.getFirst().contains("Horario confirmado.")?0:1;
        var repetida=cliente.send(peticiones.get(ganador),HttpResponse.BodyHandlers.ofString());
        assertTrue(repetida.body().contains("Horario confirmado."));
        var actualizado=clases.findById(grupo.getId()).orElseThrow();
        assertEquals(inicial+1,actualizado.getCuposOcupados());
        assertEquals(suma+3.7,actualizado.getSumaPromedios(),0.00001);
    }

    @Test
    void empateConservaOrdenDeEntradaEnJson() throws Exception {
        var estudiante=estudiantes.findAll().getFirst();
        var url=URI.create("http://localhost:"+port+"/estudiantes/"+estudiante.getId()+"/horario");
        for (boolean invertir : java.util.List.of(false,true)) {
            String primera=invertir ? "Laboratorio de Prototipos" : "Taller de Prototipos";
            String segunda=invertir ? "Taller de Prototipos" : "Laboratorio de Prototipos";
            var orden=new java.util.LinkedHashMap<String,String>();
            orden.put(primera,"Prioridad Alta");
            orden.put(segunda,"Prioridad Alta");
            var cuerpo=json.writeValueAsString(java.util.Map.of("materias",orden,"detalles",java.util.Map.of("promedio",3)));
            var r=HttpClient.newHttpClient().send(HttpRequest.newBuilder(url).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,r.statusCode());
            var salida=json.readTree(r.body());
            assertEquals(segunda,salida.get("no_asignadas").get(0).get("materia").asString());
            var sesion=salida.get("horario").get("Miércoles").get(0);
            assertEquals(primera,sesion.get("materia").asString());
            assertTrue(sesion.get("lugar").has("id") && sesion.get("profesor").has("id"));
            assertEquals("07:00",sesion.get("horario").get("hora_inicio").asString());
        }
    }

    @Test
    void generaYPersisteHorarioSinConsumirCupos() throws Exception {
        var estudiante = estudiantes.findAll().getFirst();
        var todas = materias.findAll();
        var estructuras = todas.stream().filter(m -> m.getNombre().equals("Estructuras de Datos")).findFirst().orElseThrow();
        var matematicas = todas.stream().filter(m -> m.getNombre().contains("Discretas")).findFirst().orElseThrow();
        var web = todas.stream().filter(m -> m.getNombre().equals("Desarrollo Web")).findFirst().orElseThrow();
        var ocupacion = clases.findAll().stream().map(c -> c.getCuposOcupados()).toList();
        var url = URI.create("http://localhost:"+port+"/estudiantes/"+estudiante.getId()+"/horario");
        var cliente = HttpClient.newHttpClient();
        var formulario = cliente.send(HttpRequest.newBuilder(url).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,formulario.statusCode());
        String datos = "promedio=4.2&cercania=0&volumen=libre&franja=tarde";
        for (var materia : java.util.List.of(estructuras,matematicas,web))
            datos += "&materia="+materia.getId()+"&prioridad_"+materia.getId()+"=Alta";
        var respuesta = cliente.send(HttpRequest.newBuilder(url).header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(datos)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,respuesta.statusCode());
        assertTrue(respuesta.body().contains("sin grupos publicados"));
        assertTrue(respuesta.body().contains("Fuera de la franja preferida"));
        String guardado = estudiantes.findById(estudiante.getId()).orElseThrow().getHorario();
        assertTrue(guardado.contains("Matemáticas Discretas"));
        assertTrue(guardado.contains("Estructuras de Datos"));
        assertTrue(guardado.contains("Martes"));
        assertEquals(ocupacion,clases.findAll().stream().map(c -> c.getCuposOcupados()).toList());
    }
}
