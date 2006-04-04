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
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
/**
 *
 * @author Rick Goldberg
 */
public class QstatConsole extends JFrame implements ActionListener, WindowListener {
    String usid;
    String uname;
    String passwd;
    String host;
    String port;
    int delay = 0;
    boolean paused = true;
    public static boolean showing = false; // only want one of these up at a time
    JLabel sliderLabel;
    JPanel sliderPanel;
    JFormattedTextField framesPerMin;
    JTextArea textArea;
    Timer timer;
    XmlRpcClientLite xmlrpc;
    Vector args;
    
    /** Creates a new instance of QstatConsole */
    public QstatConsole(String uname, String passwd, String host, String port) {
        super();
        this.uname=uname;
        this.passwd=passwd;
        this.host=host;
        this.port=port;
        args = new Vector();
        try {
            xmlrpc = new XmlRpcClientLite(host,Integer.parseInt(port));
        } catch (Exception e) {
            xmlrpc = null;
        }
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
        sliderPanel = new FrameRateSliderPanel(0,60);
        timer = new Timer(60000,this);
        timer.stop();
        textArea = new JTextArea(40,80);
        if (xmlrpc != null) {
            textArea.setText("Qstatus for "+host+":"+port);
        } else {
            textArea.setText("Gridkit cluster "+host+" XML-RPC unreachable at port "+port+". \nIt is possible the network is down, you are firewalled, or the service requires a restart (check with cluster admin). Please exit this window and try again.");
        }
        add(sliderPanel);
        add(textArea);
        pack();
        setVisible(true);
    }

    public void windowActivated(WindowEvent windowEvent) {
    }

    public void windowClosed(WindowEvent windowEvent) {
        showing = false;
    }

    public void windowClosing(WindowEvent windowEvent) {
        logout();
    }

    public void windowDeactivated(WindowEvent windowEvent) {
    }

    public void windowDeiconified(WindowEvent windowEvent) {
    }

    public void windowIconified(WindowEvent windowEvent) {
    }

    public void windowOpened(WindowEvent windowEvent) {
        showing = true;
    }
    
    void pauseQstat() {
        paused = true;
        timer.stop();
        logout();
    }
    
    void unPauseQstat() {
        paused = false;
        login();
        timer = new Timer(delay,this);
        timer.start();
    }
    
    void login() {
        if (xmlrpc != null) {
            args.clear();
            args.add(uname);
            args.add(passwd);
            try {
                usid = (String) xmlrpc.execute("gridkit.login",args);
            } catch (Exception e) {
                usid = "Can't connect to server";
            }
            textArea.setText(textArea.getText()+ "\n login session cookie: " + usid + "\n");
        }
    }
    
    void logout() {
        if (xmlrpc != null) {
            args.clear();
            args.add(usid);
            try {
                xmlrpc.execute("gridkit.logout",args);
                textArea.setText(textArea.getText()+"\n qstat session ended\n");
            } catch (Exception e) {
                textArea.setText(textArea.getText()+"\n can't end qstat session");
            }
            
        }
        
    }
   
    // Timer event
    public void actionPerformed(ActionEvent actionEvent) {
        textArea.setText((new java.util.Date()).toString()+"\n");
        if (xmlrpc != null) {
            args.clear();
            args.add(usid);
            try {
                textArea.setText(textArea.getText()+"\n"+(String)(xmlrpc.execute("gridkit.qstat",args)));
            } catch (Exception e) {
                textArea.setText("Can't qstat!");
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
            slider = new JSlider(JSlider.HORIZONTAL, min , max, 0);
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.addChangeListener(this);
            slider.setToolTipText("Adjust number of queries per minute to qstat at the front end");
            sliderLabel = new JLabel("Queries Per Minute", JLabel.CENTER);
            sliderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            NumberFormat numberFormat =
                    java.text.NumberFormat.getIntegerInstance();
            NumberFormatter formatter = new NumberFormatter(numberFormat);
            formatter.setMinimum(new Integer(min));
            formatter.setMaximum(new Integer(max));
            framesPerMin = new JFormattedTextField(formatter);
            framesPerMin.setValue(new Integer(0));
            framesPerMin.setColumns(2);
            framesPerMin.getInputMap().put(KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ENTER, 0),
                                        "check");
            framesPerMin.getActionMap().put("check", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (!framesPerMin.isEditValid()) {
                        Toolkit.getDefaultToolkit().beep();
                        framesPerMin.selectAll();
                    } else try {
                        framesPerMin.commitEdit();
                        slider.setValue(Integer.parseInt(framesPerMin.getText()));
                    } catch (java.text.ParseException exc) { }
                }
            });

            add(sliderLabel);
            add(framesPerMin);
            add(slider);            

        }
        
        public void stateChanged(ChangeEvent changeEvent) {
            JSlider source = (JSlider)changeEvent.getSource();
            if (!source.getValueIsAdjusting()) {
                int fpm = (int)source.getValue();
                if (fpm == 0) {
                    timer.setDelay(60000); // just set it big, will be paused anyway
                    pauseQstat();
                } else {
                    delay = 60000 / fpm;
                    timer.setDelay(delay);
                    if(paused) {
                        unPauseQstat();
                    }
                }          
                framesPerMin.setValue(new Integer(fpm));
            }

        }
    
    }
}
