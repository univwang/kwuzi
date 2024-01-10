<template>
  <div class="rightboard">
    <ContentField>
      <div class="user-color-red"
           v-if="$store.state.pk.status === 'playing' && parseInt($store.state.user.id) === parseInt($store.state.pk.b_id)">
        你是白棋
      </div>
      <div class="user-color-blue"
           v-if="$store.state.pk.status === 'playing' && parseInt($store.state.user.id) === parseInt($store.state.pk.a_id)">
        你是黑棋
      </div>

      <div class="time"
           v-if="$store.state.pk.status === 'playing' && parseInt($store.state.user.id) === parseInt($store.state.pk.b_id)">
        倒计时：{{ b_time }}
      </div>
      <div class="time"
           v-if="$store.state.pk.status === 'playing' && parseInt($store.state.user.id) === parseInt($store.state.pk.a_id)">
        倒计时：{{ a_time }}
      </div>
    </ContentField>
  </div>

</template>

<script>
import ContentField from "@/components/ContentField.vue"
import {useStore} from "vuex";
import {computed} from "vue";

export default {
  components: {
    ContentField,
  },
  setup() {
    const store = useStore();
    const a_time = computed(() => {
      const game = store.state.pk.gameObject;
      return game !== null ? game.snakes[0].time : -1;
    });

    const b_time = computed(() => {
      const game = store.state.pk.gameObject;
      return game !== null ? game.snakes[1].time : -1;
    });
    return {
      a_time,
      b_time,
    }
  }

}

</script>
<style scoped>
div.user-color-red {
  text-align: center;
  color: red;
  font-size: 30px;
  font-weight: 500;
  font-style: italic;
}

div.user-color-blue {
  text-align: center;
  color: blue;
  font-size: 30px;
  font-weight: 500;
  font-style: italic;
}

div.rightboard {
  width: 35vw;
  height: 100vh;
}



div.time {
  text-align: center;
  color: black;
  font-size: 30px;
  font-weight: 500;
  font-style: italic;
}

</style>