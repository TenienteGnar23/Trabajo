import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.List;

public class MainApp {
    private static List<QuestionItem> items = new ArrayList<>();
    private static Map<Integer, String> respuestas = new HashMap<>();
    private static int currentIndex = 0;
    private static boolean reviewMode = false;

    private static JFrame frame;
    private static JLabel lblPregunta;
    private static JPanel panelOpciones;
    private static JLabel lblFeedback;
    private static JButton btnPrev, btnNext, btnCargar, btnReview;
    private static ButtonGroup grupoOpciones;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::crearGUI);
    }

    private static void crearGUI() {
        frame = new JFrame("Prueba Taxonomía de Bloom");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        frame.setContentPane(content);

        // Top: Load button
        btnCargar = new JButton("Cargar archivo CSV");
        btnCargar.addActionListener(e -> cargarCSV());
        content.add(btnCargar, BorderLayout.NORTH);

        // Center: Question and options
        lblPregunta = new JLabel(" ");
        lblPregunta.setFont(lblPregunta.getFont().deriveFont(16f));
        lblPregunta.setVerticalAlignment(SwingConstants.TOP);

        panelOpciones = new JPanel();
        panelOpciones.setLayout(new BoxLayout(panelOpciones, BoxLayout.Y_AXIS));

        lblFeedback = new JLabel();
        lblFeedback.setFont(lblFeedback.getFont().deriveFont(14f));
        lblFeedback.setVisible(false);

        JPanel centro = new JPanel(new BorderLayout(5, 5));
        centro.add(lblPregunta, BorderLayout.NORTH);
        centro.add(panelOpciones, BorderLayout.CENTER);
        centro.add(lblFeedback, BorderLayout.SOUTH);
        content.add(centro, BorderLayout.CENTER);

        // Bottom: navigation buttons
        btnPrev = new JButton("Atrás");
        btnPrev.setEnabled(false);
        btnPrev.addActionListener(e -> {
            if (reviewMode) mostrarAnteriorReview(); else mostrarAnterior();
        });

        btnNext = new JButton("Siguiente");
        btnNext.setEnabled(false);
        btnNext.addActionListener(e -> {
            if (reviewMode) mostrarSiguienteReview(); else mostrarSiguiente();
        });

        btnReview = new JButton("Revisar respuestas");
        btnReview.setVisible(false);
        btnReview.addActionListener(e -> startReview());

        JPanel sur = new JPanel();
        sur.add(btnPrev);
        sur.add(btnNext);
        sur.add(btnReview);
        content.add(sur, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void cargarCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            items.clear(); respuestas.clear(); currentIndex = 0; reviewMode = false;
            br.readLine(); // header
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] tok = linea.split(",", -1);
                if (tok.length < 6) continue;
                String idStr = tok[0].replaceAll("^\"|\"$", "").trim();
                String enun = tok[1].replaceAll("^\"|\"$", "").trim();
                String niv = tok[2].replaceAll("^\"|\"$", "").trim();
                String tip = tok[3].replaceAll("^\"|\"$", "").trim();
                String tiemStr = tok[4].replaceAll("^\"|\"$", "").trim();
                String corr = tok[5].replaceAll("^\"|\"$", "").trim();
                int id = Integer.parseInt(idStr);
                int tiem = Integer.parseInt(tiemStr);
                List<String> opts = new ArrayList<>();
                if (tip.toLowerCase().startsWith("mul")) {
                    for (int i = 6; i < tok.length; i++) {
                        String o = tok[i].replaceAll("^\"|\"$", "").trim();
                        if (!o.isEmpty()) opts.add(o);
                    }
                }
                items.add(new QuestionItem(id, enun, niv, tip, tiem, corr, opts));
            }
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No hay preguntas válidas", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            btnCargar.setEnabled(false);
            btnNext.setEnabled(true);
            btnPrev.setEnabled(false);
            btnReview.setVisible(false);
            mostrarPreguntaActual();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error al leer CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void mostrarPreguntaActual() {
        panelOpciones.removeAll(); grupoOpciones = new ButtonGroup(); lblFeedback.setVisible(false);
        QuestionItem q = items.get(currentIndex);
        lblPregunta.setText(String.format("<html><body style='width:500px'><b>[%d/%d]</b> %s</body></html>", currentIndex+1, items.size(), q.getEnunciado()));
        if (q.getTipo().toLowerCase().startsWith("mul")) {
            for (String opt : q.getOpciones()) {
                JRadioButton rb = new JRadioButton(opt); grupoOpciones.add(rb); panelOpciones.add(rb);
                if (opt.equalsIgnoreCase(respuestas.get(q.getId()))) rb.setSelected(true);
            }
        } else {
            JRadioButton rbV = new JRadioButton("Verdadero"); JRadioButton rbF = new JRadioButton("Falso");
            grupoOpciones.add(rbV); grupoOpciones.add(rbF); panelOpciones.add(rbV); panelOpciones.add(rbF);
            String prev = respuestas.get(q.getId()); if ("V".equalsIgnoreCase(prev)) rbV.setSelected(true);
            if ("F".equalsIgnoreCase(prev)) rbF.setSelected(true);
        }
        btnPrev.setEnabled(currentIndex>0);
        btnNext.setText(currentIndex==items.size()-1 ? "Enviar respuestas" : "Siguiente");
        panelOpciones.revalidate(); panelOpciones.repaint();
    }

    private static void mostrarSiguiente() {
        guardarRespuesta();
        if (currentIndex==items.size()-1) { mostrarResumenPanel(); return; }
        currentIndex++; mostrarPreguntaActual();
    }
    private static void mostrarAnterior() {
        guardarRespuesta();
        if (currentIndex>0) { currentIndex--; mostrarPreguntaActual(); }
    }

    private static void guardarRespuesta() {
        QuestionItem q = items.get(currentIndex); String sel="";
        for (Enumeration<AbstractButton> it=grupoOpciones.getElements();it.hasMoreElements();) {
            JRadioButton rb=(JRadioButton)it.nextElement(); if(rb.isSelected()) sel=rb.getText();
        }
        if(sel.equalsIgnoreCase("Verdadero")) sel="V"; else if(sel.equalsIgnoreCase("Falso")) sel="F";
        respuestas.put(q.getId(), sel);
    }

    private static void mostrarResumenPanel() {
        btnPrev.setEnabled(false); btnNext.setEnabled(false); btnReview.setVisible(true);
        StringBuilder sb=new StringBuilder(); sb.append("<html><body style='width:400px'>");
        sb.append("<h3>Por Nivel Bloom</h3>"); Map<String,int[]> nivel=new LinkedHashMap<>(); Map<String,int[]> tipo=new LinkedHashMap<>();
        for(QuestionItem q:items){ nivel.putIfAbsent(q.getNivel(),new int[2]); tipo.putIfAbsent(q.getTipo(),new int[2]); nivel.get(q.getNivel())[0]++; tipo.get(q.getTipo())[0]++; String r=respuestas.get(q.getId()); if(r!=null&&r.equalsIgnoreCase(q.getCorrecta())){ nivel.get(q.getNivel())[1]++; tipo.get(q.getTipo())[1]++; }}
        for(var e:nivel.entrySet()){int t=e.getValue()[0],c=e.getValue()[1];double pct=t==0?0:(c*100.0/t); sb.append(String.format("%s: %d/%d → %.2f%%<br>",e.getKey(),c,t,pct));}
        sb.append("<h3>Por Tipo</h3>"); for(var e:tipo.entrySet()){int t=e.getValue()[0],c=e.getValue()[1];double pct=t==0?0:(c*100.0/t); sb.append(String.format("%s: %d/%d → %.2f%%<br>",e.getKey(),c,t,pct));}
        sb.append("</body></html>"); JOptionPane.showMessageDialog(frame,sb.toString(),"Resultados",JOptionPane.INFORMATION_MESSAGE);
    }

    private static void startReview(){ reviewMode=true; currentIndex=0; btnReview.setVisible(false); btnNext.setEnabled(true); btnPrev.setEnabled(false); mostrarPreguntaActual(); lblFeedback.setVisible(true); showFeedback(); }
    private static void showFeedback(){ QuestionItem q=items.get(currentIndex); String r=respuestas.get(q.getId()); boolean ok=r!=null&&r.equalsIgnoreCase(q.getCorrecta()); lblFeedback.setText(ok?"✔️ Correcto":"❌ Incorrecto. Correcta: "+q.getCorrecta()); lblFeedback.setForeground(ok?Color.GREEN.darker():Color.RED); }
    private static void mostrarSiguienteReview(){ guardarRespuesta(); if(currentIndex<items.size()-1){ currentIndex++; mostrarPreguntaActual(); showFeedback(); }}
    private static void mostrarAnteriorReview(){ guardarRespuesta(); if(currentIndex>0){ currentIndex--; mostrarPreguntaActual(); showFeedback(); }}
}
