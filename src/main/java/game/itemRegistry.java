package game;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

//Every kind of item in the game, as a way to make a new one. Filled from the item dictionaries (gemstones, items):
//every public method there that takes nothing and returns an item is registered, so a new item only has to be
//added to its dictionary. Gemstones come first, then the other items, each sorted by name. Conductors (copper) go
//last of all, so in the give item menu they sit too far from the boltstone to keep charging it (see itemInteractions)
public class itemRegistry {

    private List<Supplier<item>> makers = new ArrayList<Supplier<item>>();
    private java.util.Map<String, Supplier<item>> byName = new java.util.HashMap<String, Supplier<item>>();

    public itemRegistry (Object... dictionaries) {
        for (Object dictionary : dictionaries) {
            List<Supplier<item>> found = new ArrayList<Supplier<item>>();
            List<String> names = new ArrayList<String>();
            Method[] methods = dictionary.getClass().getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) continue;
                if (method.getParameterCount() != 0 || !item.class.isAssignableFrom(method.getReturnType())) continue;
                Supplier<item> maker = () -> {
                    try {
                        return (item) method.invoke(dictionary);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Kunne ikke lage " + method.getName(), e);
                    }
                };
                found.add(maker);
                names.add(maker.get().getName());
            }
            List<Integer> order = new ArrayList<Integer>();
            for (int i = 0; i < found.size(); i++) order.add(i);
            order.sort(Comparator.comparing(names::get));
            for (int i : order) {
                makers.add(found.get(i));
                byName.putIfAbsent(names.get(i), found.get(i));
            }
        }
        List<Supplier<item>> conductors = new ArrayList<Supplier<item>>();
        for (Supplier<item> maker : makers) {
            if (maker.get().isConductor()) conductors.add(maker);
        }
        makers.removeAll(conductors);
        makers.addAll(conductors);
    }

    public List<Supplier<item>> getMakers () {return new ArrayList<Supplier<item>>(makers);}

    //A new item with this name, or null when no item is called that (anymore). Savefiles store items by name
    public item make (String name) {
        Supplier<item> maker = byName.get(name);
        return maker == null ? null : maker.get();
    }
}
