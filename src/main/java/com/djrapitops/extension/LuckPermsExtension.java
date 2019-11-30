/*
    Copyright(c) 2019 Risto Lahtela (Rsl1122)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.track.Track;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LuckPerms DataExtension.
 * <p>
 * Adapted from PluginData by Vankka
 *
 * @author Rsl1122
 */
@PluginInfo(name = "LuckPerms", iconName = "exclamation-triangle", iconFamily = Family.SOLID, color = Color.LIGHT_GREEN)
@TabInfo(tab = "Permission Groups", iconName = "users-cog", elementOrder = {})
@TabInfo(tab = "Metadata", iconName = "info-circle", elementOrder = {})
public class LuckPermsExtension implements DataExtension {

    public LuckPermsExtension() {
    }

    public LuckPerms getAPI() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            throw new NotReadyException();
        }
    }

    public User getUser(UUID playerUUID) {
        return Optional.ofNullable(getAPI().getUserManager().getUser(playerUUID)).orElseThrow(NotReadyException::new);
    }

    @GroupProvider(
            text = "Primary Group",
            iconName = "users-cog",
            groupColor = Color.LIGHT_GREEN
    )
    @Tab("Permission Groups")
    public String[] primaryGroup(UUID playerUUID) {
        return new String[]{getUser(playerUUID).getPrimaryGroup()};
    }

    @GroupProvider(
            text = "Permission Group",
            iconName = "users-cog",
            groupColor = Color.LIGHT_GREEN
    )
    @Tab("Permission Groups")
    public String[] permissionGroups(UUID playerUUID) {
        return getGroupNames(playerUUID).toArray(String[]::new);
    }

    private Stream<String> getGroupNames(UUID playerUUID) {
        return getUser(playerUUID).getNodes().stream()
                .filter(node -> node.getKey().startsWith("group."))
                .map(node -> node.getKey().substring(6));
    }

    @TableProvider(
            tableColor = Color.ORANGE
    )
    @Tab("Permission Groups")
    public Table tracks(UUID playerUUID) {
        Set<Track> tracks = getAPI().getTrackManager().getLoadedTracks();
        Set<String> groups = getGroupNames(playerUUID).collect(Collectors.toSet());

        Table.Factory table = Table.builder()
                .columnOne("Track", Icon.called("ellipsis-h").build())
                .columnTwo("Group", Icon.called("users-cog").build());

        for (Track track : tracks) {
            track.getGroups().stream()
                    .filter(groups::contains)
                    // reduce is used to get the last group in the track that the user has
                    .reduce((first, second) -> second)
                    .ifPresent(currentGroup -> table.addRow(track.getName(), currentGroup));
        }

        return table.build();
    }

    @StringProvider(
            text = "Prefix",
            description = "Current user prefix",
            priority = 100,
            iconName = "file-signature",
            iconColor = Color.GREEN
    )
    @Tab("Metadata")
    public String prefix(UUID playerUUID) {
        return Optional.ofNullable(getMetaData(playerUUID).getPrefix()).orElse("None");
    }

    @StringProvider(
            text = "Suffix",
            description = "Current user suffix",
            priority = 99,
            iconName = "file-signature",
            iconColor = Color.BLUE
    )
    @Tab("Metadata")
    public String suffix(UUID playerUUID) {
        return Optional.ofNullable(getMetaData(playerUUID).getSuffix()).orElse("None");
    }

    @TableProvider(tableColor = Color.BLUE)
    @Tab("Metadata")
    public Table metaTable(UUID playerUUID) {
        Map<String, List<String>> meta = getMetaData(playerUUID).getMeta();
        if (meta.isEmpty()) throw new NotReadyException();

        Table.Factory table = Table.builder()
                .columnOne("Meta", Icon.called("info-circle").build())
                .columnTwo("Value", Icon.called("file-alt").build());

        meta.forEach((key, value) -> table.addRow(key, value.toString()));

        return table.build();
    }

    private CachedMetaData getMetaData(UUID playerUUID) {
        return getUser(playerUUID).getCachedData()
                .getMetaData(getAPI().getContextManager().getStaticQueryOptions());
    }

    @StringProvider(
            text = "Weight",
            description = "Weight of the permission group",
            priority = 5,
            iconName = "weight-hanging",
            iconColor = Color.LIGHT_GREEN
    )
    public String weight(com.djrapitops.plan.extension.Group permissionGroup) {
        Group group = getGroup(permissionGroup.getGroupName());

        OptionalInt weight = group.getWeight();
        return weight.isPresent() ? Integer.toString(weight.getAsInt()) : "None";
    }

    private Group getGroup(String groupName) {
        return Optional.ofNullable(getAPI().getGroupManager().getGroup(groupName)).orElseThrow(NotReadyException::new);
    }

    @TableProvider(
            tableColor = Color.LIGHT_BLUE
    )
    @Tab("Permission Groups")
    public Table permissions(com.djrapitops.plan.extension.Group permissionGroup) {
        Group group = getGroup(permissionGroup.getGroupName());

        Table.Factory table = Table.builder()
                .columnOne("Permission", Icon.called("object-group").build());

        group.getNodes().forEach(permission -> table.addRow(permission.getKey()));

        return table.build();
    }

    @TableProvider(
            tableColor = Color.ORANGE
    )
    @Tab("Permission Groups")
    public Table tracks() {
        Set<Track> tracks = getAPI().getTrackManager().getLoadedTracks();

        Table.Factory table = Table.builder()
                .columnOne("Track", Icon.called("ellipsis-h").build())
                .columnTwo("Size", Icon.called("list").build());

        tracks.forEach(track -> table.addRow(track.getName(), track.getGroups().size()));

        return table.build();
    }
}