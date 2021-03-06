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

package me.lucko.luckperms.common.commands.generic.meta;

import me.lucko.luckperms.api.MetaUtils;
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
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

public class MetaAddTempSuffix extends SharedSubCommand {
    public MetaAddTempSuffix() {
        super("addtempsuffix", "Adds a suffix temporarily", Permission.USER_META_ADDTEMP_SUFFIX,
                Permission.GROUP_META_ADDTEMP_SUFFIX, Predicates.notInRange(3, 5),
                Arg.list(
                        Arg.create("priority", true, "the priority to add the suffix at"),
                        Arg.create("suffix", true, "the suffix string"),
                        Arg.create("duration", true, "the duration until the suffix expires"),
                        Arg.create("server", false, "the server to add the suffix on"),
                        Arg.create("world", false, "the world to add the suffix on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        int priority = ArgumentUtils.handlePriority(0, args);
        String suffix = ArgumentUtils.handleString(1, args);
        long duration = ArgumentUtils.handleDuration(2, args);
        String server = ArgumentUtils.handleServer(3, args);
        String world = ArgumentUtils.handleWorld(4, args);
        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        final String node = "suffix." + priority + "." + MetaUtils.escapeCharacters(suffix);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(true).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.ADD_TEMP_SUFFIX_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority,
                            DateUtil.formatDateDiff(duration)
                    );
                    break;
                case SERVER:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(true).setServer(server).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.ADD_TEMP_SUFFIX_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority,
                            server, DateUtil.formatDateDiff(duration)
                    );
                    break;
                case SERVER_AND_WORLD:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(true).setServer(server).setWorld(world).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.ADD_TEMP_SUFFIX_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority,
                            server, world, DateUtil.formatDateDiff(duration)
                    );
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("meta addtempsuffix " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_SUFFIX.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }
}
