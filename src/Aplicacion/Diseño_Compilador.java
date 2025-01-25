package Aplicacion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;

public class Diseño_Compilador extends javax.swing.JFrame 
{
    NumeroLinea numeroLinea;
    StyledDocument doc;
    Map<String, Style> Estilos;
    Timer TiempoDeActualizar;
    File currentFile;
    boolean Modificado;
    boolean TieneContenido;
    String Contenido;
    String EntradaCadena;
    String Cadena;     
    int ContadorCadena = 0; 
    int Cambiotoken;
    int CadenaInicio = 0;
    Stack<Integer> pila = new Stack<>();
    
    public Diseño_Compilador() 
    {
        initComponents();
        Inicializar();
        this.setLocationRelativeTo(null);
        
        inicializarComponentes();
        configurarEstilos();
        configurarTextPane();

        TiempoDeActualizar = new Timer(50, e -> resaltarSintaxis());
        TiempoDeActualizar.setRepeats(false);
           
        try 
        {
            Image icono = ImageIO.read(new File("Icono.png"));
            setIconImage(icono);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        
        configurarDeteccionCambios();

        Contenido = "";
        actualizarEstado();
        configurarGuardado();
        configurarApertura();
        
        SwingUtilities.invokeLater(() -> 
        {
            TpCodigo.requestFocusInWindow();
            TpCodigo.setCaretPosition(0);
        });
    }
    
   public void Inicializar()
    {
        setTitle("Licxek");

        Font fuenteMono = new Font("Monospaced", Font.PLAIN, 12);
        TpCodigo.setFont(fuenteMono);

        numeroLinea = new NumeroLinea(TpCodigo);
        numeroLinea.setFont(fuenteMono);

        jScrollPane4.setRowHeaderView(numeroLinea);

        numeroLinea.setPreferredSize(new Dimension(30, TpCodigo.getHeight()));

        TpCodigo.getDocument().addDocumentListener(numeroLinea);
        TpCodigo.addCaretListener(numeroLinea);

        TpCodigo.getDocument().addDocumentListener(new DocumentListener() 
        {
            public void insertUpdate(DocumentEvent e) 
            {
                actualizarNumeroLinea();
            }
            public void removeUpdate(DocumentEvent e) 
            {
                actualizarNumeroLinea();
            }
            public void changedUpdate(DocumentEvent e) 
            {
                actualizarNumeroLinea();
            }
        });

        TpCodigo.addCaretListener(e -> actualizarNumeroLinea());
    }

    private void actualizarNumeroLinea() 
    {
        SwingUtilities.invokeLater(() -> 
        {
            numeroLinea.setPreferredSize(new Dimension(30, TpCodigo.getHeight()));
            numeroLinea.repaint();
        });
    }
    
    public void Limpiar()
    {
       TpCodigo.setText("");
       TATokens.setText("");
       TASintaxis.setText("");
       TAErrores.setText("");
    }
    
    private void inicializarComponentes() 
    {
        doc = TpCodigo.getStyledDocument();
    }

    private void configurarEstilos() 
    {
        Estilos = new HashMap<>();
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        defaultStyle = doc.addStyle("default", defaultStyle);
        StyleConstants.setFontFamily(defaultStyle, "Monospaced");
        StyleConstants.setFontSize(defaultStyle, 15);
        StyleConstants.setBold(defaultStyle, true);

        Estilos.put("default", defaultStyle);
        Estilos.put("keyword", crearEstilo(defaultStyle, Color.BLUE));
        Estilos.put("type", crearEstilo(defaultStyle, new Color(75, 0, 130)));
        Estilos.put("string", crearEstilo(defaultStyle, Color.RED));
        Estilos.put("comment", crearEstilo(defaultStyle, Color.GRAY));
        Estilos.put("number", crearEstilo(defaultStyle, Color.MAGENTA));
    }

    private Style crearEstilo(Style parent, Color color) 
    {
        Style style = doc.addStyle(null, parent);
        StyleConstants.setForeground(style, color);
        return style;
    }

    private void configurarTextPane() {
    TpCodigo.getDocument().addDocumentListener(new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { 
            actualizarTexto(); 
            // Resaltar sintaxis inmediatamente después de la inserción
            resaltarSintaxis(); 
        }
        public void removeUpdate(DocumentEvent e) { 
            actualizarTexto();
            // Resaltar sintaxis inmediatamente después de eliminar
            resaltarSintaxis(); 
        }
        public void changedUpdate(DocumentEvent e) {}
    });

    TpCodigo.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                SwingUtilities.invokeLater(() -> indentarNuevaLinea());
            }
        }
    });
}


    private void actualizarTexto() 
    {
        TiempoDeActualizar.restart();
    }

    private void resaltarSintaxis() 
    {
        SwingUtilities.invokeLater(() -> 
        {
            try 
            {
                String text = doc.getText(0, doc.getLength());
                doc.setCharacterAttributes(0, doc.getLength(), Estilos.get("default"), true);

                aplicarEstilo("\\b(IF|ELSE|EVAL|UNTIL|END|WHILE|READ|PRINT|CLASS|DEFAS|CONS|ARRAY)\\b", "keyword");
                aplicarEstilo("\\b(STRING|INT|BOOL|CHAR|FLOAT|LONG)\\b", "type");
                aplicarEstilo("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"", "string");
                aplicarEstilo("//.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "comment");
                aplicarEstilo("\\b\\d+(\\.\\d+)?\\b", "number");

            } 
            catch(BadLocationException e) 
            {
                e.printStackTrace();
            }
        });
    }

    private void aplicarEstilo(String regex, String styleName) throws BadLocationException 
    {
        String text = doc.getText(0, doc.getLength());
        Matcher matcher = Pattern.compile(regex).matcher(text);
        Style style = Estilos.get(styleName);
        if (style != null) 
        {
            while (matcher.find()) 
            {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, true);
            }
        }
    }

    private void indentarNuevaLinea() 
    {
        try 
        {
            int caretPos = TpCodigo.getCaretPosition();
            int lineStart = Utilities.getRowStart(TpCodigo, caretPos);
            String prevLine = doc.getText(lineStart, caretPos - lineStart);
            
            StringBuilder indent = new StringBuilder();
            for (char c : prevLine.toCharArray()) 
            {
                if (c == ' ' || c == '\t')
                {
                    indent.append(c);
                } 
                else 
                {
                    break;
                }
            }

            if(prevLine.endsWith("{")) 
            {
                indent.append("    ");
            }

            SwingUtilities.invokeLater(() -> 
            {
                try 
                {
                    doc.insertString(caretPos, indent.toString(), null);
                } 
                catch (BadLocationException e) 
                {
                    e.printStackTrace();
                }
            });
        } 
        catch (BadLocationException e) 
        {
            e.printStackTrace();
        }
    }
    
    private void configurarGuardado() 
    {
        BtonGuardar.addActionListener(e -> guardarArchivo());

        TpCodigo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() 
        {
            public void changedUpdate(javax.swing.event.DocumentEvent e) 
            {
                setModified(true);
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) 
            {
                setModified(true);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) 
            {
                setModified(true);
            }
        });

        addWindowListener(new WindowAdapter() 
        {
            @Override
            public void windowClosing(WindowEvent e) 
            {
                if (Modificado) 
                {
                    int option = JOptionPane.showConfirmDialog(Diseño_Compilador.this,
                            "¿Desea guardar los cambios antes de salir?",
                            "Guardar cambios", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION) 
                    {
                        guardarArchivo();
                        if (!Modificado) 
                        {
                            dispose();
                        }
                    } 
                    else if (option == JOptionPane.NO_OPTION) 
                    {
                        dispose();
                    }
                } 
                else 
                {
                    dispose();
                }
            }
        });
    }

    private void configurarApertura() 
    {
        BtonAbrir.addActionListener(e -> abrirArchivo());
    }
    
    private void configurarDeteccionCambios() 
    {
        TpCodigo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() 
        {
            public void changedUpdate(javax.swing.event.DocumentEvent e) 
            {
                actualizarEstado();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) 
            {
                actualizarEstado();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) 
            {
                actualizarEstado();
            }
        });
    }

    private void guardarArchivo() 
    {
        if (!TieneContenido) 
        {
            JOptionPane.showMessageDialog(this, "No hay contenido para guardar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (currentFile == null) 
        {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos GOLEM", "glem"));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                if (!currentFile.getName().toLowerCase().endsWith(".glem")) 
                {
                    currentFile = new File(currentFile.getParentFile(), currentFile.getName() + ".glem");
                }
            } 
            else 
            {
                return;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) 
        {
            String contenidoActual = TpCodigo.getText();
            writer.write(contenidoActual);
            Contenido = contenidoActual;
            setModified(false);
            JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente", "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } 
        catch (IOException ex) 
        {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void abrirArchivo() 
    {
        if (Modificado) 
        {
            int option = JOptionPane.showConfirmDialog(this,
                    "Hay cambios sin guardar. ¿Desea guardar antes de abrir un nuevo archivo?",
                    "Guardar cambios", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) 
            {
                guardarArchivo();
            } 
            else if (option == JOptionPane.CANCEL_OPTION) 
            {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos GOLEM", "glem"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) 
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.getName().toLowerCase().endsWith(".glem")) 
            {
                try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) 
                {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) 
                    {
                        content.append(line).append("\n");
                    }
                    TpCodigo.setText(content.toString());
                    currentFile = selectedFile;
                    Contenido = TpCodigo.getText();
                    actualizarEstado();
                } 
                catch (IOException ex) 
                {
                    JOptionPane.showMessageDialog(this, "Error al abrir el archivo: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } 
            else 
            {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione un archivo con extensión .glem",
                        "Archivo no válido", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void setModified(boolean modified) 
    {
        Modificado = modified;
        actualizarLabelCambios();
    }
    
    private void actualizarLabelCambios() 
    {
        if (!TieneContenido) 
        {
            lbCambios.setText("No hay contenido");
        } 
        else if (Modificado) 
        {
            lbCambios.setText("Hay cambios sin guardar");
        } 
        else 
        {
            lbCambios.setText("No hay cambios sin guardar");
        }
    }
    
    private void actualizarEstado() 
    {
        String contenido = TpCodigo.getText().trim();
        TieneContenido = !contenido.isEmpty();
        
        if (currentFile == null) 
        {
            // No hay archivo abierto
            Modificado = TieneContenido;
        } 
        else 
        {
            // Hay un archivo abierto, comparar con el contenido actual
            Modificado = TieneContenido && !Contenido.equals(contenido);
        }
        
        actualizarLabelCambios();
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PnPrincipal = new javax.swing.JPanel();
        PnTitulo = new javax.swing.JPanel();
        lbIcono = new javax.swing.JLabel();
        lbNomApp = new javax.swing.JLabel();
        lbMax = new javax.swing.JLabel();
        lbMin = new javax.swing.JLabel();
        lbCerrar = new javax.swing.JLabel();
        lbCambios = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        BtonGuardar = new javax.swing.JButton();
        BtonLimpiar = new javax.swing.JButton();
        BtonAbrir = new javax.swing.JButton();
        BtonNuevo = new javax.swing.JButton();
        BtonSintactico = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TATokens = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        TASintaxis = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        TAErrores = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        TpCodigo = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFocusCycleRoot(false);
        setUndecorated(true);

        PnPrincipal.setBackground(new java.awt.Color(204, 204, 204));
        PnPrincipal.setForeground(new java.awt.Color(255, 255, 255));
        PnPrincipal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        PnTitulo.setBackground(new java.awt.Color(0, 0, 153));
        PnTitulo.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbIcono.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Atlas.png")); // NOI18N
        PnTitulo.add(lbIcono, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 40, 40));

        lbNomApp.setFont(new java.awt.Font("Century Gothic", 1, 16)); // NOI18N
        lbNomApp.setForeground(new java.awt.Color(255, 255, 255));
        lbNomApp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbNomApp.setText("Licxek Atlas IDE 2.0");
        PnTitulo.add(lbNomApp, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 10, 350, -1));

        lbMax.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\MaxVerde1.png")); // NOI18N
        lbMax.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbMaxMouseClicked(evt);
            }
        });
        PnTitulo.add(lbMax, new org.netbeans.lib.awtextra.AbsoluteConstraints(1160, -10, 50, 70));

        lbMin.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\MinAmarillo1.png")); // NOI18N
        lbMin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbMinMouseClicked(evt);
            }
        });
        PnTitulo.add(lbMin, new org.netbeans.lib.awtextra.AbsoluteConstraints(1120, -20, 40, 90));

        lbCerrar.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\TachaRoja1.png")); // NOI18N
        lbCerrar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbCerrarMouseClicked(evt);
            }
        });
        PnTitulo.add(lbCerrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(1200, -10, 50, 70));

        lbCambios.setFont(new java.awt.Font("Century Gothic", 1, 14)); // NOI18N
        lbCambios.setForeground(new java.awt.Color(255, 255, 255));
        lbCambios.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PnTitulo.add(lbCambios, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 40, 360, 20));

        jLabel1.setFont(new java.awt.Font("Comic Sans MS", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Golem.png")); // NOI18N
        jLabel1.setText("GOLEM");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PnTitulo.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(1020, 10, 60, 80));

        PnPrincipal.add(PnTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1280, 100));

        BtonGuardar.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\Icon\\icons8_save_48px.png")); // NOI18N
        BtonGuardar.setText("Guardar");
        BtonGuardar.setToolTipText("Guardar Documento");
        BtonGuardar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BtonGuardar.setRolloverIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\pressed\\icons8_save_48px_p.png")); // NOI18N
        BtonGuardar.setSelectedIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\On Layer\\icons8_save_48px_on.png")); // NOI18N
        BtonGuardar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PnPrincipal.add(BtonGuardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, 80, 80));

        BtonLimpiar.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\Icon\\icons9 Limpiar.png")); // NOI18N
        BtonLimpiar.setText("Limpiar");
        BtonLimpiar.setToolTipText("Limpiar Todo");
        BtonLimpiar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BtonLimpiar.setRolloverIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\On Layer\\icons9 Limpiar.png")); // NOI18N
        BtonLimpiar.setSelectedIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\pressed\\icons10 limpiar.png")); // NOI18N
        BtonLimpiar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        BtonLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtonLimpiarActionPerformed(evt);
            }
        });
        PnPrincipal.add(BtonLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 120, -1, 80));

        BtonAbrir.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\Icon\\icons8_opened_folder_48px.png")); // NOI18N
        BtonAbrir.setText("Abrir");
        BtonAbrir.setToolTipText("Abrir Documento");
        BtonAbrir.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BtonAbrir.setRolloverIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\On Layer\\icons8_opened_folder_48px_ON.png")); // NOI18N
        BtonAbrir.setSelectedIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\pressed\\icons8_opened_folder_48px_P.png")); // NOI18N
        BtonAbrir.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PnPrincipal.add(BtonAbrir, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 120, -1, 80));

        BtonNuevo.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\Icon\\icons8_code_file_48px.png")); // NOI18N
        BtonNuevo.setText("Nuevo");
        BtonNuevo.setToolTipText("Nuevo Documento");
        BtonNuevo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BtonNuevo.setRolloverIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\On Layer\\icons8_code_file_48px_on.png")); // NOI18N
        BtonNuevo.setSelectedIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\pressed\\icons8_code_file_48px_p.png")); // NOI18N
        BtonNuevo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PnPrincipal.add(BtonNuevo, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 120, 80, 80));

        BtonSintactico.setIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\Icon\\icons8_code_48px.png")); // NOI18N
        BtonSintactico.setText("Compilar");
        BtonSintactico.setToolTipText("Compilar Codigo");
        BtonSintactico.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BtonSintactico.setRolloverIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\On Layer\\icons8_code_48px_on.png")); // NOI18N
        BtonSintactico.setSelectedIcon(new javax.swing.ImageIcon("C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Compilador\\Iconos\\pressed\\icons8_code_48px_p.png")); // NOI18N
        BtonSintactico.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        BtonSintactico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtonSintacticoActionPerformed(evt);
            }
        });
        PnPrincipal.add(BtonSintactico, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 120, 90, 80));

        TATokens.setEditable(false);
        TATokens.setBackground(new java.awt.Color(255, 255, 255));
        TATokens.setColumns(20);
        TATokens.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        TATokens.setRows(5);
        TATokens.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jScrollPane1.setViewportView(TATokens);

        PnPrincipal.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 120, 470, 160));

        TASintaxis.setEditable(false);
        TASintaxis.setBackground(new java.awt.Color(255, 255, 255));
        TASintaxis.setColumns(20);
        TASintaxis.setFont(new java.awt.Font("Century Gothic", 1, 14)); // NOI18N
        TASintaxis.setRows(5);
        TASintaxis.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jScrollPane2.setViewportView(TASintaxis);

        PnPrincipal.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 300, 470, 170));

        TAErrores.setEditable(false);
        TAErrores.setBackground(new java.awt.Color(255, 255, 255));
        TAErrores.setColumns(20);
        TAErrores.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        TAErrores.setForeground(new java.awt.Color(0, 0, 0));
        TAErrores.setRows(5);
        TAErrores.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jScrollPane3.setViewportView(TAErrores);

        PnPrincipal.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 490, 470, 160));

        TpCodigo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jScrollPane4.setViewportView(TpCodigo);

        PnPrincipal.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, 680, 420));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PnPrincipal, javax.swing.GroupLayout.PREFERRED_SIZE, 1234, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PnPrincipal, javax.swing.GroupLayout.DEFAULT_SIZE, 718, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BtonLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtonLimpiarActionPerformed
       Limpiar();
    }//GEN-LAST:event_BtonLimpiarActionPerformed

    private void lbCerrarMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbCerrarMouseClicked
        if (Modificado) 
        {
            int respu = JOptionPane.showConfirmDialog(this,
                "Hay cambios sin guardar. ¿Desea guardar antes de salir?",
                "Cerrar",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (respu == JOptionPane.YES_OPTION) 
            {
                guardarArchivo();
                if (!Modificado) 
                {
                    System.exit(0);
                }
            } 
            else if (respu == JOptionPane.NO_OPTION) 
            {
                System.exit(0);
            }
        } 
        else 
        {
            System.exit(0);
        }
    }//GEN-LAST:event_lbCerrarMouseClicked

    private void lbMaxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbMaxMouseClicked
        
    }//GEN-LAST:event_lbMaxMouseClicked

    private void lbMinMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbMinMouseClicked
        setState(this.ICONIFIED);
    }//GEN-LAST:event_lbMinMouseClicked

    private void BtonSintacticoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtonSintacticoActionPerformed
        AnalizadorSintactico();
    }//GEN-LAST:event_BtonSintacticoActionPerformed
    
    static String[] PalabrasReservadas = 
    {
        "CLASS", "INT", "FLOAT", "CHAR", "STRING", "BOOL",
        "IF", "ELSE", "EVAL", "UNTIL", "END", "WHILE",
        "READ", "PRINT", "DEFAS", "CONS", "ARRAY"
    };
    
    public int Analiza() 
    {
        int Estado = 0 ;       
        int Columna;      
        char caracter;     
           

        Cadena = LeerArchivo();
        EntradaCadena = Cadena;

        TATokens.setText("");
        int maxLexemaWidth = "Lexema".length();
        int maxTokenWidth = "Token".length();
        TATokens.append(String.format("%-" + maxLexemaWidth + "s | %-" + maxTokenWidth + "s%n", "Lexema", "Token")); 
        TATokens.append(String.format("%-" + maxLexemaWidth + "s-+-%" + maxTokenWidth + "s%n", "", "").replace(' ', '-')); 
        
        TAErrores.setText("");
        maxLexemaWidth = "Lexema".length();
        int maxErrorWidth = "Error".length();
        TAErrores.append(String.format("%-" + maxLexemaWidth + "s | %-" + maxErrorWidth + "s%n", "Lexema", "Error")); 
        TAErrores.append(String.format("%-" + maxLexemaWidth + "s-+-%" + maxErrorWidth + "s%n", "", "").replace(' ', '-'));

        boolean TerminarCiclo = false;  
        boolean EsTerminal = false;
        
        StringBuilder Lexema = new StringBuilder(); 
        
        while (ContadorCadena <= EntradaCadena.length()) 
        {
            Estado = 0;
            TerminarCiclo = false;
            CadenaInicio = ContadorCadena; 

            while (Estado < 20 && !TerminarCiclo) 
            { 
                caracter = LeerCaracter(ContadorCadena);
                Lexema.append(caracter); 
                Columna = Relaciona(caracter);
                Estado = FuncionTransitiva[Estado][Columna];
                
                System.out.println("Estado: " + Estado + ", Caracter: " + caracter + ", Columna: " + Columna);

                EsTerminal = false;
                
                if (Estado == 100 || Estado == 101 || Estado == 102 || Estado == 103 || Estado == 104 ||
                    Estado == 126 || Estado == 116 || Estado == 113 || Estado == 111 || Estado == 109) 
                {
                    Lexema.setCharAt(Lexema.length() - 1,'\0');
                    if (Estado == 100 && !VerificarPalabraReservada(Lexema.toString().trim())) 
                    {
                        Estado = 101;
                    }
                    Token(Estado,Lexema.toString());
                    TerminarCiclo = true;
                    EsTerminal = true;
                    Lexema.setLength(0);
                }
                
                if (!EsTerminal) 
                {
                    ContadorCadena++;
                }

                if (Estado >= 100 && Estado <= 129 && !EsTerminal) 
                {
                    Token(Estado, Lexema.toString());
                    Lexema.setLength(0);
                } 
                else if (Estado >= 500) 
                {
                    Error(Estado, Lexema.toString());
                    Lexema.setLength(0);
                }
            } 
            if (ContadorCadena == EntradaCadena.length()) 
            {
                break;
            }
        }
        System.out.println("Estado Final: " + Estado);
        return Estado;
    }
    
    public void AnalizadorSintactico() 
    {
        TASintaxis.setText("");
        Cadena = LeerArchivo();

        pila.push(147); // $ (fin de entrada)
        pila.push(0);      // Estado inicial

        int tokenIndex = Analiza();

        while (!pila.isEmpty()) 
        {
            int currentTop = pila.peek();

            // Seguir buscando no terminales hasta encontrar un terminal o $
            while (currentTop >= 0 && currentTop <= 39) 
            {
                int columna = TokenSintactico(tokenIndex);

                System.out.println("Pila: " + pila);
                System.out.println("Token: " + tokenIndex + ", Columna: " + columna);

                int produccion = MatrizPredictiva[currentTop][columna];
                System.out.println("Produccion " + produccion);
                
                if (produccion >= 1 && produccion <= 82) 
                {
                    pila.pop();
                    Producciones(produccion);
                    
                } 
                else if (produccion >= 600) 
                {
                    ErrorSintactico(produccion, String.valueOf(tokenIndex));
                    System.out.println("Error sintáctico, produccion: " + produccion + ", Token: " + tokenIndex);
                    return;
                }

                // Actualizar currentTop después de aplicar la producción
                currentTop = pila.peek(); 
                System.out.print(currentTop);
                System.out.println(tokenIndex);
            }

            // Verificar si el tope de la pila es un terminal o $
            if (currentTop >= 100 && currentTop <= 144) 
            {
                if (currentTop == tokenIndex) 
                {
                    pila.pop();
                    System.out.println(pila);

                    if (ContadorCadena < Cadena.length()) 
                    {
                        tokenIndex = Analiza();
                        System.out.println(tokenIndex);
                    } 
                    else if (!pila.isEmpty()) 
                    {
                        tokenIndex = 144;
                        System.out.println(tokenIndex);
                    }
                } 
                else 
                {
                    ErrorSintactico(644, String.valueOf(tokenIndex));
                    return;
                }
            }
            

            if (pila.peek() == -1) 
            {
                pila.pop();
                System.out.println(pila);
            }

            if (pila.isEmpty() || pila.peek() == 147) 
            {
                TASintaxis.append("Se Analizo correctamente!!");
                return;
            }
        }
    }
    
    private int PalabrasReservadasTokens(String lexema) 
    {
        switch (lexema) 
        {
            case "CLASS" : return 100;
            case "DEFAS": return 130;
            case "CONS": return 131;
            case "INT": return 132;
            case "FLOAT": return 133;
            case "CHAR": return 134;
            case "BOOL": return 135;
            case "STRING": return 136;
            case "IF": return 137;
            case "ELSE": return 138;
            case "WHILE": return 139;
            case "EVAL": return 140;
            case "UNTIL": return 141;
            case "PRINT": return 142;
            case "READ": return 143;
            case "END": return 144;
            default: return -1; 
        }
    }
    
    public boolean VerificarPalabraReservada(String lexema) 
    {
        for (String palabra : PalabrasReservadas) 
        {
            if (lexema.equals(palabra)) 
            {
                return true;
            }
        }
        return false;
    }   


    public String LeerArchivo() 
    {
        String texto = TpCodigo.getText();
        texto = texto.replaceAll("\\r\\n?", "\n"); 
        texto = texto.replaceAll("(?m)^\\s+|\\s+$", ""); 
        return texto;
    }

    public char LeerCaracter(int Entrada) 
    {
        if (Entrada < EntradaCadena.length()) 
        {
            return EntradaCadena.charAt(Entrada);
        } 
        else 
        {
            return (char) -1;
        }
    }

    public int Relaciona(char c)
    {
        if (c == -1) 
        {
            return 32;
        }
        if (Character.isLowerCase(c) && c != 'e') 
        {
            return 0;
        }
        if (Character.isUpperCase(c) && c != 'E') 
        {
            return 1;
        }
        if (Character.isDigit(c)) 
        {
            return 2;
        }
        switch(c)
        {
            case '_' : return 3;
            case '.' : return 4;
            case '+' : return 5;
            case '-' : return 6;
            case 'E' : return 7;
            case 'e' : return 8;
            case '/' : return 9;
            case '%' : return 10;
            case '*' : return 11;
            case '\t': return 12;
            case ' ' : return 13;
            case '\n': return 14;
            case '&' : return 15;
            case '=' : return 16;
            case '!' : return 17;
            case '<' : return 18;
            case '>' : return 19;
            case '|' : return 20;
            case ',' : return 21;
            case ';' : return 22;
            case ':' : return 23;
            case '(' : return 24;
            case ')' : return 25;
            case '[' : return 26;
            case ']' : return 27;
            case '#' : return 28;
            case '"' : return 29;
            case '\'' : return 30;
        }
        return 31;
    }
    
    public void Token(int e, String Lexema)
    {
        int Estado = 0;
        String Token = "";
        switch(e)
        {
            case 100 : Token ="Palabras Reservadas";
            Estado = PalabrasReservadasTokens(Lexema.toString().trim());
            break;
            case 101 : Token = "Identificador";
            break;
            case 102 : Token = "Enteros";
            break;
            case 103 : Token = "Real";
            break;
            case 104 : Token = "Notacion Cientifica";
            break;
            case 105 : Token = "Suma";
            break;
            case 106 : Token = "Resta";
            break;
            case 107 : Token = "Multiplicacion";
            break;
            case 108 : Token = "Division";
            break;
            case 109 : Token = "Asignar";
            break;
            case 110 : Token = "Igual";
            break;
            case 111 : Token = "Menor";
            break;
            case 112 : Token = "Menor igual";
            break;
            case 113 : Token = "Mayor";
            break;
            case 114 : Token = "Mayor igual";
            break;
            case 115 : Token = "Diferente";
            break;
            case 116 : Token = "NOT";
            break;
            case 117 : Token = "AND";
            break;
            case 118 : Token = "OR";
            break;
            case 119 : Token = "Parentesis que abre";
            break;
            case 120 : Token = "Parentesis que cierra";
            break;
            case 121 : Token = "Corchete que abre";
            break;
            case 122 : Token = "Corchete que cierra";
            break;
            case 123 : Token = "Punto y coma";
            break;
            case 124 : Token = "Coma";
            break;
            case 125 : Token = "Tipo Caracterer";
            break;
            case 126 : Token = "Tipo String";
            break;
            case 127 : Token = "Comentario de Linea";
            break;
            case 128 : Token = "Modulo";
            break;
            case 129 : Token = "Dos puntos";
            break;
        }
        String lexemaFiltrado = Lexema.replaceAll("[^\\p{Print}]", "");
        int maxLexemaWidth = Math.max(lexemaFiltrado.length(), "Lexema".length());
        int maxTokenWidth = Math.max(Token.length(), "Token".length());

        String formato = String.format
        (
            "%-" + maxLexemaWidth + "s | %-" + maxTokenWidth + "s%n", 
            lexemaFiltrado, Token
        );
        TATokens.append(formato);
        TATokens.append(String.format("%-" + maxLexemaWidth + "s-+-%" + maxTokenWidth + "s%n", "", "").replace(' ', '-'));
    }
    
    public void Error(int e, String Lexema)
    {
        String Errores = "";
        
        switch(e)
        {
            case 500 : Errores = "Error 500: Valor flotante, se esperan digitos ";
            break;
            case 501 : Errores = "Error 501: Notacion Cientifica, se esperan digitos o signos +/-";
            break;
            case 502 : Errores = "Error 502: Notacion Cientifica, se esperan digitos";
            break;
            case 503 : Errores = "Error 503: And,se espera otro simbolo &";
            break;
            case 504 : Errores = "Error 504: Or, se espera otro simbolo |";
            break;
            case 505 : Errores = "Error 505: Tipo Caracter, no se pueden poner dos apostrofes seguidos";
            break;
            case 506 : Errores = "Error 506: Delimitadores, no se puede poner algo diferente despues de un Delimitador";
            break;
            case 507 : Errores = "Error 507: Tipo Caracter, no se puede seguir otro digito,letra o simbolo,solo uno se permite";
            break;
            case 508 : Errores = "Error 508: EOF,End of File";
            break;
        } 
        String lexemaFiltrado = Lexema.replaceAll("[^\\p{Print}]", "");
        int maxLexemaWidth = Math.max(lexemaFiltrado.length(), "Lexema".length());
        int maxErrorWidth = Math.max(Errores.length(), "Error".length());

        String formato = String.format
        (
            "%-" + maxLexemaWidth + "s | %-" + maxErrorWidth + "s%n", 
            lexemaFiltrado, Errores
        );
        TAErrores.append(formato);
        TAErrores.append(String.format("%-" + maxLexemaWidth + "s-+-%" + maxErrorWidth + "s%n", "", "").replace(' ', '-'));
    }
    
    public static int[][] FuncionTransitiva = 
    {
        {2,1,3,506,506,105,106,1,2,108,128,107,0,0,0,13,9,12,10,11,14,124,123,129,119,120,121,122,19,17,15,506,508},
        {2,1,2,2,100,100,100, 1, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 508},
        {2, 2, 2, 2, 101, 101, 101, 101, 2, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 508},
        {102, 102, 3, 102, 4, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 102, 508},
        {500, 500, 5, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 508},
        {103, 103, 5, 103, 103, 103, 103, 6, 6, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103, 508},
        {501, 501, 8, 501, 501, 7, 7, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 501, 508},
        {502, 502, 8, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 502, 508},
        {104, 104, 8, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 508},
        {109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 110, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 508},
        {111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 112, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 508},
        {113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 114, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 508},
        {116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 115, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 116, 508},
        {503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 117, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 508},
        {504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 118, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 504, 508},
        {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 505, 16, 508},
        {507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 125, 507, 508},
        {17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 18, 17, 17, 508},
        {126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 17, 126, 126, 508},
        {19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 127, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 508}
    };
    
    public void Producciones(int Dato) 
    {
        int[] Produccion0 = {0};
        int[] Produccion1 = {144, 9, 1, 120, 101, 119, 100};
        int[] Produccion2 = {9, 10};
        int[] Produccion3 = {9, 11};
        int[] Produccion4 = {9, 13};
        int[] Produccion5 = {9, 14};
        int[] Produccion6 = {9, 17};
        int[] Produccion7 = {9, 15};
        int[] Produccion8 = {-1};
        int[] Produccion9 = {123, 23, 109, 19};
        int[] Produccion10 = {144, 12, 9, 129, 23, 137};
        int[] Produccion11 = {9, 138};
        int[] Produccion12 = {-1};
        int[] Produccion13 = {144, 9, 129, 23, 139};
        int[] Produccion14 = {144, 23, 141, 9, 140};
        int[] Produccion15 = {132};
        int[] Produccion16 = {133};
        int[] Produccion17 = {134};
        int[] Produccion18 = {135};
        int[] Produccion19 = {136};
        int[] Produccion20 = {4, 101};
        int[] Produccion21 = {5};
        int[] Produccion22 = {-1};
        int[] Produccion23 = {122, 6, 102, 121};
        int[] Produccion24 = {6, 102, 124};
        int[] Produccion25 = {-1};
        int[] Produccion26 = {20, 101};
        int[] Produccion27 = {21}; 
        int[] Produccion28 = {-1}; 
        int[] Produccion29 = {122, 22, 23, 121};
        int[] Produccion30 = {22, 23, 124};
        int[] Produccion31 = {-1}; 
        int[] Produccion32 = {1, 123, 7, 129, 2, 3, 130};
        int[] Produccion33 = {1, 123, 8, 109, 101, 131};
        int[] Produccion34 = {-1}; 
        int[] Produccion35 = {2, 3, 124};
        int[] Produccion36 = {-1}; 
        int[] Produccion37 = {24, 25};
        int[] Produccion38 = {23, 118};
        int[] Produccion39 = {-1}; 
        int[] Produccion40 = {26, 27};
        int[] Produccion41 = {25, 117};
        int[] Produccion42 = {-1};
        int[] Produccion43 = {28};
        int[] Produccion44 = {28, 116};
        int[] Produccion45 = {29, 30};
        int[] Produccion46 = {30, 32};
        int[] Produccion47 = {-1}; 
        int[] Produccion48 = {31, 33};
        int[] Produccion49 = {30, 105};
        int[] Produccion50 = {30, 106};
        int[] Produccion51 = {-1};
        int[] Produccion52 = {34, 35};
        int[] Produccion53 = {33, 107};
        int[] Produccion54 = {33, 108};
        int[] Produccion55 = {33, 128};
        int[] Produccion56 = {-1}; 
        int[] Produccion57 = {36};
        int[] Produccion58 = {8};
        int[] Produccion59 = {120, 23, 119};
        int[] Produccion60 = {110};
        int[] Produccion61 = {115};
        int[] Produccion62 = {111};
        int[] Produccion63 = {112};
        int[] Produccion64 = {113};
        int[] Produccion65 = {114};
        int[] Produccion66 = {102};
        int[] Produccion67 = {103};
        int[] Produccion68 = {104};
        int[] Produccion69 = {125};
        int[] Produccion70 = {126};
        int[] Produccion71 = {37, 101};
        int[] Produccion72 = {38};
        int[] Produccion73 = {-1};
        int[] Produccion74 = {102, 39, 23, 121};
        int[] Produccion75 = {39, 23, 124};
        int[] Produccion76 = {-1}; 
        int[] Produccion77 = {123, 16, 23, 129, 142};
        int[] Produccion78 = {16, 23, 124};
        int[] Produccion79 = {-1};
        int[] Produccion80 = {123, 18, 101, 129, 143};
        int[] Produccion81 = {16, 101, 124};
        int[] Produccion82 = {-1}; 
        
        int[][] Arreglo_prod = 
        {
            Produccion0, Produccion1, Produccion2, Produccion3, Produccion4, Produccion5,
            Produccion6, Produccion7, Produccion8, Produccion9, Produccion10,
            Produccion11, Produccion12, Produccion13, Produccion14, Produccion15,
            Produccion16, Produccion17, Produccion18, Produccion19, Produccion20,
            Produccion21, Produccion22, Produccion23, Produccion24, Produccion25,
            Produccion26, Produccion27, Produccion28, Produccion29, Produccion30,
            Produccion31, Produccion32, Produccion33, Produccion34, Produccion35,
            Produccion36, Produccion37, Produccion38, Produccion39, Produccion40,
            Produccion41, Produccion42, Produccion43, Produccion44, Produccion45,
            Produccion46, Produccion47, Produccion48, Produccion49, Produccion50,
            Produccion51, Produccion52, Produccion53, Produccion54, Produccion55,
            Produccion56, Produccion57, Produccion58, Produccion59, Produccion60,
            Produccion61, Produccion62, Produccion63, Produccion64, Produccion65,
            Produccion66, Produccion67, Produccion68, Produccion69, Produccion70,
            Produccion71, Produccion72, Produccion73, Produccion74, Produccion75,
            Produccion76, Produccion77, Produccion78, Produccion79, Produccion80,
            Produccion81, Produccion82
        };
        
         Stack<Integer> pila = new Stack<>();
        int[] produccionSeleccionada = Arreglo_prod[Dato]; // Obtener la producción específica

        // Vaciamos los elementos de la producción seleccionada en la pila (si no es vacía)
        if (!contieneMil(produccionSeleccionada)) {
            for (int i = produccionSeleccionada.length - 1; i >= 0; i--) {
                pila.push(produccionSeleccionada[i]);
            }

            System.out.println("Elementos de la producción " + Dato + ":");
            while (!pila.isEmpty()) {
                System.out.println(pila.pop());
            }
        } else {
            System.out.println("La producción " + Dato + " está vacía.");
        }
    
    };

    private static boolean contieneMil(int[] arreglo) 
    {
        for (int elemento : arreglo) 
        {
            if (elemento == 1000) 
            {
                return true;
            }
        }
        return false;
    }

       
    private int TokenSintactico(int e) 
    {
    switch(e) {
        case 100:
            Cambiotoken = 0;
            break;
        case 101:
            Cambiotoken = 1;
            break;
        case 102:
            Cambiotoken = 11;
            break;
        case 103:
            Cambiotoken = 18;
            break;
        case 104:
            Cambiotoken = 19;
            break;
        case 105:
            Cambiotoken = 32;
            break;
        case 106:
            Cambiotoken = 33;
            break;
        case 107:
            Cambiotoken = 40;
            break;
        case 108:
            Cambiotoken = 41;
            break;
        case 109:
            Cambiotoken = 8;
            break;
        case 110:
            Cambiotoken = 34;
            break;
        case 111:
            Cambiotoken = 36;
            break;
        case 112:
            Cambiotoken = 37;
            break;
        case 113:
            Cambiotoken = 38;
            break;
        case 114:
            Cambiotoken = 39;
            break;
        case 115:
            Cambiotoken = 35;
            break;
        case 116:
            Cambiotoken = 31;
            break;
        case 117:
            Cambiotoken = 30;
            break;
        case 118:
            Cambiotoken = 29;
            break;
        case 119:
            Cambiotoken = 2;
            break;
        case 120:
            Cambiotoken = 3;
            break;
        case 121:
            Cambiotoken = 9;
            break;
        case 122:
            Cambiotoken = 10;
            break;
        case 123:
            Cambiotoken = 6;
            break;
        case 124:
            Cambiotoken = 12;
            break;
        case 125:
            Cambiotoken = 20;
            break;
        case 126:
            Cambiotoken = 21;
            break;
        case 127:
            // Comentarios
            break;
        case 128:
            Cambiotoken = 42;
            break;
        case 129:
            Cambiotoken = 5;
            break;
        case 130:
            Cambiotoken = 4;
            break;
        case 131:
            Cambiotoken = 7;
            break;
        case 132:
            Cambiotoken = 13;
            break;
        case 133:
            Cambiotoken = 14;
            break;
        case 134:
            Cambiotoken = 15;
            break;
        case 135:
            Cambiotoken = 16;
            break;
        case 136:
            Cambiotoken = 17;
            break;
        case 137:
            Cambiotoken = 22;
            break;
        case 138:
            Cambiotoken = 23;
            break;
        case 139:
            Cambiotoken = 24;
            break;
        case 140:
            Cambiotoken = 25;
            break;
        case 141:
            Cambiotoken = 26;
            break;
        case 142:
            Cambiotoken = 27;
            break;
        case 143:
            Cambiotoken = 28;
            break;
        case 144:
            Cambiotoken = 43;
            break;
        
    }
    return Cambiotoken;
}
    
    public void ErrorSintactico(int e, String Lexema)
    {    
        String Errores = "";
        
        switch(e)
        {
            case 600: Errores = "600: SE ESPERABA UN CLASS ";
                break;
            case 601: Errores = "601: SE ESPERABA UN ID	";
                break;
            case 602: Errores = "602: SE ESPERABA UN (";
                break;
            case 603: Errores = "603: SE ESPERABA UN )";
                break;
            case 604: Errores = "604: SE ESPERABA UN DEFAS";
                break;
            case 605: Errores = "605: SE ESPERABA : ";
                break;
            case 606: Errores = "606: SE ESPERABA  ; ";
                break;
            case 607: Errores = "607: SE ESPERABA CONS ";
                break;
            case 608: Errores = "608: SE ESPERABA UN  = ";
                break;
            case 609: Errores = "609: SE ESPERABA UN [ ";
                break;
            case 610: Errores = "610: SE ESPERABA UN ] ";
                break;
            case 611: Errores = "611: SE ESPERABA UN CONSTANTE ENTERO";
                break;
            case 612: Errores = "612: SE ESPERABA UN , ";
                break;
            case 613: Errores = "613: SE ESPERABA UN TIPO INT";
                break;
            case 614: Errores = "614: SE ESPERABA UN TIPO FLOAT";
                break;
            case 615: Errores = "615: SE ESPERABA UN TIPO CHAR";
                break;
            case 616: Errores = "616: SE ESPERABA UN TIPO BOOL";
                break;
            case 617: Errores = "617: SE ESPERABA UN TIPO STRING";
                break;
            case 618: Errores = "618: SE ESPERABA UN CONSTANTE REAL";
                break;
            case 619: Errores = "619: SE ESPERABA UN CONSTANTE DE NOTACION";
                break;
            case 620: Errores = "620: SE ESPERABA UN CONSTANTE CARÁCTER";
                break;
            case 621: Errores = "621: SE ESPERABA UN CONSTANTE STRING";
                break;
            case 622: Errores = "622: SE ESPERABA UN IF";
                break;
            case 623: Errores = "623: SE ESPERABA UN ELSE";
                break;
            case 624: Errores = "624: SE ESPERA UN WHILE ";
                break;
            case 625: Errores = "625: SE ESPERA UN EVAL ";
                break;
            case 626: Errores = "626: SE ESPERA UN UNTIL ";
                break;
            case 627: Errores = "627: SE ESPERA UN PRINT ";
                break;
            case 628: Errores = "628: SE ESPERA UN READ ";
                break;
            case 629: Errores = "629: SE ESPERA UN || ";
                break;
            case 630: Errores = "630: SE ESPERA UN && ";
                break;
            case 631: Errores = "631: SE ESPERA ! ";
                break;
            case 632: Errores = "632: SE ESPERA + ";
                break;
            case 633: Errores = "633: SE ESPERA - ";
                break;
            case 634: Errores = "634: SE ESPERA == ";
                break;
            case 635: Errores = "635: SE ESPERA != ";
                break;
            case 636: Errores = "636: SE ESPRA < ";
                break;
            case 637: Errores = "637: SE ESPERA <= ";
                break;
            case 638: Errores = "638: SE ESPERA > ";
                break;
            case 639: Errores = "639 SE ESPERA >= ";
                break;
            case 640: Errores = "640: SE ESPERA * ";
                break;
            case 641: Errores = "641: SE ESPERA  UN / ";
                break;
            case 642: Errores = "642: SE ESPERA % ";
                break;
            case 643: Errores = "643: SE ESPERA UN END ";
                break;
            case 644: Errores = "644: No se reconocio ningun Token ";
                break;
        } 
        String lexemaFiltrado = Lexema.replaceAll("[^\\p{Print}]", "");
        int maxLexemaWidth = Math.max(lexemaFiltrado.length(), "Lexema".length());
        int maxErrorWidth = Math.max(Errores.length(), "Error".length());

        String formato = String.format
        (
            "%-" + maxLexemaWidth + "s | %-" + maxErrorWidth + "s%n", 
            lexemaFiltrado, Errores
        );
        TASintaxis.append(formato);
        TASintaxis.append(String.format("%-" + maxLexemaWidth + "s-+-%" + maxErrorWidth + "s%n", "", "").replace(' ', '-'));
    }
     
    public static int[][] MatrizPredictiva =
    {
        {1,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	34,	602,	603,	32,	605,	606,	33,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	34,	34,	34,	34,	34,	34,	34,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	34},
        {600,	601,	602,	603,	604,	36,	606,	607,	608,	609,	610,	611,	35,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	20,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	22,	606,	607,	608,	21,	610,	611,	22,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	23,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	25,	611,	24,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	15,	16,	17,	18,	19,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	66,	612,	613,	614,	615,	616,	617,	67,	68,	69,	70,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	2,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	3,	8,	4,	5,	8,	7,	6,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	8},
        {600,	9,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	10,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	11,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	12},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	13,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	14,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	77,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	79,	607,	608,	609,	610,	611,	78,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	80,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	82,	607,	608,	609,	610,	611,	81,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	26,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	28,	27,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	29,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	31,	611,	30,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	37,	37,	603,	604,	605,	606,	607,	608,	609,	610,	37,	612,	613,	614,	615,	616,	617,	37,	37,	37,	37,	622,	623,	624,	625,	626,	627,	628,	629,	630,	37,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	39,	604,	39,	39,	607,	608,	609,	39,	611,	39,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	38,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	39},
        {600,	40,	40,	603,	604,	605,	606,	607,	608,	609,	610,	40,	612,	613,	614,	615,	616,	617,	40,	40,	40,	40,	622,	623,	624,	625,	626,	627,	628,	629,	630,	40,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	42,	604,	42,	42,	607,	608,	609,	42,	611,	42,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	42,	41,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	42},
        {600,	43,	43,	603,	604,	605,	606,	607,	608,	609,	610,	43,	612,	613,	614,	615,	616,	617,	43,	43,	43,	43,	622,	623,	624,	625,	626,	627,	628,	629,	630,	44,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	45,	45,	603,	604,	605,	606,	607,	608,	609,	610,	45,	612,	613,	614,	615,	616,	617,	45,	45,	45,	45,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	47,	604,	47,	47,	607,	608,	609,	47,	611,	47,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	47,	47,	631,	632,	633,	46,	46,	46,	46,	46,	46,	640,	641,	642,	47},
        {600,	48,	48,	603,	604,	605,	606,	607,	608,	609,	610,	48,	612,	613,	614,	615,	616,	617,	48,	48,	48,	48,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	51,	604,	51,	51,	607,	608,	609,	51,	611,	51,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	51,	51,	631,	49,	50,	51,	51,	51,	51,	51,	51,	640,	641,	642,	51},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	60,	61,	62,	63,	64,	65,	640,	641,	642,	643},
        {600,	52,	52,	603,	604,	605,	606,	607,	608,	609,	610,	52,	612,	613,	614,	615,	616,	617,	52,	52,	52,	52,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	56,	604,	56,	56,	607,	608,	609,	56,	611,	56,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	56,	56,	631,	56,	56,	56,	56,	56,	56,	56,	56,	53,	54,	55,	56},
        {600,	57,	59,	603,	604,	605,	606,	607,	608,	609,	610,	58,	612,	613,	614,	615,	616,	617,	58,	58,	58,	58,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	71,	602,	603,	604,	605,	606,	607,	608,	609,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	73,	604,	73,	73,	607,	608,	72,	73,	611,	73,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	73,	73,	631,	73,	73,	73,	73,	73,	73,	73,	73,	73,	73,	73,	73},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	74,	610,	611,	612,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643},
        {600,	601,	602,	603,	604,	605,	606,	607,	608,	609,	76,	611,	75,	613,	614,	615,	616,	617,	618,	619,	620,	621,	622,	623,	624,	625,	626,	627,	628,	629,	630,	631,	632,	633,	634,	635,	636,	637,	638,	639,	640,	641,	642,	643}    
    };
    
    public static void main(String args[]) 
    {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Diseño_Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Diseño_Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Diseño_Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Diseño_Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Diseño_Compilador().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtonAbrir;
    private javax.swing.JButton BtonGuardar;
    private javax.swing.JButton BtonLimpiar;
    private javax.swing.JButton BtonNuevo;
    private javax.swing.JButton BtonSintactico;
    private javax.swing.JPanel PnPrincipal;
    private javax.swing.JPanel PnTitulo;
    private javax.swing.JTextArea TAErrores;
    private javax.swing.JTextArea TASintaxis;
    private javax.swing.JTextArea TATokens;
    private javax.swing.JTextPane TpCodigo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lbCambios;
    private javax.swing.JLabel lbCerrar;
    private javax.swing.JLabel lbIcono;
    private javax.swing.JLabel lbMax;
    private javax.swing.JLabel lbMin;
    private javax.swing.JLabel lbNomApp;
    // End of variables declaration//GEN-END:variables
}
