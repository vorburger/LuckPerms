/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bukkit.model;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class SubscriptionManager {

    private final LPPermissible permissible;
    private Set<String> currentSubscriptions = ImmutableSet.of();

    public synchronized void subscribe(Set<String> perms) {
        Set<String> newPerms = ImmutableSet.copyOf(perms);

        Map.Entry<Set<String>, Set<String>> changes = compareSets(newPerms, currentSubscriptions);

        Set<String> toAdd = changes.getKey();
        Set<String> toRemove = changes.getValue();

        permissible.getPlugin().doSync(() -> {
            for (String s : toAdd) {
                permissible.getPlugin().getServer().getPluginManager().subscribeToPermission(s, permissible.getParent());
            }
            for (String s : toRemove) {
                permissible.getPlugin().getServer().getPluginManager().unsubscribeFromPermission(s, permissible.getParent());
            }
        });

        this.currentSubscriptions = newPerms;
    }

    /**
     * Compares two sets
     * @param local the local set
     * @param remote the remote set
     * @return the entries to add to remote, and the entries to remove from remote
     */
    private static Map.Entry<Set<String>, Set<String>> compareSets(Set<String> local, Set<String> remote) {
        // entries in local but not remote need to be added
        // entries in remote but not local need to be removed

        Set<String> toAdd = new HashSet<>(local);
        toAdd.removeAll(remote);

        Set<String> toRemove = new HashSet<>(remote);
        toRemove.removeAll(local);

        return Maps.immutableEntry(toAdd, toRemove);
    }

}
