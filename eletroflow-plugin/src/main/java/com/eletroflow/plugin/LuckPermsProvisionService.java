package com.eletroflow.plugin;

import com.eletroflow.shared.dto.PendingRewardResponse;
import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

public class LuckPermsProvisionService {

    private final LuckPerms luckPerms;

    public LuckPermsProvisionService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public String grant(PendingRewardResponse reward) throws Exception {
        UUID uniqueId = UUID.fromString(reward.minecraftUuid());
        User user = luckPerms.getUserManager().loadUser(uniqueId).get();
        Node node = Node.builder("group." + reward.luckPermsGroup()).build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
        return reward.minecraftUuid() + ":" + reward.luckPermsGroup();
    }
}
