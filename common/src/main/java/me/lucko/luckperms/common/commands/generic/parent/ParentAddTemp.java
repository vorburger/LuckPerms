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

package me.lucko.luckperms.common.commands.generic.parent;

import me.lucko.luckperms.api.event.events.GroupAddEvent;
import me.lucko.luckperms.common.api.delegate.GroupDelegate;
import me.lucko.luckperms.common.api.delegate.PermissionHolderDelegate;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.generic.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.SubCommand.getGroupTabComplete;

public class ParentAddTemp extends SharedSubCommand {
    public ParentAddTemp() {
        super("addtemp", "Sets another group for the object to inherit permissions from temporarily",
                Permission.USER_PARENT_ADDTEMP, Permission.GROUP_PARENT_ADDTEMP, Predicates.notInRange(2, 4),
                Arg.list(
                        Arg.create("group", true, "the group to inherit from"),
                        Arg.create("duration", true, "the duration of the group membership"),
                        Arg.create("server", false, "the server to add the group on"),
                        Arg.create("world", false, "the world to add the group on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String groupName = ArgumentUtils.handleName(0, args);
        long duration = ArgumentUtils.handleDuration(1, args);
        String server = ArgumentUtils.handleServer(2, args);
        String world = ArgumentUtils.handleWorld(3, args);
        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        if (!plugin.getStorage().loadGroup(groupName).join()) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        try {
            if (group.getName().equalsIgnoreCase(holder.getObjectName())) {
                throw new ObjectAlreadyHasException();
            }

            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    duration = holder.setPermission(new NodeBuilder("group." + group.getName()).setValue(true).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SET_TEMP_INHERIT_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(),
                            DateUtil.formatDateDiff(duration)
                    );
                    plugin.getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderDelegate(holder), new GroupDelegate(group), null, null, duration));
                    break;
                case SERVER:
                    duration = holder.setPermission(new NodeBuilder("group." + group.getName()).setValue(true).setServer(server).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SET_TEMP_INHERIT_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(),
                            server, DateUtil.formatDateDiff(duration)
                    );
                    plugin.getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderDelegate(holder), new GroupDelegate(group), server, null, duration));
                    break;
                case SERVER_AND_WORLD:
                    duration = holder.setPermission(new NodeBuilder("group." + group.getName()).setValue(true).setServer(server).setWorld(world).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SET_TEMP_INHERIT_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(),
                            server, world, DateUtil.formatDateDiff(duration)
                    );
                    plugin.getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderDelegate(holder), new GroupDelegate(group), server, world, duration));
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("parent addtemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_TEMP_INHERITS.send(sender, holder.getFriendlyName(), group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
