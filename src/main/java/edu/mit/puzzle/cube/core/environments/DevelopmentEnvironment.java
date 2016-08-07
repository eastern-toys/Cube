package edu.mit.puzzle.cube.core.environments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.core.model.User;

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
                    .setTeamId(teamId)
                    .build());
        }

        try {
            this.inMemoryConnectionFactory = new InMemoryConnectionFactory(
                    huntDefinition.getVisibilityStatusSet(),
                    teamIdList,
                    huntDefinition.getPuzzles(),
                    userList
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectionFactory getConnectionFactory() {
        return inMemoryConnectionFactory;
    }

}
