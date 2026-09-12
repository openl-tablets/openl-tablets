package org.openl.rules.ui.tree.view;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.ui.tree.OpenMethodsGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

/**
 * Verifies that two trees can be built at the same time without reading each other's module.
 *
 * <p>A tree node builder carries the state of the build it serves — the dictionary naming the groups of
 * overloaded methods — so a view that handed the same builders to every build let one build overwrite what
 * another was in the middle of reading, and the second one then asked for a method its dictionary had never
 * heard of.
 */
class RulesTreeViewTest {

    @Test
    void buildsOfTwoModulesDoNotShareTheirBuilders() {
        for (RulesTreeView view : Profile.TREE_VIEWS) {
            TreeNodeBuilder<Object>[] first = view.getBuilders();
            TreeNodeBuilder<Object>[] second = view.getBuilders();

            assertNotSame(first, second, view.getName());
            for (var index = 0; index < first.length; index++) {
                assertNotSame(first[index], second[index], view.getName() + " builder " + index);
            }
        }
    }

    @Test
    void keepsTheDictionaryTheBuildWasGiven() throws Exception {
        // What the shared builders lost: each build must still see its own dictionary after another one ran.
        List<Callable<Boolean>> builds = new ArrayList<>();
        for (var round = 0; round < 64; round++) {
            builds.add(RulesTreeViewTest::buildsWithItsOwnDictionary);
        }

        try (var threads = Executors.newFixedThreadPool(8)) {
            for (var outcome : threads.invokeAll(builds)) {
                assertTrue(outcome.get(), "a build read a dictionary that was not its own");
            }
        }
    }

    /** One tree build: take the builders, hand them a dictionary, and read it back as the build would. */
    private static boolean buildsWithItsOwnDictionary() {
        var dictionary = new OverloadedMethodsDictionary();
        List<OpenMethodsGroupTreeNodeBuilder> builders = new ArrayList<>();
        for (RulesTreeView view : Profile.TREE_VIEWS) {
            // The views declare their builders over Object, which is not the type the group builders implement.
            for (Object builder : view.getBuilders()) {
                if (builder instanceof OpenMethodsGroupTreeNodeBuilder group) {
                    group.setOpenMethodGroupsDictionary(dictionary);
                    builders.add(group);
                }
            }
        }
        Thread.yield();
        return builders.stream().allMatch(builder -> builder.getOpenMethodGroupsDictionary() == dictionary);
    }
}
