package viskit.view;

import edu.nps.util.LogUtils;
import edu.nps.util.SpringUtilities;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.ViskitStatics;
import viskit.model.VInstantiator;
import viskit.xsd.bindings.eventgraph.Parameter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects MOVES Institute
 * Naval Postgraduate School, Monterey, CA www.nps.edu
 *
 * @author Mike Bailey
 * @since Jun 8, 2004
 * @since 8:31:41 AM
 * @version $Id$
 */
public class InstantiationPanel extends JPanel implements ActionListener, CaretListener {

    private static final int FREEFORM = 0, CONSTRUCTOR = 1, FACTORY = 2;

    private JLabel typeLabel, methodLabel;

    private JTextField typeTF;

    private JComboBox<String> methodCB;

    private JPanel instantiationPane;

    private CardLayout instantiationPaneLayoutManager;

    private FreeFormPanel freeFormPanel;

    private ConstrPanel constructorPanel;

    private FactoryPanel     factoryPanel;

    private ActionListener modifiedListener;

    private JDialog packMeOwnerDialog;

    boolean constructorOnly = false;

    public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener) {
        this(ownerDialog, changedListener, false);
    }

    public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener, boolean onlyConstructor) {
        this(ownerDialog, changedListener, onlyConstructor, false);
    }

    public InstantiationPanel(final JDialog ownerDialog, ActionListener changedListener, boolean onlyConstructor, boolean typeEditable)
	{
        modifiedListener     = changedListener;
        packMeOwnerDialog    = ownerDialog;
        this.constructorOnly = onlyConstructor;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new SpringLayout());
        typeLabel = new JLabel("type", JLabel.TRAILING);
        typeTF    = new JTextField();
        typeTF.setEditable(typeEditable);
        typeTF.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                methodCB.actionPerformed(e);
            }
        });
        typeLabel.setLabelFor(typeTF);

        methodLabel = new JLabel("method", JLabel.TRAILING);

        methodCB = new JComboBox<>(new String[]{"free form", "constructor", "factory"});
        //or
        JTextField constructorTF = new JTextField("Constructor");
        constructorTF.setEditable(false);

        topPanel.add(typeLabel);
        topPanel.add(typeTF);
        topPanel.add(methodLabel);
        if (constructorOnly)
		{
            methodLabel.setLabelFor(constructorTF);
            topPanel.add(constructorTF);
        } else {
            methodLabel.setLabelFor(methodCB);
            topPanel.add(methodCB);
        }
        SpringUtilities.makeCompactGrid(topPanel, 2, 2, 10, 10, 5, 5);
        add(topPanel);

        instantiationPane = new JPanel();
        instantiationPaneLayoutManager = new CardLayout();
        instantiationPane.setLayout(instantiationPaneLayoutManager);

        instantiationPane.setBorder(BorderFactory.createEtchedBorder());
        instantiationPane.setAlignmentX(Box.CENTER_ALIGNMENT);

           freeFormPanel = new FreeFormPanel(this);
        constructorPanel = new   ConstrPanel(this); // don't try to rename this
            factoryPanel = new  FactoryPanel(this);

        instantiationPane.add(   freeFormPanel,    "freeFormPanel");
        instantiationPane.add(constructorPanel, "constructorPanel");
        instantiationPane.add(    factoryPanel,     "factoryPanel");

        add(Box.createVerticalStrut(5));
        add(instantiationPane);

        methodCB.addActionListener(new ActionListener()
		{
            int lastIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e)
			{
                if (!typeTF.getText().trim().equals(myVi.getType())) {
                    String newType = typeTF.getText().trim();
                    // update the panels
                    try {
                        freeFormPanel.setType(newType);
                        constructorPanel.setType(newType);
                            factoryPanel.setType(newType);
                    } catch (ClassNotFoundException cnfe) {
                        JOptionPane.showMessageDialog(InstantiationPanel.this, "Unknown type: " + cnfe );
                        return;
                    }
                    freeFormPanel.setData(new VInstantiator.FreeForm(newType, ""));
                        factoryPanel.setData(new VInstantiator.Factory(newType,
                            ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS,
                            ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                            new Vector<>()
                    ));
                }
                int selectedIndex = methodCB.getSelectedIndex();
                if (lastIndex != selectedIndex) {
                    if (modifiedListener != null) {
                        modifiedListener.actionPerformed(new ActionEvent(methodCB, 0, "modified"));
                    }
                }
                switch (selectedIndex) {
                    case FREEFORM:
                        instantiationPaneLayoutManager.show(instantiationPane, "freeFormPanel");
                        freeFormPanel.valueTF.requestFocus();
                        freeFormPanel.valueTF.selectAll();
                        break;
                    case CONSTRUCTOR:
                        instantiationPaneLayoutManager.show(instantiationPane, "constructorPanel");
                        break;
                    case FACTORY:
                        instantiationPaneLayoutManager.show(instantiationPane, "factoryPanel");
                            factoryPanel.factoryClassCB.requestFocus();
                        break;
                    default:
                        System.err.println("bad data Instantiation panel");
                }
            }
        });
    }

    public VInstantiator getData() {
        switch (methodCB.getSelectedIndex()) {
            case FREEFORM:
                return freeFormPanel.getData();
            case CONSTRUCTOR:
                return constructorPanel.getData();
            case FACTORY:
                return     factoryPanel.getData();
            default:
                System.err.println("bad data Instantiation panel getData()");
                return null;
        }
    }

    VInstantiator myVi;

    public void setData(VInstantiator vi) throws ClassNotFoundException {
        myVi = vi.vcopy();
        String typeName = myVi.getType();
        typeTF.setText(typeName);

        // inform all panels of the type of the object
        constructorPanel.setType(typeName);
            factoryPanel.setType(typeName);
           freeFormPanel.setType(typeName);

        if (vi instanceof VInstantiator.Construct) {
            constructorPanel.setData((VInstantiator.Construct) myVi);
            methodCB.setSelectedIndex(CONSTRUCTOR);
        } else if (vi instanceof VInstantiator.Factory) {
                factoryPanel.setData((VInstantiator.Factory) myVi);
            methodCB.setSelectedIndex(FACTORY);
        } else if (vi instanceof VInstantiator.FreeForm) {
            freeFormPanel.setData((VInstantiator.FreeForm) myVi);
            methodCB.setSelectedIndex(FREEFORM);
        } else {
            System.err.println("Internal error InstantiationPanel.setData()");
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (modifiedListener != null)
		{
            modifiedListener.actionPerformed(null);
        }
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        actionPerformed(null);
    }

    /**
     * ********************************************************************
     */
    class FreeFormPanel extends JPanel implements CaretListener {

        private JTextField valueTF;

        private InstantiationPanel instantiationPanel;

        public FreeFormPanel(InstantiationPanel instantiationPanel)
		{
            this.instantiationPanel = instantiationPanel;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            valueTF = new JTextField("");
            valueTF.addCaretListener(FreeFormPanel.this);
            valueTF.setAlignmentX(Box.CENTER_ALIGNMENT);
            ViskitStatics.clampHeight(valueTF);

            add(valueTF);
            add(Box.createVerticalGlue());
        }

        public void setData(VInstantiator.FreeForm viff) {
            if (viff == null) {
                return;
            }
            valueTF.setText(viff.getValue());
        }

        String typeName;

        public void setType(String newTypeName) throws ClassNotFoundException {
            this.typeName = newTypeName;
            if (ViskitStatics.classForName(newTypeName) == null) // just to check exception
            {
                throw new ClassNotFoundException(newTypeName);
            }
        }

        public VInstantiator getData() {
            return new VInstantiator.FreeForm(typeName, valueTF.getText().trim());
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            if (instantiationPanel.modifiedListener != null) {
                instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "Textfield touched"));
            }
        }
    }

    /**
     * ********************************************************************
     */
    class ConstrPanel extends JPanel implements ActionListener, CaretListener {

        private JTabbedPane tabbedPane;

        private ConstructorPanel[] constructorPanels;

        private String noParametersString = "(no parameters)";

        private ImageIcon checkMark;

        private InstantiationPanel instantiationPanel;

        public ConstrPanel(InstantiationPanel instantiationPanel) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.instantiationPanel = instantiationPanel;
            tabbedPane = new JTabbedPane();
            checkMark = new ImageIcon(ClassLoader.getSystemResource("viskit/images/checkMark.png"));
        }

        String typeName;

        public void setType(String className) throws ClassNotFoundException
		{
            LogUtils.getLogger(InstantiationPanel.class).debug("Constructor for class " + className);
            List<Object>[] parameters = ViskitStatics.resolveParameters(ViskitStatics.classForName(className));
            typeName = className;
            removeAll();
            tabbedPane.removeAll();

            if (parameters == null)
			{
                tabbedPane.addTab("Constructor 0", null, new JLabel("No constructor, Factory, Abstract or Interface, "));
            } 
			else
			{
                constructorPanels = new ConstructorPanel[parameters.length];
                VInstantiator.Construct constructor;
                for (int i = 0; i < parameters.length; ++i) {

                    constructor = new VInstantiator.Construct(parameters[i], className);
                    String parametersSignature = noParametersString;
                    for (int j = 0; j < constructor.getArgs().size(); j++)
					{
                        parametersSignature += ((Parameter)parameters[i].get(j)).getType() + ", ";

                        if (!((VInstantiator) (constructor.getArgs().get(j))).getName().equals(((Parameter)parameters[i].get(j)).getName()))
                             ((VInstantiator) (constructor.getArgs().get(j))).setName(((Parameter)parameters[i].get(j)).getName());
                    }
                    parametersSignature = parametersSignature.substring(0, parametersSignature.length() - 2); // strip trailing comma

                    constructorPanels[i] = new ConstructorPanel(this, parameters.length != 1, this, packMeOwnerDialog);
                    constructorPanels[i].setData(constructor.getArgs());

                    tabbedPane.addTab("Constructor " + i, null, constructorPanels[i], parametersSignature);
                }
            }
            add(tabbedPane);
            actionPerformed(null);    // set icon for initially selected pane
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            instantiationPanel.caretUpdate(e);
        }

        @Override
        public void actionPerformed(ActionEvent e)
		{
            int selectedTypeIndex = tabbedPane.getSelectedIndex();

            // tell mommy...put up here to emphasize that it is the chief reason for having this listener
            instantiationPanel.actionPerformed(e);

            // But we can do this: leave off the red border if only one to choose from
            if (tabbedPane.getTabCount() > 1) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if (i == selectedTypeIndex) {
                        tabbedPane.setIconAt(i, checkMark);
                        constructorPanels[i].setBorder(BorderFactory.createLineBorder(Color.red));
                        constructorPanels[i].setSelected(true);
                    } else {
                        tabbedPane.setIconAt(i, null);
                        constructorPanels[i].setBorder(null);
                        constructorPanels[i].setSelected(false);
                    }
                }
            }
        }

        public void setData(VInstantiator.Construct vi) {
            if (vi == null) {
                return;
            }
            if (viskit.ViskitStatics.debug) {
                System.out.println("setting data for " + vi.getType());
            }

            int argumentIndex = vi.indexOfArgNames(vi.getType(), vi.getArgs());
            if (viskit.ViskitStatics.debug) {
                System.out.println("found a matching constructor at " + argumentIndex);
            }
            if (argumentIndex != -1) {
                constructorPanels[argumentIndex].setData(vi.getArgs());
                tabbedPane.setSelectedIndex(argumentIndex);
            }
            actionPerformed(null);
        }

        public VInstantiator getData()
		{
            ConstructorPanel constructorPanel = (ConstructorPanel) tabbedPane.getSelectedComponent();
            if (constructorPanel == null)
                return null;
            else
                return new VInstantiator.Construct(typeName, constructorPanel.getData());
        }
    }

    /**
     * ********************************************************************
     */
    class FactoryPanel extends JPanel {

        private InstantiationPanel instantiationPanel;

        private JLabel factoryClassLabel;

        private JComboBox<Object> factoryClassCB;

        private JPanel topPanel;

        private ObjectListPanel objectListPanel;

        private String typeName;

        private Class<?> myObjectClass;
        private boolean noClassAction = false;

        // TODO: Sometimes, there is a weird artifact that appears that looks
        //       like [...], like a button with elipses.  It happens on this
        //       panel, but is proving difficult to track down.  (TDN 15 APR 15)
        public FactoryPanel(InstantiationPanel instantiationPanel) {
            this.instantiationPanel = instantiationPanel;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            topPanel = new JPanel(new SpringLayout());
            factoryClassLabel = new JLabel("Factory class", JLabel.TRAILING);
            factoryClassCB = new JComboBox<>(new Object[]{ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS});
            ViskitStatics.clampHeight(factoryClassCB);
            factoryClassLabel.setLabelFor(factoryClassCB);

            JLabel dummyLabel = new JLabel("");
            JLabel classHelpLabel = new JLabel("(Press return after selecting "
                    + "factory class to start a new RandomVariate)", JLabel.LEADING);
            classHelpLabel.setFont(factoryClassCB.getFont());
            dummyLabel.setLabelFor(classHelpLabel);

            topPanel.add(factoryClassLabel);
            topPanel.add(factoryClassCB);
            topPanel.add(dummyLabel);
            topPanel.add(classHelpLabel);
            SpringUtilities.makeCompactGrid(topPanel, 2, 2, 5, 5, 5, 5);

            add(topPanel);

            factoryClassCB.addActionListener(new MyClassListener());
        }

        public void setType(String className) throws ClassNotFoundException {
            typeName = className;
            myObjectClass = ViskitStatics.classForName(typeName);
            if (myObjectClass == null) {
                throw new ClassNotFoundException(typeName);
            }
        }

        public void setData(VInstantiator.Factory vi) {
            if (vi == null) {
                return;
            }

            removeAll();
            noClassAction = true;
            factoryClassCB.setSelectedItem(vi.getFactoryClass()); // this fires action event
            noClassAction = false;
            add(topPanel);

            boolean foundString = false;
            for (Object o : vi.getParams()) {
                if (o instanceof String) {
                    foundString = true;
                    break;
                }
            }

            if (foundString) {
                Vector<Object> v = new Vector<>();
                v.add(vi);
                addObjectListPanel(v, foundString);
            } else {
                addObjectListPanel((Vector<Object>) vi.getParams(), !foundString);
            }
        }

        private void addObjectListPanel(Vector<Object> params, boolean showLabels) {
            objectListPanel = new ObjectListPanel(instantiationPanel);
            objectListPanel.setBorder(BorderFactory.createTitledBorder (
                    BorderFactory.createLineBorder(Color.black),
                    "Method arguments",
                    TitledBorder.CENTER,
                    TitledBorder.DEFAULT_POSITION));
            objectListPanel.setDialogInfo(packMeOwnerDialog);

            objectListPanel.setData(params, showLabels);
            add(objectListPanel);

            add(Box.createVerticalGlue());
            revalidate();
        }

        public VInstantiator getData() {
            String factoryClassName = (String) factoryClassCB.getSelectedItem();
            factoryClassName = (factoryClassName == null) ? ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS : factoryClassName.trim();
            String methodName = ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD;
            List<Object> objectList = (objectListPanel != null) ? objectListPanel.getData() : new Vector<>();
            return new VInstantiator.Factory(typeName, factoryClassName, methodName, objectList);
        }

        class MyChangedListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (instantiationPanel.modifiedListener != null) {
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "Button pressed"));
                }
            }
        }

        class MyCaretListener implements CaretListener {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (instantiationPanel.modifiedListener != null) {
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "TF edited pressed"));
                }
            }
        }

        class MyClassListener implements ActionListener 
		{
            @Override
            public void actionPerformed(ActionEvent e) {
                if (noClassAction) {
                    return;
                }

                Class<?> factoryClass;
                String factoryClassName = factoryClassCB.getSelectedItem().toString();
                try {
                    factoryClass = ViskitStatics.classForName(factoryClassName);
                    if (factoryClass == null) {
                        throw new ClassNotFoundException();
                    }
                } catch (ClassNotFoundException e1) {
                    JOptionPane.showMessageDialog(instantiationPanel, factoryClassName + " not found on the classpath");
                    factoryClassCB.requestFocus();
                    return;
                }

                Method[] staticMethods = factoryClass.getMethods();
                if (staticMethods == null || staticMethods.length <= 0) {
                    JOptionPane.showMessageDialog(instantiationPanel, factoryClassName + " contains no methods");
                    factoryClassCB.requestFocus();
                    return;
                }
                Vector<String> randomVariateFactoryStringObjectMethodVector = new Vector<>();
                Map<String, Method> methodsHashMap = new HashMap<>();

                for (Method method : staticMethods) {
                    int methodModifiers = method.getModifiers();
                    Class<?> methodReturnTypeClass = method.getReturnType();
                    if (Modifier.isStatic(methodModifiers)) {
                        if (methodReturnTypeClass == myObjectClass) {
                            String methodSignature = method.toString();
                            int startColumn = methodSignature.lastIndexOf('.', methodSignature.indexOf('(')); // go to ( , back to .
                            methodSignature = methodSignature.substring(startColumn + 1, methodSignature.length());

                            // Strip out java.lang
                            methodSignature = ViskitStatics.stripOutJavaDotLang(methodSignature);

                            // Show varargs symbol vice []
                            methodSignature = ViskitStatics.makeVarArgs(methodSignature);

                            // We only want to promote the RVF.getInstance(String, Object...) static method
                            if (method.getParameterCount() == 2 && methodSignature.contains("String") && methodSignature.contains("Object...")) {
                                methodsHashMap.put(methodSignature, method);
                                randomVariateFactoryStringObjectMethodVector.add(methodSignature);
                            }
                        }
                    }
                }
                if (randomVariateFactoryStringObjectMethodVector.isEmpty()) {
                    JOptionPane.showMessageDialog(instantiationPanel, "<html><center>" + factoryClassName + " contains no static methods<br>returning " + typeName + ".");
                    factoryClassCB.requestFocus();
                    return;
                }
                String[] methodNames = new String[0];
                methodNames = randomVariateFactoryStringObjectMethodVector.toArray(methodNames);
				Object returnValue;
				if (methodNames.length == 0)
				{
					// TODO no where to go
				}
				if (methodNames.length == 1)
				{
					returnValue = methodNames[0];
				}
				else // user selects
				{
					returnValue = JOptionPane.showInputDialog(packMeOwnerDialog,
							"Choose method",
							"Factory methods",
							JOptionPane.PLAIN_MESSAGE,
							null,
							methodNames,
							methodNames[0]);
					if (returnValue == null) {
						factoryClassCB.requestFocus();
						return;
					}
				}

                Method randomVariateFactorySelectedMethod = methodsHashMap.get((String) returnValue);
                Vector<Object> vInstantiatorVector = VInstantiator.buildDummyInstantiators(randomVariateFactorySelectedMethod);
                addObjectListPanel(vInstantiatorVector, true);

                if (instantiationPanel.modifiedListener != null) {
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "Factory method chosen"));
                }
            }
        }
    }
}
