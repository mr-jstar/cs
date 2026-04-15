package gui;

/**
 *
 * @author jstar
 */
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;
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

    private PassiveResistiveCircuit circ;

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
        initGui();
    }

    private void initGui() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createMenuBar();
        createCanvasPanel();
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

        JMenu setupMenu = new JMenu("Setup");
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

    // Action for Load mesh button/menu item
    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser(getLastUsedDirectory());
        setFontRecursively(fileChooser, currentFont, 0);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File circuitFile = fileChooser.getSelectedFile();
            try {
                circ = CircuitIO.readPassiveResistiveCircuit(circuitFile.getAbsolutePath());
                saveLastUsedDirectory(circuitFile.getParent());
                canvasPanel.repaint();
                message.setText("Circuit loaded from: " + circuitFile.getAbsolutePath() + "\n" + circ.noNodes() + " nodes");
            } catch (Exception e) {
                circ = null;
                JOptionPane.showMessageDialog(this, "Unable to load circuit from: " + circuitFile.getAbsolutePath());
            }
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
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int nr = 1;
                if (circ == null) {
                    return;
                } else {
                    while( circ.resistance(nr-1, nr) < Double.POSITIVE_INFINITY )
                        nr++;
                    //System.out.println("nr=" + nr );
                }

                int width = getWidth();
                int height = getHeight();
                int margin = Math.min(width / 10, height / 10);
                int nc = circ.noNodes() / nr;
                int dist = Math.min((width - 2 * margin) / nc, (height - 2 * margin) / nr);
                for (int i = 0; i < circ.noNodes(); i++) {
                    int x = margin + dist * (i / nr);
                    int y = margin + dist * (i % nr);
                    g.drawOval(x-3, y-3, 6, 6);
                }
                for (int i = 0; i < circ.noNodes(); i++) {
                    Set<Integer> nbrs = circ.neighbourNodes(i);
                    for (Integer j : nbrs) {
                        if (i < j && circ.resistance(i, j) != Double.POSITIVE_INFINITY) {
                            drawResistor(g, margin, dist, i, j, nr);
                        }
                    }
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
}
