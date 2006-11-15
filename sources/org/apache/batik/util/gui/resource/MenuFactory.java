/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.util.gui.resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

/**
 * This class represents a menu factory which builds
 * menubars and menus from the content of a resource file. <br>
 *
 * The resource entries format is (for a menubar named 'MenuBar'):<br>
 * <pre>
 *   MenuBar           = Menu1 Menu2 ...
 *
 *   Menu1.type        = RADIO | CHECK | MENU | ITEM
 *   Menu1             = Item1 Item2 - Item3 ...
 *   Menu1.text        = text 
 *   Menu1.icon        = icon_name 
 *   Menu1.mnemonic    = mnemonic 
 *   Menu1.accelerator = accelerator
 *   Menu1.action      = action_name
 *   Menu1.selected    = true | false
 *   Menu1.enabled     = true | false
 *   ...
 * mnemonic is a single character
 * accelerator is of the form described in {@link javax.swing.KeyStroke#getKeyStroke(String)}.
 * '-' represents a separator
 * </pre>
 * All entries are optional except the '.type' entry
 * Consecutive RADIO items are put in a ButtonGroup
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class MenuFactory extends ResourceManager {
    // Constants
    //
    private final static String TYPE_MENU          = "MENU";
    private final static String TYPE_ITEM          = "ITEM";
    private final static String TYPE_RADIO         = "RADIO";
    private final static String TYPE_CHECK         = "CHECK";
    private final static String SEPARATOR          = "-";

    private final static String TYPE_SUFFIX        = ".type";
    private final static String TEXT_SUFFIX        = ".text";
    private final static String MNEMONIC_SUFFIX    = ".mnemonic";
    private final static String ACCELERATOR_SUFFIX = ".accelerator";
    private final static String ACTION_SUFFIX      = ".action";
    private final static String SELECTED_SUFFIX    = ".selected";
    private final static String ENABLED_SUFFIX     = ".enabled";
    private final static String ICON_SUFFIX        = ".icon";

    /**
     * The table which contains the actions
     */
    private ActionMap actions;

    /**
     * The current radio group
     */
    private ButtonGroup buttonGroup;

    /**
     * Creates a new menu factory
     * @param rb the resource bundle that contains the menu bar
     *           description.
     * @param am the actions to add to menu items
     */
    public MenuFactory(ResourceBundle rb, ActionMap am) {
        super(rb);
        actions = am;
	buttonGroup = null;
    }

    /**
     * Creates and returns a swing menu bar
     * @param name the name of the menu bar in the resource bundle
     * @throws MissingResourceException if one of the keys that compose the
     *         menu is missing.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character and if the accelerator is malformed
     * @throws MissingListenerException if an item action is not found in the
     *         action map
     */
    public JMenuBar createJMenuBar(String name)
	throws MissingResourceException,
               ResourceFormatException,
	       MissingListenerException {
        JMenuBar result = new JMenuBar();
        List     menus  = getStringList(name);
        Iterator it     = menus.iterator();

        while (it.hasNext()) {
            result.add(createJMenuComponent((String)it.next()));
        }
        return result;
    }

    /**
     * Creates and returns a menu item or a separator
     * @param name the name of the menu item or "-" to create a separator
     * @throws MissingResourceException if key is not the name of a menu item.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException in case of malformed entry
     * @throws MissingListenerException if an item action is not found in the
     *         action map
     */
    protected JComponent createJMenuComponent(String name)
	throws MissingResourceException,
	       ResourceFormatException,
	       MissingListenerException {
	if (name.equals(SEPARATOR)) {
	    buttonGroup = null;
	    return new JSeparator();
	}
        String     type = getString(name+TYPE_SUFFIX);
        JComponent item = null;

	if (type.equals(TYPE_RADIO)) {
	    if (buttonGroup == null) {
		buttonGroup = new ButtonGroup();
	    }
	} else {
	    buttonGroup = null;
	}

        if (type.equals(TYPE_MENU)) {
            item = createJMenu(name);
        } else if (type.equals(TYPE_ITEM)) {
            item = createJMenuItem(name);
        } else if (type.equals(TYPE_RADIO)) {
            item = createJRadioButtonMenuItem(name);
	    buttonGroup.add((AbstractButton)item);
        } else if (type.equals(TYPE_CHECK)) {
            item = createJCheckBoxMenuItem(name);
        } else {
	    throw new ResourceFormatException("Malformed resource",
					      bundle.getClass().getName(),
					      name+TYPE_SUFFIX);
	}
	
        return item;
    }

    /**
     * Creates and returns a new swing menu
     * @param name the name of the menu bar in the resource bundle
     * @throws MissingResourceException if one of the keys that compose the
     *         menu is missing.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if a item action is not found in the
     *         action map.
     */
    public JMenu createJMenu(String name)
	throws MissingResourceException,
	       ResourceFormatException,
	       MissingListenerException {
        JMenu result = new JMenu(getString(name+TEXT_SUFFIX));
        initializeJMenuItem(result, name);

        List     items = getStringList(name);
        Iterator it    = items.iterator();

        while (it.hasNext()) {
            result.add(createJMenuComponent((String)it.next()));
        }
        return result;
    }

    /**
     * Creates and returns a new swing menu item
     * @param name the name of the menu item
     * @throws MissingResourceException if one of the keys that compose the
     *         menu item is missing.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if then item action is not found in
     *         the action map.
     */
    public JMenuItem createJMenuItem(String name)
	throws MissingResourceException,
	       ResourceFormatException,
	       MissingListenerException {
        JMenuItem result = new JMenuItem(getString(name+TEXT_SUFFIX));
        initializeJMenuItem(result, name);
        return result;
    }

    /**
     * Creates and returns a new swing radio button menu item
     * @param name the name of the menu item
     * @throws MissingResourceException if one of the keys that compose the
     *         menu item is missing.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if then item action is not found in
     *         the action map.
     */
    public JRadioButtonMenuItem createJRadioButtonMenuItem(String name)
	throws MissingResourceException,
	       ResourceFormatException,
	       MissingListenerException {
        JRadioButtonMenuItem result;
	result = new JRadioButtonMenuItem(getString(name+TEXT_SUFFIX));
        initializeJMenuItem(result, name);

        // is the item selected?
	try {
	    result.setSelected(getBoolean(name+SELECTED_SUFFIX));
	} catch (MissingResourceException e) {
	}
	
        return result;
    }

    /**
     * Creates and returns a new swing check box menu item
     * @param name the name of the menu item
     * @throws MissingResourceException if one of the keys that compose the
     *         menu item is missing.
     *         It is not thrown if the mnemonic, the accelerator and the
     *         action keys are missing
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if then item action is not found in
     *         the action map.
     */
    public JCheckBoxMenuItem createJCheckBoxMenuItem(String name)
	throws MissingResourceException,
	       ResourceFormatException,
	       MissingListenerException {
        JCheckBoxMenuItem result;
        result = new JCheckBoxMenuItem(getString(name+TEXT_SUFFIX));
        initializeJMenuItem(result, name);

        // is the item selected?
	try {
	    result.setSelected(getBoolean(name+SELECTED_SUFFIX));
	} catch (MissingResourceException e) {
	}
	
        return result;
    }

    /**
     * Initializes a swing menu item
     * @param item the menu item to initialize
     * @param name the name of the menu item
     * @throws ResourceFormatException if the mnemonic is not a single
     *         character.
     * @throws MissingListenerException if then item action is not found in
     *         the action map.
     */
    protected void initializeJMenuItem(JMenuItem item, String name)
	throws ResourceFormatException,
	       MissingListenerException {
        // Action
	try {
	    Action a = actions.getAction(getString(name+ACTION_SUFFIX));
	    if (a == null) {
		throw new MissingListenerException("", "Action",
                                                   name+ACTION_SUFFIX);
	    }
	    item.setAction(a);
            item.setText(getString(name+TEXT_SUFFIX));
	    if (a instanceof JComponentModifier) {
		((JComponentModifier)a).addJComponent(item);
	    }
	} catch (MissingResourceException e) {
	}

	// Icon
	try {
	    String s = getString(name+ICON_SUFFIX);
	    URL url  = actions.getClass().getResource(s);
	    if (url != null) {
		item.setIcon(new ImageIcon(url));
	    }
	} catch (MissingResourceException e) {
	}

        // Mnemonic
	try {
	    String str = getString(name+MNEMONIC_SUFFIX);
	    if (str.length() == 1) {
		item.setMnemonic(str.charAt(0));
	    } else {
		throw new ResourceFormatException("Malformed mnemonic",
						  bundle.getClass().getName(),
						  name+MNEMONIC_SUFFIX);
	    }
	} catch (MissingResourceException e) {
	}

        // Accelerator
	try {
	    if (!(item instanceof JMenu)) {
		String str = getString(name+ACCELERATOR_SUFFIX);
		KeyStroke ks = KeyStroke.getKeyStroke(str);
		if (ks != null) {
		    item.setAccelerator(ks);
		} else {
		    throw new ResourceFormatException
                        ("Malformed accelerator",
                         bundle.getClass().getName(),
                         name+ACCELERATOR_SUFFIX);
		}
	    }
	} catch (MissingResourceException e) {
	}

        // is the item enabled?
	try {
	    item.setEnabled(getBoolean(name+ENABLED_SUFFIX));
	} catch (MissingResourceException e) {
	}
    }
}
