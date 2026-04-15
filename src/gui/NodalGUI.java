package gui;

/**
 *
 * @author jstar
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import prc.CircuitIO;
import prc.PassiveResistiveCircuit;

public class NodalGUI extends JFrame {

    private final boolean printDiag = false;

    private JPanel canvasPanel;
    private JPanel bottomPanel;
    private JLabel message;

    private final static Font[] fonts = {
        new Font("Courier", Font.PLAIN, 12),
        new Font("Courier", Font.PLAIN, 18),
        new Font("Courier", Font.PLAIN, 24)
    };
    private static Font currentFont = fonts[1];

    private static final String CONFIG_FILE = ".nodal_gui_config";
    private final Configuration configuration = new Configuration(CONFIG_FILE);
    private final String LAST_DIR = "NodalGUI.last.dir";

    private final String DEFAULT_BND_TEXT = "Set potential(s)";

    private PassiveResistiveCircuit circ;

    private final Map<String, Boolean> options = new HashMap<>();
    private final Set<Integer> currentSelection = new TreeSet<>();
    private final ArrayList<PointPosition> xy = new ArrayList<>();

    private final TreeMap<Integer, Double> sources = new TreeMap<>();

    private void setFontRecursively(Component comp, Font font, int d) {
        if (comp == null) {
            return;
        }
        comp.setFont(font);
        // Diagnostics
        if (printDiag) {
            for (int i = 0; i < d; i++) {
                System.err.print("\t");
            }
            System.err.println(comp.getClass().getName() + " : " + (comp instanceof Container ? ("container (" + ((Container) comp).getComponentCount() + ")") : "other"));
        }
        //
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontRecursively(child, font, d + 1);
            }
        }
        // Needs specific navigation, since JMenu does not show menu components as Components
        if (comp instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                setFontRecursively(menu.getItem(i), font, d + 1);
            }
        }
    }

    public NodalGUI() {
        super("Simple Swing GUI");
        options.put("inDefBoundary", false);
        initGui();
    }

    private void initGui() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createMenuBar();
        createCanvasPanel();
        InputMap im = canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvasPanel.getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "escPressed");

        am.put("escPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifySources();
            }
        });

        createBottomPanel();

        add(canvasPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setFontRecursively(this, currentFont, 0);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem readItem = new JMenuItem("Read circuit");
        readItem.setFont(currentFont);
        readItem.addActionListener(e -> loadFile());
        fileMenu.add(readItem);
        JMenuItem saveItem = new JMenuItem("Save circuit");
        saveItem.setFont(currentFont);
        saveItem.addActionListener(e -> saveCircuit());
        fileMenu.add(saveItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setFont(currentFont);
        exitItem.addActionListener(e -> doExit());
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu setupMenu = new JMenu("Setup");
        JMenuItem srcItem = new JMenuItem("Add potentials");
        srcItem.setFont(currentFont);
        srcItem.addActionListener(e -> modifySources());
        setupMenu.add(srcItem);
        JMenuItem clrsrcItem = new JMenuItem("Clear potentials");
        clrsrcItem.setFont(currentFont);
        clrsrcItem.addActionListener(e -> clearSources());
        setupMenu.add(clrsrcItem);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.add(new JMenuItem("Font size"));
        for (Font f : fonts) {
            JRadioButtonMenuItem fontOpt = new JRadioButtonMenuItem("\t\t\t" + String.valueOf(f.getSize()));
            final Font cf = f;
            fontOpt.addActionListener(e -> {
                currentFont = cf;
                setFontRecursively(this, currentFont, 0);
                UIManager.put("OptionPane.messageFont", currentFont);
                UIManager.put("OptionPane.buttonFont", currentFont);
                UIManager.put("OptionPane.messageFont", currentFont);
            });
            fontOpt.setSelected(f == currentFont);
            optionsMenu.add(fontOpt);
        }

        menuBar.add(fileMenu);
        menuBar.add(setupMenu);
        menuBar.add(optionsMenu);

        setJMenuBar(menuBar);
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser(getLastUsedDirectory());
        setFontRecursively(fileChooser, currentFont, 0);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File circuitFile = fileChooser.getSelectedFile();
            try {
                circ = CircuitIO.readPassiveResistiveCircuit(circuitFile.getAbsolutePath());
                saveLastUsedDirectory(circuitFile.getParent());
                sources.clear();
                currentSelection.clear();
                canvasPanel.repaint();
                message.setText("Circuit loaded from: " + circuitFile.getAbsolutePath() + "\n" + circ.noNodes() + " nodes");
            } catch (Exception e) {
                circ = null;
                JOptionPane.showMessageDialog(this, "Unable to load circuit from: " + circuitFile.getAbsolutePath());
            }
        }
    }

    private void saveCircuit() {
        JFileChooser fileChooser = new JFileChooser(getLastUsedDirectory());
        setFontRecursively(fileChooser, currentFont, 0);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                CircuitIO.savePassiveResistiveCircuit(circ, fileChooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Saved as: " + fileChooser.getSelectedFile().getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void doExit() {
        System.exit(0);
    }

    private void clearSources() {
        sources.clear();
        currentSelection.clear();
        canvasPanel.repaint();
    }

    private void modifySources() {
        if (options.get("inDefBoundary")) {
            if (currentSelection.isEmpty()) {
                if (JOptionPane.showConfirmDialog(this, "No nodes selected, want to try again?",
                        DEFAULT_BND_TEXT, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    options.put("inDefBoundary", false);
                    message.setText("OK");
                    return;
                }
            } else {
                double value;
                try {
                    String m = JOptionPane.showInputDialog(this, "Value?", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                    if (m == null) {
                        throw new NumberFormatException();
                    }
                    value = Double.parseDouble(m);
                    for (Integer v : currentSelection) {
                        sources.put(v, value);
                    }
                    options.put("inDefBoundary", false);
                    message.setText("OK");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid value, click the button once more.", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                }
                if (printDiag) {
                    System.err.println(sources);
                }
            }
            canvasPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, """
                                                    Click on the node to select it, click on selected to unselect,
                                                    drag mouse to select/deselect all nodes in the rectangle
                                                    when done click "Setup->Add potentials" again or press ESC
                                                    to be asked for the value.""", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
            
            options.put("inDefBoundary", true);
            currentSelection.clear();
        }
    }

    // Helper - retrieves the last used directory from the config file
    private String getLastUsedDirectory() {
        String lsd = configuration.getValue(LAST_DIR);
        if (lsd == null) {
            lsd = ".";
        }
        return lsd;
    }

    // Helper - saves the last used directory
    private void saveLastUsedDirectory(String directory) {
        try {
            configuration.saveValue(LAST_DIR, directory);
        } catch (IOException e) {
            message.setText(e.getLocalizedMessage());
        }
    }

    private void createCanvasPanel() {
        canvasPanel = new JPanel() {
            private int prevX, prevY;
            private int currX, currY;
            private int vertexSelectionRadius;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (options.get("inDefBoundary")) {
                            int nearestVertex = findNearestVertex(prevX, prevY);
                            if (nearestVertex >= 0) {
                                if (printDiag) {
                                    System.err.println(prevX + " " + prevY + " => " + nearestVertex);
                                }
                                if (currentSelection.contains(nearestVertex)) {
                                    currentSelection.remove(nearestVertex);
                                } else {
                                    currentSelection.add(nearestVertex);
                                }
                                message.setText("selected nodes: " + currentSelection.toString());
                            }
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        prevX = e.getX();
                        prevY = e.getY();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        currX = e.getX();
                        currY = e.getY();
                        repaint();
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        currX = e.getX();
                        currY = e.getY();
                        repaint();
                    }
                });
            }

            // Helper -finds vertex nearest to (x,y) - clicked by the mouse
            private int findNearestVertex(int x, int y) {
                int nV = -1;
                int minDistance = Integer.MAX_VALUE;
                for (int v = 0; v < xy.size(); v++) {
                    PointPosition p = xy.get(v);
                    int distance = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                    if (distance < minDistance && distance < vertexSelectionRadius) {
                        minDistance = distance;
                        nV = v;
                    }
                }
                return nV;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int nr = 1;
                if (circ == null) {
                    return;
                } else {
                    while (circ.resistance(nr - 1, nr) < Double.POSITIVE_INFINITY) {
                        nr++;
                    }
                    //System.out.println("nr=" + nr );
                }

                xy.clear();
                int width = getWidth();
                int height = getHeight();
                int margin = Math.min(width / 10, height / 10);
                int nc = circ.noNodes() / nr;
                int dist = Math.min((width - 2 * margin) / nc, (height - 2 * margin) / nr);
                vertexSelectionRadius = dist / 3;
                for (int i = 0; i < circ.noNodes(); i++) {
                    int x = margin + dist * (i / nr);
                    int y = margin + dist * (i % nr);
                    g.drawOval(x - 3, y - 3, 6, 6);
                    xy.add(new PointPosition(x, y));
                }
                for (int i = 0; i < circ.noNodes(); i++) {
                    Set<Integer> nbrs = circ.neighbourNodes(i);
                    for (Integer j : nbrs) {
                        if (i < j && circ.resistance(i, j) != Double.POSITIVE_INFINITY) {
                            drawResistor(g, margin, dist, i, j, nr);
                        }
                    }
                }

                if (options.get("inDefBoundary") && (currX != prevX || currY != prevY)) {
                    int xp = Math.min(currX, prevX);
                    int yp = Math.min(currY, prevY);
                    int rw = Math.abs(currX - prevX);
                    int rh = Math.abs(currY - prevY);
                    g.setColor(Color.ORANGE);
                    g.drawRect(xp, yp, rw, rh);
                    for (int v = 0; v < xy.size(); v++) {
                        PointPosition p = xy.get(v);
                        if (p.x >= xp && p.x <= xp + rw && p.y >= yp && p.y <= yp + rh) {
                            if (currentSelection.contains(v)) {
                                currentSelection.remove(v);
                            } else {
                                currentSelection.add(v);
                            }
                        }
                    }
                    message.setText("selected nodes: " + currentSelection.toString());
                    prevX = currX;
                    prevY = currY;
                }

                g.setColor(Color.BLUE);
                g.setFont(currentFont);
                for (int v = 0; v < circ.noNodes(); v++) {
                    PointPosition p = xy.get(v);
                    if (options.get("inDefBoundary") && currentSelection.contains(v)) {
                        g.setColor(Color.RED);
                        g.fillOval(p.x - 3, p.y - 3, 6, 6);
                        g.setColor(Color.BLUE);
                    } else {
                        g.fillOval(p.x - 3, p.y - 3, 6, 6);
                    }
                }

                g.setColor(Color.RED);
                int dh = g.getFontMetrics().getHeight();
                for (Integer b : sources.keySet()) {
                    PointPosition p = xy.get(b);
                    g.drawString(String.valueOf(sources.get(b)), p.x, p.y + dh);
                }

            }
        };

        canvasPanel.setBackground(Color.WHITE);
        canvasPanel.setPreferredSize(new Dimension(1000, 650));
    }

    private void drawResistor(Graphics g, int margin, int dist, int i, int j, int nr) {
        int lead = dist / 5;          // długość przewodów
        int bodyHeight = dist - 2 * lead;
        int bodyWidth = dist / 5;     // szerokość prostokąta
        int xi = margin + dist * (i / nr);
        int yi = margin + dist * (i % nr);
        int xj = margin + dist * (j / nr);
        int yj = margin + dist * (j % nr);
        if (xi == xj) { // vertical
            //System.out.println("V" + i + "-" + j );
            int rectY = yi + lead;
            // przewód górny
            g.drawLine(xi, yi, xi, rectY);
            // prostokąt (rezystor)
            g.drawRect(xi - bodyWidth / 2, rectY, bodyWidth, bodyHeight);
            // przewód dolny
            g.drawLine(xi, rectY + bodyHeight, xi, yj);
        } else { // horizontal
            //System.out.println("H" + i + "-" + j );
            int rectX = xi + lead;
            // przewód górny
            g.drawLine(xi, yi, rectX, yj);
            // prostokąt (rezystor)
            g.drawRect(rectX, yi - bodyWidth / 2, bodyHeight, bodyWidth);
            // przewód dolny
            g.drawLine(rectX + bodyHeight, yi, xj, yj);
        }
    }

    private void createBottomPanel() {
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        message = new JLabel("");
        bottomPanel.add(message);

        // Przykładowe elementy - możesz usunąć
        /*
        bottomPanel.add(new JLabel("Input:"));
        bottomPanel.add(new JTextField(20));
        bottomPanel.add(new JButton("OK"));
        bottomPanel.add(new JButton("Cancel"));
         */
    }

    public JPanel getCanvasPanel() {
        return canvasPanel;
    }

    public JPanel getBottomPanel() {
        return bottomPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NodalGUI gui = new NodalGUI();
            gui.setVisible(true);
        });
    }

    // Helper holding integer (relative to drawing window) coordinates of a Vertex
    class PointPosition {

        int x;
        int y;

        public PointPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
