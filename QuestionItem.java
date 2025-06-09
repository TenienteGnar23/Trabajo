import java.util.List;

public class QuestionItem {
    private int id;
    private String enunciado;
    private String nivel;       // Recordar, Entender, Aplicar, Analizar, Evaluar, Crear
    private String tipo;        // "Multiple" o "VF"
    private int tiempo;         // en segundos
    private String correcta;    // texto exacto (o "V"/"F")
    private List<String> opciones;

    public QuestionItem(int id,
                        String enunciado,
                        String nivel,
                        String tipo,
                        int tiempo,
                        String correcta,
                        List<String> opciones) {
        this.id = id;
        this.enunciado = enunciado;
        this.nivel = nivel;
        this.tipo = tipo;
        this.tiempo = tiempo;
        this.correcta = correcta.trim();
        this.opciones = opciones;
    }

    public int getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public String getNivel() { return nivel; }
    public String getTipo() { return tipo; }
    public int getTiempo() { return tiempo; }
    public String getCorrecta() { return correcta; }
    public List<String> getOpciones() { return opciones; }
}