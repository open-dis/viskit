/*
 * QstatConsole.java
 *
 * Created on April 3, 2006, 11:47 AM
 *
 * Poll the SGE Queue at a selectable rate.
 *
 */

package viskit.doe;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import javax.swing.text.NumberFormatter;

/**
 * @author Rick Goldberg
 */
public class QstatConsole extends JFrame implements ActionListener, WindowListener {
    DoeRunDriver doe;
    int delay = 0;
    boolean paused = true;
    JLabel sliderLabel;
    JPanel sliderPanel;
    JFormattedTextField framesPerMin;
    JTextArea textArea;
    JScrollPane scrollPane;
    Timer timer;
    int tsize=-1;
    static final int MIN_ROWS = 30;
    static final int MIN_FPM = 0;
    static final int MAX_FPM = 60;
    static final int INIT_FPM = 0; // should always start at 0
    /**
     * Creates a new instance of QstatConsole
     */
    private JPanel content;
    
    public QstatConsole() {
        super("Viskit Grid Queue Status Console");
        
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        sliderPanel = new FrameRateSliderPanel(MIN_FPM, MAX_FPM);
        timer = new Timer(MAX_FPM * 1000, this);
        timer.stop();
        reset(); // also gets new textArea
        scrollPane = new JScrollPane(textArea);
        content.add(sliderPanel);
        content.add(Box.createVerticalStrut(5));
        content.add(scrollPane);
        textArea.setForeground(Color.GREEN);
        textArea.setBackground(Color.BLACK);
        pack();
        //setVisible(true);
        
    }
    
    public void setDoe(DoeRunDriver doe) {
        this.doe = doe;
    }
    
    public Container getContent() {
        return content;
    }
    
    @Override
    public void windowActivated(WindowEvent windowEvent) {
        
    }
    
    @Override
    public void windowClosed(WindowEvent windowEvent) {
        
    }
    
    @Override
    public void windowClosing(WindowEvent windowEvent) {
        pauseQstat();
        reset();
    }
    
    @Override
    public void windowDeactivated(WindowEvent windowEvent) {
    }
    
    @Override
    public void windowDeiconified(WindowEvent windowEvent) {
        
    }
    
    @Override
    public void windowIconified(WindowEvent windowEvent) {
    }
    
    @Override
    public void windowOpened(WindowEvent windowEvent) {
        
    }
    
    void pauseQstat() {
        paused = true;
        timer.stop();
    }
    
    void unPauseQstat() {
        paused = false;
        timer = new Timer(delay, this);
        timer.start();
    }
    
    void reset() {
        ((FrameRateSliderPanel) sliderPanel).reset();
        if (textArea == null) {
            textArea = new JTextArea(30, 80);
        }
        if (doe != null) {
            textArea.setText("Ready");
        } else {
            textArea.setText("Awaiting run");
        }
    }
    
    // Timer event
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        textArea.setText((new java.util.Date()).toString() + "\n");
        if (doe != null) {
            
            try {
                String data = doe.qstat();
                int lines = data.split("\\n").length;
                textArea.setRows(lines > MIN_ROWS ? lines : MIN_ROWS);
                textArea.setText(textArea.getText() + "\n" + data);
               
            } catch (Exception e) {
                textArea.setText("Can't qstat! \n" + e.toString());
            }
        }
    }
    
    class FrameRateSliderPanel extends JPanel implements ChangeListener {
        private JSlider slider;
        private JLabel sliderLabel;
        
        FrameRateSliderPanel(int min, int max) {
            super();
            setLayout(new FlowLayout());
            setBorder(new EtchedBorder());
            setMaximumSize(new java.awt.Dimension(400, 60));
            setPreferredSize(new java.awt.Dimension(400, 60));
            slider = new JSlider(JSlider.HORIZONTAL, min, max, INIT_FPM);
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(2);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.addChangeListener(this);
            slider.setToolTipText("Adjust number of queries per minute to qstat at the front end");
            sliderLabel = new JLabel("Queries Per Minute", JLabel.CENTER);
            sliderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            NumberFormat numberFormat =
                    java.text.NumberFormat.getIntegerInstance();
            NumberFormatter formatter = new NumberFormatter(numberFormat);
            formatter.setMinimum(min);
            formatter.setMaximum(max);
            framesPerMin = new JFormattedTextField(formatter);
            framesPerMin.setValue(INIT_FPM);
            framesPerMin.setColumns(2);
            framesPerMin.getInputMap().put(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ENTER, 0),
                    "check");
            framesPerMin.getActionMap().put("check", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!framesPerMin.isEditValid()) {
                        Toolkit.getDefaultToolkit().beep();
                        framesPerMin.selectAll();
                    } else try {
                        framesPerMin.commitEdit();
                        slider.setValue(Integer.parseInt(framesPerMin.getText()));
                    } catch (java.text.ParseException exc) {
                    }
                }
            });
            
            add(sliderLabel);
            add(framesPerMin);
            add(slider);
            
        }
        
        public Container getContent() {
            return getContentPane();
        }
        
        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            JSlider source = (JSlider) changeEvent.getSource();
            int fpm = source.getValue();
            if (!source.getValueIsAdjusting()) {
                if (fpm == 0) {
                    timer.setDelay(MAX_FPM * 1000); // just set it big, will be paused anyway
                    pauseQstat();
                } else {
                    delay = MAX_FPM * 1000 / fpm;
                    timer.setDelay(delay);
                    if (paused) {
                        unPauseQstat();
                    }
                }
                
            }
            framesPerMin.setValue(fpm);
            
        }
        
        void reset() {
            framesPerMin.setValue(INIT_FPM);
            slider.setValue(INIT_FPM);
        }
        
    }
    
}
