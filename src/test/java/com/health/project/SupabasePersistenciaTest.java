package com.health.project;

import com.health.project.entitys.Estudiante;
import com.health.project.repository.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="ADA_VERIFICAR_SUPABASE", matches="true")
class SupabasePersistenciaTest {
    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(ProjectApplication.class).run("--server.address=127.0.0.1","--server.port=0",
            "--spring.jpa.hibernate.ddl-auto=none","--spring.jpa.show-sql=false","--logging.level.com.zaxxer.hikari=INFO",
            "--logging.level.com.zaxxer.hikari.HikariConfig=INFO");
    }
    private URI ruta(ConfigurableApplicationContext ctx, long id, String sufijo) {
        int port=((WebServerApplicationContext)ctx).getWebServer().getPort();
        return URI.create("http://127.0.0.1:"+port+"/estudiantes/"+id+"/horario"+sufijo);
    }

    @Test
    void recuperaDespuesDeReiniciar() throws Exception {
        Long id=null;
        String correo="prueba-"+UUID.randomUUID()+"@example.org";
        String generado;
        var http=HttpClient.newHttpClient();
        try {
            try (var ctx=iniciar()) {
                var repo=ctx.getBean(EstudianteRepository.class);
                var catalogo=ctx.getBean(MateriaRepository.class).findAll();
                assertFalse(catalogo.isEmpty(),"El catálogo remoto está vacío");
                id=repo.save(new Estudiante(correo,"Prueba temporal ADA",4.0)).getId();
                var json=ctx.getBean(ObjectMapper.class);
                var solicitud=Map.of("materias",Map.of(catalogo.getFirst().getNombre(),"Alta"),
                    "detalles",Map.of("cercania",0,"promedio",4.0));
                var r=http.send(HttpRequest.newBuilder(ruta(ctx,id,"")).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(solicitud))).build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,r.statusCode());
                generado=r.body();
                assertTrue(json.readTree(generado).has("horario"));
            }
            try (var ctx=iniciar()) {
                var r=http.send(HttpRequest.newBuilder(ruta(ctx,id,"/guardado")).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,r.statusCode());
                assertEquals(ctx.getBean(ObjectMapper.class).readTree(generado),ctx.getBean(ObjectMapper.class).readTree(r.body()));
            }
        } finally {
            if (id!=null) try (var ctx=iniciar()) {
                var repo=ctx.getBean(EstudianteRepository.class);
                repo.findById(id).filter(e -> correo.equals(e.getCorreo())).ifPresent(repo::delete);
            }
        }
    }

    @Test
    void confirmaEnPostgresSinDuplicarYConservaTrasReinicio() throws Exception {
        Long id = null;
        Long grupoId = null;
        int ocupados = 0;
        double suma = 0;
        String correo = "confirmacion-" + UUID.randomUUID() + "@example.org";
        var http = HttpClient.newHttpClient();
        try {
            try (var ctx = iniciar()) {
                var alumnos = ctx.getBean(EstudianteRepository.class);
                var grupos = ctx.getBean(ClaseRepository.class);
                var candidato = grupos.findAll().stream()
                    .filter(c -> c.getCuposOcupados() < c.getCupos()).findFirst().orElseThrow();
                id = alumnos.save(new Estudiante(correo, "Prueba confirmacion PostgreSQL", 3.7)).getId();
                String cuerpo = "materia=" + candidato.getMateria().getId()
                    + "&prioridad_" + candidato.getMateria().getId() + "=Alta&cercania=0&promedio=3.7&volumen=libre";
                var pagina = http.send(HttpRequest.newBuilder(ruta(ctx,id,""))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200,pagina.statusCode());
                var alumno = alumnos.findById(id).orElseThrow();
                var json = ctx.getBean(ObjectMapper.class);
                for (var dia : json.readTree(alumno.getHorario()).get("horario").properties())
                    for (var sesion : dia.getValue()) grupoId = sesion.get("numero_de_clase").asLong();
                assertNotNull(grupoId,"Se necesita un grupo disponible con sesiones");
                var grupo = grupos.findById(grupoId).orElseThrow();
                ocupados = grupo.getCuposOcupados();
                suma = grupo.getSumaPromedios();
                System.out.println("PRUEBA POSTGRES grupo " + grupoId + " ANTES: " + ocupados + " / " + suma);
                var token = java.util.regex.Pattern.compile("name=\"firma\" value=\"([^\"]+)").matcher(pagina.body());
                assertTrue(token.find());
                var confirmar = HttpRequest.newBuilder(ruta(ctx,id,"/confirmar"))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("firma=" + token.group(1))).build();
                for (int intento=1; intento<=2; intento++) {
                    var respuesta = http.send(confirmar,HttpResponse.BodyHandlers.ofString());
                    assertEquals(200,respuesta.statusCode());
                    assertTrue(respuesta.body().contains("Horario confirmado."));
                    var actualizado = grupos.findById(grupoId).orElseThrow();
                    assertEquals(ocupados+1,actualizado.getCuposOcupados());
                    assertEquals(suma+3.7,actualizado.getSumaPromedios(),0.000001);
                    System.out.println("CONFIRMACION " + intento + ": " + actualizado.getCuposOcupados() + " / " + actualizado.getSumaPromedios());
                }
            }
            try (var ctx = iniciar()) {
                var alumno = ctx.getBean(EstudianteRepository.class).findById(id).orElseThrow();
                assertEquals(3.7,alumno.getPromedioConfirmado());
                assertEquals(alumno.getHorario(),alumno.getHorarioConfirmado());
                var grupo = ctx.getBean(ClaseRepository.class).findById(grupoId).orElseThrow();
                assertEquals(ocupados+1,grupo.getCuposOcupados());
                assertEquals(suma+3.7,grupo.getSumaPromedios(),0.000001);
                System.out.println("REINICIO: inscripcion y contadores conservados.");
            }
        } finally {
            if (id != null) try (var ctx = iniciar()) {
                final long alumnoId = id;
                new org.springframework.transaction.support.TransactionTemplate(
                    ctx.getBean(org.springframework.transaction.PlatformTransactionManager.class)).executeWithoutResult(tx -> {
                    var alumnos = ctx.getBean(EstudianteRepository.class);
                    var alumno = alumnos.bloquear(alumnoId).orElseThrow();
                    assertEquals(correo,alumno.getCorreo());
                    if (alumno.getHorarioConfirmado() != null) {
                        Set<Long> ids = new TreeSet<>();
                        var json = ctx.getBean(ObjectMapper.class);
                        for (var dia : json.readTree(alumno.getHorarioConfirmado()).get("horario").properties())
                            for (var sesion : dia.getValue()) ids.add(sesion.get("numero_de_clase").asLong());
                        for (long clave : ids) {
                            var grupo = ctx.getBean(ClaseRepository.class).bloquear(clave).orElseThrow();
                            grupo.setCuposOcupados(grupo.getCuposOcupados()-1);
                            grupo.setSumaPromedios(grupo.getSumaPromedios()-alumno.getPromedioConfirmado());
                        }
                    }
                    alumnos.delete(alumno);
                });
                System.out.println("LIMPIEZA: retirado el aporte de prueba y eliminado el estudiante temporal.");
            }
        }
    }

}
