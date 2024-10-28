package game.dispatcher.core;

import com.alibaba.fastjson2.JSONObject;
import game.dispatcher.event.MatchEvent;
import game.dispatcher.event.PlayEvent;
import game.dispatcher.event.RoomEvent;
import jsa.event.EventContainer;
import jsa.event.EventListener;
import net.datafaker.providers.base.Money;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;

public class CreateRoom implements EventListener<RoomEvent> {
    @Override
    public void onEvent(RoomEvent roomEvent) {
        // PointId
        long pointId = Long.parseLong(ServerConfigContext.getServerConfigValue("PointId"));
        // Get users in the same room
        List<UserModel> users = roomEvent.getUsers();
        // Channel ID
        Long channelId = roomEvent.getChannelId();
        ChannelVo channel = ServerConfigContext.getChannelById(channelId);
        // Fee ratio
        double ratio = channel.getRatio();
        // User bets
        int gameType = roomEvent.getGameType();
        // Fee
        int fee = (int) (gameType * ratio);
        // Total Fee
        int totalFee = 0;
        // User pool remaining bets
        int chip = gameType - fee;

        // Integrate user bets
        List<UserPointViewModel> sendPointViewModels = new ArrayList<>();
        for (UserModel user : users) {
            UserPointViewModel userPointViewModel = new UserPointViewModel(user.getUserId(), pointId, gameType);
            sendPointViewModels.add(userPointViewModel);
        }

        // Deduct user's bet
        boolean status = HttpServerUtils.spendPoints(sendPointViewModels);
        if (!status) {
            for (UserModel user : users) {
                // If the deduction fails, query the user's points
                Money money = HttpServerUtils.getPoint(user.getUserId(), pointId);
                // If the points are insufficient, cancel. If the points are sufficient, re-enter the queue.
                if (money == null || money.getAmount() < gameType) {
                    WebSocketSession session = user.getSession();
                    try {
                        // Not enough money to push
                        if (session != null && session.isOpen() && user.isOnline()) {
                            ResultUtils.sendClintMessage(Cmd.S_C_Points_Not_Enough, "", session);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        DebugUtil.logError("CreateRoom error!!! S_C_Points_Not_Enough cmd : " + Cmd.S_C_Points_Not_Enough + "  ," + e.getMessage());
                    }
                    user.setState(UserState.Free.index);
                } else {
                    // Join Match
                    MatchEvent event = new MatchEvent(MatchEvent.Join_Matching, null);
                    event.setBindingId(Matching.random.nextInt(16));
                    event.setChannelId(channelId);
                    event.setUserId(user.getUserId());
                    event.setRank(RankType.getRankByType(gameType));
                    EventContainer.dispatchEvent(event);
                }
            }
            return;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("users", users);
        String Body = jsonObject.toJSONString();
        for (UserModel user : users) {
            // Query Contribution
            UserExtraInfoVo userExtraInfoVo = DbManager.selectByIdAndUserId(user.getUserId(), user.getUserId(), MapperContext.userExtraInfoMapper, UserExtraInfoVo.class);
            long battlePoints = userExtraInfoVo.getBattlePoints();
            long limitBattlePoints = Long.parseLong(ServerConfigContext.getServerConfigValue("LimitBattlePoints"));
            if (battlePoints > limitBattlePoints) {
                // Total handling fee increase
                totalFee += fee;
            } else {
                battlePoints += fee;
                userExtraInfoVo.setBattlePoints(battlePoints);
                DbManager.updateVo(userExtraInfoVo.getId(), userExtraInfoVo, MapperContext.userExtraInfoMapper);
            }

            // User Session
            WebSocketSession session = user.getSession();
            // Send Message
            try {
                if (session != null && session.isOpen() && user.isOnline()) {
                    ResultUtils.sendClintMessage(Cmd.S_C_CreateRoom, Body, session);
                }
            } catch (Exception e) {
                e.printStackTrace();
                DebugUtil.logError("CreateRoom error!!! S_C_CreateRoom cmd : " + Cmd.S_C_CreateRoom + "  ," + e.getMessage());
            }
        }

        // Create a Room
        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setChannelId(channelId);
        roomInfo.setRatio(ratio);
        roomInfo.setType(chip);
        roomInfo.setFee(fee);
        roomInfo.setUsers(users);
        roomInfo.setTotalFee(totalFee);
        Long roomId = roomInfo.getRoomId();
        // Cache Room
        RoomContext.ROOM_MAP.put(roomId, roomInfo);
        for (UserModel user : users) {
            // Update User Information
            user.setRoomId(roomId);
            user.setState(UserState.Battle.index);
        }

        // Increase channel fees
        HttpServerUtils.addChannelPoint(channelId, pointId, totalFee);

        // Shuffle
        PlayEvent playEvent = new PlayEvent(PlayEvent.Shuffle, null);
        playEvent.setBindingId((int) (roomId % 0x7FFFFFFF));
        playEvent.setChannelId(channelId);
        playEvent.setRoomId(roomId);
        playEvent.setNewBie(roomEvent.isNewBie());
        EventContainer.dispatchEvent(playEvent);
    }
}
