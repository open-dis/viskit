package viskit.view;

import edu.nps.util.LogUtilities;
import edu.nps.util.SpringUtilities;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Executable;
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
import org.apache.log4j.Logger;
import viskit.ViskitStatics;
import viskit.model.ViskitInstantiator;
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
public class InstantiationPanel extends JPanel implements ActionListener, CaretListener
{
    static final Logger LOG = LogUtilities.getLogger(InstantiationPanel.class);

    private CardLayout instantiationPaneLayoutManager;
    private JPanel    instantiationPanel = new JPanel();
    private JPanel              topPanel = new JPanel(new SpringLayout());
    private FreeFormPanel   freeFormPanel;
    private ConstrPanel  constructorPanel;
    private FactoryPanel     factoryPanel;
    private JLabel     typeLabel = new JLabel("type",   JLabel.TRAILING);
	private JLabel   methodLabel = new JLabel("method", JLabel.TRAILING);
    private JTextField typeTF    = new JTextField();
	
    private static final int FREEFORM    = 0, 
			                 CONSTRUCTOR = 1, 
			                 FACTORY     = 2;
    private JComboBox<String>     methodCB    = new JComboBox<>(new String[]{"free form", "constructor", "factory"});
    // alternate display: one or the other
    private JTextField constructorTF = new JTextField("Constructor");
	
    private ActionListener modifiedListener;
    private JDialog packMeOwnerDialog;
    private ViskitInstantiator copiedViskitInstantiator;

    private boolean constructorOnly = false;
	private boolean typeEditable    = false;
	

	/** Constructor
	 * @param ownerDialog
	 * @param changedListener */
    public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener) 
	{
        this(ownerDialog, changedListener, false);
    }

	/** Constructor
	 * @param ownerDialog
	 * @param changedListener
	 * @param onlyConstructor */
    public InstantiationPanel(JDialog ownerDialog, ActionListener changedListener, boolean onlyConstructor) 
	{
        this(ownerDialog, changedListener, onlyConstructor, false);
    }

	/** Constructor
	 * @param ownerDialog
	 * @param changedListener
	 * @param onlyConstructor
	 * @param typeEditable */
    public InstantiationPanel(final JDialog ownerDialog, ActionListener changedListener, boolean onlyConstructor, boolean typeEditable)
	{
        packMeOwnerDialog    = ownerDialog;
        modifiedListener     = changedListener;
        this.constructorOnly = onlyConstructor;
		this.typeEditable    = typeEditable;

		initialize ();
    }
	private void initialize ()
	{
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        typeTF.setEditable(typeEditable);
        typeTF.addActionListener(new ActionListener() 
		{
            @Override
            public void actionPerformed(ActionEvent e) 
			{
                methodCB.actionPerformed(e);
            }
        });
        typeLabel.setLabelFor(typeTF);

        constructorTF.setEditable(false);

        topPanel.add(typeLabel);
        topPanel.add(typeTF);
        topPanel.add(methodLabel);
        if (constructorOnly)
		{
            methodLabel.setLabelFor(constructorTF);
            topPanel.add(constructorTF);
        } 
		else 
		{
            methodLabel.setLabelFor(methodCB);
            topPanel.add(methodCB);
        }
        SpringUtilities.makeCompactGrid(topPanel, 2, 2, 10, 10, 5, 7); // 7: add a little extra space between rows
        add(topPanel);

        instantiationPaneLayoutManager = new CardLayout();
        instantiationPanel.setLayout(instantiationPaneLayoutManager);
        instantiationPanel.setBorder(BorderFactory.createEtchedBorder());
        instantiationPanel.setAlignmentX(Box.CENTER_ALIGNMENT);

           freeFormPanel = new FreeFormPanel(this);
        constructorPanel = new   ConstrPanel(this); // don't try to rename this
            factoryPanel = new  FactoryPanel(this);

        instantiationPanel.add(   freeFormPanel,    "freeFormPanel");
        instantiationPanel.add(constructorPanel, "constructorPanel");
        instantiationPanel.add(    factoryPanel,     "factoryPanel");
        add(Box.createVerticalStrut(5));
        add(instantiationPanel);

        methodCB.addActionListener(new ActionListener()
		{
            int lastIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e)
			{
                if (!typeTF.getText().trim().equals(copiedViskitInstantiator.getTypeName())) 
				{
                    String newType = typeTF.getText().trim();
                    // update the panels
                    try {
                           freeFormPanel.setType(newType);
                        constructorPanel.setType(newType);
                            factoryPanel.setType(newType);
                    } 
					catch (ClassNotFoundException cnfe) 
					{
                        JOptionPane.showMessageDialog(InstantiationPanel.this, "Unknown type: " + cnfe );
						LOG.error ("type problem #1 in methodCB ActionListener", cnfe);
                        return;
                    }
                       freeFormPanel.setData(new ViskitInstantiator.FreeForm(newType, "")); // no value
                        factoryPanel.setData(new ViskitInstantiator.Factory(newType,
                            ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS,
                            ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                            new Vector<>()
                    ));
                }
                int selectedIndex = methodCB.getSelectedIndex();
                if (lastIndex != selectedIndex) 
				{
                    if (modifiedListener != null) 
					{
                        modifiedListener.actionPerformed(new ActionEvent(methodCB, 0, "modified"));
                    }
                }
                switch (selectedIndex) 
				{
                    case FREEFORM:
                        instantiationPaneLayoutManager.show(instantiationPanel, "freeFormPanel");
                        freeFormPanel.valueTF.requestFocus();
                        freeFormPanel.valueTF.selectAll();
                        break;
                    case CONSTRUCTOR:
                        instantiationPaneLayoutManager.show(instantiationPanel, "constructorPanel");
						constructorPanel.requestFocus();
                        break;
                    case FACTORY:
                        instantiationPaneLayoutManager.show(instantiationPanel, "factoryPanel");
                        factoryPanel.factoryClassCB.requestFocus();
                        break;
                    default:
                        LOG.error("type problem #2 in methodCB ActionListener");
                }
            }
        });
    }

    public ViskitInstantiator getData()
	{
        switch (methodCB.getSelectedIndex())
		{
            case FREEFORM:
                return    freeFormPanel.getData();
            case CONSTRUCTOR:
                return constructorPanel.getData();
            case FACTORY:
                return     factoryPanel.getData();
            default:
				LOG.error("Instantiation panel has bad type-related invocation for getData()");
                return null;
        }
    }

    public void setData(ViskitInstantiator passedViskitInstantiator) throws ClassNotFoundException
	{
        copiedViskitInstantiator = passedViskitInstantiator.vcopy(); // TODO why is this going to Construct?
        String typeName = copiedViskitInstantiator.getTypeName();
        typeTF.setText(typeName);

        // inform all panels of the type of the object
        constructorPanel.setType(typeName);
            factoryPanel.setType(typeName);
           freeFormPanel.setType(typeName);

        if (passedViskitInstantiator instanceof ViskitInstantiator.Construct)
		{
            methodCB.setSelectedIndex(CONSTRUCTOR);
            constructorPanel.setData((ViskitInstantiator.Construct) copiedViskitInstantiator);
        } 
		else if (passedViskitInstantiator instanceof ViskitInstantiator.Factory)
		{
            methodCB.setSelectedIndex(FACTORY);
            factoryPanel.setData((ViskitInstantiator.Factory) copiedViskitInstantiator);
        }
		else if (passedViskitInstantiator instanceof ViskitInstantiator.FreeForm)
		{
            methodCB.setSelectedIndex(FREEFORM);
            freeFormPanel.setData((ViskitInstantiator.FreeForm) copiedViskitInstantiator);
        }
		else
		{
            LOG.error("Internal error InstantiationPanel.setData()");
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

        private final InstantiationPanel instantiationPanel;

        public FreeFormPanel(InstantiationPanel instantiationPanel)
		{
            this.instantiationPanel = instantiationPanel;
			
			initialize ();
        }
		
		private void initialize ()
		{
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            valueTF = new JTextField("");
            valueTF.addCaretListener(FreeFormPanel.this);
            valueTF.setAlignmentX(Box.CENTER_ALIGNMENT);
            ViskitStatics.clampHeight(valueTF);

            add(valueTF);
            add(Box.createVerticalGlue());
		}

        public void setData(ViskitInstantiator.FreeForm viff) {
            if (viff == null) {
                return;
            }
            valueTF.setText(viff.getValue());
        }

        String typeName;

        public void setType(String newTypeName) throws ClassNotFoundException {
            this.typeName = newTypeName;
            if (ViskitStatics.ClassForName(newTypeName) == null) // just to check exception
            {
                throw new ClassNotFoundException(newTypeName);
            }
        }

        public ViskitInstantiator getData() {
            return new ViskitInstantiator.FreeForm(typeName, valueTF.getText().trim());
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
    class ConstrPanel extends JPanel implements ActionListener, CaretListener 
	{
        private final JTabbedPane tabbedPane;

        private ConstructorPanel[]        constructorPanels;
		private ViskitInstantiator.Construct[] constructors;
		private String[]                  parametersSignature;

        private final String noParametersString = "(no parameters)";

        private final ImageIcon checkMark;

        private final InstantiationPanel instantiationPanel;

        public ConstrPanel(InstantiationPanel instantiationPanel)
		{
            this.instantiationPanel = instantiationPanel;
            tabbedPane = new JTabbedPane();
            checkMark  = new ImageIcon(ClassLoader.getSystemResource("viskit/images/checkMark.png"));
			
			initialize ();
        }
		private void initialize ()
		{
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		}

        private String typeName;
		private int constructorTabCount = 0;

        public void setType(String className) throws ClassNotFoundException
		{
            LOG.debug("ConstrPanel setType for class " + className);
            List<Object>[] parameters = ViskitStatics.resolveParametersUsingReflection(ViskitStatics.ClassForName(className));

            typeName = className;
            removeAll();
            tabbedPane.removeAll();

            if (parameters == null) // no longer how we approach this
			{
                tabbedPane.addTab("No-parameter Constructor", null, new JLabel("No-parameter constructor. Factory, Abstract or Interface"));
            } 
			else
			{
                constructorPanels   = new ConstructorPanel            [parameters.length];
				constructors        = new ViskitInstantiator.Construct[parameters.length];
				parametersSignature = new String                      [parameters.length];
				
                for (int i = 0; i < parameters.length; ++i)
				{
                           constructors[i] = new ViskitInstantiator.Construct(parameters[i], className);
					parametersSignature[i] = "";
					if (constructors[i].getParameterTypesList().isEmpty())
					{
						parametersSignature[i] = noParametersString;
					}
					else
					{
						for (int j = 0; j < constructors[i].getParameterTypesList().size(); j++)
						{
							String typeName = ((Parameter)parameters[i].get(j)).getType();
							if      (typeName == null)
								     typeName = ""; // Error condition
							else if (typeName.contains("."))
								     typeName = typeName.substring(typeName.lastIndexOf(".")+1);
							parametersSignature[i] += typeName;

							if (!((ViskitInstantiator) (constructors[i].getParameterTypesList().get(j))).getName().equals(((Parameter)parameters[i].get(j)).getName()))
							{
								String name = ((Parameter)parameters[i].get(j)).getName();
								((ViskitInstantiator) (constructors[i].getParameterTypesList().get(j))).setName(name);
								parametersSignature[i] += " " + name;
							}
							parametersSignature[i] += ", ";
						}
						parametersSignature[i] = parametersSignature[i].substring(0, parametersSignature[i].length() - 2); // strip trailing comma from set
					}

					// this performs further hiding of duplicate constructors, also see below
					if (!constructors[i].getParameterTypesList().isEmpty() || zeroArgumentConstructorAllowed)
					{
						constructorTabCount++;
					}
                }
                for (int i = 0; i < parameters.length; ++i) // only add tabs of interest
				{
					// this performs further hiding of duplicate constructors, also see above
					if (!constructors[i].getParameterTypesList().isEmpty() || zeroArgumentConstructorAllowed)
					{
						constructorPanels[i] = new ConstructorPanel(this, parameters.length != 1, this, packMeOwnerDialog);
						packMeOwnerDialog.pack(); // TODO check OK
						String constructorTabName;
						if (constructorTabCount <= 1) 
						{
							constructorTabName = "Parameter List";
							constructorPanels[i].hideSelectConstructorButton();
						}
						else constructorTabName = "Constructor " + i;
						
						constructorPanels[i].setData(constructors[i].getParameterTypesList());
						tabbedPane.addTab(constructorTabName, null, constructorPanels[i], parametersSignature[i]); // null icon
					}
				}
            }
			if (constructorTabCount > 0)
			{
				add(tabbedPane);
			}
            actionPerformed(null);    // set icon for initially selected pane
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            instantiationPanel.caretUpdate(e);
        }

        @Override
        public void actionPerformed(ActionEvent e)
		{
            int selectedTabIndex = tabbedPane.getSelectedIndex();

            // tell parent... put up here to emphasize that it is the chief reason for having this listener
            instantiationPanel.actionPerformed(e);

            // But we can do this: leave off the red border if only one to choose from
            if (tabbedPane.getTabCount() > 1)
			{
                for (int i = 0; i < tabbedPane.getTabCount(); i++) 
				{
					JPanel selectedPanel = ((JPanel) tabbedPane.getComponentAt(i));
                    if (i == selectedTabIndex)
					{
                        tabbedPane.setIconAt(i, checkMark);
						selectedPanel.setBorder(BorderFactory.createLineBorder(Color.red));
						// TODO turn off "Select this constructor" button
						if (selectedPanel instanceof ConstructorPanel)
							((ConstructorPanel) selectedPanel).hideSelectConstructorButton();
                    } 
					else
					{
                        tabbedPane.setIconAt(i, null);
                        selectedPanel.setBorder(null);
						// TODO turn off "Select this constructor" button
						if (selectedPanel instanceof ConstructorPanel)
							((ConstructorPanel) selectedPanel).showSelectConstructorButton();
                    }
                }
            }
        }

        public void setData(ViskitInstantiator.Construct viskitInstantiatorConstructor)
		{
            if (viskitInstantiatorConstructor == null)
			{
                return;
            }
            if (viskit.ViskitStatics.debug)
			{
                System.out.println("setting data for " + viskitInstantiatorConstructor.getTypeName());
            }

            int argumentIndex = viskitInstantiatorConstructor.indexOfArgumentNames(viskitInstantiatorConstructor.getTypeName(), viskitInstantiatorConstructor.getParameterTypesList());
            if (viskit.ViskitStatics.debug)
			{
                System.out.println("found a matching constructor at " + argumentIndex);
            }
            if (argumentIndex != -1)
			{
				if ((constructorPanels != null) && (constructorPanels[argumentIndex] != null))
				{
					constructorPanels[argumentIndex].setData(viskitInstantiatorConstructor.getParameterTypesList());
					tabbedPane.setSelectedIndex(argumentIndex);
				}
				else 
				{
					String constructorStatus;
					if ((constructorPanels == null) || (constructorPanels[0] == null))
						 constructorStatus = "constructor[0] not available";
					else constructorStatus = "using index 0";
					LOG.error ("Internal problem with constructorPanels[" + argumentIndex + "] array generation, " + constructorStatus);
					constructorPanels[0].setData(viskitInstantiatorConstructor.getParameterTypesList());
					tabbedPane.setSelectedIndex(0);
				}
            }
            actionPerformed(null);
        }

        public ViskitInstantiator getData()
		{
            ConstructorPanel constructorPanel = (ConstructorPanel) tabbedPane.getSelectedComponent();
            if (constructorPanel == null)
                return null;
            else
                return new ViskitInstantiator.Construct(typeName, constructorPanel.getData());
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
        public FactoryPanel(InstantiationPanel instantiationPanel)
		{
            this.instantiationPanel = instantiationPanel;
			initialize ();
        }
		
		private void initialize ()
		{
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            topPanel = new JPanel(new SpringLayout());
            factoryClassLabel = new JLabel("Factory class", JLabel.TRAILING);
            factoryClassCB = new JComboBox<>(new Object[]{ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS}); // TODO support more, depending on class chosen
            ViskitStatics.clampHeight(factoryClassCB);
            factoryClassLabel.setLabelFor(factoryClassCB);

            JLabel dummyLabel = new JLabel("");
            JLabel classHelpLabel = new JLabel(
					"(Apply changes after selecting factory class" + 
                    " to create a new RandomVariate)", JLabel.LEADING);
            classHelpLabel.setFont(factoryClassCB.getFont());
            dummyLabel.setLabelFor(classHelpLabel);
			classHelpLabel.setVisible (factoryClassCB.getItemCount() > 1); // only shown if more than one factory class is present

            topPanel.add(factoryClassLabel);
            topPanel.add(factoryClassCB);
            topPanel.add(dummyLabel);
            topPanel.add(classHelpLabel);
            SpringUtilities.makeCompactGrid(topPanel, 2, 2, 5, 5, 5, 5);

            add(topPanel);

            factoryClassCB.addActionListener(new MyClassListener());
		}

        public void setType(String className) throws ClassNotFoundException
		{
            typeName = className;
            myObjectClass = ViskitStatics.ClassForName(typeName);
            if (myObjectClass == null)
			{
                throw new ClassNotFoundException(typeName);
            }
        }

        public void setData(ViskitInstantiator.Factory vi) 
		{
            if (vi == null) 
			{
                return; // nothing to work with.  TODO is this a bad invocation?
            }
            removeAll();
            noClassAction = true;
            factoryClassCB.setSelectedItem(vi.getFactoryClass()); // this fires action event
            noClassAction = false;
            add(topPanel);

            boolean foundString = false;
            for (Object o : vi.getParametersList()) 
			{
                if (o instanceof String) 
				{
                    foundString = true;
                    break;
                }
            }
            if (foundString) 
			{
                Vector<Object> v = new Vector<>();
                v.add(vi);
                addObjectListPanel(v, foundString);
            } 
			else 
			{
                addObjectListPanel((Vector<Object>) vi.getParametersList(), !foundString);
            }
        }

        private void addObjectListPanel(Vector<Object> params, boolean showLabels)
		{
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

        public ViskitInstantiator getData()
		{
            String factoryClassName = (String) factoryClassCB.getSelectedItem();
             factoryClassName = (factoryClassName == null) ? ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS : factoryClassName.trim();
            String methodName = ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD;
            List<Object> objectList = (objectListPanel != null) ? objectListPanel.getData() : new Vector<>();
            return new ViskitInstantiator.Factory(typeName, factoryClassName, methodName, objectList);
        }

        class MyChangedListener implements ActionListener 
		{
            @Override
            public void actionPerformed(ActionEvent e) 
			{
                if (instantiationPanel.modifiedListener != null)
				{
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "Button pressed"));
                }
            }
        }

        class MyCaretListener implements CaretListener 
		{
            @Override
            public void caretUpdate(CaretEvent e) 
			{
                if (instantiationPanel.modifiedListener != null)
				{
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "TF edited pressed"));
                }
            }
        }

        class MyClassListener implements ActionListener 
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
                if (noClassAction) 
				{
                    return;
                }
                Class<?> factoryClass;
                String factoryClassName = factoryClassCB.getSelectedItem().toString();
                try {
                    factoryClass = ViskitStatics.ClassForName(factoryClassName);
                    if (factoryClass == null) 
					{
                        throw new ClassNotFoundException();
                    }
                } 
				catch (ClassNotFoundException e1) 
				{
					// TODO dialog, log message
                    JOptionPane.showMessageDialog(instantiationPanel, factoryClassName + " not found on the classpath");
                    factoryClassCB.requestFocus();
                    return;
                }

                Method[] staticMethods = factoryClass.getMethods();
                if (staticMethods == null || staticMethods.length <= 0) 
				{
					// TODO dialog, log message
                    JOptionPane.showMessageDialog(instantiationPanel, factoryClassName + " contains no methods");
                    factoryClassCB.requestFocus();
                    return;
                }
                Vector<String> randomVariateFactoryStringObjectMethodVector = new Vector<>();
                Map<String, Method> methodsHashMap = new HashMap<>();

                for (Method method : staticMethods)
				{
                    int methodModifiers = method.getModifiers();
                    Class<?> methodReturnTypeClass = method.getReturnType();
                    if (Modifier.isStatic(methodModifiers)) 
					{
                        if (methodReturnTypeClass == myObjectClass)
						{
                            String methodSignature = method.toString();
                            int startColumn = methodSignature.lastIndexOf('.', methodSignature.indexOf('(')); // go to ( , back to .
                            methodSignature = methodSignature.substring(startColumn + 1, methodSignature.length());

                            // Strip out java.lang
                            methodSignature = ViskitStatics.stripOutJavaDotLang(methodSignature);

                            // Show varargs symbol vice []
                            methodSignature = ViskitStatics.applyVarArgSymbol(methodSignature);

                            // We only want to promote the randomVariateFactory getInstance(String, Object...) static method
                            if (method.getParameterCount() == 2 && methodSignature.contains("String") && methodSignature.contains("Object...")) 
							{
                                methodsHashMap.put(methodSignature, method);
                                randomVariateFactoryStringObjectMethodVector.add(methodSignature);
                            }
                        }
                    }
                }
                if (randomVariateFactoryStringObjectMethodVector.isEmpty()) 
				{
					// TODO dialog, log message
                    JOptionPane.showMessageDialog(instantiationPanel, "<html><center>" + factoryClassName + " contains no static methods <br/> returning " + typeName + ".");
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
					// TODO dialog, log message
					returnValue = JOptionPane.showInputDialog(packMeOwnerDialog,
							"Choose method",
							"Factory methods",
							JOptionPane.PLAIN_MESSAGE,
							null,
							methodNames,
							methodNames[0]);
					if (returnValue == null) 
					{
						factoryClassCB.requestFocus();
						return;
					}
				}

                Method   randomVariateFactorySelectedMethod  = methodsHashMap.get((String) returnValue);
				Method[] randomVariateFactorySelectedMethods = { randomVariateFactorySelectedMethod };
                Vector<Object> vInstantiatorVector = ViskitInstantiator.buildDummyInstantiatorsFromReflection(randomVariateFactorySelectedMethods);
                addObjectListPanel(vInstantiatorVector, true);

                if (instantiationPanel.modifiedListener != null) 
				{
                    instantiationPanel.modifiedListener.actionPerformed(new ActionEvent(this, 0, "Factory method chosen"));
                }
            }
        }
    }

	private boolean zeroArgumentConstructorAllowed = false;
	/**
	 * @return the zeroArgumentConstructorAllowed
	 */
	public boolean isZeroArgumentConstructorAllowed() {
		return zeroArgumentConstructorAllowed;
	}

	/**
	 * @param zeroArgumentConstructorAllowed the zeroArgumentConstructorAllowed to set
	 */
	public void setZeroArgumentConstructorAllowed(boolean zeroArgumentConstructorAllowed) {
		this.zeroArgumentConstructorAllowed = zeroArgumentConstructorAllowed;
	}
}
