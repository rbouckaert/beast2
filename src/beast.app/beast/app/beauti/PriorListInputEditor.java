package beast.app.beauti;



import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import beast.app.inputeditor.BEASTObjectPanel;
import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.BeautiPanelConfig;
import beast.app.inputeditor.InputEditor;
import beast.app.inputeditor.ListInputEditor;
import beast.app.inputeditor.SmallButton;
import beast.app.inputeditor.TaxonSetDialog;
import beast.app.inputeditor.InputEditor.ButtonStatus;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.Logger;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.OneOnX;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;



public class PriorListInputEditor extends ListInputEditor {
    private static final long serialVersionUID = 1L;

    List<JButton> rangeButtons;

    List<JButton> taxonButtons;

	public PriorListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public PriorListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Distribution.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	List<?> list = (List<?>) input.get();
    	Collections.sort(list, (Object o1, Object o2) -> {
				if (o1 instanceof BEASTInterface && o2 instanceof BEASTInterface) {
					String d1 = ((BEASTInterface)o1).getID();
					String id2 = ((BEASTInterface)o2).getID();
					// first the tree priors
					if (o1 instanceof TreeDistribution) {
						if (o2 instanceof TreeDistribution) {
							TreeInterface tree1 = ((TreeDistribution)o1).treeInput.get();
							if (tree1 == null) {
								tree1 = ((TreeDistribution)o1).treeIntervalsInput.get().treeInput.get();
							}
							TreeInterface tree2 = ((TreeDistribution)o2).treeInput.get();
							if (tree2 == null) {
								tree2 = ((TreeDistribution)o2).treeIntervalsInput.get().treeInput.get();
							}
							return d1.compareTo(id2);
						} else {
							return -1;
						}
					} else if (o1 instanceof MRCAPrior) {
						// last MRCA priors
						if (o2 instanceof MRCAPrior) {
							return d1.compareTo(id2);
						} else {
							return 1;
						}
					} else {
						if (o2 instanceof TreeDistribution) {
							return 1;
						}
						if (o2 instanceof MRCAPrior) {
							return -1;
						}
						if (o1 instanceof Prior) {
							d1 = ((Prior) o1).getParameterName(); 
						}
						if (o2 instanceof Prior) {
							id2 = ((Prior) o2).getParameterName(); 
						}
						return d1.compareTo(id2);
					}
				}
				return 0;
			}
		);
    	
    	
        rangeButtons = new ArrayList<>();
        taxonButtons = new ArrayList<>();
        
        //m_buttonStatus = ButtonStatus.NONE;
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);

        
        if (beastObject instanceof BeautiPanelConfig) {
        	BeautiPanelConfig config = (BeautiPanelConfig) beastObject;
        	if (config.parentBEASTObjects != null && config.parentBEASTObjects.size() > 0 && config.parentBEASTObjects.get(0).getID().equals("speciescoalescent")) {
        		m_buttonStatus = ButtonStatus.NONE;
        	}
        }
        
        if (m_buttonStatus == ButtonStatus.ALL || m_buttonStatus == ButtonStatus.ADD_ONLY) {
	        addButton = new SmallButton("+ Add Prior", true);
	        addButton.setName("addItem");
	        addButton.setToolTipText("Add new prior (like an MRCA-prior) to the list of priors");
	        addButton.addActionListener(e -> {
	                addItem();
	            });
	        buttonBox.add(addButton);
            buttonBox.add(Box.createHorizontalGlue());
        }
    }


    /**
     * add components to box that are specific for the beastObject.
     * By default, this just inserts a label with the beastObject ID
     *
     * @param itemBox box to add components to
     * @param beastObject  beastObject to add
     */
    @Override
    protected InputEditor addPluginItem(Box itemBox, BEASTInterface beastObject) {
		try {
	    	int listItemNr = ((List<?>) m_input.get()).indexOf(beastObject);
	    	InputEditor editor = doc.getInputEditorFactory().createInputEditor(m_input, listItemNr, beastObject, false, ExpandOption.FALSE, ButtonStatus.NONE, null, doc);
	    	itemBox.add((Component) editor);
	    	return editor;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
    }	


    String paramToString(RealParameter p) {
        Double lower = p.lowerValueInput.get();
        Double upper = p.upperValueInput.get();
        return "initial = " + p.valuesInput.get() +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    static protected Set<Taxon> getTaxonCandidates(MRCAPrior prior, BeautiDoc doc) {
        Set<Taxon> candidates = new HashSet<>();
        Tree tree = prior.treeInput.get();
        String [] taxa = null;
        if (tree.m_taxonset.get() != null) {
        	try {
            	TaxonSet set = tree.m_taxonset.get();
        		set.initAndValidate();
            	taxa = set.asStringList().toArray(new String[0]);
        	} catch (Exception e) {
            	taxa = prior.treeInput.get().getTaxaNames();
			}
        } else {
        	taxa = prior.treeInput.get().getTaxaNames();
        }
        
        for (String taxon : taxa) {
            candidates.add(doc.getTaxon(taxon));
        }
        return candidates;
    }

    /**
     * class to deal with toggling monophyletic flag on an MRCAPrior *
     */
    class MRCAPriorActionListener implements ActionListener {
        MRCAPrior m_prior;

        MRCAPriorActionListener(MRCAPrior prior) {
            m_prior = prior;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                m_prior.isMonophyleticInput.setValue(((JCheckBox) e.getSource()).isSelected(), m_prior);
                refreshPanel();
            } catch (Exception ex) {
                Log.err.println("PriorListInputEditor " + ex.getMessage());
            }
        }
    }

    @Override
    protected void addItem() {
        super.addItem();
        sync();
        refreshPanel();
    } // addItem
    
    List<PriorProvider> priorProviders;
    
    private void initProviders() {
    	priorProviders = new ArrayList<>();
    	priorProviders.add(new MRCAPriorProvider());
    	
        // build up list of data types
    	List<String> providerClasses = new ArrayList<>();
    	for (PriorProvider provider : ServiceLoader.load(PriorProvider.class)) {
    		providerClasses.add(provider.getClass().getName());
    	}
        // = PackageManager.find(PriorProvider.class, new String[]{"beast.app"});
        for (String _class: providerClasses) {
        	try {
        		if (!_class.startsWith(this.getClass().getName())) {
        			PriorProvider priorProvider = (PriorProvider) BEASTClassLoader.forName(_class).newInstance();
					priorProviders.add(priorProvider);
        		}
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

    }
    
    @Override
	protected List<BEASTInterface> pluginSelector(Input<?> input, BEASTInterface parent, List<String> tabooList) {
    	if (priorProviders == null) {
    		initProviders();
    	}
    	PriorProvider priorProvider = priorProviders.get(0);
    	if (priorProviders.size() > 1) {
			// let user choose a PriorProvider
			List<String> descriptions = new ArrayList<>();
			List<PriorProvider> availableProviders = new ArrayList<>();
			for (PriorProvider i : priorProviders) {
				if (i.canProvidePrior(doc)) {
					descriptions.add(i.getDescription());
					availableProviders.add(i);
				}
			}
			String option = (String)JOptionPane.showInputDialog(null, "Which prior do you want to add", "Option",
                    JOptionPane.WARNING_MESSAGE, null, descriptions.toArray(), descriptions.get(0));
			if (option == null) {
				return null;
			}
			int i = descriptions.indexOf(option);
			priorProvider = availableProviders.get(i);

    	}
    	
        List<BEASTInterface> selectedPlugins = new ArrayList<>();
        List<Distribution> distrs = priorProvider.createDistribution(doc);
        if (distrs == null) {
        	return null;
        }
        for (Distribution distr : distrs) {
        	selectedPlugins.add(distr);
        }
        return selectedPlugins;
    }
    
	public static void addCollapsedID(String id) {
		g_collapsedIDs.add(id);		
	}
}
