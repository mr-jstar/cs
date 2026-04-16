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
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import prc.CircuitFactory;
import prc.CircuitIO;
import prc.PassiveResistiveCircuit;
import solvers.NodalSolver;

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
    private double[] V;
    private double tolerance = 1e-6;
    private int maxit = 100;
    private int nThreads = Runtime.getRuntime().availableProcessors();

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

        JMenu fileMenu = new JMenu("Circuit");
        JMenuItem readItem = new JMenuItem("Read circuit");
        readItem.setFont(currentFont);
        readItem.addActionListener(e -> loadFile());
        fileMenu.add(readItem);
        JMenuItem makeItem = new JMenuItem("Generate circuit");
        makeItem.setFont(currentFont);
        makeItem.addActionListener(e -> generateCircuit());
        fileMenu.add(makeItem);
        JMenuItem saveItem = new JMenuItem("Save circuit");
        saveItem.setFont(currentFont);
        saveItem.addActionListener(e -> saveCircuit());
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setFont(currentFont);
        exitItem.addActionListener(e -> doExit());
        fileMenu.add(exitItem);

        JMenu circuitMenu = new JMenu("Analysis");
        JMenuItem srcItem = new JMenuItem("Add potentials");
        srcItem.setFont(currentFont);
        srcItem.addActionListener(e -> modifySources());
        circuitMenu.add(srcItem);
        JMenuItem clrsrcItem = new JMenuItem("Clear potentials");
        clrsrcItem.setFont(currentFont);
        clrsrcItem.addActionListener(e -> clearSources());
        circuitMenu.add(clrsrcItem);
        circuitMenu.addSeparator();
        JMenuItem solveItem = new JMenuItem("Solve");
        solveItem.setFont(currentFont);
        solveItem.addActionListener(e -> solve());
        circuitMenu.add(solveItem);

        JMenu optionsMenu = new JMenu("Options");
        JMenuItem tolItem = new JMenuItem("Tolerance (solver)");
        tolItem.setFont(currentFont);
        tolItem.addActionListener(e -> {
            tolerance = Double.parseDouble(readSomething("Value=", "Tolerance", "" + tolerance));
        });
        optionsMenu.add(tolItem);
        JMenuItem maxitItem = new JMenuItem("Max # of iterations (solver)");
        maxitItem.setFont(currentFont);
        maxitItem.addActionListener(e -> {
            maxit = Integer.parseInt(readSomething("Number=", "Max # of iterations", "" + maxit));
        });
        optionsMenu.add(maxitItem);
        JMenuItem nthreadsItem = new JMenuItem("# of parallel threads");
        nthreadsItem.setFont(currentFont);
        nthreadsItem.addActionListener(e -> {
            nThreads = Integer.parseInt(readSomething("Number=", "# of parallel threads", "" + nThreads));
        });
        optionsMenu.add(nthreadsItem);
        optionsMenu.addSeparator();
        optionsMenu.add(new JMenuItem("Font size"));
        ButtonGroup fgroup = new ButtonGroup();
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
            fgroup.add(fontOpt);
            optionsMenu.add(fontOpt);
        }

        menuBar.add(fileMenu);
        menuBar.add(circuitMenu);
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
                V = null;
                maxit = 10 * circ.noNodes();
                sources.clear();
                currentSelection.clear();
                canvasPanel.repaint();
                saveLastUsedDirectory(circuitFile.getParent());
                message.setText("Circuit loaded from: " + circuitFile.getAbsolutePath() + "\n" + circ.noNodes() + " nodes");
            } catch (Exception e) {
                circ = null;
                JOptionPane.showMessageDialog(this, "Unable to load circuit from: " + circuitFile.getAbsolutePath());
            }
        }
    }

    private void generateCircuit() {
        try {
            String params = readSomething("Enter col# row# Rmin Rmax", "Make grid circuit", "10 5  1.0  2.0");
            String [] f = params.split("\\s+");
            circ = CircuitFactory.makeGridRCircuit(
                    Integer.parseInt(f[0]), 
                    Integer.parseInt(f[1]), 
              Double.parseDouble(f[2]), 
              Double.parseDouble(f[3])
                    );
            maxit = 10 * circ.noNodes();
        } catch (Exception ex) {
            message.setText("Bad parameters");
            circ = null;
        }
        V = null;
        sources.clear();
        currentSelection.clear();
        canvasPanel.repaint();
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
        if (circ == null) {
            return;
        }
        V = null;
        sources.clear();
        currentSelection.clear();
        canvasPanel.repaint();
    }

    private void modifySources() {
        if (circ == null) {
            return;
        }
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
                    String m = readSomething("Value?", DEFAULT_BND_TEXT, "");
                    //JOptionPane.showInputDialog(this, "Value?", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                    if (m == null) {
                        throw new NumberFormatException();
                    }
                    value = Double.parseDouble(m);
                    for (Integer v : currentSelection) {
                        sources.put(v, value);
                    }
                    currentSelection.clear();
                    V = null;
                    options.put("inDefBoundary", false);
                    message.setText("OK");
                } catch (NumberFormatException e) {
                    options.put("inDefBoundary", false);
                    JOptionPane.showMessageDialog(this, "Invalid value, try again.", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
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

    public void solve() {
        if (circ != null && sources.size() > 1) {
            int[] srcNodes = new int[sources.size()];
            double[] srcValues = new double[sources.size()];
            int i = 0;
            for (int src : sources.keySet()) {
                srcNodes[i] = src;
                srcValues[i] = sources.get(src);
                i++;
            }

            NodalSolver s = new NodalSolver(circ, null, srcNodes, srcValues);
            Thread solver = new Thread() {
                @Override
                public void run() {
                    message.setText("Solving...");
                    try {
                        s.solveInParallel(tolerance, maxit, nThreads);
                        //System.out.println("solver with " + nThreads + " threads for maxit=" + maxit + " and tol=" + tolerance + " has finished.");
                    } catch (Exception ex) {
                        message.setText("The solver was interrupted: " + ex.getMessage());
                        return;
                    }
                    V = s.getPotential();
                    canvasPanel.repaint();
                    List<Double> err = s.err();
                    if (err.isEmpty() || err.get(err.size() - 1) > tolerance) {
                        message.setText("The solver failed to converge, err=" + err.get(err.size() - 1));
                    } else {
                        message.setText("The solver converged in " + err.size() + " iterations.");
                    }
                }
            };
            solver.start();
        }
    }

    // Helper - open dialog and read something
    private String readSomething(String prompt, String title, String defaultValue) {
        try {
            JTextField field = new JTextField(defaultValue);
            field.setFont(currentFont);

            JOptionPane pane = new JOptionPane(
                    new Object[]{prompt, field},
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION
            );

            JDialog dialog = pane.createDialog(title);
            dialog.setVisible(true);
            String m = field.getText();
            if (m == null) {
                throw new NumberFormatException();
            }
            return m;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid value.", prompt, JOptionPane.QUESTION_MESSAGE);
            return defaultValue;
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

    // Helper - get range values in a double vector
    private static double[] range(double[] v) {
        double[] range = {v[0], v[0]};
        for (int i = 1; i < v.length; i++) {
            if (v[i] < range[0]) {
                range[0] = v[i];
            }
            if (v[i] > range[1]) {
                range[1] = v[i];
            }
        }
        return range;
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
            private int vertexSelectionCircle;

            JWindow tooltip = new JWindow(NodalGUI.this);
            JLabel label = new JLabel("");
            Timer hideTimer = new Timer(2000, e -> tooltip.setVisible(false));

            {
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                tooltip.add(label);
                tooltip.pack();
                hideTimer.setRepeats(false);
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
                        if (tooltip.isVisible()) {
                            tooltip.setVisible(false);
                            hideTimer.stop();
                        }
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            //System.out.println("(" + e.getX() + "," + e.getY() + ")");
                            int nV = findNearestVertex(e.getX(), e.getY());
                            //System.out.println(elementData);
                            if (V != null && nV >= 0) {
                                message.setText("V=" + V[nV]);
                                label.setText("V=" + V[nV]);
                                label.setFont(currentFont);
                                tooltip.pack();
                                Point pt = e.getLocationOnScreen();
                                tooltip.setLocation(pt.x, pt.y);
                                tooltip.setVisible(true);
                                hideTimer.restart();
                            }
                        }
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            //System.out.println("(" + e.getX() + "," + e.getY() + ")");
                            String elementData = findElement(e.getX(), e.getY());
                            //System.out.println(elementData);
                            if (elementData != null) {
                                message.setText(elementData);
                                label.setText(elementData);
                                label.setFont(currentFont);
                                tooltip.pack();
                                Point pt = e.getLocationOnScreen();
                                tooltip.setLocation(pt.x, pt.y);
                                tooltip.setVisible(true);
                                hideTimer.restart();
                            }
                        }
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
                //System.out.print("circle=" + vertexSelectionCircle + " ");
                int nV = -1;
                int minDistance = Integer.MAX_VALUE;
                for (int v = 0; v < xy.size(); v++) {
                    PointPosition p = xy.get(v);
                    int distance = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                    if (distance < minDistance && distance < vertexSelectionCircle) {
                        minDistance = distance;
                        nV = v;
                    }
                }
                return nV;
            }

            // Helper -finds vertex nearest to (x,y) - clicked by the mouse
            private String findElement(int x, int y) {
                int nV = findNearestVertex(x, y);
                //System.out.println("nV=" + nV);
                if (nV >= 0) {
                    int minDistance = Integer.MAX_VALUE;
                    int sV = nV;
                    for (int n : circ.neighbourNodes(nV)) {
                        PointPosition p = xy.get(n);
                        int distance = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                        if (distance < minDistance && distance < 3 * vertexSelectionCircle) {
                            minDistance = distance;
                            sV = n;
                        }
                    }
                    //System.out.println("sV=" + sV);
                    if (sV > -1 && sV != nV) {
                        double R = circ.resistance(nV, sV);
                        return "R= " + R + (V != null && R > 0 && R != Double.POSITIVE_INFINITY ? " I=" + Math.abs(V[sV] - V[nV]) / R : "");
                    }
                }
                return null;
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
                vertexSelectionCircle = 9 * dist * dist / 4;  // (0.75*dist)^2
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

                if (V != null) {
                    double[] frng = range(V);
                    ColorMap cm = new ColorMap((float) frng[0], (float) frng[1]);
                    for (int n = 0; n < circ.noNodes(); n++) {
                        PointPosition p = xy.get(n);
                        g.setColor(cm.getColorForValue(V[n]));
                        g.fillOval(p.x - 3, p.y - 3, 6, 6);
                    }
                    for (int i = 0; i < circ.noNodes(); i++) {
                        for (int n : circ.neighbourNodes(i)) {
                            if (n > i) {
                                fillResistor(g, margin, dist, i, n, nr, cm);
                            }
                        }
                    }
                    // The legend
                    int lwidth = (int) (0.7 * getWidth());
                    int lheight = 20;
                    int bottomMargin = 20;
                    int xmargin = 3;
                    int pxl = (int) (0.15 * getWidth());
                    int pyl = getHeight() - lheight - bottomMargin;
                    Image legend = cm.createColorScaleImage(lwidth, lheight, ColorMap.Menu.HORIZONTAL);

                    g.drawImage(legend, pxl, pyl, this);
                    g.setColor(Color.BLACK);
                    String low = String.format("%.3g", frng[0]);

                    g.drawString(low, pxl - getFontMetrics(currentFont).stringWidth(low) - xmargin, pyl + 3 * lheight / 4);
                    String high = String.format("%.3g", frng[1]);
                    g.drawString(high, pxl + lwidth + xmargin, pyl + 3 * lheight / 4);
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

    private void fillResistor(Graphics g, int margin, int dist, int i, int j, int nr, ColorMap cm) {
        int lead = dist / 5;          // długość przewodów
        int bodyHeight = dist - 2 * lead;
        int bodyWidth = dist / 5;     // szerokość prostokąta
        int xi = margin + dist * (i / nr);
        int yi = margin + dist * (i % nr);
        int xj = margin + dist * (j / nr);
        int yj = margin + dist * (j % nr);
        if (xi == xj) { // vertical
            //System.out.println("V" + i + "-" + j );
            int dy = (int) Math.signum(yj - yi);
            for (int h = 0; h < bodyHeight; h += dy) {
                double vh = V[i] + (V[j] - V[i]) * h / bodyHeight;
                if (vh < 0 || vh > 1.0) {
                    System.out.println("v=" + vh);
                }
                g.setColor(cm.getColorForValue(vh));
                g.drawRect(xi - bodyWidth / 2, h + yi + lead, bodyWidth, 1);
            }
        } else { // horizontal
            //System.out.println("H" + i + "-" + j );
            int dx = (int) Math.signum(xj - xi);
            for (int w = 0; w < bodyHeight; w += dx) {
                double vw = V[i] + (V[j] - V[i]) * w / bodyHeight;
                if (vw < 0 || vw > 1.0) {
                    System.out.println("v=" + vw);
                }
                g.setColor(cm.getColorForValue(vw));
                g.drawRect(w + xi + lead, yi - bodyWidth / 2, 1, bodyWidth);
            }
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
