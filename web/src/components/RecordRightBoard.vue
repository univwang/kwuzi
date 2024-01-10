<template>
  <div class="rightboard">
    <ContentField>
      <div class="user-color-red"
           v-if="parseInt($store.state.user.id) === parseInt($store.state.pk.b_id)">
        你是白棋
      </div>
      <div class="user-color-blue"
           v-if="parseInt($store.state.user.id) === parseInt($store.state.pk.a_id)">
        你是黑棋
      </div>
      <div class="win">
        胜利者是 {{win}}
      </div>
    </ContentField>
  </div>

</template>

<script>
import ContentField from "@/components/ContentField.vue"
import {useStore} from "vuex";
import {ref} from "vue";

export default {
  components: {
    ContentField,
  },
  setup() {
    const store = useStore();
    const loser = store.state.record.record_loser;
    let win = ref("123")
    if (loser === "A") {
      win = store.state.record.b_player
    } else {
      win = store.state.record.a_player
    }
    return {
      win
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

div.win {
  text-align: center;
  color: black;
  font-size: 30px;
  font-weight: 500;
  font-style: italic;
}
</style>