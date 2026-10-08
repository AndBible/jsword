/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.crosswire.common.util.PropertyMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link Config}, {@link ChoiceFactory} and the reflected
 * {@link Choice} implementations: pins the behaviour of the Java classes before
 * they are converted to Kotlin. The options are built from an in-test JDOM
 * document that introspects a static test bean; expected values were derived by
 * reading the Java, not by observing it.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class ConfigCharacterizationTest {
    /**
     * The target of the reflection: public static getter/setter pairs.
     */
    public static class Bean {
        static boolean flag;
        static int mode;
        static String title;
        static String[] words;
        static int count;
        static String kind;
        static String color;
        static String secret;
        static String strict;
        static File dir;
        static File[] path;
        static String font;
        static Class<?> klass;

        static void reset() {
            flag = false;
            mode = 1;
            title = "initial";
            words = new String[] {"a", "b"};
            count = 5;
            kind = "x";
            color = "g";
            secret = "pw";
            strict = "s";
            dir = new File("d");
            path = new File[] {new File("p")};
            font = "Serif.plain.12";
            klass = Object.class;
        }

        public static boolean isFlag() {
            return flag;
        }

        public static void setFlag(boolean newFlag) {
            flag = newFlag;
        }

        public static int getMode() {
            return mode;
        }

        public static void setMode(int newMode) {
            mode = newMode;
        }

        public static String getTitle() {
            return title;
        }

        public static void setTitle(String newTitle) {
            title = newTitle;
        }

        public static String[] getWords() {
            return words;
        }

        public static void setWords(String[] newWords) {
            words = newWords;
        }

        public static int getCount() {
            return count;
        }

        public static void setCount(int newCount) {
            count = newCount;
        }

        public static String getKind() {
            return kind;
        }

        public static void setKind(String newKind) {
            kind = newKind;
        }

        public static String getColor() {
            return color;
        }

        public static void setColor(String newColor) {
            color = newColor;
        }

        public static String getSecret() {
            return secret;
        }

        public static void setSecret(String newSecret) {
            secret = newSecret;
        }

        public static String getStrict() {
            return strict;
        }

        public static void setStrict(String newStrict) {
            throw new IllegalStateException("no way");
        }

        public static File getDir() {
            return dir;
        }

        public static void setDir(File newDir) {
            dir = newDir;
        }

        public static File[] getPath() {
            return path;
        }

        public static void setPath(File[] newPath) {
            path = newPath;
        }

        public static String getFont() {
            return font;
        }

        public static void setFont(String newFont) {
            font = newFont;
        }

        public static Class<?> getKlass() {
            return klass;
        }

        public static void setKlass(Class<?> newKlass) {
            klass = newKlass;
        }

        /** A getter whose return type does not match the setter parameter. */
        public static long getMismatch() {
            return 0L;
        }

        public static void setMismatch(int ignored) {
        }
    }

    /** A bundle backed by a map: a missing key throws MissingResourceException like a real one. */
    private static final class MapBundle extends ResourceBundle {
        private final Map<String, Object> map = new HashMap<String, Object>();

        void put(String key, String value) {
            map.put(key, value);
        }

        @Override
        protected Object handleGetObject(String key) {
            return map.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(map.keySet());
        }
    }

    /** Records every property change as {@code key:old->new}. */
    private static final class ChangeRecorder implements PropertyChangeListener {
        final List<String> events = new ArrayList<String>();

        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt.getPropertyName() + ":" + evt.getOldValue() + "->" + evt.getNewValue());
        }
    }

    /** Records config events as {@code added:key} / {@code removed:key}. */
    private static final class ConfigRecorder implements ConfigListener {
        final List<String> events = new ArrayList<String>();
        final List<Choice> choices = new ArrayList<Choice>();

        public void choiceAdded(ConfigEvent ev) {
            events.add("added:" + ev.getKey());
            choices.add(ev.getChoice());
        }

        public void choiceRemoved(ConfigEvent ev) {
            events.add("removed:" + ev.getKey());
            choices.add(ev.getChoice());
        }
    }

    private static final String BEAN = ConfigCharacterizationTest.class.getName() + "$Bean";

    private MapBundle bundle;

    @Before
    public void setUp() {
        Bean.reset();

        Map<String, Object> data = ChoiceFactory.getDataMap();
        data.put("test.kinds", new String[] {"x", "y", "z"});
        Map<String, String> colors = new TreeMap<String, String>();
        colors.put("r", "Red");
        colors.put("g", "Green");
        data.put("test.colors", colors);

        bundle = new MapBundle();
        bundle.put("test.name", "Test");
        for (String key : new String[] {"flag", "mode", "title", "words", "count", "kind", "color", "secret", "strict", "dir", "path",
                "font", "klass", "mismatch", "nointro"}) {
            bundle.put("test." + key + ".name", key);
            bundle.put("test." + key + ".help", key + " help");
        }
        bundle.put("test.mode.alternative.0", "Zero");
        bundle.put("test.mode.alternative.1", "One");
        bundle.put("test.mode.alternative.2", "Two");
        bundle.put("test.kind.hidden", "TRUE");
        // An ignored option needs neither help nor name resources
        bundle.put("test.skipped.ignore", "true");
    }

    private static Element option(String key, String type, String property) {
        Element element = new Element("option");
        element.setAttribute("key", key);
        element.setAttribute("type", type);
        element.addContent(new Element("introspect").setAttribute("class", BEAN).setAttribute("property", property));
        return element;
    }

    private static Element withMap(Element option, String mapName) {
        option.addContent(new Element("map").setAttribute("name", mapName));
        return option;
    }

    /** Nine good options, one ignored and one that cannot be built (no introspect element). */
    private Document document() {
        Element root = new Element("config");
        root.addContent(option("test.flag", "boolean", "Flag").setAttribute("restart", "true"));
        Element mode = option("test.mode", "int-options", "Mode");
        for (int i = 0; i < 3; i++) {
            mode.addContent(new Element("alternative").setAttribute("number", Integer.toString(i)));
        }
        root.addContent(mode);
        root.addContent(option("test.title", "string", "Title"));
        root.addContent(option("test.words", "string-array", "Words").setAttribute("separator", ","));
        root.addContent(option("test.count", "number", "Count"));
        root.addContent(withMap(option("test.kind", "string-options", "Kind"), "test.kinds"));
        root.addContent(withMap(option("test.color", "map-options", "Color"), "test.colors"));
        root.addContent(option("test.secret", "password", "Secret").setAttribute("external", "true"));
        root.addContent(option("test.strict", "string", "Strict"));
        root.addContent(option("test.skipped", "string", "Title"));
        root.addContent(new Element("option").setAttribute("key", "test.nointro").setAttribute("type", "string"));
        return new Document(root);
    }

    private Config config() {
        Config config = new Config("test");
        config.add(document(), bundle);
        return config;
    }

    @Test
    public void testBuildFromDocument() {
        Config config = new Config("test");
        Assert.assertEquals("test", config.getTitle());
        Assert.assertEquals(0, config.size());

        ConfigRecorder recorder = new ConfigRecorder();
        config.addConfigListener(recorder);
        config.add(document(), bundle);

        // The ignored option and the one that fails with a StartupException are skipped, not fatal
        Assert.assertEquals(9, config.size());
        List<String> keys = new ArrayList<String>();
        for (Choice choice : config) {
            keys.add(choice.getKey());
        }
        Assert.assertEquals("[test.flag, test.mode, test.title, test.words, test.count, test.kind, test.color, test.secret, test.strict]", keys.toString());
        Assert.assertEquals("[added:test.flag, added:test.mode, added:test.title, added:test.words, added:test.count, added:test.kind, added:test.color, added:test.secret, added:test.strict]",
                recorder.events.toString());
        Assert.assertSame(config.getChoice("test.mode"), recorder.choices.get(1));

        Assert.assertNull(config.getChoice("test.skipped"));
        Assert.assertNull(config.getChoice("test.nointro"));
        Assert.assertNull(config.getChoice("nope"));
        Assert.assertNull(config.getChoice(null));

        // Choice metadata comes from the bundle: path parts "test" and "flag" are joined with dots
        Choice flag = config.getChoice("test.flag");
        Assert.assertTrue(flag instanceof BooleanChoice);
        Assert.assertEquals("boolean", flag.getType());
        Assert.assertEquals("Test.flag", flag.getFullPath());
        Assert.assertEquals("flag help", flag.getHelpText());
        Assert.assertEquals(boolean.class, flag.getConversionClass());
        Assert.assertTrue(flag.requiresRestart());
        Assert.assertTrue(flag.isSaveable());
        Assert.assertFalse(flag.isHidden());
        Assert.assertFalse(flag.isIgnored());
        Assert.assertFalse(config.getChoice("test.title").requiresRestart());
        Assert.assertTrue(config.getChoice("test.kind").isHidden());
        Assert.assertFalse(config.getChoice("test.secret").isSaveable());
        Assert.assertTrue(config.getChoice("test.secret") instanceof StringChoice);

        // The initial local values are the application's current values, as strings
        Assert.assertEquals("false", config.getLocal("test.flag"));
        Assert.assertEquals("1", config.getLocal("test.mode"));
        Assert.assertEquals("initial", config.getLocal("test.title"));
        Assert.assertEquals("a,b", config.getLocal("test.words"));
        Assert.assertEquals("5", config.getLocal("test.count"));
        Assert.assertEquals("x", config.getLocal("test.kind"));
        Assert.assertEquals("g", config.getLocal("test.color"));
        Assert.assertEquals("pw", config.getLocal("test.secret"));
        Assert.assertNull(config.getLocal("nope"));
    }

    @Test
    public void testIgnoredChoiceStopsInitEarly() throws Exception {
        Choice choice = ChoiceFactory.getChoice(option("test.skipped", "string", "Title"), bundle);
        Assert.assertTrue(choice.isIgnored());
        // Ignoring implies hidden, but the rest (help text, path, type) is never read
        Assert.assertTrue(choice.isHidden());
        Assert.assertNull(choice.getHelpText());
        Assert.assertNull(choice.getFullPath());
        Assert.assertNull(choice.getType());
    }

    @Test
    public void testLocalToApplication() {
        Config config = config();
        ChangeRecorder recorder = new ChangeRecorder();
        config.addPropertyChangeListener(recorder);

        config.setLocal("test.flag", "yes");
        config.setLocal("test.mode", "Two");
        config.setLocal("test.title", "new");
        config.setLocal("test.words", "x,y,z");
        config.setLocal("test.count", "42");
        config.setLocal("test.kind", "y");
        config.setLocal("test.color", "Red");
        config.setLocal("test.secret", "pw2");
        Assert.assertEquals("new", config.getLocal("test.title"));
        // Nothing has reached the application yet
        Assert.assertEquals("initial", Bean.title);
        Assert.assertFalse(Bean.flag);

        config.localToApplication();

        Assert.assertTrue(Bean.flag);
        // "Two" is not a number, so IntOptionsChoice looks it up among the alternatives' names
        Assert.assertEquals(2, Bean.mode);
        Assert.assertEquals("new", Bean.title);
        Assert.assertArrayEquals(new String[] {"x", "y", "z"}, Bean.words);
        Assert.assertEquals(42, Bean.count);
        Assert.assertEquals("y", Bean.kind);
        // MappedOptionsChoice maps the displayed value back to its key
        Assert.assertEquals("r", Bean.color);
        // Not saveable (external) does not stop it being applied
        Assert.assertEquals("pw2", Bean.secret);

        // One event per changed choice, in key order, carrying the string values; the
        // normalised "yes" is reported as typed
        Assert.assertEquals("[test.flag:false->yes, test.mode:1->Two, test.title:initial->new, test.words:a,b->x,y,z, test.count:5->42, test.kind:x->y, test.color:g->Red, test.secret:pw->pw2]",
                recorder.events.toString());

        // Nothing differs now? "yes" vs "true" and "Two" vs "2" still do, so those two fire again
        recorder.events.clear();
        config.localToApplication();
        Assert.assertEquals("[test.flag:true->yes, test.mode:2->Two, test.color:r->Red]", recorder.events.toString());
    }

    @Test
    public void testLocalToApplicationBlankKeepsApplicationValue() {
        Config config = config();
        ChangeRecorder recorder = new ChangeRecorder();
        config.addPropertyChangeListener(recorder);

        config.setLocal("test.title", "");
        config.localToApplication();

        // A blank local value is replaced by the application's current value; nothing is set
        Assert.assertEquals("initial", config.getLocal("test.title"));
        Assert.assertEquals("initial", Bean.title);
        Assert.assertTrue(recorder.events.isEmpty());
    }

    @Test
    public void testApplicationToLocal() {
        Config config = config();
        config.setLocal("test.title", "local");

        Bean.title = "direct";
        Bean.count = 7;
        Bean.flag = true;
        config.applicationToLocal();

        Assert.assertEquals("direct", config.getLocal("test.title"));
        Assert.assertEquals("7", config.getLocal("test.count"));
        Assert.assertEquals("true", config.getLocal("test.flag"));
    }

    @Test
    public void testBadNumberAndOptionConversion() {
        Config config = config();

        config.setLocal("test.count", "abc");
        config.setLocal("test.mode", "bogus");
        config.localToApplication();

        // Convert.string2Int answers 0 for junk; IntOptionsChoice answers 0 for an unknown name
        Assert.assertEquals(0, Bean.count);
        Assert.assertEquals(0, Bean.mode);
    }

    @Test
    public void testProperties() {
        Config config = config();
        config.setLocal("test.title", "edited");

        PropertyMap saved = config.getProperties();
        // Everything but the external (not saveable) secret
        Assert.assertEquals(8, saved.size());
        Assert.assertFalse(saved.containsKey("test.secret"));
        Assert.assertEquals("edited", saved.get("test.title"));
        Assert.assertEquals("a,b", saved.get("test.words"));
        Assert.assertEquals("false", saved.get("test.flag"));

        PropertyMap loaded = new PropertyMap();
        loaded.put("test.count", "abc");
        loaded.put("test.title", "fromProps");
        loaded.put("test.secret", "hack");
        loaded.put("unknown.key", "1");
        config.setProperties(loaded);

        Assert.assertEquals("abc", config.getLocal("test.count"));
        Assert.assertEquals("fromProps", config.getLocal("test.title"));
        // Not saveable: the stored value is ignored. Unknown keys are ignored too.
        Assert.assertEquals("pw", config.getLocal("test.secret"));
        Assert.assertNull(config.getLocal("unknown.key"));
        // Untouched keys keep their value
        Assert.assertEquals("a,b", config.getLocal("test.words"));

        // The bad number is only noticed when it is applied, as 0
        config.localToApplication();
        Assert.assertEquals(0, Bean.count);
        Assert.assertEquals("fromProps", Bean.title);
    }

    @Test
    public void testPermanentStorage() throws IOException {
        File file = File.createTempFile("config-characterization", ".properties");
        try {
            Config config = config();
            config.setLocal("test.title", "persisted");
            config.setLocal("test.secret", "hidden");
            config.localToPermanent(file.toURI());

            Bean.reset();
            Config other = config();
            other.permanentToLocal(file.toURI());
            Assert.assertEquals("persisted", other.getLocal("test.title"));
            // The secret was never written
            Assert.assertEquals("pw", other.getLocal("test.secret"));
        } finally {
            Assert.assertTrue(file.delete());
        }
    }

    @Test
    public void testRemoveAndListeners() {
        Config config = config();
        ConfigRecorder recorder = new ConfigRecorder();
        config.addConfigListener(recorder);

        Choice flag = config.getChoice("test.flag");
        config.remove("test.flag");
        Assert.assertEquals(8, config.size());
        Assert.assertNull(config.getChoice("test.flag"));
        Assert.assertEquals("[removed:test.flag]", recorder.events.toString());
        Assert.assertSame(flag, recorder.choices.get(0));
        // The local value is deliberately left behind
        Assert.assertEquals("false", config.getLocal("test.flag"));
        // Removed from iteration, the first is now the mode
        Assert.assertEquals("test.mode", config.iterator().next().getKey());

        // Removing an unknown key still tells the listeners, with no choice
        config.remove("never.there");
        Assert.assertEquals("[removed:test.flag, removed:never.there]", recorder.events.toString());
        Assert.assertNull(recorder.choices.get(1));
        Assert.assertEquals(8, config.size());

        config.removeConfigListener(recorder);
        config.remove("test.mode");
        Assert.assertEquals(2, recorder.events.size());

        // Adding a choice directly stores its string value and notifies
        config.addConfigListener(recorder);
        config.add(flag);
        Assert.assertEquals(8, config.size());
        Assert.assertEquals("added:test.flag", recorder.events.get(2));
        Assert.assertEquals("false", config.getLocal("test.flag"));
    }

    @Test
    public void testPropertyChangeListenerRemoval() {
        Config config = config();
        ChangeRecorder all = new ChangeRecorder();
        ChangeRecorder named = new ChangeRecorder();
        config.addPropertyChangeListener(all);
        config.addPropertyChangeListener("test.title", named);

        config.setLocal("test.title", "t1");
        config.setLocal("test.count", "9");
        config.localToApplication();
        Assert.assertEquals("[test.title:initial->t1, test.count:5->9]", all.events.toString());
        Assert.assertEquals("[test.title:initial->t1]", named.events.toString());

        config.removePropertyChangeListener(all);
        config.removePropertyChangeListener("test.title", named);
        config.setLocal("test.title", "t2");
        config.localToApplication();
        Assert.assertEquals(2, all.events.size());
        Assert.assertEquals(1, named.events.size());

        // Removing before any listener was ever added is harmless
        Config fresh = new Config("fresh");
        fresh.removePropertyChangeListener(all);
        fresh.removePropertyChangeListener("x", named);
    }

    @Test
    public void testPathAndLeaf() {
        Assert.assertEquals("a.b", Config.getPath("a.b.c"));
        Assert.assertEquals("c", Config.getLeaf("a.b.c"));
        Assert.assertEquals("a", Config.getPath("a.b"));
        Assert.assertEquals("b", Config.getLeaf("a.b"));
        Assert.assertEquals("", Config.getPath(".x"));
        Assert.assertEquals("x", Config.getLeaf(".x"));
        Assert.assertEquals("a", Config.getPath("a."));
        Assert.assertEquals("", Config.getLeaf("a."));

        try {
            Config.getPath("abc");
            Assert.fail("no dot");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("key=abc does not contain a dot.", e.getMessage());
        }
        try {
            Config.getLeaf("abc");
            Assert.fail("no dot");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("key=abc does not contain a dot.", e.getMessage());
        }
    }

    @Test
    public void testSetStringFailureIsWrapped() throws Exception {
        Config config = config();
        Choice strict = config.getChoice("test.strict");
        Assert.assertEquals("s", strict.getString());
        try {
            strict.setString("anything");
            Assert.fail("setter throws");
        } catch (ConfigException e) {
            Assert.assertTrue(e.getCause() instanceof InvocationTargetException);
            Assert.assertTrue(e.getCause().getCause() instanceof IllegalStateException);
        }

        // Through the config the failure is reported, not thrown, and the other choices still apply
        config.setLocal("test.strict", "other");
        config.setLocal("test.title", "after");
        config.localToApplication();
        Assert.assertEquals("after", Bean.title);
        Assert.assertEquals("s", Bean.strict);
    }

    @Test
    public void testStartupFailures() throws Exception {
        // No introspect element
        try {
            ChoiceFactory.getChoice(new Element("option").setAttribute("key", "test.title").setAttribute("type", "string"), bundle);
            Assert.fail("missing introspect");
        } catch (StartupException e) {
            Assert.assertEquals("Missing introspect element in config.xml", e.getMessage());
        }

        // Unknown class
        Element noClass = new Element("option").setAttribute("key", "test.title").setAttribute("type", "string");
        noClass.addContent(new Element("introspect").setAttribute("class", "no.such.Clazz").setAttribute("property", "Title"));
        try {
            ChoiceFactory.getChoice(noClass, bundle);
            Assert.fail("unknown class");
        } catch (StartupException e) {
            Assert.assertTrue(e.getCause() instanceof ClassNotFoundException);
        }

        // Unknown setter
        try {
            ChoiceFactory.getChoice(option("test.title", "string", "Nothing"), bundle);
            Assert.fail("unknown property");
        } catch (StartupException e) {
            Assert.assertTrue(e.getCause() instanceof NoSuchMethodException);
        }

        // Getter whose type differs from the choice's conversion class
        try {
            ChoiceFactory.getChoice(option("test.mismatch", "number", "Mismatch"), bundle);
            Assert.fail("mismatched getter");
        } catch (StartupException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Mismatch of return types"));
        }

        // Missing map element
        try {
            ChoiceFactory.getChoice(option("test.kind", "string-options", "Kind"), bundle);
            Assert.fail("missing map");
        } catch (StartupException e) {
            Assert.assertEquals("Missing map element in config.xml", e.getMessage());
        }

        // A key with no help text in the bundle is a MissingResourceException, not a StartupException
        try {
            ChoiceFactory.getChoice(option("test.unknown", "string", "Title"), bundle);
            Assert.fail("missing help");
        } catch (java.util.MissingResourceException e) {
            Assert.assertEquals("test.unknown.help", e.getKey());
        }
    }

    @Test
    public void testChoiceFactoryTypes() throws Exception {
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.dir", "directory", "Dir"), bundle) instanceof DirectoryChoice);
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.dir", "file", "Dir"), bundle) instanceof FileChoice);
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.path", "path", "Path"), bundle) instanceof PathChoice);
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.font", "font", "Font"), bundle) instanceof FontChoice);
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.klass", "class", "Klass"), bundle) instanceof ClassChoice);
        Assert.assertTrue(ChoiceFactory.getChoice(option("test.count", "number", "Count"), bundle) instanceof NumberChoice);

        // "custom" names the choice class directly
        Element custom = option("test.title", "custom", "Title").setAttribute("class", FontChoice.class.getName());
        Assert.assertTrue(ChoiceFactory.getChoice(custom, bundle) instanceof FontChoice);

        // An unregistered type has no class to construct
        try {
            ChoiceFactory.getChoice(option("test.title", "no-such-type", "Title"), bundle);
            Assert.fail("unknown type");
        } catch (NullPointerException e) {
            // expected
        }
    }

    private Choice choice(String key, String type, String property) throws Exception {
        return ChoiceFactory.getChoice(option(key, type, property), bundle);
    }

    @Test
    public void testSimpleConversions() throws Exception {
        AbstractReflectedChoice bool = (AbstractReflectedChoice) choice("test.flag", "boolean", "Flag");
        Assert.assertEquals("true", bool.convertToString(Boolean.TRUE));
        Assert.assertEquals("false", bool.convertToString(Boolean.FALSE));
        Assert.assertEquals(Boolean.TRUE, bool.convertToObject("on"));
        Assert.assertEquals(Boolean.TRUE, bool.convertToObject("1"));
        Assert.assertEquals(Boolean.FALSE, bool.convertToObject("off"));
        Assert.assertEquals(Boolean.FALSE, bool.convertToObject(null));
        try {
            bool.convertToString(null);
            Assert.fail("unboxing null");
        } catch (NullPointerException e) {
            // expected
        }

        AbstractReflectedChoice number = (AbstractReflectedChoice) choice("test.count", "number", "Count");
        Assert.assertEquals("-3", number.convertToString(Integer.valueOf(-3)));
        Assert.assertEquals(Integer.valueOf(7), number.convertToObject("7"));
        Assert.assertEquals(Integer.valueOf(0), number.convertToObject("x"));
        Assert.assertEquals(Integer.valueOf(0), number.convertToObject(null));

        AbstractReflectedChoice string = (AbstractReflectedChoice) choice("test.title", "string", "Title");
        Assert.assertEquals("s", string.convertToString("s"));
        Assert.assertNull(string.convertToString(null));
        Assert.assertEquals("s", string.convertToObject("s"));
        Assert.assertNull(string.convertToObject(null));

        AbstractReflectedChoice font = (AbstractReflectedChoice) choice("test.font", "font", "Font");
        Assert.assertEquals("Serif.plain.12", font.convertToString("Serif.plain.12"));
        Assert.assertEquals("Serif.bold.9", font.convertToObject("Serif.bold.9"));
        Assert.assertEquals(String.class, font.getConversionClass());
        Assert.assertEquals("Serif.plain.12", font.getString());
    }

    @Test
    public void testFileAndPathConversions() throws Exception {
        AbstractReflectedChoice file = (AbstractReflectedChoice) choice("test.dir", "file", "Dir");
        Assert.assertEquals("", file.convertToString(null));
        Assert.assertEquals(new File("rel").getAbsolutePath(), file.convertToString(new File("rel")));
        Assert.assertEquals(new File("some/where"), file.convertToObject("some/where"));
        Assert.assertEquals(File.class, file.getConversionClass());
        Assert.assertEquals(new File("d").getAbsolutePath(), file.getString());

        AbstractReflectedChoice path = (AbstractReflectedChoice) choice("test.path", "path", "Path");
        File a = new File("a");
        File b = new File("b");
        Assert.assertEquals(a.getAbsolutePath() + File.pathSeparator + b.getAbsolutePath(), path.convertToString(new File[] {a, b}));
        Assert.assertEquals("", path.convertToString(new File[0]));
        File[] parsed = (File[]) path.convertToObject("a" + File.pathSeparator + "b");
        Assert.assertArrayEquals(new File[] {a, b}, parsed);
        Assert.assertEquals(0, ((File[]) path.convertToObject("")).length);
        Assert.assertEquals(File[].class, path.getConversionClass());
        Assert.assertEquals(new File("p").getAbsolutePath(), path.getString());

        // Setting through reflection
        path.setString("x" + File.pathSeparator + "y");
        Assert.assertArrayEquals(new File[] {new File("x"), new File("y")}, Bean.path);
    }

    @Test
    public void testClassConversions() throws Exception {
        AbstractReflectedChoice klass = (AbstractReflectedChoice) choice("test.klass", "class", "Klass");
        Assert.assertNull(klass.convertToString(null));
        Assert.assertEquals("java.lang.String", klass.convertToString(String.class));
        Assert.assertEquals(String.class, klass.convertToObject("java.lang.String"));
        // An unknown class is logged and answers null, which setString does not apply
        Assert.assertNull(klass.convertToObject("no.such.Clazz"));
        Assert.assertEquals("java.lang.Object", klass.getString());
        klass.setString("no.such.Clazz");
        Assert.assertEquals(Object.class, Bean.klass);
        klass.setString("java.lang.Integer");
        Assert.assertEquals(Integer.class, Bean.klass);
    }

    @Test
    public void testOptionChoices() throws Exception {
        Element modeElement = option("test.mode", "int-options", "Mode");
        modeElement.addContent(new Element("alternative").setAttribute("number", "2"));
        modeElement.addContent(new Element("alternative").setAttribute("number", "0"));
        IntOptionsChoice mode = (IntOptionsChoice) ChoiceFactory.getChoice(modeElement, bundle);
        Map<Integer, String> options = mode.getOptions();
        // Sorted by number whatever the document order
        Assert.assertEquals("{0=Zero, 2=Two}", options.toString());
        // A copy: changing it does not change the choice
        options.clear();
        Assert.assertEquals(2, mode.getOptions().size());
        Assert.assertEquals("1", mode.convertToString(Integer.valueOf(1)));
        Assert.assertEquals(Integer.valueOf(12), mode.convertToObject("12"));
        Assert.assertEquals(Integer.valueOf(2), mode.convertToObject("Two"));
        // "One" is not among the alternatives of this element
        Assert.assertEquals(Integer.valueOf(0), mode.convertToObject("One"));
        Assert.assertEquals(int.class, mode.getConversionClass());

        StringOptionsChoice kind = (StringOptionsChoice) ChoiceFactory.getChoice(withMap(option("test.kind", "string-options", "Kind"), "test.kinds"), bundle);
        String[] kinds = kind.getOptions();
        Assert.assertArrayEquals(new String[] {"x", "y", "z"}, kinds);
        kinds[0] = "changed";
        Assert.assertEquals("x", kind.getOptions()[0]);
        Assert.assertEquals("anything", kind.convertToObject("anything"));

        MappedOptionsChoice color = (MappedOptionsChoice) ChoiceFactory.getChoice(withMap(option("test.color", "map-options", "Color"), "test.colors"), bundle);
        Assert.assertEquals("{g=Green, r=Red}", color.getOptions().toString());
        Assert.assertEquals("r", color.convertToObject("Red"));
        Assert.assertEquals("g", color.convertToObject("g"));
        Assert.assertEquals("", color.convertToObject("Blue"));
        Assert.assertEquals("", color.convertToString(null));
        Assert.assertEquals("x", color.convertToString("x"));
        Assert.assertEquals("g", color.getString());

        // A map name with nothing registered gives an empty option map
        MappedOptionsChoice none = (MappedOptionsChoice) ChoiceFactory.getChoice(withMap(option("test.color", "map-options", "Color"), "test.nothing"), bundle);
        Assert.assertTrue(none.getOptions().isEmpty());
        Assert.assertEquals("", none.convertToObject("g"));
    }

    @Test
    public void testStringArrayChoiceSeparator() throws Exception {
        StringArrayChoice withSeparator = (StringArrayChoice) ChoiceFactory.getChoice(option("test.words", "string-array", "Words").setAttribute("separator", "|"), bundle);
        Assert.assertEquals("a|b", withSeparator.convertToString(new String[] {"a", "b"}));
        Assert.assertArrayEquals(new String[] {"a", "b"}, (String[]) withSeparator.convertToObject("a|b"));
        Assert.assertEquals(String[].class, withSeparator.getConversionClass());

        // The field default of " " is overwritten by init() with the missing attribute, i.e. null:
        // join uses an empty separator and split breaks on any whitespace
        StringArrayChoice noSeparator = (StringArrayChoice) ChoiceFactory.getChoice(option("test.words", "string-array", "Words"), bundle);
        Assert.assertEquals("ab", noSeparator.convertToString(new String[] {"a", "b"}));
        Assert.assertArrayEquals(new String[] {"a", "b"}, (String[]) noSeparator.convertToObject("a  b"));
        Assert.assertEquals("a,b", withSeparator.getString().replace('|', ','));

        // An empty Config iterates nothing
        Iterator<Choice> empty = new Config("empty").iterator();
        Assert.assertFalse(empty.hasNext());
    }
}
