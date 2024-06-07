package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner sc = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=ea5fb936";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie> serieBuscada;
    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }
    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por titulo
                    5 - Top 5 mejores series
                    6 - Buscar series por categoria
                    7 - Buscar serie por numero de temporada y evaluacion
                    8 - Buscar episodio por titulo
                    9 - Top 5 mejores episodios de una serie
                    77 - Buscar serie por temporada y evaluacion (prueba)
                                  
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    buscarTop5Series();
                    break;
                case 6:
                    buscarSeriePorCategoria();
                    break;
                case 7:
                    buscarPorTemporadasYEvaluacion();
                    break;
                case 8:
                    buscarEpisodioPorTitulo();
                    break;
                case 9:
                    buscarTop5Episodios();
                    break;
                case 77:
                    buscarPorTemporadasYEvaluacionDos();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }
    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = sc.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la serie de la cual quieres ver los episodios");
        var nombreSerie = sc.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if(serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }
    }
    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        //datosSeries.add(datos);
        System.out.println(datos);
    }
    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }
    private void buscarSeriesPorTitulo() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = sc.nextLine();
        serieBuscada = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if(serieBuscada.isPresent()){
            System.out.println("La serie buscada es: " + serieBuscada.get());
        } else {
            System.out.println("Serie no encontrada");
        }
    }
    private void buscarTop5Series() {
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s -> System.out.println("Serie: " + s.getTitulo()
                + " Evaluacion: " + s.getEvaluacion()));
    }
    private void buscarSeriePorCategoria() {
        System.out.println("Escriba el genero/categoria de la serie que desea buscar");
        var genero = sc.nextLine();
        var categoria = Categoria.fromEspañol(genero);
        List<Serie> seriePorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Las series de la categoria " + genero);
        seriePorCategoria.forEach(System.out::println);
    }
    private void buscarPorTemporadasYEvaluacion() {
        System.out.println("Escriba el numero de temporadas de las serie que desea buscar");
        var numeroTemporadas = sc.nextInt();
        System.out.println("Escriba la evaluacion de la serie que desea buscar");
        var evaluacionSerie = sc.nextDouble();
        List<Serie> seriePorTemparadasYEvaluacion = repositorio
                .findByTotalTemporadasLessThanEqualAndEvaluacionGreaterThanEqual(numeroTemporadas, evaluacionSerie);
        seriePorTemparadasYEvaluacion.forEach(System.out::println);
    }

    private void buscarPorTemporadasYEvaluacionDos(){
        System.out.println("Escriba el numero de temporadas de las serie que desea buscar");
        var numeroTemporadas = sc.nextInt();
        System.out.println("Escriba la evaluacion de la serie que desea buscar");
        var evaluacionSerie = sc.nextDouble();
        List<Serie> filtroSerie = repositorio.seriesPorTemporadaYEvaluacion(numeroTemporadas, evaluacionSerie);
        System.out.println("-- Series encontradas --");
        filtroSerie.forEach(s -> System.out.println("Serie: " + s.getTitulo()
                + " - " + "Evaluacion: " + s.getEvaluacion()
                + " - " + "Temporadas: " + s.getTotalTemporadas()));
    }
    private void buscarEpisodioPorTitulo() {
        System.out.println("Escribe el nombre del episodio que deseas buscar");
        var nombreEpisodio = sc.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodioPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e -> System.out.println("Serie: " + e.getSerie().getTitulo()
                + " - " + "Temporada: " + e.getTemporada()
                + " - " + "Episodios: " + e.getNumeroEpisodio()
                + " - " + "Evaluacion: " + e.getEvaluacion()));
    }
    private void buscarTop5Episodios() {
        buscarSeriesPorTitulo();
        if(serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repositorio.top5Episodios(serie);
            topEpisodios.forEach(e -> System.out.println("Serie: " + e.getSerie().getTitulo()
                    + " - " + "Temporada: " + e.getTemporada()
                    + " - " + "Episodios: " + e.getNumeroEpisodio()
                    + " - " + "Evaluacion: " + e.getEvaluacion()
                    + " - " + "Episodio: " + e.getTitulo()));
        }
    }
}

