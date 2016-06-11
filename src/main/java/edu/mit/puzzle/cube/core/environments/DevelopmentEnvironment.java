package edu.mit.puzzle.cube.core.environments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.model.UserStore;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DevelopmentEnvironment implements ServiceEnvironment {

    private final HuntDefinition huntDefinition;
    private final InMemoryConnectionFactory inMemoryConnectionFactory;

    public DevelopmentEnvironment(
            HuntDefinition huntDefinition
    ) {
        this.huntDefinition = huntDefinition;

        List<String> teamIdList = Lists.newArrayList("testerteam");
        teamIdList.addAll(IntStream.rangeClosed(2,70).mapToObj(i -> "testerteam" + i).collect(Collectors.toList()));

        List<User> userList = Lists.newArrayList();
        userList.add(User.builder()
                .setUsername("adminuser")
                .setPassword("adminpassword")
                .setRoles(ImmutableList.of("admin"))
                .build());
        userList.add(User.builder()
                .setUsername("writingteamuser")
                .setPassword("writingteampassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build());
        for (String teamId : teamIdList) {
            userList.add(User.builder()
                    .setUsername(teamId)
                    .setPassword(teamId + "password")
                    .build());
        }

        try {
            this.inMemoryConnectionFactory = new InMemoryConnectionFactory(
                    huntDefinition.getVisibilityStatusSet(),
                    teamIdList,
                    huntDefinition.getPuzzleList(),
                    userList
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        UserStore userStore = new UserStore(this.inMemoryConnectionFactory);
        for (String teamId : teamIdList) {
            List<String> instanceLevelPermissions = ImmutableList.of(
                    "userinfo:read:" + teamId,
                    "teaminfo:read:" + teamId,
                    "submissions:read,create:" + teamId,
                    "visibilities:read:" + teamId);
            userStore.addUserPermissions(teamId, instanceLevelPermissions);
        }
    }

    public ConnectionFactory getConnectionFactory() {
        return inMemoryConnectionFactory;
    }

}
