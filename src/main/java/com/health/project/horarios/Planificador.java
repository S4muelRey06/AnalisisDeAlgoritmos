package com.health.project.horarios;

import com.health.project.entitys.*;
import java.util.*;

/**
 * Programación dinámica por etapas (una etapa = una materia, en orden de
 * prioridad).
 *
 * Tabla: para cada etapa se guarda un horario parcial por cada "llave" distinta
 * (horas ocupadas + edificio menor + edificio mayor). Si dos caminos llegan a
 * la misma llave,
 * se conserva el de mejor puntaje. Costo: O(n * S * k), con S = llaves
 * distintas por etapa.
 */

/*
 * 1. Se crea una máscara de bits de 7*15 franjas. 1 es ocupada y 0 es
 * desocupada
 * 2. La llave es la estructura que almacena en nuestra programación dinámica.
 * Tiene las ocupadas, el numero de edificio más pequeño y el numero de edificio
 * más grandes
 * - Esa información es suficiente para seguir evaluando el algoritmo. No
 * necesitamos ver todas las clases inscritas

*/
class Planificador {
    private static final List<String> DIAS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado");
    static final int HORA_MIN = 7; // primera hora de clase
    static final int HORAS_DIA = 15; // 7:00 a 22:00 -> 15 horas por día (6 días x 15 = 90 bits)

    /**
     * Estado: horas ocupadas de la semana + rango de edificios usados (para la
     * cercanía).
     */
    record Llave(BitSet ocupadas, long edMin, long edMax) {
    }
/* 
     * 3. Tenemos un sistema de puntaje que evalúa "que tan buena" es la
 * configuración del horario
 * - La idea es comparar entre potenciales soluciones y se escoge la de mayor
 * puntaje
 * - Se inicializa en 0 cuando no hay materias de nada
 * - Cada que se asigna una materia sobre el horario se recalcula el puntaje
 * - La comparación del puntaje se hace atributo por atributo. Se mira primero
 * si asignó una materia más.
 * - Si ambas soluciones añaden una, se pasa al siguiente criterio, y así
 * desempatando hasta el ultimo criterio
 */
    static final class Puntaje implements Comparable<Puntaje> {
        final int asignadas; // 1. cuántas materias
        final long prioridad; // 2. cuáles (la de mayor prioridad pesa más)
        final int franja, volumen, profesor, cercania, promedio; // 3 a 7

        Puntaje(int asignadas, long prioridad, int franja, int volumen, int profesor, int cercania, int promedio) {
            this.asignadas = asignadas;
            this.prioridad = prioridad;
            this.franja = franja;
            this.volumen = volumen;
            this.profesor = profesor;
            this.cercania = cercania;
            this.promedio = promedio;
        }

        static final Puntaje CERO = new Puntaje(0, 0, 0, 0, 0, 0, 0);

        Puntaje sumar(long peso, int fr, int vo, int pr, int ce, int pm) {
            return new Puntaje(asignadas + 1, prioridad + peso, franja + fr, volumen + vo, profesor + pr, cercania + ce,
                    promedio + pm);
        }

        @Override
        public int compareTo(Puntaje o) {
            int r = Integer.compare(asignadas, o.asignadas);
            if (r != 0)
                return r;
            r = Long.compare(prioridad, o.prioridad);
            if (r != 0)
                return r;
            r = Integer.compare(franja, o.franja);
            if (r != 0)
                return r;
            r = Integer.compare(volumen, o.volumen);
            if (r != 0)
                return r;
            r = Integer.compare(profesor, o.profesor);
            if (r != 0)
                return r;
            r = Integer.compare(cercania, o.cercania);
            if (r != 0)
                return r;
            return Integer.compare(promedio, o.promedio);
        }
    }

    /*
     * 
     * 4.Se crea una estructura para guardar soluciones parciales, es la de entrada
     * - Guarda la materia hasta la que ha procesado, la estructura hasta el
     * momento, la ultima clase escogida, y la entrada anterior)
     * 
     */
    static final class Entrada {
        final int etapa;
        final Llave llave;
        final Puntaje puntaje;
        final Clase clase;
        final Entrada padre;

        Entrada(int etapa, Llave llave, Puntaje puntaje, Clase clase, Entrada padre) {
            this.etapa = etapa;
            this.llave = llave;
            this.puntaje = puntaje;
            this.clase = clase;
            this.padre = padre;
        }
    }
     /* 
 * 5. Se pasa del horario a la máscara de bits que creamos en el paso 1
    */

    static BitSet mascara(Clase c) {
        BitSet m = new BitSet(DIAS.size() * HORAS_DIA);
        for (ClaseHorario enlace : c.getClaseHorarios()) {
            Horario h = enlace.getHorario();
            int dia = DIAS.indexOf(h.getDia());
            int ini = h.getHoraInicio().getHour();
            int fin = h.getHoraFin().getHour() + (h.getHoraFin().getMinute() > 0 ? 1 : 0);
            if (dia < 0 || ini < HORA_MIN || fin > HORA_MIN + HORAS_DIA || ini >= fin)
                return null;
            m.set(dia * HORAS_DIA + (ini - HORA_MIN), dia * HORAS_DIA + (fin - HORA_MIN));
        }
        return m;
    }

    /**
     * @param candidatas candidatas[i] = clases de la materia i (ya sin las que no
     *                   tienen cupo ni horario),
     *                   en el orden de prioridad de las materias; las clases de
     *                   cada materia deben venir
     *                   ordenadas por id (así los empates se resuelven siempre
     *                   igual).
     * @return lista de tamaño n: la clase escogida para cada materia, o null si
     *         quedó sin asignar.
     */
/*
    Aqui es el algoritmo oficial, usando todo lo anterior.

    1. Se mira cuantas materias o etapas hay que hacer.
       Cada materia representa una etapa del algoritmo.

    2. Se crea una estructura preliminar de bits, recorriendo
       todas las clases candidatas de cada materia.
       A cada clase se le calcula su mascara de bits, que
       representa las horas que ocupa durante la semana.
       Estas mascaras se guardan en un mapa para no tener
       que calcularlas repetidamente.

    3. Se inicializa la estructura para programacion dinamica,
       que guarda la relacion entre una estructura Llave
       (estado del horario) y una estructura Entrada
       (solucion parcial que llega a ese estado).

    4. Se crea el estado inicial, con todas las horas libres,
       sin edificios seleccionados y con un puntaje de cero.

    5. Se hace una iteracion por cada materia, es decir,
       se procesa una etapa a la vez.

    6. Se calcula el peso de prioridad moviendo los bits.
       La prioridad va disminuyendo conforme avanzan las
       materias, dado que estas entran ordenadas por prioridad.
       Ese valor se utilizara para calcular el puntaje acumulado.

    7. Se construye una nueva estructura de programacion
       dinamica, llamada siguiente, donde se guardaran los
       estados que se generen al procesar la materia actual.
       Se utiliza una estructura diferente para no modificar
       los estados de la etapa anterior.

    8. Se recorren todos los estados existentes en la etapa
       anterior. Para cada uno se prueban dos posibilidades:

       - Opcion 1: dejar la materia sin asignar. Se conserva
         el mismo estado y el mismo puntaje, pero se avanza
         a la siguiente etapa.

       - Opcion 2: seleccionar una de las clases candidatas
         de la materia actual.

    9. Para cada clase candidata se recupera su mascara de
       bits y se comprueba si tiene algun cruce con las horas
       que ya estan ocupadas en el estado actual.

       Si la mascara es nula o existen cruces, se descarta
       esa clase y se prueba la siguiente.

    10. Si no existen cruces, se crea una copia de las horas
        ocupadas anteriormente y se agregan las horas de la
        nueva clase mediante la operacion OR entre bits.

        Asi se obtiene la nueva distribucion de horas ocupadas
        sin modificar el estado anterior.

    11. Se actualizan los edificios minimo y maximo,
        incorporando el edificio de la nueva clase.
        Con estos valores se calcula si la combinacion
        cumple la preferencia de cercania.

    12. Se calcula el nuevo puntaje acumulado, sumando:
        - Una materia asignada.
        - El peso de prioridad de esa materia.
        - Un punto si cumple la franja horaria.
        - Un punto si cumple el volumen de compañeros.
        - Un punto si el profesor es preferido.
        - Un punto si cumple la cercania.
        - Un punto si cumple el promedio solicitado.

    13. Se crea una nueva Entrada con:
        - La etapa actual.
        - La nueva Llave, con las horas y edificios actualizados.
        - El nuevo puntaje.
        - La clase que se acaba de seleccionar.
        - Una referencia al estado anterior (padre).

    14. Se utiliza ofrecer() para guardar la nueva posibilidad
        en la tabla siguiente.

        Si la llave no existe, se agrega el nuevo estado.
        Si ya existe, se compara el puntaje de ambas entradas
        y se conserva unicamente la que tenga mejor puntaje.

        De esta manera se eliminan caminos equivalentes que
        no aportan una mejor solucion.

    15. Cuando se terminan de evaluar todos los estados y
        todas las clases candidatas de la materia actual,
        la estructura siguiente reemplaza a capa.

        Asi comienza la siguiente etapa utilizando los estados
        generados en la etapa anterior.

    16. Cuando se terminan de procesar todas las materias,
        se recorren los estados finales y se selecciona la
        Entrada con el mejor puntaje, utilizando compareTo().

    17. Se crea una lista de resultados con una posicion
        por cada materia, inicialmente todas en null.

    18. Se reconstruye la solucion final recorriendo las
        referencias padre desde la mejor Entrada hasta el
        estado inicial.

        En cada etapa se guarda la clase seleccionada en
        la posicion correspondiente de la lista.
        Si una materia no fue asignada, su valor permanece
        en null.

    19. Finalmente se devuelve la lista con los grupos
        seleccionados para cada materia, respetando el
        orden original de las materias.
*/
    static List<Clase> resolver(List<List<Clase>> candidatas, GeneradorHorario.Preferencias p, GeneradorHorario g) {
        int n = candidatas.size();
        Map<Clase, BitSet> mascaras = new IdentityHashMap<>();
        for (List<Clase> lista : candidatas)
            for (Clase c : lista)
                mascaras.put(c, mascara(c));

        Map<Llave, Entrada> capa = new LinkedHashMap<>(); //TABLA DE PROGRAMACIÓN DINÁMICA
        Llave vacia = new Llave(new BitSet(), Long.MAX_VALUE, Long.MIN_VALUE);
        capa.put(vacia, new Entrada(-1, vacia, Puntaje.CERO, null, null));

        for (int i = 0; i < n; i++) {
            long peso = 1L << Math.max(0, Math.min(62, n - 1 - i));
            Map<Llave, Entrada> siguiente = new LinkedHashMap<>();
            for (Entrada e : capa.values()) {
                // Opción 1: dejar la materia sin asignar
                ofrecer(siguiente, new Entrada(i, e.llave, e.puntaje, null, e));
                // Opción 2: una de sus clases, si no choca con las horas ya ocupadas (Nivel 2)
                for (Clase c : candidatas.get(i)) {
                    BitSet m = mascaras.get(c);
                    if (m == null || m.intersects(e.llave.ocupadas()))
                        continue;
                    BitSet ocupadas = (BitSet) e.llave.ocupadas().clone();
                    ocupadas.or(m);
                    long edificio = c.getLugar().getNumeroEdificio();
                    long min = Math.min(e.llave.edMin(), edificio), max = Math.max(e.llave.edMax(), edificio);
                    boolean cerca = p.cercania() == 0 || (max - min) <= (p.cercania() == 1 ? 2 : 0);
                    Puntaje pt = e.puntaje.sumar(peso,
                            g.enFranja(c, p) ? 1 : 0,
                            g.volumenPreferido(c, p.volumen()) ? 1 : 0,
                            p.profesores().contains(c.getProfesor().getId()) ? 1 : 0,
                            cerca ? 1 : 0,
                            g.afin(c, p) ? 1 : 0);
                    ofrecer(siguiente, new Entrada(i, new Llave(ocupadas, min, max), pt, c, e));
                }
            }
            capa = siguiente;
        }

        Entrada mejor = null;
        for (Entrada e : capa.values())
            if (mejor == null || e.puntaje.compareTo(mejor.puntaje) > 0)
                mejor = e;

        List<Clase> resultado = new ArrayList<>(Collections.nCopies(n, (Clase) null));
        for (Entrada e = mejor; e != null && e.etapa >= 0; e = e.padre)
            resultado.set(e.etapa, e.clase);
        return resultado;
    }

   /*
    Esta funcion se encarga de administrar los estados de
    la tabla de programacion dinamica.

    1. Recibe la tabla de estados de la etapa actual y una
       nueva Entrada que se acaba de generar.

    2. Busca en la tabla si ya existe una Entrada con la
       misma Llave que la nueva Entrada.

    3. Si la llave no existe:
       - Se agrega la nueva Entrada a la tabla.
       - Esto significa que encontramos un estado que
         todavia no habia sido registrado.

    4. Si la llave ya existe:
       - Se comparan los puntajes de la nueva Entrada y
         de la Entrada que ya estaba almacenada.
       - Se utiliza compareTo() para determinar cual
         de las dos soluciones parciales es mejor.

    5. Si la nueva Entrada tiene un puntaje estrictamente
       superior, reemplaza a la anterior.

    6. Si tiene un puntaje inferior o igual, se conserva
       la Entrada que ya estaba almacenada.

    7. Esto es posible porque ambas Entradas llegan al
       mismo estado: tienen las mismas horas ocupadas
       y el mismo rango de edificios.

       Por tanto, tienen las mismas posibilidades de
       continuar con las materias siguientes.

    8. Se conserva solamente la mejor solucion parcial,
       evitando seguir trabajando con caminos equivalentes
       que no pueden producir un resultado superior.

    Esta funcion es fundamental para la programacion
    dinamica, porque permite reutilizar la mejor solucion
    encontrada para cada estado y reducir la cantidad
    de combinaciones que deben seguir explorandose.
*/
    private static void ofrecer(Map<Llave, Entrada> capa, Entrada nueva) {
        Entrada actual = capa.get(nueva.llave);
        if (actual == null || nueva.puntaje.compareTo(actual.puntaje) > 0)
            capa.put(nueva.llave, nueva);
    }
}