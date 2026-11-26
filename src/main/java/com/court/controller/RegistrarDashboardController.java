package com.court.controller;

import com.court.dao.CourtroomDao;
import com.court.dao.UserDao;
import com.court.dao.impl.CourtroomDaoImpl;
import com.court.dao.impl.UserDaoImpl;
import com.court.model.Courtroom;
import com.court.model.Judge;
import com.court.model.Role;
import com.court.model.User;
import com.court.util.BackgroundTask;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registrar's dashboard. Home screen shows user counts and which judge sits in which room.
 */
public class RegistrarDashboardController extends DashboardController {

    @FXML private Label judgeCountLabel;
    @FXML private Label lawyerCountLabel;
    @FXML private Label courtroomCountLabel;
    @FXML private ListView<String> allocationList;

    private final UserDao userDao = new UserDaoImpl();
    private final CourtroomDao courtroomDao = new CourtroomDaoImpl();

    /** Data needed by the home screen, loaded together in the background. */
    private record HomeData(List<User> users, List<Courtroom> rooms) { }

    @Override
    protected void onDashboardReady(User user) {
        BackgroundTask.run(
                () -> new HomeData(userDao.findAll(), courtroomDao.findAll()),
                this::showHomeData);
    }

    private void showHomeData(HomeData data) {
        // Streams + Collectors.groupingBy: count users per role in one pass
        Map<Role, Long> countByRole = data.users().stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

        judgeCountLabel.setText(String.valueOf(countByRole.getOrDefault(Role.JUDGE, 0L)));
        lawyerCountLabel.setText(String.valueOf(countByRole.getOrDefault(Role.LAWYER, 0L)));
        courtroomCountLabel.setText(String.valueOf(data.rooms().size()));

        // HashMap: courtroom id -> judge sitting there
        Map<Integer, Judge> judgeByRoom = new HashMap<>();
        for (User u : data.users()) {
            if (u instanceof Judge judge && judge.hasCourtroom()) {
                judgeByRoom.put(judge.getCourtroomId(), judge);
            }
        }

        allocationList.getItems().setAll(data.rooms().stream()
                .map(room -> {
                    Judge judge = judgeByRoom.get(room.getId());
                    String who = (judge == null) ? "No judge assigned" : judge.getName();
                    return room + "   →   " + who;
                })
                .toList());
    }
}
