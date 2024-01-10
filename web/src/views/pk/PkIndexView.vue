<template>

  <MatchGround v-if="$store.state.pk.status === 'matching'"/>

  <div class="game-container">
    <!-- 游戏区域 -->
    <div class="game-area">
      <PlayGround v-if="$store.state.pk.status === 'playing'"/>
    </div>

    <!-- 信息展示区域 -->
    <div class="info-area">
      <RightBoard v-if="$store.state.pk.status === 'playing'"/>
      <ResultBoard v-if="$store.state.pk.loser != 'none'"/>
    </div>
  </div>
</template>

<script>
import ResultBoard from "@/components/ResultBoard.vue"
import {onMounted, onUnmounted} from 'vue'
import {useStore} from 'vuex'
import MatchGround from '../../components/MatchGround.vue'
import PlayGround from '../../components/PlayGround.vue'
import Global from '@/global';
import RightBoard from "@/components/RightBoard.vue"

export default {
  components: {
    PlayGround,
    MatchGround,
    ResultBoard,
    RightBoard,
  },
  setup() {
    const store = useStore();
    const socketUrl = Global.wsApiUrl + `/websocket/pk/${store.state.user.token}/`
    store.commit("updateLoser", "none");
    store.commit("updateIsRecord", false);
    let socket = null;
    onMounted(() => {
      store.commit("updateOpponent", {
        username: "我的对手",
        photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
      })
      socket = new WebSocket(socketUrl);

      socket.onopen = () => {
        console.log("connected!");
        store.commit("updateSocket", socket);
      }

      socket.onmessage = msg => {
        const data = JSON.parse(msg.data);
        if (data.event === "start-matching") {  // 匹配成功
          store.commit("updateOpponent", {
            username: data.opponent_username,
            photo: data.opponent_photo,
          });
          // 等待加载地图
          setTimeout(() => {
            store.commit("updateStatus", "playing");
          }, 1000);
          // console.log(data.game)
          store.commit("updateGame", data.game);
        } else if (data.event === "move") {
          const game = store.state.pk.gameObject;
          const [snake0, snake1] = game.snakes;
          if ('a_direction' in data) {
            snake0.set_direction(data.a_direction);
          }
          if ('b_direction' in data) {
            snake1.set_direction(data.b_direction);
          }

        } else if (data.event === "time") {
          const game = store.state.pk.gameObject;
          if (game !== null) {
            const [snake0, snake1] = game.snakes;
            if("a_time" in data)
              snake0.set_time(data.a_time)
            if("b_time" in data)
              snake1.set_time(data.b_time)
          }
        } else if (data.event === "result") {

          const game = store.state.pk.gameObject;
          const [snake0, snake1] = game.snakes;

          if (data.loser === "all" || data.loser === "A") {
            snake0.status = "die"
          }
          if (data.loser === "all" || data.loser === "B") {
            snake1.status = "die"
          }
          store.commit("updateLoser", data.loser);
        }
      }

      socket.onclose = () => {
        console.log("disconnected!");
      }
    });

    onUnmounted(() => {
      socket.close();
      store.commit("updateStatus", "matching");
    })
  }
}
</script>

<style scoped>


.game-container {
  display: flex;
  height: 100%; /* 根据需要调整 */
}

.game-area {
  flex: 1; /* 游戏区域占据更多空间 */
  /* 根据需要添加其他样式，如padding、margin等 */
}

.info-area {
  flex: 1; /* 信息展示区域占据剩余空间 */
  /* 根据需要添加其他样式，如padding、margin、background等 */
}
</style>